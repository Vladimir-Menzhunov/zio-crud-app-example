import api.HttpRoutes
import config.Config
import flyway.FlywayAdapter
import repo.CustomerRepositoryImpl
import zio.http.{ConnectionPoolConfig, Server}
import zio.sql.ConnectionPool
import zio.{Scope, ZIO, ZIOAppArgs, ZIOAppDefault, http}

import scala.language.postfixOps

object StartApp extends ZIOAppDefault {
  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = {
    val server = for {
      flyway <- ZIO.service[FlywayAdapter.Service]
      _ <- flyway.migration
      server <- zio.http.Server.serve(HttpRoutes.app)
    } yield server

    server.provide(
      Config.live,
      FlywayAdapter.live,
      Server.live,
      Config.serverLive,
      CustomerRepositoryImpl.live,
      ConnectionPool.live,
      Config.connectionPoolConfigLive
    )
  }
}
