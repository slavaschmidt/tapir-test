package spike.endpoints

import spray.json.{DefaultJsonProtocol, RootJsonFormat}
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.generic.auto._
import sttp.tapir.json.spray._

import java.util.UUID

trait CommonResponse

case class SpikeRequest(name: String, count: Int)

case class SpikeResponse(result: String) extends CommonResponse

case class ErrorResponse(http_response_code: Int, description: String) extends CommonResponse

case class SpikeHeader(correlationId: UUID, decisionId: Option[UUID])

case class AuthResult(user: Option[String])


object EndpointDefinitions extends DefaultJsonProtocol {

  implicit val requestJsonFormat: RootJsonFormat[SpikeRequest] = jsonFormat2(SpikeRequest.apply)
  implicit val responseJsonFormat: RootJsonFormat[SpikeResponse] = jsonFormat1(SpikeResponse.apply)
  implicit val errorJsonFormat: RootJsonFormat[ErrorResponse] = jsonFormat2(ErrorResponse.apply)

  private val correlationId = "correlation-id"

  private val correlationHeader = header[UUID](correlationId)

  private val spikeHeader =
    correlationHeader
      .and(header[Option[UUID]]("decision-id"))
      .map(SpikeHeader.tupled)(SpikeHeader.unapply(_).get)


  private val statusCodes = statusCode
    .description(StatusCode.BadRequest, "Client request could not be parsed or invalid")
    .description(StatusCode.Unauthorized, "Unauthorized")
    .description(StatusCode.Forbidden, "Forbidden")
    .description(StatusCode.NotFound, "Invalid URL")
    .description(StatusCode.MethodNotAllowed, "Method Not Allowed")
    .description(StatusCode.InternalServerError, "Internal Server Error")

  private val jsonBodyRequest = jsonBody[SpikeRequest].description("Request Structure").example(SpikeRequest("spike", 1))
  private val jsonBodyResponse = jsonBody[SpikeResponse].description("Response Structure").example(SpikeResponse("1 spike"))

  private def commonEndpointDefinition(v: String, desc: String) =
    endpoint
      .in("prefix" / v / "suffix")
      .post
      .in(jsonBodyRequest)
      .out(jsonBodyResponse)
      .errorOut(jsonBody[ErrorResponse])
      .errorOut(statusCodes)
      .out(statusCode(StatusCode.Ok).description("Successful response"))
      .description(s"$v, $desc")
      .securityIn(spikeHeader)
      .securityIn(auth.bearer[Option[String]]())

  val epV1: Endpoint[(SpikeHeader, Option[String], List[String]), SpikeRequest, (ErrorResponse, StatusCode), SpikeResponse, Any] =
    commonEndpointDefinition("v1", "without authentication")
      .securityIn(extractFromRequest(_.pathSegments))

  val epV2: Endpoint[(SpikeHeader, Option[String], List[String]), SpikeRequest, (ErrorResponse, StatusCode), SpikeResponse, Any] =
    commonEndpointDefinition("v2", "with authentication")
      .securityIn(paths)

}
