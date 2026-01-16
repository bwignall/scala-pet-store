organization := "io.github.pauljamescleary"
name := "scala-pet-store"
version := "0.0.1-SNAPSHOT"
// crossScalaVersions := Seq("2.13.13")

val scala2Version = "2.13.18"
val scala3Version = "3.7.4"

scalaVersion := scala2Version
//scalaVersion := scala3Version

resolvers += Resolver.sonatypeCentralSnapshots

// Scala2

// migration settings
scalacOptions ++= Seq(
  "-Xsource:3",
  "-Xsource:3-cross"
)

// Scala3

//scalacOptions ++= Seq(
//  "-Xsource:3",
//  "-Xsource:3-cross",
//  "-source:3.0-migration",
////  "-new-syntax",
//  "-old_syntax",
//  "-rewrite"
//)

val CatsVersion = "2.13.0"
val CirceVersion = "0.14.15"
val CirceGenericExVersion = "0.14.5-RC1" //"0.14.4"
val CirceConfigVersion = "0.10.2"
val DoobieVersion = "1.0.0-RC11"
val EnumeratumCirceVersion = "1.9.2"
val H2Version = "2.4.240"
val Http4sVersion = "0.23.17" // Stuck at 0.23.17 because of blaze-server
val KindProjectorVersion = "0.13.4"
val LogbackVersion = "1.5.24"
val Slf4jVersion = "2.0.17"
val ScalaCheckVersion = "1.19.0"
val ScalaTestVersion = "3.2.19"
val ScalaTestPlusVersion = "3.2.19.0"
val FlywayVersion = "11.20.2"
val TsecVersion = "0.5.0"

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-core" % CatsVersion,
  "io.circe" %% "circe-generic" % CirceVersion,
  "io.circe" %% "circe-literal" % CirceVersion,
  "io.circe" %% "circe-generic-extras" % CirceGenericExVersion,
  "io.circe" %% "circe-parser" % CirceVersion,
  "io.circe" %% "circe-config" % CirceConfigVersion,
  "org.tpolecat" %% "doobie-core" % DoobieVersion,
  "org.tpolecat" %% "doobie-h2" % DoobieVersion,
  "org.tpolecat" %% "doobie-scalatest" % DoobieVersion,
  "org.tpolecat" %% "doobie-hikari" % DoobieVersion,
  "com.beachape" %% "enumeratum-circe" % EnumeratumCirceVersion,
  "com.h2database" % "h2" % H2Version,
  "org.http4s" %% "http4s-blaze-server" % Http4sVersion,
  "org.http4s" %% "http4s-ember-server" % Http4sVersion,
  "org.http4s" %% "http4s-circe" % Http4sVersion,
  "org.http4s" %% "http4s-dsl" % Http4sVersion,
  "ch.qos.logback" % "logback-classic" % LogbackVersion,
  "org.flywaydb" % "flyway-core" % FlywayVersion,
  "org.http4s" %% "http4s-blaze-client" % Http4sVersion % Test,
  "org.scalacheck" %% "scalacheck" % ScalaCheckVersion % Test,
  "org.scalatest" %% "scalatest" % ScalaTestVersion % Test,
//  "org.scalatestplus" %% "scalacheck-1-14" % ScalaTestPlusVersion % Test,
  "org.scalatestplus" %% "scalacheck-1-19" % ScalaTestPlusVersion % Test,
  // Authentication dependencies
  "io.github.jmcardon" %% "tsec-common" % TsecVersion,
  "io.github.jmcardon" %% "tsec-password" % TsecVersion,
  "io.github.jmcardon" %% "tsec-mac" % TsecVersion,
  "io.github.jmcardon" %% "tsec-signatures" % TsecVersion,
  "io.github.jmcardon" %% "tsec-jwt-mac" % TsecVersion,
  "io.github.jmcardon" %% "tsec-jwt-sig" % TsecVersion,
  "io.github.jmcardon" %% "tsec-http4s" % TsecVersion
)

dependencyOverrides += "org.slf4j" % "slf4j-api" % Slf4jVersion

addCompilerPlugin(
//  ("org.typelevel" %% "kind-projector" % KindProjectorVersion).cross(CrossVersion.for3Use2_13)
  "org.typelevel" % "kind-projector_2.13.18" % KindProjectorVersion // .cross(CrossVersion.for3Use2_13)
)

enablePlugins(ScalafmtPlugin, JavaAppPackaging, GhpagesPlugin, MicrositesPlugin, MdocPlugin)

// Microsite settings
git.remoteRepo := "git@github.com:pauljamescleary/scala-pet-store.git"

micrositeGithubOwner := "pauljamescleary"

micrositeGithubRepo := "scala-pet-store"

micrositeName := "Scala Pet Store"

micrositeDescription := "An example application using FP techniques in Scala"

micrositeBaseUrl := "scala-pet-store"

// Note: This fixes error with sbt run not loading config properly
run / fork := true

dockerExposedPorts ++= Seq(8080)
