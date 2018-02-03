package org.vaslabs.urlshortener.server

import akka.actor.ActorRef
import akka.pattern._
import akka.util.Timeout
import org.vaslabs.urlshortener.ShortenedUrlHolder.FullUrl
import org.vaslabs.urlshortener.{PermissionsLayer, ShortenedUrlHolder, UrlShortener}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class ClusterBasedShortenedUrlApi(clusterRegion: ActorRef, permissionsLayer: ActorRef)
       (implicit val requestTimeout: Timeout = Timeout(2 seconds),
        val executionContext: ExecutionContext)
    extends ShortenedUrlApi{

  override def fetchUrl(urlId: String) =
    (clusterRegion ? ShortenedUrlHolder.Get(urlId)).mapTo[FullUrl].map(_.url)

  override def shortenUrl(rq: ShortenUrlRQ, apiKey: String) =
    (permissionsLayer ? PermissionsLayer.ShortenCommand(rq.url, rq.customShortKey, apiKey))
      .mapTo[UrlShortener.ShortUrl].map(_.shortVersion)

}
