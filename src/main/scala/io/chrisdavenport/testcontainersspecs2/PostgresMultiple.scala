package io.chrisdavenport.testcontainersspecs2

import com.dimafeng.testcontainers.{ Container, GenericContainer }
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy
import java.time.Duration
import java.time.temporal.ChronoUnit.SECONDS

/** A mix-in trait, which can be used with ForAllTestContainer or ForEachTestContainer,
  * that uses a container based on https://github.com/mrts/docker-postgresql-multiple-databases
  */
trait UsesPostgresqlMultipleDatabases { self: { def container: Container } =>

  override lazy val container = new PostgresqlMultipleDatabases(
    name = "christopherdavenport/postgres-multi-db:10.3",
    exposedPort = 5432,
    dbName = dbName,
    dbUserName = dbUserName,
    dbPassword = dbPassword
  )

  lazy val driverName = "org.postgresql.Driver"
  lazy val dbUserName = "user"
  lazy val dbPassword = "password"
  lazy val dbName = "db"
  lazy val jdbcUrl = container.jdbcUrl

}

final class PostgresqlMultipleDatabases(
  name: String,
  exposedPort: Int,
  dbName: String,
  dbUserName: String,
  dbPassword: String
){

  lazy val container: GenericContainer = GenericContainer(
    name,
    exposedPorts = Seq(exposedPort),
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

  lazy val ipAddress = container.containerIpAddress
  lazy val mappedPort = container.mappedPort(exposedPort)
  lazy val jdbcUrl: String = s"jdbc:postgresql://$ipAddress:$mappedPort/$dbName"
}
