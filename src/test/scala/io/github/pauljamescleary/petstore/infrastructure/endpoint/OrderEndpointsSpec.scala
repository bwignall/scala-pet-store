package io.github.pauljamescleary.petstore
package infrastructure.endpoint

import domain.orders._
import infrastructure.repository.inmemory._
import cats.effect._
import cats.effect.unsafe.implicits.global
import cats.implicits._
import io.circe._
import io.circe.generic.semiauto._
import org.http4s._
import org.http4s.implicits._
import org.http4s.dsl._
import org.http4s.circe._
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.server.Router
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import tsec.mac.jca.HMACSHA256
import org.scalatest.matchers.should.Matchers

class OrderEndpointsSpec
    extends AnyFunSuite
    with Matchers
    with ScalaCheckPropertyChecks
    with PetStoreArbitraries
    with Http4sDsl[IO]
    with Http4sClientDsl[IO] {
  implicit val statusDec: EntityDecoder[IO, OrderStatus] = jsonOf
  implicit val statusEnc: EntityEncoder[IO, OrderStatus] = jsonEncoderOf

  implicit val orderEncoder: Encoder[Order] = deriveEncoder
  implicit val orderEnc: EntityEncoder[IO, Order] = jsonEncoderOf
  implicit val orderDecoder: Decoder[Order] = deriveDecoder
  implicit val orderDec: EntityDecoder[IO, Order] = jsonOf

  def getTestResources(): (AuthTest[IO], HttpApp[IO]) = {
    val userRepo = UserRepositoryInMemoryInterpreter[IO]()
    val auth = new AuthTest[IO](userRepo)
    val orderService = OrderService(OrderRepositoryInMemoryInterpreter[IO]())
    val orderEndpoint =
      OrderEndpoints.endpoints[IO, HMACSHA256](orderService, auth.securedRqHandler)
    val orderRoutes = Router(("/orders", orderEndpoint)).orNotFound
    (auth, orderRoutes)
  }

  test("place and get order") {
    val (auth, orderRoutes) = getTestResources()

    forAll { (order: Order, user: AdminUser) =>
      (for {
        createRq <- Request[IO](POST, uri"/orders")
          .withEntity(order)
          .pure[IO]
        createRqAuth <- auth.embedToken(user.value, createRq)
        createResp <- orderRoutes.run(createRqAuth)
        orderResp <- createResp.as[Order]
        getOrderRq <- Request[IO](GET, Uri.unsafeFromString(s"/orders/${orderResp.id.get}"))
          .pure[IO]
        getOrderRqAuth <- auth.embedToken(user.value, getOrderRq)
        getOrderResp <- orderRoutes.run(getOrderRqAuth)
        orderResp2 <- getOrderResp.as[Order]
      } yield {
        createResp.status shouldEqual Ok
        orderResp.petId shouldBe order.petId
        getOrderResp.status shouldEqual Ok
        orderResp2.userId shouldBe defined
      }).unsafeRunSync()
    }
  }

  test("user roles") {
    val (auth, orderRoutes) = getTestResources()

    forAll { user: CustomerUser =>
      (for {
        deleteRq <- Request[IO](DELETE, Uri.unsafeFromString(s"/orders/1"))
          .pure[IO]
          .flatMap(auth.embedToken(user.value, _))
        deleteResp <- orderRoutes.run(deleteRq)
      } yield deleteResp.status shouldEqual Unauthorized).unsafeRunSync()
    }

    forAll { user: AdminUser =>
      (for {
        deleteRq <- Request[IO](DELETE, Uri.unsafeFromString(s"/orders/1"))
          .pure[IO]
          .flatMap(auth.embedToken(user.value, _))
        deleteResp <- orderRoutes.run(deleteRq)
      } yield deleteResp.status shouldEqual Ok).unsafeRunSync()
    }
  }
}
