import sbt._
import Keys._

object TractionBuild extends Build {
  val gravityRepo = "gravitydev" at "http://repos.gravitydev.com/app/repos/12"

  val commonSettings = Seq(
    organization  := "com.gravitydev",
    version       := "0.0.5-SNAPSHOT",
    scalaVersion  := "2.10.3",
    scalacOptions ++= Seq("-deprecation","-unchecked"/*,"-Xlog-implicits","-XX:-OmitStackTraceInFastThrow"*/),
    testOptions in Test += Tests.Argument("-oF"),
    publishTo := Some(gravityRepo),
    resolvers ++= Seq(
      "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/", 
      Resolver.sonatypeRepo("snapshots") // for scala-pickling
    )
  )

  lazy val core: Project = Project(id = "traction-core", base = file("core"))
    .settings(commonSettings:_*)
    .settings(
      name := "traction-core",
      //publishTo := Some(gravityRepo),
      libraryDependencies ++= Seq(
        "org.scala-lang" %% "scala-pickling" % "0.8.0-SNAPSHOT",
        "org.slf4j"     % "slf4j-api"     % "1.6.4",
        "org.scalatest" %%  "scalatest"   % "1.9.1"     % "test",
        "org.scalaz"    %% "scalaz-core" % "7.0.2"
      )
    )

  lazy val amazonswf: Project = Project(id = "traction-amazonswf", base = file("amazonswf"))
    .settings(commonSettings:_*)
    .settings(
      name := "traction-amazonswf",
      libraryDependencies ++= Seq(
        "com.typesafe.akka" %% "akka-actor"   % "2.1.0",
        "com.typesafe.akka" %% "akka-agent"   % "2.1.0",
        "com.amazonaws"     % "aws-java-sdk"  % "1.7.5"
      )
    ) dependsOn core 

  lazy val sample = Project(id = "traction-sample", base = file("sample"))
    .dependsOn(core, amazonswf)
    .settings(commonSettings:_*)
}

