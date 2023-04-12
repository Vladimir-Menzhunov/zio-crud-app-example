package api

import io.circe.generic.codec.DerivedAsObjectCodec.deriveCodec
import io.circe.jawn._
import io.circe.syntax.EncoderOps
import model.Customer
import repo.CustomerRepository
import zio.ZIO
import zio.http._
import zio.http.model.{Method, Status}

object HttpRoutes {
  val app: HttpApp[CustomerRepository, Response] =
    Http.collectZIO[Request] {
      case Method.GET -> !! / "customers" =>
        CustomerRepository
          .findAll()
          .runCollect
          .map(chunk => chunk.toArray)
          .tapError(e => ZIO.logError(e.getMessage))
          .either
          .map {
            case Right(customers) => Response.json(customers.asJson.spaces2)
            case Left(e) => Response.status(Status.InternalServerError)
          }

      case req @ Method.POST -> !! / "add" / "customer" =>
        (for {
          bodyStr <- req.body.asString
          customer <-
            ZIO.fromEither(decode[Customer](bodyStr)).tapError(e => ZIO.logError(e.getMessage))
          _ <- CustomerRepository.add(customer)
          _ <- ZIO.logInfo(s"Created new customer $customer")
        } yield ()).either.map {
          case Right(_) => Response.status(Status.Created)
          case Left(_) => Response.status(Status.BadRequest)
        }

      case req @ Method.PUT -> !! / "update" / "customer" =>
        (for {
          bodyStr <- req.body.asString
          id <- ZIO.fromOption(req.url.queryParams.get("id").flatMap(_.headOption)).tapError(_ => ZIO.logError("not provide id"))
          customerUpdate <-
            ZIO.fromEither(decode[Customer](bodyStr)).tapError(e => ZIO.logError(e.getMessage))
          _ <- CustomerRepository.update(customerUpdate)
          _ <- ZIO.logInfo(s"Update customer $customerUpdate, id $id")
        } yield ()).either.map {
          case Right(_) => Response.status(Status.Created)
          case Left(_) => Response.status(Status.BadRequest)
        }

      case req@Method.DELETE -> !! / "delete" / "customer" =>
        (for {
          id <- ZIO.fromOption(req.url.queryParams.get("id").flatMap(_.headOption)).tapError(_ => ZIO.logError("not provide id"))
          _ <- CustomerRepository.delete(id.toInt)
          _ <- ZIO.logInfo(s"Delete customer by id $id")
        } yield ()).either.map {
          case Right(_) => Response.status(Status.Created)
          case Left(_) => Response.status(Status.BadRequest)
        }
    }
}
