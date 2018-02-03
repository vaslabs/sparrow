package org.vaslabs.urlshortener

import akka.actor.{ActorRef, ActorSystem}
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB

object ShortenedUrlCluster {
  def region(tableName: String)
        (implicit actorSystem: ActorSystem, dynamoDBClient: AmazonDynamoDB): ActorRef =
    ShortenedUrlHolder.counterRegion(actorSystem, dynamoDBClient, tableName)
}

