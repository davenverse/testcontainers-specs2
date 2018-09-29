lazy val core = project.in(file("."))
    .settings(commonSettings, releaseSettings)
    .settings(
      name := "testcontainers-specs2"
    )

val catsEffectV = "0.10.1"
val doobieV = "0.5.3"
val flyWayV = "5.1.4"
val specs2V = "4.2.0"
val testcontainersSV = "0.18.0"

lazy val contributors = Seq(
  "aeons"                -> "Bjørn Madsen",
  "ChristopherDavenport" -> "Christopher Davenport"
)

// check for library updates whenever the project is [re]load
onLoad in Global := { s =>
  "dependencyUpdates" :: s
}

lazy val commonSettings = Seq(
  organization := "io.chrisdavenport",

  scalaVersion := "2.12.6",
  crossScalaVersions := Seq(scalaVersion.value, "2.11.12"),

  addCompilerPlugin("org.spire-math" % "kind-projector" % "0.9.8" cross CrossVersion.binary),

  libraryDependencies ++= Seq(
    "org.typelevel"               %% "cats-effect"                % catsEffectV % Test,
    "org.flywaydb"                %  "flyway-core"                % flyWayV % Test,
    "org.specs2"                  %% "specs2-core"                % specs2V,
    "org.tpolecat"                %% "doobie-core"                % doobieV % Test,
    "org.tpolecat"                %% "doobie-specs2"              % doobieV % Test,
    "org.tpolecat"                %% "doobie-postgres"            % doobieV % Test,
    "com.dimafeng"                %% "testcontainers-scala"       % testcontainersSV
      exclude("org.scalatest", "scalatest")
  )
)

lazy val releaseSettings = {
  import ReleaseTransformations._
  Seq(
    releaseCrossBuild := true,
    releaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      runClean,
      runTest,
      setReleaseVersion,
      commitReleaseVersion,
      tagRelease,
      // For non cross-build projects, use releaseStepCommand("publishSigned")
      releaseStepCommandAndRemaining("+publishSigned"),
      setNextVersion,
      commitNextVersion,
      releaseStepCommand("sonatypeReleaseAll"),
      pushChanges
    ),
    publishTo := {
      val nexus = "https://oss.sonatype.org/"
      if (isSnapshot.value)
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases" at nexus + "service/local/staging/deploy/maven2")
    },
    credentials ++= (
      for {
        username <- Option(System.getenv().get("SONATYPE_USERNAME"))
        password <- Option(System.getenv().get("SONATYPE_PASSWORD"))
      } yield
        Credentials(
          "Sonatype Nexus Repository Manager",
          "oss.sonatype.org",
          username,
          password
        )
    ).toSeq,
    publishArtifact in Test := false,
    releasePublishArtifactsAction := PgpKeys.publishSigned.value,
    scmInfo := Some(
      ScmInfo(
        url("https://github.com/ChristopherDavenport/testcontainers-specs2"),
        "git@github.com:ChristopherDavenport/testcontainers-specs2.git"
      )
    ),
    homepage := Some(url("https://github.com/ChristopherDavenport/testcontainers-specs2")),
    licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
    publishMavenStyle := true,
    pomIncludeRepository := { _ =>
      false
    },
    pomExtra := {
      <developers>
        {for ((username, name) <- contributors) yield
        <developer>
          <id>{username}</id>
          <name>{name}</name>
          <url>http://github.com/{username}</url>
        </developer>
        }
      </developers>
    }
  )
}
