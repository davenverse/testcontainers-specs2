# testcontainers-specs2 [![Build Status](https://travis-ci.org/ChristopherDavenport/testcontainers-specs2.svg?branch=master)](https://travis-ci.org/ChristopherDavenport/testcontainers-specs2) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.chrisdavenport/testcontainers-specs2_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.chrisdavenport/testcontainers-specs2_2.12)

Integration for testing components with docker.

## Quick Start

To use testcontainers-specs2 in an existing SBT project with Scala 2.11 or a later version, add the following dependency to your
`build.sbt`:

```scala
libraryDependencies += "io.chrisdavenport" %% "testcontainers-specs2" % "<version>" % Test
```

## Examples

### Migrations

```scala
import cats.effect._
import org.flywaydb.core.Flyway
import org.specs2.mutable.Specification
import cats.effect.unsafe.implicits.global

class MigrationsSpec
    extends Specification
    with ForAllTestContainer
    with UsesPostgreSQLMultipleDatabases {

  "Migrations should run Correctly" in {
    IO {
      Flyway
        .configure()
        .dataSource(jdbcUrl, dbUserName, dbPassword)
        .load()
        .migrate
    }.attempt
      .map(_.isRight)
      .unsafeRunSync() must_=== true
  }

}
```

### Doobie

```scala
import cats.effect._
import doobie._
import doobie.implicits._
import doobie.specs2._
import org.flywaydb.core.Flyway
import org.specs2.mutable.Specification
import cats.effect.unsafe.implicits.global

class QueriesSpec[F[_]]
    extends Specification
    with IOChecker
    with ForAllTestContainer
    with UsesPostgreSQLMultipleDatabases {

  override lazy val transactor: Transactor[IO] = Transactor.fromDriverManager[IO](
    driverName,
    jdbcUrl,
    dbUserName,
    dbPassword
  )

  sequential

  // afterStart / beforeStop available for actions at the begininning
  // and end of a particular container session.
  // In this case we make sure migrations have run before we check the sql statements.
  override def afterStart(): Unit = {
    Flyway
      .configure()
      .dataSource(jdbcUrl, dbUserName, dbPassword)
      .load()
      .migrate
    ()
  }

  check(sql"SELECT 1".query[Int])

}
```
