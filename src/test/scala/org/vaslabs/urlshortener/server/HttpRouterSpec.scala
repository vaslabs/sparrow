package org.vaslabs.urlshortener.server

import akka.dispatch.Futures
import akka.http.scaladsl.testkit.ScalatestRouteTest
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import org.scalatest.{Matchers, WordSpec}
import io.circe.generic.auto._

class HttpRouterSpec extends WordSpec with ScalatestRouteTest with Matchers with FailFastCirceSupport
{

  "Http requests" should {
    val httpRouter = new MockShortenedApi with HttpRouter
    "give a shortened url back" in {
      Post("/entry", ShortenUrlRQ("http://foo.com")) ~> httpRouter.main ~> check {
        responseAs[String] shouldBe "http://l/bar"
      }
    }

  }
}

class MockShortenedApi() extends ShortenedUrlApi {
  override def fetchUrl(urlId: String) = Futures.successful("http://foo.com")

  override def shortenUrl(url: String) = Futures.successful("http://l/bar")
}
