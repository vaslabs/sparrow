package org.vaslabs.urlshortener.server

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.dynamodbv2.{AmazonDynamoDB, AmazonDynamoDBClientBuilder}
import org.vaslabs.urlshortener.ShortenedUrlCluster
import pureconfig._

case class ShortenerConfig(dynamodb: DynamoDBConfig)
case class DynamoDBConfig(endpoint: Option[String], tableName: String, region: String)

object Boot extends App{

  implicit val actorSystem = ActorSystem("ShortenedUrlSystem")
  implicit val actorMaterializer = ActorMaterializer()

  loadConfig[ShortenerConfig]("shortener").map(
    shortenerConfig => {


      implicit val dynamoDBClient: AmazonDynamoDB =
        shortenerConfig.dynamodb.endpoint.map(endpoint => {
          AmazonDynamoDBClientBuilder.standard()
            .withEndpointConfiguration(
              new EndpointConfiguration(
                endpoint,
                shortenerConfig.dynamodb.region
              )
            ).build()
        }).getOrElse(AmazonDynamoDBClientBuilder.defaultClient())


      val cluster = ShortenedUrlCluster.region(shortenerConfig.dynamodb.tableName)

      import actorSystem.dispatcher
      val server = new WebServer(cluster)

      server.start()

      sys.addShutdownHook {
        println("Shutting down")
        server.shutDown().foreach(_ => println("server shut down completed"))
      }
    }
  ).left.foreach(println)
}
