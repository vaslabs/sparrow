package org.vaslabs.urlshortener

import java.time.ZonedDateTime

import akka.actor.{Actor, ActorLogging, Props}
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{Publish, Subscribe, SubscribeAck}
import cats.data.Ior.{Both, Left, Right}
import cats.data._
class StatsGatherer private () extends Actor with ActorLogging{

  import StatsGatherer.Protocol._
  import eu.timepit.refined.auto._

  val mediator = DistributedPubSub(context.system).mediator

  override def aroundPreStart(): Unit = {
    mediator ! Subscribe("visits", self)
    log.info("Subscribing to visits")
  }


  override def receive: Receive = {
    case visitEvent @ Visit(ip, url, time) =>
      context.become(behaviourWithStats(
        Map(url -> Set(IpVisit(ip, 1L)))
      ))
      log.info("Received visit event {}", visitEvent)
      mediator ! Publish("visitstats", StatsUpdated)

    case GetStats => sender() ! VisitStats(Map.empty)
    case SubscribeAck(subscribe) => log.info("Subscribed {}", subscribe)
  }

  def behaviourWithStats(visitStats: Map[String, Set[IpVisit]]): Receive = {
    case GetStats => sender() ! VisitStats(visitStats)
    case visitEvent @ Visit(ip, url, time) =>
      val statsEntry = for {
        visitsOnUrl <- visitStats.get(url)
        ipVisit = for {
          ipVisitsFromIp <- visitsOnUrl.find(_.ip == ip)
        } yield(ipVisitsFromIp.copy(timesVisited = ipVisitsFromIp.timesVisited + 1L))
        ipVisits = visitsOnUrl.filterNot(_.ip == ip) + ipVisit.getOrElse(IpVisit(ip, 1L))
      } yield (url -> ipVisits)
      context.become(behaviourWithStats(visitStats + statsEntry.getOrElse(url -> Set(IpVisit(ip, 1L)))))
      mediator ! Publish("visitstats", StatsUpdated)
  }

}

object StatsGatherer {
  def props(): Props = Props(new StatsGatherer)

  object Protocol {
    import ShortenedUrlHolder.{Ipv4, Ipv6}

    case class Visit(ip: Ior[Ipv4, Ipv6], url: String, time: ZonedDateTime)

    case object GetStats

    case class VisitStats(stats: Map[String, Set[IpVisit]])

    case class IpVisit(ip: Ior[Ipv4, Ipv6], timesVisited: Long) {
      def ipString: String = ip match {
        case Left(ipv4) => s"ipv4: $ipv4"
        case Right(ipv6) => s"ipv6: $ipv6"
        case Both(ipv4, ipv6) => s"ipv4: $ipv4, ipv6: $ipv6"
        case _ => "Unknown"
      }

    }

    case object StatsUpdated

  }

}
