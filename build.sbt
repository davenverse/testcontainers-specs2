lazy val core = project.in(file("."))
    .settings(commonSettings, releaseSettings, mimaSettings)
    .settings(
      name := "testcontainers-specs2"
    )

val catsEffectV = "2.0.0"    //https://github.com/typelevel/cats-effect/releases
val doobieV = "0.8.0-RC1"        //https://github.com/tpolecat/doobie/releases
val flyWayV = "6.0.7"           //https://github.com/flyway/flyway/releases
val specs2V = "4.7.1"           //https://github.com/etorreborre/specs2/releases
val testcontainersSV = "0.26.0" //https://github.com/testcontainers/testcontainers-scala/releases

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

  scalaVersion := "2.13.0",
  crossScalaVersions := Seq(scalaVersion.value, "2.12.8", "2.11.12"),

  addCompilerPlugin("org.typelevel" % "kind-projector" % "0.10.3" cross CrossVersion.binary),

  libraryDependencies ++= Seq(
    "org.specs2"                  %% "specs2-core"                % specs2V,
    "com.dimafeng"                %% "testcontainers-scala"       % testcontainersSV
      exclude("org.scalatest", "scalatest"),
    "org.typelevel"               %% "cats-effect"                % catsEffectV % Test,
    "org.flywaydb"                %  "flyway-core"                % flyWayV % Test,
    "org.tpolecat"                %% "doobie-core"                % doobieV % Test,
    "org.tpolecat"                %% "doobie-specs2"              % doobieV % Test,
    "org.tpolecat"                %% "doobie-postgres"            % doobieV % Test
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

lazy val mimaSettings = {
  import sbtrelease.Version

  def semverBinCompatVersions(major: Int, minor: Int, patch: Int): Set[(Int, Int, Int)] = {
    val majorVersions: List[Int] = List(major)
    val minorVersions : List[Int] = 
      if (major >= 1) Range(0, minor).inclusive.toList
      else List(minor)
    def patchVersions(currentMinVersion: Int): List[Int] = 
      if (minor == 0 && patch == 0) List.empty[Int]
      else if (currentMinVersion != minor) List(0)
      else Range(0, patch - 1).inclusive.toList

    val versions = for {
      maj <- majorVersions
      min <- minorVersions
      pat <- patchVersions(min)
    } yield (maj, min, pat)
    versions.toSet
  }

  def mimaVersions(version: String): Set[String] = {
    Version(version) match {
      case Some(Version(major, Seq(minor, patch), _)) =>
        semverBinCompatVersions(major.toInt, minor.toInt, patch.toInt)
          .map{case (maj, min, pat) => maj.toString + "." + min.toString + "." + pat.toString}
      case _ =>
        Set.empty[String]
    }
  }
  // Safety Net For Exclusions
  lazy val excludedVersions: Set[String] = Set()

  // Safety Net for Inclusions
  lazy val extraVersions: Set[String] = Set()

  Seq(
    mimaFailOnProblem := mimaVersions(version.value).toList.nonEmpty,
    mimaPreviousArtifacts := (mimaVersions(version.value) ++ extraVersions)
      .filterNot(excludedVersions.contains)
      .map{v => 
        val moduleN = moduleName.value + "_" + scalaBinaryVersion.value.toString
        organization.value % moduleN % v
      },
    mimaBinaryIssueFilters ++= {
      import com.typesafe.tools.mima.core._
      import com.typesafe.tools.mima.core.ProblemFilters._
      Seq()
    }
  )
}

lazy val skipOnPublishSettings = Seq(
  skip in publish := true,
  publish := (),
  publishLocal := (),
  publishArtifact := false,
  publishTo := None
)

