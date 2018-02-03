name := "url-shortener"

version := "0.1"

scalaVersion := "2.12.4"

val akkaVersion = "2.5.9"
val circeVersion = "0.8.0"


enablePlugins(JavaServerAppPackaging)
enablePlugins(UniversalPlugin)
parallelExecution in ThisBuild := false


libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-http" % "10.0.10",
  "com.typesafe.akka" %% "akka-http-testkit" % "10.0.10" % Test,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
  "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-sharding" % akkaVersion,
  "org.scalatest" %% "scalatest" % "3.0.4" % Test,
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "io.circe" %% "circe-java8" % circeVersion,
  "de.heikoseeberger" %% "akka-http-circe" % "1.16.0",
  "com.gu" %% "scanamo" % "0.9.5",
  "com.github.pureconfig" %% "pureconfig" % "0.8.0",
  "eu.timepit" %% "refined"            % "0.8.7"
)