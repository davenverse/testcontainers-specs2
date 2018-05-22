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
import cats.effect.IO
import org.specs2.mutable.Specification
import org.flywaydb.core.Flyway
import io.chrisdavenport.testcontainersspecs2.{ ForAllTestContainer, PostgresqlMultipleDatabases }

class MigrationsSpec extends Specification with ForAllTestContainer {

  private[this] val multiple = new PostgresqlMultipleDatabases(
    name = "christopherdavenport/postgres-multi-db:10.3",
    exposedPort = 5432,
    dbName = dbName,
    dbLoginName = dbLogin,
    dbPassword = dbPassword
  )
  // IMPORTANT: MUST BE LAZY VAL
  override lazy val container = multiple.container

  lazy val driverName = "org.postgresql.Driver"
  lazy val dbLogin = "user"
  lazy val dbPassword = "password"
  // optionally "dbuser@dbname" if the container supports it
  lazy val dbName = "db"
  lazy val jdbcUrl = multiple.jdbcUrl

  "Migrations should run Correctly" in {
    IO {
      lazy val flyway = new Flyway
      flyway.setDataSource(jdbcUrl, dbLogin, dbPassword)
      flyway.migrate()
      ()
    }.attempt
      .map(_.isRight)
      .unsafeRunSync() must_=== (true)
  }
```

### Doobie

Common Use Case which has a tricky inheritance component to define

```scala
import cats.effect._
import com.dimafeng.testcontainers._
import doobie._
import doobie.implicits._
import doobie.specs2._
import io.chrisdavenport.testcontainersspecs2.ForAllTestContainer
import org.specs2.mutable.Specification
import org.flywaydb.core.Flyway

class IODoobieQueriesSpec extends QueriesSpec[IO] {
  // Using this instead of IOAnalysisMatchers to avoid uninitialized field error
  override implicit val M: Effect[IO] = IO.ioConcurrentEffect
}

trait QueriesSpec[F[_]] extends Specification with Checker[F] with ForAllTestContainer {

  private[this] val multiple = new PostgresqlMultipleDatabases(
    name = "christopherdavenport/postgres-multi-db:10.3",
    exposedPort = 5432,
    dbName = dbName,
    dbLoginName = dbLogin,
    dbPassword = dbPassword
  )

  // IMPORTANT: MUST BE LAZY VAL
  override lazy val container = multiple.container

  lazy val driverName = "org.postgresql.Driver"
  lazy val dbLogin = "user"
  lazy val dbPassword = "password"
  // optionally "dbuser@dbname" if the container supports it
  lazy val dbName = "db"

  // This thing is a bit screwy
  lazy val transactor = Transactor.fromDriverManager[F](
    driverName,
    multiple.jdbcUrl,
    dbLogin,
    dbPassword
  )

  sequential

  // afterStart / beforeStop available for actions at the begininning
  // and end of a particular container session.
  // In this case we make sure migrations have run before we check the sql statements.
  override def afterStart(): Unit = {
    lazy val flyway = new Flyway
    flyway.setDataSource(multiple.jdbcUrl, dbLogin, dbPassword)
    flyway.migrate()
    ()
  }

  check(sql"SELECT 1".query[Int])

}
```
