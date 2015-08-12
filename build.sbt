organization := "com.pagerduty"

name := "eris-mapper"

scalaVersion := "2.10.4"

libraryDependencies ++= Seq(
  "com.pagerduty" %% "eris-core" % "0.4" % "compile->compile;test->test",
  "com.pagerduty" %% "mapper" % "0.4")

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "2.2.4" % Test,
  "org.scalamock" %% "scalamock-scalatest-support" % "3.2" % Test,
  "org.scalacheck" %% "scalacheck" % "1.12.3" % Test)
