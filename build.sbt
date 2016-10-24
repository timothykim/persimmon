
name := "persimmon"

version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "org.typelevel"     %% "cats"               % "0.7.0",
  "org.mongodb.scala" %% "mongo-scala-driver" % "1.1.1",
  "org.json4s"        %% "json4s-jackson"     % "3.4.2",

  "org.scalatest"     %% "scalatest"          % "3.0.0" % "test"
)

