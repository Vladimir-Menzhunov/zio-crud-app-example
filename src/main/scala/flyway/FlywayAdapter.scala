package flyway

import config.Config
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.FlywayException
import org.flywaydb.core.api.output.MigrateResult
import zio._

object FlywayAdapter {
  trait Service extends {
    def migration: IO[FlywayException, MigrateResult]
  }

  val live: ZLayer[Config.Service, Nothing, FlywayAdapter.Service] =
    ZLayer.fromFunction(new FlywayAdapterImpl(_))
}

class FlywayAdapterImpl(config: Config.Service) extends FlywayAdapter.Service {
  val flyway: UIO[Flyway] = {
    val dbConfig = config.dbConfig
    ZIO
      .succeed(
        Flyway
          .configure()
          .dataSource(dbConfig.url, dbConfig.user, dbConfig.password)
      )
      .map(new Flyway(_))
  }

  override def migration: IO[FlywayException, MigrateResult] =
    flyway.map(_.migrate())
}
