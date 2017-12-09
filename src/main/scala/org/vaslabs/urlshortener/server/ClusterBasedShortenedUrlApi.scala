package org.vaslabs.urlshortener.server

import akka.actor.ActorRef
import akka.util.Timeout

import scala.concurrent.duration._
import akka.pattern._
import org.vaslabs.urlshortener.{ShortenedUrl, ShortenedUrlHolder, UrlShortener}
import org.vaslabs.urlshortener.ShortenedUrlHolder.FullUrl

import scala.concurrent.ExecutionContext

class ClusterBasedShortenedUrlApi(clusterRegion: ActorRef, urlShortener: ActorRef)
       (implicit val requestTimeout: Timeout = Timeout(2 seconds),
        val executionContext: ExecutionContext)
  extends ShortenedUrlApi{

  override def fetchUrl(urlId: String) =
    (clusterRegion ? ShortenedUrlHolder.Get(urlId)).mapTo[FullUrl].map(_.url)

  override def shortenUrl(url: String) =
    (urlShortener ? UrlShortener.shorten(url)).mapTo[ShortenedUrl].map(_.shortVersion)
}
