package io.github.pauljamescleary.petstore

import config._
import domain.users._
import domain.orders._
import domain.pets._
import infrastructure.endpoint._
import infrastructure.repository.doobie.{
  DoobieAuthRepositoryInterpreter,
  DoobieOrderRepositoryInterpreter,
  DoobiePetRepositoryInterpreter,
  DoobieUserRepositoryInterpreter
}
import cats.effect._
import org.http4s.server.{Router, Server => H4Server}
import org.http4s.ember.server._
import org.http4s.implicits._
import tsec.passwordhashers.jca.BCrypt
import doobie.util.ExecutionContexts
import io.circe.config.parser
import domain.authentication.Auth
import tsec.authentication.SecuredRequestHandler
import tsec.mac.jca.HMACSHA256
import com.comcast.ip4s.{Host, Port}
import fs2.io.net.Network

object Server extends IOApp {
  def createServer[F[_]: Async: Network]: Resource[F, H4Server] =
    for {
      conf <- Resource.eval(parser.decodePathF[F, PetStoreConfig]("petstore"))
      host = conf.server.host
      port = conf.server.port
      _ <- ExecutionContexts.cachedThreadPool[F]
      connEc <- ExecutionContexts.fixedThreadPool[F](conf.db.connections.poolSize)
      _ <- ExecutionContexts.cachedThreadPool[F]
      xa <- DatabaseConfig.dbTransactor(conf.db, connEc)
      key <- Resource.eval(HMACSHA256.generateKey[F])
      authRepo = DoobieAuthRepositoryInterpreter[F, HMACSHA256](key, xa)
      petRepo = DoobiePetRepositoryInterpreter[F](xa)
      orderRepo = DoobieOrderRepositoryInterpreter[F](xa)
      userRepo = DoobieUserRepositoryInterpreter[F](xa)
      petValidation = PetValidationInterpreter[F](petRepo)
      petService = PetService[F](petRepo, petValidation)
      userValidation = UserValidationInterpreter[F](userRepo)
      orderService = OrderService[F](orderRepo)
      userService = UserService[F](userRepo, userValidation)
      authenticator = Auth.jwtAuthenticator[F, HMACSHA256](key, authRepo, userRepo)
      routeAuth = SecuredRequestHandler(authenticator)
      httpApp = Router(
        "/users" -> UserEndpoints
          .endpoints[F, BCrypt, HMACSHA256](userService, BCrypt.syncPasswordHasher[F], routeAuth),
        "/pets" -> PetEndpoints.endpoints[F, HMACSHA256](petService, routeAuth),
        "/orders" -> OrderEndpoints.endpoints[F, HMACSHA256](orderService, routeAuth)
      ).orNotFound
      _ <- Resource.eval(DatabaseConfig.initializeDb(conf.db))
      server <- EmberServerBuilder
        .default[F]
        .withHostOption(Host.fromString(host))
        .withPort(
          Port
            .fromInt(port)
            .orElse(Port.fromInt(org.http4s.server.defaults.HttpPort))
            .get
        )
        .withHttpApp(httpApp)
        .build
    } yield server

  def run(args: List[String]): IO[ExitCode] = createServer[IO]
    .use(_ => IO.never)
    .as(ExitCode.Success)
}
