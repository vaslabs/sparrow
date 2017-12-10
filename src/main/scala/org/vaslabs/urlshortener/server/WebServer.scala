package org.vaslabs.urlshortener.server

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.util.Timeout
import org.vaslabs.urlshortener.UrlShortener

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

class WebServer(cluster: ActorRef)
               (implicit actorSystem: ActorSystem,
                actorMaterializer: ActorMaterializer,
                requestTimeout: Timeout = Timeout(2 seconds),
                executionContext: ExecutionContext)
  extends ClusterBasedShortenedUrlApi(
    cluster,
    actorSystem.actorOf(UrlShortener.props(cluster))) with HttpRouter{

    def start(): Unit = {
        Http().bindAndHandle(this.main, "0.0.0.0", 8080)
    }

    def shutDown(): Future[Unit] = {
        Http().shutdownAllConnectionPools() andThen {
            case _ => actorSystem.terminate()
        }
    }

}
