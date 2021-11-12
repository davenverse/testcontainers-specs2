package io.chrisdavenport.testcontainersspecs2

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
