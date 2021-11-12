package io.chrisdavenport.testcontainersspecs2

import java.util.UUID

import cats.effect._
import doobie._
import doobie.implicits._
import doobie.postgres.implicits._
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

  private case class Person(person_id: UUID, firstName: String, lastName: String)

  check(sql"SELECT 1".query[Int])
  check(sql"SELECT person_id, first_name, last_name FROM persons".query[Person])

}
