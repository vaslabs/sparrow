package org.vaslabs.urlshortener

import java.time.ZonedDateTime

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props, Stash}
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings, ShardRegion}
import cats.data.Ior
import cats.syntax.either._
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.gu.scanamo._
import com.gu.scanamo.error.DynamoReadError
import com.gu.scanamo.syntax._
import eu.timepit.refined.api.Refined
import org.vaslabs.urlshortener.StatsGatherer.Protocol.Visit


class ShortenedUrlHolder(
        dynamoDBClient: AmazonDynamoDB,
        dynamoDBTable: Table[ShortenedUrl]) extends Actor with ActorLogging with Stash
{

  import ShortenedUrlHolder._
  val mediator = DistributedPubSub(context.system).mediator

  def publishVisitEvent(urlId: String, maybeVisitorDetails: Option[VisitorDetails]): Unit = {
    maybeVisitorDetails.flatMap {
      _ match {
        case VisitorDetails(Some(ipv4), Some(ipv6)) =>
          Some(Ior.Both(ipv4, ipv6))
        case VisitorDetails(Some(ipv4), None) => Some(Ior.Left(ipv4))
        case VisitorDetails(None, Some(ipv6)) => Some(Ior.Right(ipv6))
        case other => None
      }

    }.map(Visit(_, urlId, ZonedDateTime.now()))
     .foreach {
        visit => mediator ! Publish("visits", visit)
    }
  }

  private[this] def postUrlHold(fullUrl: FullUrl): Receive = {
    case Get(urlId, maybeVisitorDetails) =>
      sender() ! fullUrl
      publishVisitEvent(urlId, maybeVisitorDetails)
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
    case Get(urlId, maybeVisitorDetails) =>
      sender() ! FullUrl(shortenedUrl.url)
      publishVisitEvent(urlId, maybeVisitorDetails)
    case err: StoreError =>
    case StoreUrl(shortenedUrl) => sender() ! UrlIdAlreadyReserved(shortenedUrl.shortVersion)
    case StoreCustomUrl(url, custom) => sender() ! UrlIdAlreadyReserved(custom)
  }

  private def receivePostEntityStart(): Receive = {
    case StoreUrl(shortenedUrl) =>
      persist(shortenedUrl)
      val senderRef = sender()
      context.become(waitForPersistentResult(senderRef, shortenedUrl))
    case StoreCustomUrl(url, custom) =>
      val shortenedUrl = ShortenedUrl(url, custom)
      val senderRef = sender()
      persist(shortenedUrl)
      context.become(waitForPersistentResult(senderRef, shortenedUrl))
    case Get(urlId, _) =>
      sender() ! NotFound
      context.stop(self)
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

  import eu.timepit.refined.string._
  case class StoreCustomUrl(url: String, custom: String)

  def storeCustomUrl(url: String, custom: String): StoreCustomUrl = StoreCustomUrl(url, custom)

  type Ipv4 = String Refined IPv4
  type Ipv6 = String Refined IPv6

  case class VisitorDetails(ipv4: Option[Ipv4], ipv6: Option[Ipv6])

  object VisitorDetails {
    import eu.timepit.refined.auto._
    import eu.timepit.refined._
    def apply(ip: String): VisitorDetails = {
      val ipv4 = refineV[IPv4](ip).toOption
      val ipv6 = refineV[IPv6](ip).toOption
      new VisitorDetails(ipv4, ipv6)
    }
  }

  case class StoreUrl(shortenedUrl: ShortenedUrl)
  case class Get(urlId: String, visitor: Option[VisitorDetails] = None)
  case class FullUrl(url: String)

  def storeUrl(shortenedUrl: ShortenedUrl): StoreUrl = StoreUrl(shortenedUrl)

  private val extractEntityId: ShardRegion.ExtractEntityId = {
    case msg @ Get(id, _)               => (id.toString, msg)
    case msg @ StoreUrl(shortenedUrl) => (shortenedUrl.shortVersion, msg)
    case msg @ StoreCustomUrl(url, custom) => (url, msg)
  }

  private val numberOfShards = 36*36

  private val extractShardId: ShardRegion.ExtractShardId = {
    case Get(id, _)               => extractShardIdFromShortUrlId(id)
    case ShardRegion.StartEntity(id) =>
      extractShardIdFromShortUrlId(id)
    case StoreUrl(shortenedUrl) => extractShardIdFromShortUrlId(shortenedUrl.shortVersion)
    case StoreCustomUrl(url, custom) => extractShardIdFromShortUrlId(custom)
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
