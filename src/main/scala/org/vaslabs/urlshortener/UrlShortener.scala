package org.vaslabs.urlshortener

import akka.actor.{Actor, Props}

class UrlShortener extends Actor{
  import UrlShortener.ShortenCommand
  override def receive = {
    case sc: ShortenCommand =>
      context.actorOf(Props[UrlShortenerDelegate]).forward(sc)
  }
}

private class UrlShortenerDelegate extends Actor {
  import UrlShortener.{ShortenCommand, ShortUrl}
  import java.security.MessageDigest
  override def receive = {
    case ShortenCommand(url) =>
      val shortId = sha(url)
      sender() ! ShortUrl(shortId.substring(0, 4), shortId)
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
  def props() = Props[UrlShortener]

  case class ShortenCommand(url: String)
  def shorten(url: String): ShortenCommand = ShortenCommand(url)

  case class ShortUrl(shortVersion: String, sha: String)

}
