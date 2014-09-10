import sbt._
import Keys._

object TractionBuild extends Build {
  val gravityRepo = "gravitydev" at "https://devstack.io/repo/gravitydev/public"

  override def rootProject = Some(core)

  val commonSettings = Seq(
    organization  := "com.gravitydev",
    version       := "0.1.1-upickle-SNAPSHOT",
    scalaVersion  := "2.11.2",
    crossScalaVersions := Seq("2.11.2", "2.10.3"),
    scalacOptions ++= Seq("-deprecation","-unchecked"/*,"-Xlog-implicits","-XX:-OmitStackTraceInFastThrow"*/),
    testOptions in Test += Tests.Argument("-oF"),
    publishTo := Some(gravityRepo),
    resolvers ++= Seq(
      "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/", 
      "bintray/non" at "http://dl.bintray.com/non/maven",
      gravityRepo
    )
  )

  lazy val core: Project = Project(id = "traction-core", base = file("core"))
    .settings(commonSettings:_*)
    .settings(
      name := "traction-core",
      libraryDependencies ++= Seq(
        "com.lihaoyi" %% "upickle" % "0.2.4",
        "org.slf4j"     % "slf4j-api"     % "1.6.4",
        "org.scalatest" %% "scalatest" % "2.1.6" % "test",
        "org.scalaz"    %% "scalaz-core" % "7.1.0-M7",
        "com.typesafe.scala-logging" %% "scala-logging-slf4j" % "2.1.2"
      )
    )

  lazy val amazonswf: Project = Project(id = "traction-amazonswf", base = file("amazonswf"))
    .settings(commonSettings:_*)
    .settings(
      name := "traction-amazonswf",
      libraryDependencies ++= (Seq(
        "com.typesafe.akka" %% "akka-actor"   % "2.3.3",
        "com.typesafe.akka" %% "akka-agent"   % "2.3.3",
        "com.gravitydev" %% "awsutil" % "0.0.2-SNAPSHOT",
        "org.scalatest" %% "scalatest" % "2.1.6" % "test"
      ) ++ (if (scalaVersion.value startsWith "2.10") Seq("org.scalamacros" %% "quasiquotes" % "2.0.1") else Seq())),
      addCompilerPlugin("org.scalamacros" % "paradise" % "2.0.1" cross CrossVersion.full)
    ) dependsOn core 

  lazy val sample = Project(id = "traction-sample", base = file("sample"))
    .dependsOn(core, amazonswf)
    .settings(commonSettings:_*)
    .settings(
      libraryDependencies ++= Seq(
        "ch.qos.logback" % "logback-classic" % "1.1.2" % "runtime"
      )
    )
}

