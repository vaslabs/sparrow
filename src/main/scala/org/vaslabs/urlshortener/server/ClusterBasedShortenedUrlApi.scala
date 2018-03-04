package org.vaslabs.urlshortener.server

import akka.actor.ActorRef
import akka.http.scaladsl.model.{StatusCode, StatusCodes}
import akka.pattern._
import akka.util.Timeout
import org.vaslabs.urlshortener.ShortenedUrlHolder.{FullUrl, VisitorDetails}
import org.vaslabs.urlshortener.UrlShortener.ShortUrl
import org.vaslabs.urlshortener.permissions.Permissions.Unauthorised
import org.vaslabs.urlshortener.{PermissionsLayer, ShortenedUrlHolder, UrlShortener}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

class ClusterBasedShortenedUrlApi(clusterRegion: ActorRef, permissionsLayer: ActorRef)
       (implicit val requestTimeout: Timeout = Timeout(2 seconds),
        val executionContext: ExecutionContext)
    extends ShortenedUrlApi{

  override def fetchUrl(urlId: String, clientIp: Option[String]) =
    (clusterRegion ? ShortenedUrlHolder.Get(urlId, clientIp.map(VisitorDetails(_)))).mapTo[FullUrl].map(_.url)

  override def shortenUrl(rq: ShortenUrlRQ, apiKey: String): Future[Either[StatusCode, String]] =
    (permissionsLayer ? PermissionsLayer.ShortenCommand(rq.url, rq.customShortKey, apiKey))
      .map{
        _ match {
          case ShortUrl(shortVersion, _) => Right(shortVersion)
          case PermissionsLayer.AuthorizationFailure => Left(StatusCodes.Unauthorized)
          case other =>
            Left(StatusCodes.InternalServerError)
        }
      }

  override def stats(urlId: String, apiKey: String): Future[Either[StatusCode, model.Stats]] = {
    (permissionsLayer ? PermissionsLayer.FetchStats(urlId, apiKey)).map {
      _ match {
        case ShortenedUrlHolder.Stats(visits) =>
          Right(model.Stats(visits.map(v => model.Stat(v.ip, v.numberOfVisits, v.lastVisit))))
        case PermissionsLayer.AuthorizationFailure => Left(StatusCodes.Unauthorized)
        case other =>
          Left(StatusCodes.InternalServerError)
      }
    }
  }
}
