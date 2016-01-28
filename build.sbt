name := "websocket-scratch"

version := "1.0"

scalaVersion := "2.11.7"

libraryDependencies ++= {
  val akkaHttpVersion = "2.0.2"
  Seq(
    "com.typesafe.akka" %% "akka-stream-experimental"    % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-core-experimental" % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-experimental"      % akkaHttpVersion,
  )
}

fork in run := true