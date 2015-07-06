lazy val commonSettings = Seq(
  organization := "com.moshensky",
  version := "0.1.0",
  scalaVersion := "2.11.7"
)

resolvers ++= Seq(
  "Apache Repository" at "https://repository.apache.org/content/groups/snapshots/"
)

libraryDependencies ++= Seq(
  "org.apache.pdfbox" % "pdfbox-app" % "2.0.0-SNAPSHOT",
  "ch.qos.logback" % "logback-classic" % "1.1.3",
  "org.log4s" %% "log4s" % "1.1.5"
)

lazy val root = (project in file(".")).
  settings(commonSettings: _*).
  settings(
    name := "boxable-scala"
  )
