package spike.endpoints

import sttp.model.StatusCode

import java.util.concurrent.Executors.newWorkStealingPool
import scala.concurrent.{ExecutionContext, Future}

object EndpointImplementations {

  implicit private val ec: ExecutionContext = ExecutionContext.fromExecutor(newWorkStealingPool())

  val api: ((Future[AuthResult], SpikeHeader)) => SpikeRequest => Future[Either[(ErrorResponse, StatusCode), SpikeResponse]] = in =>
    request => {
      val (check, header) = in
      val result          = handleRequest(check, header, request)
      result.map {
        case e: ErrorResponse  => Left(e -> StatusCode(e.http_response_code))
        case r: SpikeResponse => Right(r)
      }
    }

  private val handleRequest: (Future[AuthResult], SpikeHeader, SpikeRequest) => Future[CommonResponse] =
    (_, _, body) => Future.successful(SpikeResponse(body.count + " " + body.name))

}
