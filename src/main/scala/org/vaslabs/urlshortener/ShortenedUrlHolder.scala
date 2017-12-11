package org.vaslabs.urlshortener

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props, Stash}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings, ShardRegion}
import cats.syntax.either._
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.gu.scanamo._
import com.gu.scanamo.error.DynamoReadError
import com.gu.scanamo.syntax._
import org.vaslabs.urlshortener.ShortenedUrlHolder.{DynamoNotFound, FullUrl}

class ShortenedUrlHolder(
        dynamoDBClient: AmazonDynamoDB,
        dynamoDBTable: Table[ShortenedUrl]) extends Actor with ActorLogging with Stash
{

  import ShortenedUrlHolder.{Get, NotFound, StoreError, StoreUrl, StoredAck, UrlIdAlreadyReserved}

  private[this] def postUrlHold(fullUrl: FullUrl): Receive = {
    case Get(urlId) =>
      sender() ! fullUrl
    case StoreUrl(shortenedUrl) =>
      if (shortenedUrl.url != fullUrl.url)
        sender() ! UrlIdAlreadyReserved(shortenedUrl.shortVersion)
      else
        sender() ! StoredAck
  }

  def persist(shortenedUrl: ShortenedUrl) = {
    val result = Either.catchNonFatal(
      Scanamo.exec(dynamoDBClient)(dynamoDBTable.put(shortenedUrl))
    ).map(r => self ! shortenedUrl).left.map(t => StoreError(t.getMessage))

  }

  def waitForPersistentResult(senderRef: ActorRef, shortenedUrl: ShortenedUrl): Receive = {
    case su: ShortenedUrl =>
      context.become(postUrlHold(FullUrl(su.url)))
      senderRef ! StoredAck
    case Get(urlId) =>
      sender() ! FullUrl(shortenedUrl.url)
    case err: StoreError =>
    case StoreUrl(shortenedUrl) => sender() ! UrlIdAlreadyReserved(shortenedUrl.shortVersion)
  }

  private def receivePostEntityStart(): Receive = {
    case StoreUrl(shortenedUrl) =>
      persist(shortenedUrl)
      val senderRef = sender()
      context.become(waitForPersistentResult(senderRef, shortenedUrl))
    case Get(urlId) =>
      sender() ! NotFound
  }

  override def receive: Receive = {
    case s: ShortenedUrl =>
      context.become(postUrlHold(FullUrl(s.url)))
      unstashAll()
    case error: DynamoReadError =>
      context.become(receivePostEntityStart())
      unstashAll()
    case DynamoNotFound(urlId) =>
      context.become(receivePostEntityStart())
      unstashAll()
    case _ => stash()
  }

  override def preStart(): Unit = {
    val entityId = self.path.name
    val result = Scanamo.exec(dynamoDBClient)(dynamoDBTable.get('shortVersion -> entityId)).map(
      either => either.map(self ! _).left.map(self ! _)
    )
    if (result.isEmpty)
      self ! DynamoNotFound(entityId)


    super.preStart()
  }

}

object ShortenedUrlHolder {

  case class StoreUrl(shortenedUrl: ShortenedUrl)
  case class Get(urlId: String)
  case class FullUrl(url: String)

  def storeUrl(shortenedUrl: ShortenedUrl): StoreUrl = StoreUrl(shortenedUrl)

  private val extractEntityId: ShardRegion.ExtractEntityId = {
    case msg @ Get(id)               => (id.toString, msg)
    case msg @ StoreUrl(shortenedUrl) => (shortenedUrl.shortVersion, msg)
  }

  private val numberOfShards = 36*36

  private val extractShardId: ShardRegion.ExtractShardId = {
    case Get(id)               => extractShardIdFromShortUrlId(id)
    case ShardRegion.StartEntity(id) =>
      extractShardIdFromShortUrlId(id)
    case StoreUrl(shortenedUrl) => extractShardIdFromShortUrlId(shortenedUrl.shortVersion)
  }

  private[this] def extractShardIdFromShortUrlId(id: String) = id.substring(0, 2)

  private def props(dynamoDBClient: AmazonDynamoDB,
                    tableName: String): Props =
      Props(new ShortenedUrlHolder(dynamoDBClient, Table[ShortenedUrl](tableName)))

  def counterRegion(system:ActorSystem, dynamoDBClient: AmazonDynamoDB, tableName: String): ActorRef =
    ClusterSharding(system).start(
      typeName = "ShortenedUrlHolder",
      entityProps = ShortenedUrlHolder.props(dynamoDBClient, tableName),
      settings = ClusterShardingSettings(system),
      extractEntityId = extractEntityId,
      extractShardId = extractShardId)


  case class UrlIdAlreadyReserved(urlId: String)
  case class NotFound(urlId: String)

  case object StoredAck

  case class StoreError(errorMessage: String)

  private case class DynamoNotFound(urlId: String)

}
