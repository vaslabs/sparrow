name := "url-shortener"

version := "0.1"

scalaVersion := "2.12.6"

val akkaVersion = "2.5.12"
val circeVersion = "0.9.3"


enablePlugins(JavaServerAppPackaging)
enablePlugins(UniversalPlugin)
parallelExecution in ThisBuild := false


libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-http" % "10.1.1",
  "com.typesafe.akka" %% "akka-http-testkit" % "10.1.1" % Test,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
  "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-sharding" % akkaVersion,
  "org.scalatest" %% "scalatest" % "3.0.5" % Test,
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "io.circe" %% "circe-java8" % circeVersion,
  "io.circe" %% "circe-refined" % circeVersion,
  "de.heikoseeberger" %% "akka-http-circe" % "1.20.1",
  "com.gu" %% "scanamo" % "1.0.0-M6",
  "com.github.pureconfig" %% "pureconfig" % "0.9.1",
  "eu.timepit" %% "refined"            % "0.9.0"
)