lazy val core = project.in(file("."))
    .settings(commonSettings)
    .settings(
      name := "testcontainers-specs2"
    )

val catsEffectV = "2.1.3"    //https://github.com/typelevel/cats-effect/releases
val doobieV = "0.9.0"        //https://github.com/tpolecat/doobie/releases
val flyWayV = "6.4.4"           //https://github.com/flyway/flyway/releases
val specs2V = "4.10.5"           //https://github.com/etorreborre/specs2/releases
val testcontainersSV = "0.39.9" //https://github.com/testcontainers/testcontainers-scala/releases

lazy val contributors = Seq(
  "aeons"                -> "Bjørn Madsen",
  "ChristopherDavenport" -> "Christopher Davenport"
)

lazy val commonSettings = Seq(
  scalaVersion := "2.13.8",
  crossScalaVersions := Seq(scalaVersion.value, "2.12.18"),

  addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.13.2" cross CrossVersion.full),

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

inThisBuild(
  List(
    organization := "io.chrisdavenport",
    developers := List(
      Developer(
        "ChristopherDavenport",
        "Christopher Davenport",
        "chris@christopherdavenport.tech",
        url("https://github.com/ChristopherDavenport"))
    ),
    homepage := Some(url("https://github.com/ChristopherDavenport/testcontainers-specs2")),
    licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
    pomIncludeRepository := { _ => false },
    (Compile / doc / scalacOptions) ++= Seq(
      "-groups",
      "-sourcepath",
      (LocalRootProject / baseDirectory).value.getAbsolutePath,
      "-doc-source-url",
      "https://github.com/ChristopherDavenport/testcontainers-specs2/blob/v" + version.value + "€{FILE_PATH}.scala"
    )
  ))

