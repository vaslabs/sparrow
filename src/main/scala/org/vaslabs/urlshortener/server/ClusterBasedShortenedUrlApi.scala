package org.vaslabs.urlshortener.server

import akka.actor.ActorRef
import akka.http.scaladsl.model.{StatusCode, StatusCodes}
import akka.pattern._
import akka.util.Timeout
import org.vaslabs.urlshortener.ShortenedUrlHolder.{FullUrl, VisitorDetails}
import org.vaslabs.urlshortener.StatsGatherer.Protocol.VisitStats
import org.vaslabs.urlshortener.UrlShortener.ShortUrl
import org.vaslabs.urlshortener.server.model.{IpStats, Stats}
import org.vaslabs.urlshortener.{PermissionsLayer, ShortenedUrlHolder}

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

  override def stats(apiKey: String): Future[Either[StatusCode, Stats]] = {
    (permissionsLayer ? PermissionsLayer.FetchStats(apiKey)).map {
      _ match {
        case VisitStats(visits) =>
          Right(Stats(visits.mapValues(_.map(ipVisit => IpStats(ipVisit.ip.toString, ipVisit.timesVisited)))))
        case PermissionsLayer.AuthorizationFailure => Left(StatusCodes.Unauthorized)
        case other =>
          Left(StatusCodes.InternalServerError)
      }
    }
  }
}
