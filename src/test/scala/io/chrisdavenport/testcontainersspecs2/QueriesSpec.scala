package io.chrisdavenport.testcontainersspecs2

import cats.effect._
import doobie._
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.specs2._
import org.flywaydb.core.Flyway
import org.specs2.mutable.Specification
import java.util.UUID

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

  private case class Person(person_id: UUID, firstName: String, lastName: String)

  check(sql"SELECT 1".query[Int])
  check(sql"SELECT person_id, first_name, last_name FROM persons".query[Person])

}