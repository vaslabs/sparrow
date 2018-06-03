package org.vaslabs.urlshortener.server

import akka.http.scaladsl.model.StatusCode

import scala.concurrent.Future

trait ShortenedUrlApi {
  def fetchUrl(urlId: String, clientIp: Option[String]): Future[String]
  def shortenUrl(shortenUrlRQ: ShortenUrlRQ, apiKey: String): Future[Either[StatusCode, String]]
  def stats(apiKey: String): Future[Either[StatusCode, model.Stats]]
}

object model {

  case class IpStats(ip: String, visits: Long)

  case class Stats(stats: Map[String, Set[IpStats]])

}