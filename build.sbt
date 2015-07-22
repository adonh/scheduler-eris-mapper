organization := "com.pagerduty"

name := "eris-mapper"

scalaVersion := "2.10.5"

libraryDependencies ++= Seq(
  "com.pagerduty" %% "eris-core" % "0.1-SNAPSHOT" % "compile->compile;test->test",
  "com.pagerduty" %% "mapper" % "0.1-SNAPSHOT")

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "2.2.4" % Test,
  "org.scalamock" %% "scalamock-scalatest-support" % "3.2" % Test,
  "org.scalacheck" %% "scalacheck" % "1.12.3" % Test)
