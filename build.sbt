logLevel := Level.Debug

lazy val root = (project in file("."))
  .settings(name := "lagom-fund-transfer-saga-scala")
  .aggregate(accountApi, accountImpl,
             fundTransferApi, fundTransferImpl)
  .settings(commonSettings: _*)

organization in ThisBuild := "com.klikix"

// the Scala version that will be used for cross-compiled libraries
scalaVersion in ThisBuild := "2.12.7"
EclipseKeys.projectFlavor in Global := EclipseProjectFlavor.ScalaIDE

version in ThisBuild := "1.0.0-SNAPSHOT"

val playJsonDerivedCodecs = "org.julienrf" %% "play-json-derived-codecs" % "4.0.0"
val macwire = "com.softwaremill.macwire" %% "macros" % "2.3.1" % "provided"
val scalaTest = "org.scalatest" %% "scalatest" % "3.0.5" % Test
val scalaTestPlusPlay = "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test
val mockito = "org.mockito" % "mockito-core" % "2.23.4" % Test
val scalaLogging = "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0"
val cassandraDriverExtras = "com.datastax.cassandra" % "cassandra-driver-extras" % "3.0.0"

lazy val util = (project in file("util"))
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi,
      lagomScaladslServer % Optional,
      playJsonDerivedCodecs,
      scalaTest
    )
  )

lazy val accountApi = (project in file("account-api"))
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi,
      playJsonDerivedCodecs
    )
  )
  .dependsOn(util)

lazy val accountImpl = (project in file("account-impl"))
  .settings(commonSettings: _*)
  .enablePlugins(LagomScala, SbtReactiveAppPlugin)
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslPersistenceCassandra,
      lagomScaladslTestKit,
      lagomScaladslKafkaBroker,
      lagomScaladslPubSub,
      cassandraDriverExtras,
      macwire,
      scalaTest,
      scalaLogging
    )
  )
  .settings(lagomForkedTestSettings: _*)
  .dependsOn(accountApi, util)

lazy val fundTransferApi = (project in file("fund-transfer-api"))
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi,
      playJsonDerivedCodecs
    )
  )
  .dependsOn(util)

lazy val fundTransferImpl = (project in file("fund-transfer-impl"))
  .settings(commonSettings: _*)
  .enablePlugins(LagomScala, SbtReactiveAppPlugin)
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslPersistenceCassandra,
      lagomScaladslTestKit,
      lagomScaladslKafkaBroker,
      cassandraDriverExtras,
      macwire,
      scalaTest,
      scalaLogging
    )
  )
  .settings(lagomForkedTestSettings: _*)
  .dependsOn(fundTransferApi,accountApi,util)


def evictionSettings: Seq[Setting[_]] = Seq(
  // This avoids a lot of dependency resolution warnings to be showed.
  // They are not required in Lagom since we have a more strict whitelist
  // of which dependencies are allowed. So it should be safe to not have
  // the build logs polluted with evictions warnings.
  evictionWarningOptions in update := EvictionWarningOptions.default
    .withWarnTransitiveEvictions(false)
    .withWarnDirectEvictions(false)
)

def commonSettings: Seq[Setting[_]] = evictionSettings ++ Seq(
  javacOptions in Compile ++= Seq("-encoding", "UTF-8", "-source", "1.8"),
  javacOptions in(Compile, compile) ++= Seq("-Xlint:unchecked", "-Xlint:deprecation", "-parameters", "-Werror"),
  scalacOptions ++= Seq("-feature", "-deprecation"),

  testOptions in Test ++= Seq(
    // Show the duration of tests
    Tests.Argument(TestFrameworks.ScalaTest, "-oD")
  )
)

lagomCassandraCleanOnStart in ThisBuild := true
