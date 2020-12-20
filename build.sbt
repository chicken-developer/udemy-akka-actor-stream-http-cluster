name := "Akka"

version := "0.1"

scalaVersion := "2.12.12"

val akkaVersion = "2.6.10"
val scalaTestVersion = "3.0.5"
val akkaHttpVersion = "10.2.2"


libraryDependencies ++= Seq(
  //Log
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "ch.qos.logback" % "logback-classic" % "1.2.3"
)

libraryDependencies ++= Seq(
  //akka actor
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
  // akka streams
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  // akka http
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
  // testing
  "org.scalatest" %% "scalatest" % scalaTestVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion,
  "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion,
  // JWT
  "com.pauldijou" %% "jwt-spray-json" % "2.1.0",

  //Akka Remoting and Cluster
  "com.typesafe.akka" %% "akka-remote" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-sharding" % "2.5.32",
  "com.typesafe.akka" %% "akka-cluster-tools" % akkaVersion,
  "io.netty" % "netty" % "3.10.6.Final"

)
