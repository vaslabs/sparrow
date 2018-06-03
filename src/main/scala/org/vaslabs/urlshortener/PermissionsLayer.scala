package org.vaslabs.urlshortener

import akka.actor.{Actor, ActorRef, Props}
import org.vaslabs.urlshortener.permissions.PermissionMapping
import eu.timepit.refined._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.MatchesRegex
import org.vaslabs.urlshortener.permissions.Permissions.User
import org.vaslabs.urlshortener.server.model

class PermissionsLayer private(urlShortener: Props, cluster: ActorRef) extends Actor{


  import org.vaslabs.urlshortener.PermissionsLayer._
  import org.vaslabs.urlshortener.permissions.Permissions.{SpecialGuest, SuperUser, Unauthorised, WriteUser}

  private val urlShortenerActorRef = context.actorOf(urlShortener, "urlShortener")

  private val statsGatherer = context.actorOf(StatsGatherer.props(), "statsGatherer")

  override def receive = {
    case s: ShortenCommand =>
      val user = refineV[MatchesRegex[W.`"[a-f0-9]{16}"`.T]](s.apiKey).map(
        apiKey => PermissionMapping.apply(apiKey)
      ).left.map(_ => Unauthorised).merge

      user match {
        case SuperUser =>
          urlShortenerActorRef forward s.asUrlShortenerCommand(SuperUser)
        case WriteUser =>
          s.customKey.fold(
            urlShortenerActorRef forward s.asUrlShortenerCommand(WriteUser)
          )(_ => sender() ! AuthorizationFailure)
        case SpecialGuest =>
          urlShortenerActorRef forward  s.asUrlShortenerCommand(SpecialGuest)
        case _ =>
          sender() ! AuthorizationFailure
      }
    case fs @ FetchStats(apiKey) =>
      val user = refineV[MatchesRegex[W.`"[a-f0-9]{16}"`.T]](apiKey).map(apiKey =>
        PermissionMapping(apiKey)
      ).left.map(_ => Unauthorised).merge
      user match {
        case SuperUser =>
          statsGatherer forward StatsGatherer.Protocol.GetStats
        case _ =>
          sender() ! AuthorizationFailure
      }
  }
}

object PermissionsLayer {

  import UrlShortener.{ShortenCommand => UrlShortenerCommand}
  import org.vaslabs.urlshortener.permissions.Permissions.CanCreateNew
  case class ShortenCommand(url: String, customKey: Option[String] = None, apiKey: String)

  object ShortenCommand {
    def apply(url: String, apiKey: String): ShortenCommand =
      new ShortenCommand(url, apiKey = apiKey)
  }

  case object AuthorizationFailure

  def props(urlShortenerProps: Props, cluster: ActorRef): Props = Props(new PermissionsLayer(urlShortenerProps, cluster))

  final implicit class UrlShortenerAdapter(val shortenedCommand: ShortenCommand) extends AnyVal {
    def asUrlShortenerCommand(user: CanCreateNew) =
      UrlShortenerCommand(shortenedCommand.url, shortenedCommand.customKey, user)
  }

  case class FetchStats(apiKey: String)

  import eu.timepit.refined.types.numeric._

}