package io.chrisdavenport.testcontainersspecs2

import cats.effect._
import doobie._
import doobie.implicits._
import doobie.specs2._
import org.flywaydb.core.Flyway
import org.specs2.mutable.Specification

class IODoobieQueriesSpec extends QueriesSpec[IO] {
  // Using this instead of IOAnalysisMatchers to avoid uninitialized field error
  override implicit val M: Effect[IO] = IO.ioConcurrentEffect
}

trait QueriesSpec[F[_]] extends Specification with Checker[F] with ForAllTestContainer {

  private[this] val multiple = new PostgresqlMultipleDatabases(
    name = "christopherdavenport/postgres-multi-db:10.3",
    exposedPort = 5432,
    dbName = dbName,
    dbUserName = dbUserName,
    dbPassword = dbPassword
  )

  // IMPORTANT: MUST BE LAZY VAL
  override lazy val container = multiple.container

  lazy val driverName = "org.postgresql.Driver"
  lazy val dbUserName = "user"
  lazy val dbPassword = "password"
  lazy val dbName = "db"

  // This thing is a bit screwy
  lazy val transactor = Transactor.fromDriverManager[F](
    driverName,
    multiple.jdbcUrl,
    dbUserName,
    dbPassword
  )

  sequential

  // afterStart / beforeStop available for actions at the begininning
  // and end of a particular container session.
  // In this case we make sure migrations have run before we check the sql statements.
  override def afterStart(): Unit = {
    lazy val flyway = new Flyway
    flyway.setDataSource(multiple.jdbcUrl, dbUserName, dbPassword)
    flyway.setLocations("classpath:db/banno_all_migrations")
    flyway.migrate()
    ()
  }

  check(sql"SELECT 1".query[Int])

}