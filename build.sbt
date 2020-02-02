name := "authorizer"

version := "1.0"

scalaVersion := "2.13.1"

lazy val akkaVersion = "2.6.3"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor"               % akkaVersion,
  "ch.qos.logback"    % "logback-classic"           % "1.2.3",
  "io.spray"          %% "spray-json"               % "1.3.5",
  "joda-time"         % "joda-time"                 % "2.10.1",
  "org.joda"          % "joda-convert"              % "2.2.0",
  "com.typesafe.akka" %% "akka-testkit"             % akkaVersion % Test,
  "org.scalatest"     %% "scalatest"                % "3.1.0" % Test
)
