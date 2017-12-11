package org.vaslabs.urlshortener

import akka.actor.ActorSystem
import com.amazonaws.services.dynamodbv2.{AmazonDynamoDB, AmazonDynamoDBClient}

object ShortenedUrlCluster {
  def region(tableName: String)
        (implicit actorSystem: ActorSystem, dynamoDBClient: AmazonDynamoDB) =
    ShortenedUrlHolder.counterRegion(actorSystem, dynamoDBClient, tableName)
}

