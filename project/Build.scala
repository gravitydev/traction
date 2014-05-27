import sbt._
import Keys._

object TractionBuild extends Build {
  val gravityRepo = "gravitydev" at "https://devstack.io/repo/gravitydev/public"

  override def rootProject = Some(core)

  val commonSettings = Seq(
    organization  := "com.gravitydev",
    version       := "0.1.0-SNAPSHOT",
    scalaVersion  := "2.10.3",
    crossScalaVersions := Seq("2.11.0", "2.10.3"),
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
      libraryDependencies ++= Seq(
        "org.scala-lang" %% "scala-pickling" % "0.9.0-SNAPSHOT",
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
      libraryDependencies ++= Seq(
        "com.typesafe.akka" %% "akka-actor"   % "2.3.3",
        "com.typesafe.akka" %% "akka-agent"   % "2.3.3",
        "com.amazonaws"     % "aws-java-sdk"  % "1.7.5",
        "org.scalatest" %% "scalatest" % "2.1.6" % "test"
      ),
      addCompilerPlugin("org.scalamacros" % "paradise" % "2.0.0" cross CrossVersion.full)
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

