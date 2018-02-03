package org.vaslabs.urlshortener

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import org.vaslabs.urlshortener.ShortenedUrlHolder.{StoredAck, UrlIdAlreadyReserved}
import org.vaslabs.urlshortener.permissions.Permissions.{CanCreateNew, User}
import org.vaslabs.urlshortener.server.ShortenUrlRQ

class UrlShortener(clusterRegion: ActorRef) extends Actor with ActorLogging{
  import UrlShortener.ShortenCommand
  override def receive = {
    case sc: ShortenCommand =>
      val senderRef = sender()
      context.actorOf(Props(new UrlShortenerDelegate(senderRef, clusterRegion))).forward(sc)
  }
}

private class UrlShortenerDelegate(replyTo: ActorRef, validateWith: ActorRef) extends Actor with ActorLogging
{
  import UrlShortener.{ShortenCommand, ShortUrl}
  import java.security.MessageDigest
  override def receive = {
    case ShortenCommand(url, Some(custom), user) =>
      validateWith ! ShortenedUrlHolder.storeCustomUrl(url, custom)
      context.become(waitingForValidation(ShortenedUrl(url, custom), custom, user))
    case ShortenCommand(url, None, user) =>
      val hash = sha(url)
      val shortenedUrl = ShortenedUrl(url, hash.substring(0, user.urlIdMinSize.value))
      validateWith ! ShortenedUrlHolder.storeUrl(shortenedUrl)
      context.become(waitingForValidation(shortenedUrl, hash, user))

  }

  private[this] def waitingForValidation(shortenedUrl: ShortenedUrl, hash: String, user: CanCreateNew): Receive = {
    case UrlIdAlreadyReserved(urlId) =>
      val rehash = sha(hash)
      val newShortenedUrl = shortenedUrl.copy(shortVersion = rehash.substring(0, user.urlIdMinSize.value))
      validateWith ! ShortenedUrlHolder.storeUrl(newShortenedUrl)
      context.become(waitingForValidation(newShortenedUrl, rehash, user))
    case StoredAck =>
      replyTo ! ShortUrl(shortenedUrl.shortVersion, hash)
      context.stop(self)
  }

  private[this] def sha(url: String): String =
    MessageDigest.getInstance("SHA-256").digest(url.getBytes("UTF-8")).map(
      b => {
        val s = Integer.toHexString(0xff & b)
        if (s.size == 1)
          s"0$s"
        else
          s
      }
    ).mkString("")
}

object UrlShortener {

  import org.vaslabs.urlshortener.permissions.Permissions.CanCreateNew

  def props(clusterRegion: ActorRef) = Props(new UrlShortener(clusterRegion))

  case class ShortenCommand(url: String, customKey: Option[String], user: CanCreateNew)

  def shorten(shortenUrlRQ: ShortenUrlRQ, user: CanCreateNew): ShortenCommand =
    ShortenCommand(shortenUrlRQ.url, shortenUrlRQ.customShortKey, user)

  def shorten(url: String, user: CanCreateNew): ShortenCommand =
    ShortenCommand(url, None, user)

  case class ShortUrl(shortVersion: String, sha: String)

}
