package org.vaslabs.urlshortener

import akka.actor.Actor

class UrlFetcher(persistentStoreFetch: PersistentStore) extends Actor{

  import UrlFetcher.Fetch

  override def receive = {
    case Fetch(shortId) =>
      sender() ! persistentStoreFetch.fetch(shortId)
      context.stop(self)
  }

}

object UrlFetcher {

  case class Fetch(shortId: String)

  def fetch(shortId: String): Fetch = Fetch(shortId)
}
