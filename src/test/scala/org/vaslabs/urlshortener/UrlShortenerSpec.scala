package org.vaslabs.urlshortener

import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import org.scalatest.FlatSpecLike

class UrlShortenerSpec extends TestKit(ActorSystem("ShortenedUrlSystem"))
      with FlatSpecLike
      with ImplicitSender
{
  "requesting to shorten a url" should "give us back the 4 first characters of a sha" in {
    val urlShortener: ActorRef = TestActorRef(UrlShortener.props())
    urlShortener ! UrlShortener.shorten("http://foo.com")
    expectMsg(UrlShortener.ShortUrl("20c9", "20c97674155e53d998eca74551e19f0e2dd3ef80643fdace3492d6c9d2d6b3fb"))
  }
}
