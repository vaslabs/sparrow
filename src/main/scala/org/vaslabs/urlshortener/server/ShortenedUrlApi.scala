package org.vaslabs.urlshortener.server

import java.time.ZonedDateTime

import akka.http.scaladsl.model.StatusCode

import scala.concurrent.Future

trait ShortenedUrlApi {
  def fetchUrl(urlId: String): Future[String]
  def shortenUrl(shortenUrlRQ: ShortenUrlRQ, apiKey: String): Future[Either[StatusCode, String]]
  def stats(urlId: String, apiKey: String): Future[Either[StatusCode, model.Stats]]
}

object model {

  case class Stat(ip: String, visits: Int, lastVisit: ZonedDateTime)

  case class Stats(stats: List[Stat])

}