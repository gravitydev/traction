import sbt._
import Keys._

object TractionBuild extends Build {
  val gravityRepo = "gravitydev" at "http://repos.gravitydev.com/app/repos/12"

  val commonSettings = Seq(
    organization  := "com.gravitydev",
    version       := "0.0.5-SNAPSHOT",
    scalaVersion  := "2.10.2",
    scalacOptions ++= Seq("-deprecation","-unchecked"/*,"-XX:-OmitStackTraceInFastThrow"*/),
    testOptions in Test += Tests.Argument("-oF"),
    publishTo := Some(gravityRepo)
  )

  lazy val core: Project = Project(id = "traction-core", base = file("core")).settings(commonSettings:_*).settings(
    name          := "traction-core",
    //publishTo := Some(gravityRepo),
    libraryDependencies ++= Seq(
      "org.slf4j"     % "slf4j-api"     % "1.6.4",
      "com.typesafe.play" %% "play"         % "2.2.0", // TODO: remove play dependency from core package
      "org.scalatest" %%  "scalatest"   % "1.9.1"     % "test",
      "org.scalaz"    %% "scalaz-core" % "7.0.2"
    ),
    resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
  )

  lazy val amazonswf: Project = Project(id = "traction-amazonswf", base = file("amazonswf")).settings(commonSettings:_*).settings(
    name          := "traction-amazonswf",
    resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor"   % "2.1.0",
      "com.typesafe.akka" %% "akka-agent"   % "2.1.0",
      "com.amazonaws"     % "aws-java-sdk"  % "1.4.7"
      "com.typesafe.play" %% "play"         % "2.2.0"
    )
  ) dependsOn core 

  lazy val sample = Project(id = "traction-sample", base = file("sample"))
    .dependsOn(core, amazonswf)
    .settings(commonSettings:_*)
}

