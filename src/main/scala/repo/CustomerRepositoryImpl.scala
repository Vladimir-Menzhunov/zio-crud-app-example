package repo

import model.Customer
import zio.{ZIO, ZLayer}
import zio.sql.ConnectionPool
import zio.stream.ZStream

final class CustomerRepositoryImpl(
    pool: ConnectionPool
) extends CustomerRepository
    with PostgresTableDescription {

  val driverLayer: ZLayer[Any, Nothing, SqlDriver] =
    ZLayer
      .make[SqlDriver](
        SqlDriver.live,
        ZLayer.succeed(pool)
      )

  override def findAll(): ZStream[Any, Throwable, Customer] = {
    val selectAll =
      select(customerId, fName, lName).from(customers)

    ZStream.fromZIO(
      ZIO.logInfo(s"Query to execute findAll is ${renderRead(selectAll)}")
    ) *>
      execute(selectAll.to((Customer.apply _).tupled))
        .provideSomeLayer(driverLayer)
  }

  override def add(customer: Customer): ZIO[Any, Throwable, Unit] = {
    val query =
      insertInto(customers)(customerId, fName, lName)
        .values(
          (
            customer.id,
            customer.firstName,
            customer.lastName
          )
        )

    ZIO.logInfo(s"Query to insert customer is ${renderInsert(query)}") *>
      execute(query)
        .provideSomeLayer(driverLayer)
        .unit
  }

  override def updateCustomer(
      customer: Customer
  ): ZIO[Any, Throwable, Unit] = {
    val query =
      update(customers)
      .set(fName, customer.firstName)
        .set(lName, customer.lastName)
        .where(customerId === customer.id)
        //.where(Expr.Relational(customerId, customer.id, RelationalOp.Equals))

    ZIO.logInfo(s"Query to update customer is ${renderUpdate(query)}") *>
      execute(query)
        .provideSomeLayer(driverLayer)
        .unit
  }

  override def delete(id: Int): ZIO[Any, Throwable, Unit] = {
    val delete = deleteFrom(customers).where(customerId === id)
    execute(delete)
      .provideSomeLayer(driverLayer)
      .unit
  }
}

object CustomerRepositoryImpl {
  val live: ZLayer[ConnectionPool, Throwable, CustomerRepository] =
    ZLayer.fromFunction(new CustomerRepositoryImpl(_))
}
