package org.vaslabs.urlshortener.server

import scala.concurrent.Future

trait ShortenedUrlApi {
  def fetchUrl(urlId: String): Future[String]
  def shortenUrl(shortenUrlRQ: ShortenUrlRQ, apiKey: String): Future[String]
}
