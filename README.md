# testcontainers-specs2 [![Build Status](https://travis-ci.org/ChristopherDavenport/testcontainers-specs2.svg?branch=master)](https://travis-ci.org/ChristopherDavenport/testcontainers-specs2) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.chrisdavenport/testcontainers-specs2_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.chrisdavenport/testcontainers-specs2_2.12)

Integration for testing components with docker.

## Quick Start

To use testcontainers-specs2 in an existing SBT project with Scala 2.11 or a later version, add the following dependency to your
`build.sbt`:

```scala
libraryDependencies += "io.chrisdavenport" %% "testcontainers-specs2" % "<version>"
```

## Examples

### Migrations

```scala
import com.dimafeng.testcontainers._
import org.testcontainers.containers.wait._
import cats.effect.IO
import java.time.Duration
import java.time.temporal.ChronoUnit.SECONDS
import org.specs2.mutable.Specification
import io.chrisdavenport.testcontainersspecs2.ForAllTestContainer

class MigrationsSpec extends Specification with ForAllTestContainer {
  // IMPORTANT: MUST BE LAZY VAL
  override lazy val container = GenericContainer(
    "christopherdavenport/postgres-multi-db:10.3",
    exposedPorts = Seq(5432),
    env = Map(
      "REPO" -> "https://github.com/mrts/docker-postgresql-multiple-databases",
      "POSTGRES_USER" -> dbUserName,
      "POSTGRES_PASSWORD" ->  dbPassword,
      "POSTGRES_MULTIPLE_DATABASES"  -> dbName
    ),
    waitStrategy = new LogMessageWaitStrategy()
      .withRegEx(".*database system is ready to accept connections.*\\s")
      .withTimes(2)
      .withStartupTimeout(Duration.of(60, SECONDS))
  )
  lazy val driverName = "org.postgresql.Driver"
  lazy val jdbcUrl = s"jdbc:postgresql://${container.container.getContainerIpAddress()}:${container.container.getMappedPort(5432)}/${dbName}"
  lazy val dbUserName = "user"
  lazy val dbPassword = "password"
  lazy val dbName = "db"

  "Migrations" should {
    "runCorrectly" in {
      IO{
        lazy val flyway = new Flyway
        flyway.setDataSource(jdbcUrl, dbUserName, dbPassword)
        flyway.setLocations("classpath:db/migration")
        flyway.migrate()
      }.attempt
        .map(_.isRight)
        .unsafeRunSync() must_===(true)
    }
  }
```

### Doobie

Common Use Case which has a tricky inheritance component to define

```scala
import cats.effect._
import com.dimafeng.testcontainers._
import org.testcontainers.containers.wait._
import java.time.Duration
import java.time.temporal.ChronoUnit.SECONDS
import doobie._
import doobie.implicits._
import doobie.specs2._
import io.chrisdavenport.testcontainersspecs2.ForAllTestContainer
import org.specs2.mutable.Specification

class IODoobieQueriesSpec extends QueriesSpec[IO] {
  // Using this instead of IOAnalysisMatchers to avoid uninitialized field error
  override implicit val M: Effect[IO] = IO.ioConcurrentEffect
}

trait QueriesSpec[F[_]] extends Specification with Checker[F] with ForAllTestContainer {
  sequential

  // IMPORTANT: MUST BE LAZY VAL
  override lazy val container = GenericContainer(
    "christopherdavenport/postgres-multi-db:10.3",
    exposedPorts = Seq(5432),
    env = Map(
      "REPO" -> "https://github.com/mrts/docker-postgresql-multiple-databases",
      "POSTGRES_USER" -> dbUserName,
      "POSTGRES_PASSWORD" ->  dbPassword,
      "POSTGRES_MULTIPLE_DATABASES"  -> dbName
    ),
    waitStrategy = new LogMessageWaitStrategy()
      .withRegEx(".*database system is ready to accept connections.*\\s")
      .withTimes(2)
      .withStartupTimeout(Duration.of(60, SECONDS))
  )
  lazy val driverName = "org.postgresql.Driver"
  lazy val jdbcUrl = s"jdbc:postgresql://${container.container.getContainerIpAddress()}:${container.container.getMappedPort(5432)}/${dbName}"
  lazy val dbUserName = "user"
  lazy val dbPassword = "password"
  lazy val dbName = "db"

  // This thing is a bit screwy
  lazy val transactor = Transactor.fromDriverManager[F](
    driverName,
    jdbcUrl,
    dbUserName,
    dbPassword
  )

  // afterStart / beforeStop available for actions at the begininning
  // and end of a particular container session.
  // In this case we make sure migrations have run before
  // we check the sql statements.
  override def afterStart(): Unit = {
    lazy val flyway = new Flyway
    flyway.setDataSource(jdbcUrl, dbUserName, dbPassword)
    flyway.setLocations("classpath:db/migration")
    flyway.migrate()
  }

  check(sql"SELECT 1".query[Int])

}
```