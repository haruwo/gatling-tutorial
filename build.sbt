enablePlugins(GatlingPlugin)

name := "gatling-tutorial"

version := "0.1"

scalaVersion := "2.12.6"

// https://mvnrepository.com/artifact/io.gatling.highcharts/gatling-charts-highcharts-bundle
libraryDependencies += "io.gatling.highcharts" % "gatling-charts-highcharts" % "2.3.1" % "test"
libraryDependencies += "io.gatling" % "gatling-test-framework" % "2.3.1" % "test"

scalacOptions := Seq(
  "-encoding", "UTF-8",
  "-deprecation",
  "-feature",
  "-unchecked",
  "-language:implicitConversions",
  "-language:postfixOps"
)

