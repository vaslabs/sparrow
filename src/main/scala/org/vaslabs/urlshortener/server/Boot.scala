package org.vaslabs.urlshortener.server

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.vaslabs.urlshortener.ShortenedUrlCluster

object Boot extends App{

  implicit val actorSystem = ActorSystem("ShortenedUrlSystem")
  implicit val actorMaterializer = ActorMaterializer()

  val cluster = ShortenedUrlCluster.region

  import actorSystem.dispatcher
  val server = new WebServer(cluster)

  server.start()

  sys.addShutdownHook {
    println("Shutting down")
    server.shutDown().foreach(_ => println("server shut down completed"))
  }
}
