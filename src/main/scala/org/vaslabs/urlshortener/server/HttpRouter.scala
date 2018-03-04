package org.vaslabs.urlshortener.server

import akka.http.scaladsl.marshalling.{PredefinedToEntityMarshallers, ToEntityMarshaller, ToResponseMarshallable, ToResponseMarshaller}
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{ModeledCustomHeader, ModeledCustomHeaderCompanion}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{PathMatcher, PathMatcher1, Route}
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.{Decoder, Encoder}

import scala.util.Try

trait HttpRouter extends FailFastCirceSupport {
  this: ShortenedUrlApi =>


  import encoders._
  import decoders._

  def extractFromCustomHeader = headerValuePF {
    case t@ApiTokenHeader(token) => t.value()
  }

  def handleResponse[A](response: Either[StatusCode, A])
                       (implicit toResponseMarshallable: ToResponseMarshaller[A]): Route = {
    response.map(right => complete(right)
    ).left.map(statusCode =>
      complete(HttpResponse(statusCode))
    ).merge
  }

  def main: Route = {
    get {
      path("stats" / ShortenedPathMatchers.urlIds) { urlId =>
        extractFromCustomHeader {
          headerValue => {
            onSuccess(this.stats(urlId, headerValue)) {
              res => handleResponse(res)
            }
          }
        }
      }
    } ~
      get {
        path(ShortenedPathMatchers.urlIds) { urlId =>
          extractClientIP { clientIp =>
            println(s"Visit from ${clientIp}")
            onComplete(this.fetchUrl(urlId, clientIp.toIP.map(_.ip.getHostName))) {
              _.map(url => redirect(Uri(url), StatusCodes.TemporaryRedirect))
                .getOrElse(complete(HttpResponse(StatusCodes.NotFound)))
            }
          }
        }
      } ~ post {
      path("entry") {
        entity(as[ShortenUrlRQ]) { rq =>
          extractFromCustomHeader { headerValue =>
            onSuccess(this.shortenUrl(rq, headerValue)) {
              import PredefinedToEntityMarshallers.StringMarshaller
              result => handleResponse(result)
            }
          }
        }
      }
    }

  }
}

object ShortenedPathMatchers {
  val urlIds: PathMatcher1[String] =
    PathMatcher("""[a-z0-9]{2,16}""".r)
}

final class ApiTokenHeader(token: String) extends ModeledCustomHeader[ApiTokenHeader] {
  override def renderInRequests = false

  override def renderInResponses = false

  override val companion = ApiTokenHeader

  override def value: String = token
}

object ApiTokenHeader extends ModeledCustomHeaderCompanion[ApiTokenHeader] {
  override val name = "X_SPARROW_AUTH"

  override def parse(value: String) = Try(new ApiTokenHeader(value))
}



object encoders {
  import io.circe.generic.auto._

  import io.circe.generic.semiauto._
  import io.circe.java8.time._

  import io.circe.refined._

  implicit val statEncoder: Encoder[model.Stat] = deriveEncoder[model.Stat]

  implicit val statsEncoder: Encoder[model.Stats] = deriveEncoder[model.Stats]

}

object decoders {
  import io.circe.generic.auto._

  import io.circe.generic.semiauto._
  import io.circe.refined._
  import io.circe.java8.time._


  implicit val rqDecoder: Decoder[ShortenUrlRQ] = deriveDecoder[ShortenUrlRQ]
  implicit val statDecoder: Decoder[model.Stat] = deriveDecoder[model.Stat]
  implicit val statsDecoder: Decoder[model.Stats] = deriveDecoder[model.Stats]
}
