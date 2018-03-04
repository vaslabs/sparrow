package org.vaslabs.urlshortener

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike}
import com.gu.scanamo.syntax._
import com.gu.scanamo._
import org.vaslabs.urlshortener.ShortenedUrlHolder.StoredAck

class ShortenedUrlClusterSpec
  extends TestKit(ActorSystem("ShortenedUrlSystem")) with FlatSpecLike with ImplicitSender with ClusterBaseSpec with BeforeAndAfterAll
{

  import system.dispatcher
  override def afterAll() = system.terminate().foreach(_ => println("terminated"))

  implicit val dynamoDBClient = dynamoDBTestClient
  Scanamo.exec(dynamoDBTestClient)(Table[ShortenedUrl]("url-shortener").delete('shortVersion -> "bar"))
  "given that we pass a shortened url pair the cluster" should "give the url back" in {
    Scanamo.exec(dynamoDBTestClient)(Table[ShortenedUrl]("url-shortener").delete('shortVersion -> "bar"))
    val clusterRegion = ShortenedUrlCluster.region("url-shortener")
    clusterRegion ! ShortenedUrlHolder.storeUrl(ShortenedUrl("http://foo.com", "bar"))
    expectMsg(StoredAck)
    clusterRegion ! ShortenedUrlHolder.Get("bar")
    expectMsg(ShortenedUrlHolder.FullUrl("http://foo.com"))
  }

  "given that we pass a shortened url pair that already exists it" should "give us an error" in {
    Scanamo.exec(dynamoDBTestClient)(Table[ShortenedUrl]("url-shortener").delete('shortVersion -> "bar"))
    val clusterRegion = ShortenedUrlCluster.region("url-shortener")
    clusterRegion ! ShortenedUrlHolder.storeUrl(ShortenedUrl("http://foo.com", "bar"))
    expectMsg(StoredAck)
    clusterRegion ! ShortenedUrlHolder.storeUrl(ShortenedUrl("http://otherfoo.com", "bar"))
    expectMsg(ShortenedUrlHolder.UrlIdAlreadyReserved("bar"))
  }

  "given that we pass a custom shortened url it" should "give us the custom short version back" in {
    val clusterRegion = ShortenedUrlCluster.region("url-shortener")
    clusterRegion ! ShortenedUrlHolder.storeCustomUrl("http://bar.com", "custom")
    expectMsg(StoredAck)
    clusterRegion ! ShortenedUrlHolder.Get("custom")
    expectMsg(ShortenedUrlHolder.FullUrl("http://bar.com"))
  }

  "given that we request for stats it" should "give us the stats" in {
    val clusterRegion = ShortenedUrlCluster.region("url-shortener")
    clusterRegion ! ShortenedUrlHolder.storeCustomUrl("http://bar.com", "forstats")
    expectMsg(StoredAck)
    clusterRegion ! ShortenedUrlHolder.GetStats("forstats")

    expectMsg(ShortenedUrlHolder.Stats(List.empty))
  }

}
