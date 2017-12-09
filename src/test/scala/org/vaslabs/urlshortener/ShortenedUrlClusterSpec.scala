package org.vaslabs.urlshortener

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.FlatSpecLike

class ShortenedUrlClusterSpec
  extends TestKit(ActorSystem("ShortenedUrlSystem")) with FlatSpecLike with ImplicitSender
{
  "given that we pass a shortened url pair the cluster" should "give the url back" in {
    val clusterRegion = ShortenedUrlCluster.region
    clusterRegion ! ShortenedUrlHolder.storeUrl(ShortenedUrl("http://foo.com", "bar"))
    clusterRegion ! ShortenedUrlHolder.Get("bar")
    expectMsg(ShortenedUrlHolder.FullUrl("http://foo.com"))
  }

}
