package org.vaslabs.urlshortener

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings, ShardRegion}
import org.vaslabs.urlshortener.ShortenedUrlHolder.FullUrl

class ShortenedUrlHolder extends Actor{

  import ShortenedUrlHolder.{StoreUrl, Get, UrlIdAlreadyReserved, NotFound, StoredAck}

  private[this] def postUrlHold(fullUrl: FullUrl): Receive = {
    case Get(urlId) =>
      sender() ! fullUrl
    case StoreUrl(shortenedUrl) =>
      if (shortenedUrl.url != fullUrl.url)
        sender() ! UrlIdAlreadyReserved(shortenedUrl.shortVersion)
      else
        sender() ! StoredAck
  }

  override def receive: Receive = {
    case StoreUrl(shortenedUrl) =>
      context.become(postUrlHold(FullUrl(shortenedUrl.url)))
      sender() ! StoredAck
    case Get(urlId) =>
      sender() ! NotFound
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

  private def props: Props = Props[ShortenedUrlHolder]

  def counterRegion(system:ActorSystem): ActorRef = ClusterSharding(system).start(
    typeName = "ShortenedUrlHolder",
    entityProps = ShortenedUrlHolder.props,
    settings = ClusterShardingSettings(system),
    extractEntityId = extractEntityId,
    extractShardId = extractShardId)

  case class UrlIdAlreadyReserved(urlId: String)
  case class NotFound(urlId: String)

  case object StoredAck

}
