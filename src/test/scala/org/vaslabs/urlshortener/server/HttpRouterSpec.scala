package org.vaslabs.urlshortener.server

import akka.dispatch.Futures
import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.util.ByteString
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}
import io.circe.generic.auto._

class HttpRouterSpec extends WordSpec with ScalatestRouteTest with Matchers with FailFastCirceSupport with BeforeAndAfterAll
{

  override def afterAll() = {
    import scala.concurrent.ExecutionContext.Implicits
    system.terminate().foreach(_ => println("terminated"))
  }


  import akka.http.scaladsl.marshalling.Marshaller._

  "Http requests" should {
    val httpRouter = new MockShortenedApi with HttpRouter
    "give a shortened url back" in {
      Post("/entry", ShortenUrlRQ("http://foo.com"))
        .withHeaders(ApiTokenHeader.parse("0000000000000000").get) ~> httpRouter.main ~> check {
        response shouldBe HttpResponse(StatusCodes.OK, List(), HttpEntity.Strict(ContentTypes.`text/plain(UTF-8)`, ByteString("http://l/b4r")),HttpProtocol("HTTP/1.1"))
      }
    }

    "redirect when short id matches a full url" in {
      Get("/b4r") ~> httpRouter.main ~> check {
        response.status shouldBe StatusCodes.TemporaryRedirect
      }
    }
  }
}

class MockShortenedApi() extends ShortenedUrlApi {
  override def fetchUrl(urlId: String) = Futures.successful("http://foo.com")

  override def shortenUrl(url: String) = Futures.successful("http://l/b4r")
}
