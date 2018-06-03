package org.vaslabs.urlshortener

import java.time.{Clock, Instant, ZoneOffset, ZonedDateTime}

import akka.actor.ActorSystem
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{Subscribe, SubscribeAck, Publish}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{FlatSpecLike, Matchers}
import eu.timepit.refined.numeric._
import eu.timepit.refined.auto._
import cats.data._
import scala.concurrent.duration._

class StatsGathererSpec extends TestKit(ActorSystem("ShortenedUrlSystem")) with
  FlatSpecLike with Matchers with ImplicitSender {

  val TestClock = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC)

  "stats gatherer collects stats and" must "report them" in {
    val statsGatherer = system.actorOf(StatsGatherer.props(), "statsGatherer")
    val mediator = DistributedPubSub(system).mediator
    val visitEvent = StatsGatherer.Protocol.Visit(Ior.Left("127.0.0.1"), "foo", ZonedDateTime.now(TestClock))
    mediator ! Subscribe("visitstats", self)
    expectMsgType[SubscribeAck]
    mediator ! Publish("visits", visitEvent)

    expectMsg(StatsGatherer.Protocol.StatsUpdated)

    statsGatherer ! StatsGatherer.Protocol.GetStats

    expectMsg(StatsGatherer.Protocol.VisitStats(Map(
      "foo" -> Set(StatsGatherer.Protocol.IpVisit(Ior.Left("127.0.0.1"), 1L))
    )))

    mediator ! Publish("visits", visitEvent)
    expectMsg(StatsGatherer.Protocol.StatsUpdated)

    statsGatherer ! StatsGatherer.Protocol.GetStats
    expectMsg(
      (StatsGatherer.Protocol.VisitStats(Map(
        "foo" -> Set(StatsGatherer.Protocol.IpVisit(Ior.Left("127.0.0.1"), 2L))
      )))
    )

    val visitEventFromOtherIp = StatsGatherer.Protocol.Visit(Ior.Left("192.168.101.254"),
      "foo", ZonedDateTime.now(TestClock))
    mediator ! Publish("visits", visitEventFromOtherIp)
    expectMsg(StatsGatherer.Protocol.StatsUpdated)
    statsGatherer ! StatsGatherer.Protocol.GetStats

    expectMsg(
      (StatsGatherer.Protocol.VisitStats(Map(
        "foo" -> Set(
          StatsGatherer.Protocol.IpVisit(Ior.Left("127.0.0.1"), 2L),
          StatsGatherer.Protocol.IpVisit(Ior.Left("192.168.101.254"), 1L))
      )))
    )

    val visitEventOnOtherUrl = StatsGatherer.Protocol.Visit(Ior.Left("192.168.101.254"),
      "bar", ZonedDateTime.now(TestClock))
    mediator ! Publish("visits", visitEventOnOtherUrl)
    expectMsg(StatsGatherer.Protocol.StatsUpdated)
    statsGatherer ! StatsGatherer.Protocol.GetStats
    expectMsg(
      (StatsGatherer.Protocol.VisitStats(Map(
        "foo" -> Set(
          StatsGatherer.Protocol.IpVisit(Ior.Left("127.0.0.1"), 2L),
          StatsGatherer.Protocol.IpVisit(Ior.Left("192.168.101.254"), 1L)),
        "bar" -> Set(StatsGatherer.Protocol.IpVisit(Ior.Left("192.168.101.254"), 1L))
      )))
    )
  }

}
