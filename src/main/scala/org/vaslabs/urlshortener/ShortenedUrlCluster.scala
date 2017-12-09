package org.vaslabs.urlshortener

import akka.actor.ActorSystem

object ShortenedUrlCluster {
  def region(implicit actorSystem: ActorSystem) =
    ShortenedUrlHolder.counterRegion(actorSystem)
}

