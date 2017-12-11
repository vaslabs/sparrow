package org.vaslabs.urlshortener.server

import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{PathMatcher, PathMatcher1, Route}
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport

trait HttpRouter extends FailFastCirceSupport{ this: ShortenedUrlApi =>
  import io.circe.generic.auto._

  def main: Route = {
    get {
      path(ShortenedPathMatchers.urlIds) { urlId =>
        onComplete(this.fetchUrl(urlId)) {
          _.map(url => redirect(Uri(url), StatusCodes.TemporaryRedirect))
              .getOrElse(complete(HttpResponse(StatusCodes.NotFound)))
        }
      }
    } ~ post {
      path("entry") {
        entity(as[ShortenUrlRQ]) {
          rq => onComplete(this.shortenUrl(rq.url)) {
              _.map(shortenedUrl =>
                complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, shortenedUrl)))
                .getOrElse(complete(HttpResponse(StatusCodes.InternalServerError)))
          }
        }
      }
    }
  }

}

object ShortenedPathMatchers {
  val urlIds: PathMatcher1[String] =
    PathMatcher("""[a-z0-9]{2,8}""".r)
}
