package spike

import akka.actor.{ActorSystem, CoordinatedShutdown}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server._
import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory
import spike.endpoints.{AuthResult, Authenticator, EndpointDefinitions, EndpointImplementations, SpikeEndpoint, SpikeHeader}
import sttp.capabilities.WebSockets
import sttp.capabilities.akka.AkkaStreams
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.akkahttp.AkkaHttpServerInterpreter

import java.util.concurrent.Executors.newWorkStealingPool
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success}

class HttpServer(serviceHost: String, servicePort: Int, route: Route)(implicit as: ActorSystem) {
  def start(): Future[Http.ServerBinding] = Http().newServerAt(serviceHost, servicePort).bindFlow(route)
  def shutdown(): Unit = Await.ready(as.terminate(), 5.seconds)
}

object Routes extends Directives {

    implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(newWorkStealingPool())

    val tapirEp1: ServerEndpoint[AkkaStreams with WebSockets, Future] =
      EndpointDefinitions.epV1
        .serverSecurityLogicSuccess[(Future[AuthResult], SpikeHeader), Future](Authenticator.security)
        .serverLogic(EndpointImplementations.api)
        .name("v1")

    val tapirEp2: ServerEndpoint[AkkaStreams with WebSockets, Future] =
      EndpointDefinitions.epV2
        .serverSecurityLogicSuccess[(Future[AuthResult], SpikeHeader), Future](Authenticator.security)
        .serverLogic(EndpointImplementations.api)
        .name("v2")

    val pureTapirRoute: Route = AkkaHttpServerInterpreter().toRoute(List(tapirEp2, tapirEp1))

    val tapirRoute: Route = pathPrefixTest("prefix")(pureTapirRoute)

    val routes: Route =
      path("swagger")(get(complete(SpikeEndpoint.yml))) ~
        path("openapi")(get(complete(SpikeEndpoint.yml))) ~
        tapirRoute
}
object HttpServer extends App {
  private val logger = LoggerFactory.getLogger(getClass)
  private val config = ConfigFactory.load()
  implicit private val actorSystem = ActorSystem("spike", config)
  private val apiServer = new HttpServer("localhost", 8080, Routes.routes)

  implicit private val ec: ExecutionContext = actorSystem.dispatcher

  case object UserInitiatedShutdown extends CoordinatedShutdown.Reason

  sys.addShutdownHook(CoordinatedShutdown(actorSystem).run(UserInitiatedShutdown))

  apiServer.start()
    .onComplete {
      case Failure(throwable) =>
        logger.error("Could not start HTTP server", throwable)
        actorSystem.terminate()
      case Success(serverBindings) =>
        logger.info(s"HTTP server(s) started: $serverBindings")
    }
}
