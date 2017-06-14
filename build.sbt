organization := "com.pagerduty"

name := "eris-mapper"

scalaVersion := "2.11.11"

crossScalaVersions := Seq("2.10.6", "2.11.11", "2.12.2")

resolvers += "bintray-pagerduty-oss-maven" at "https://dl.bintray.com/pagerduty/oss-maven"

// Prevents logging configuration from being included in the test jar.
mappings in (Test, packageBin) ~= {
  _.filterNot(_._2.endsWith("logback-test.xml"))
}
mappings in (IntegrationTest, packageBin) ~= {
  _.filterNot(_._2.endsWith("logback-it.xml"))
}

// Dependencies in this configuration are not exported.
ivyConfigurations += config("transient").hide

fullClasspath in Test ++= update.value.select(configurationFilter("transient"))

lazy val root = (project in file("."))
  .configs(IntegrationTest extend (Test))
  .settings(inConfig(IntegrationTest)(scalafmtSettings))
  .settings(Defaults.itSettings: _*)
  .settings(
    libraryDependencies ++= Seq("com.pagerduty" %% "eris-core" % "3.0.0",
                                "com.pagerduty" %% "entity-mapper" % "1.0.0"),
    libraryDependencies ++= Seq(
      "ch.qos.logback" % "logback-classic" % "1.0.13" % "transient",
      "com.pagerduty" %% "eris-core-test-support" % "3.0.0" % "it,test",
      "org.scalatest" %% "scalatest" % "3.0.1" % "it,test"
    )
  )

scalafmtOnCompile in ThisBuild := true
