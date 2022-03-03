package spike.endpoints

import akka.http.scaladsl.model.RequestEntity
import akka.http.scaladsl.server.RequestContext
import akka.stream.Materializer
import org.slf4j.LoggerFactory
import sttp.model.{Header, Method, QueryParams, Uri}
import sttp.monad.MonadError
import sttp.tapir.model.{ConnectionInfo, ServerRequest}
import sttp.tapir.server.interceptor._

import scala.collection.immutable
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{Await, Future}

class StrictEntityRequestInterceptor(implicit fm: Materializer) extends RequestInterceptor[Future] {
  override def apply[B](responder: Responder[Future, B], requestHandler: EndpointInterceptor[Future] => RequestHandler[Future, B]): RequestHandler[Future, B] = {
    new RequestHandler[Future, B] {
      override def apply(nonStrict: ServerRequest)(implicit monad: MonadError[Future]): Future[RequestResult[B]] = {
        val request = RequestBuffer.prepareRequest(nonStrict)
        val next = requestHandler(EndpointInterceptor.noop)
        next(request)
      }
    }
  }
}

object RequestBuffer {
  private val logger = LoggerFactory.getLogger(getClass)

  val timeout: FiniteDuration = 5.seconds

  private def makeEntityStrict(implicit fm: Materializer): RequestEntity => RequestEntity = re =>
    Await.result(re.toStrict(timeout), timeout)

  def prepareRequest(request: ServerRequest)(implicit fm: Materializer): ServerRequest = {
    val sr = request.underlying match {
      case ctx: RequestContext if ctx.request.entity.isStrict =>
        ctx
      case ctx: RequestContext =>
        val akkaRequest = ctx.request.mapEntity(makeEntityStrict)
        ctx.withRequest(akkaRequest)
    }
    val strictRequest = new ServerRequest {
      override def protocol: String               = request.protocol
      override def connectionInfo: ConnectionInfo = request.connectionInfo
      override def underlying: RequestContext     = sr
      override def pathSegments: List[String]     = request.pathSegments
      override def queryParameters: QueryParams   = request.queryParameters
      override def method: Method                 = request.method
      override def uri: Uri                       = request.uri
      override def headers: immutable.Seq[Header] = request.headers
    }
    if (!strictRequest.underlying.request.entity.isStrict()) {
      logger.warn(s"Could not make entity strict: ${strictRequest.underlying.request}")
    } else {
      val body = strictRequest.underlying.request.entity.toStrict(timeout).value.get.get.data.utf8String
      logger.info(s"Strict entity length: ${body.length}")
    }
    strictRequest
  }
}
