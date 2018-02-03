package org.vaslabs.urlshortener.server

import akka.actor.ActorRef
import akka.dispatch.Futures
import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.testkit.TestActor.AutoPilot
import akka.testkit.{TestActors, TestProbe}
import akka.util.ByteString
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.generic.auto._
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}
import org.vaslabs.urlshortener.ShortenedUrlHolder.FullUrl
import org.vaslabs.urlshortener.{PermissionsLayer, ShortenedUrlHolder, UrlShortener}

class HttpRouterSpec extends WordSpec with ScalatestRouteTest with Matchers with FailFastCirceSupport with BeforeAndAfterAll {

  override def afterAll() = {
    system.terminate().foreach(_ => println("terminated"))
  }

  "Http requests" should {
    val clusterTestProbe: TestProbe = TestProbe()
    val urlShortener: TestProbe = TestProbe()
    val permissionLayer: ActorRef = system.actorOf(PermissionsLayer.props(TestActors.forwardActorProps(urlShortener.ref), clusterTestProbe.ref))

    urlShortener.setAutoPilot(new AutoPilot {
      override def run(sender: ActorRef, msg: Any): AutoPilot = {
        msg match {
          case UrlShortener.ShortenCommand(_, customKey, _) =>
            customKey.fold(sender ! UrlShortener.ShortUrl("b4r", "b4rb4rb4r"))(key => sender ! UrlShortener.ShortUrl(key, "foobar"))
        }
        this
      }
    })

    clusterTestProbe.setAutoPilot(new AutoPilot {
      override def run(sender: ActorRef, msg: Any): AutoPilot = {
        msg match {
          case ShortenedUrlHolder.Get(urlId) => sender ! FullUrl("http://foo.com")
        }
        this
      }
    })

    val httpRouter = new ClusterBasedShortenedUrlApi(clusterTestProbe.ref, permissionLayer) with HttpRouter
    "give a shortened url back" in {
      Post("/entry", ShortenUrlRQ("http://foo.com", None))
        .withHeaders(ApiTokenHeader.parse("0000000000000000").get) ~> httpRouter.main ~> check {
        response shouldBe HttpResponse(StatusCodes.OK, List(), HttpEntity.Strict(ContentTypes.`text/plain(UTF-8)`, ByteString("b4r")), HttpProtocol("HTTP/1.1"))
      }
    }

    "redirect when short id matches a full url" in {
      Get("/b4r") ~> httpRouter.main ~> check {
        response.status shouldBe StatusCodes.TemporaryRedirect
      }
    }

    "give custom urls" in {
      Post("/entry", ShortenUrlRQ("http://foo.com", Some("foo")))
        .withHeaders(ApiTokenHeader.parse("0000000000000000").get) ~> httpRouter.main ~> check {
          response shouldBe
            HttpResponse(StatusCodes.OK, List(),
              HttpEntity.Strict(ContentTypes.`text/plain(UTF-8)`, ByteString("foo")), HttpProtocol("HTTP/1.1")
            )
      }
    }

    "give unauthorised" in {
      Post("/entry", ShortenUrlRQ("http://foo.com", Some("foo")))
        .withHeaders(ApiTokenHeader.parse("0000000000000003").get) ~> httpRouter.main ~> check {
          response.status shouldBe StatusCodes.Unauthorized
      }
    }
  }
}