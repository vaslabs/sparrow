package org.vaslabs.urlshortener

import com.amazonaws.auth.{AWSCredentials, AWSStaticCredentialsProvider}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder

trait ClusterBaseSpec {

  final lazy val dynamoDBTestClient = {
    AmazonDynamoDBClientBuilder.standard().withEndpointConfiguration(
      new EndpointConfiguration("http://0.0.0.0:4569", "eu-west-1")
    ).withCredentials(new AWSStaticCredentialsProvider(new AWSCredentials(){
      override def getAWSAccessKeyId = "foo"

      override def getAWSSecretKey = "bar"
    })).build()
  }
}
