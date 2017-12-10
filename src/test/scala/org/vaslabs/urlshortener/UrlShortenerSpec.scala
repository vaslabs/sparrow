package org.vaslabs.urlshortener

import akka.actor.Status.Failure
import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit, TestProbe}
import org.scalatest.FlatSpecLike

class UrlShortenerSpec extends TestKit(ActorSystem("ShortenedUrlSystem"))
      with FlatSpecLike
      with ImplicitSender
{
  val urlShortener: ActorRef = TestActorRef(UrlShortener.props(ShortenedUrlCluster.region(system)))

  "requesting to shorten a url" should "give us back the 4 first characters of a sha" in {
    urlShortener ! UrlShortener.shorten("http://foo.com")
    expectMsg(UrlShortener.ShortUrl("20c9", "20c97674155e53d998eca74551e19f0e2dd3ef80643fdace3492d6c9d2d6b3fb"))
  }

  "requesting to shorten a url that conflicts" should "give us a rehash" in {
    val mockCluster = TestProbe()
    val urlShortenerWithMockCluster = TestActorRef(UrlShortener.props(mockCluster.ref))
    urlShortenerWithMockCluster ! UrlShortener.shorten("http://someotherfoothatconflicts.com")
    mockCluster.expectMsg(
      ShortenedUrlHolder.storeUrl(
        ShortenedUrl("http://someotherfoothatconflicts.com", "6602"))
    )
    mockCluster.reply(ShortenedUrlHolder.UrlIdAlreadyReserved("6602"))

    mockCluster.expectMsg(
      ShortenedUrlHolder.storeUrl(
        ShortenedUrl("http://someotherfoothatconflicts.com", "37a6"))
    )

    mockCluster.reply(ShortenedUrlHolder.StoredAck)
    expectMsg(UrlShortener.ShortUrl("37a6", "37a609ccb4fa954f73a257d0ce67f8a63d041efebeb6651a76c549a79f022f7c"))
  }
}
