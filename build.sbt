organization := "xyz.dziman"

lazy val `play-arangodb` = (project in file(".")).enablePlugins(PlayLibrary)

val PlayVersion = playVersion("2.4.6")

scalaVersion := "2.11.7"

scalacOptions ++= Seq("-language:postfixOps")

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play" % PlayVersion % Provided,
  "com.typesafe.play" %% "play-specs2" % PlayVersion % Test
)

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

playBuildRepoName in ThisBuild := "play-arangodb"
