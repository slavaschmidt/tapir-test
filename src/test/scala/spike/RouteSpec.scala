package spike

import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.Checkpoints
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class RouteSpec extends AnyWordSpec with Matchers with ScalatestRouteTest with Checkpoints {

  private val correlationIdHeader = RawHeader("correlation-id", "33333333-3333-3333-3333-333333333333")

  private val route = Route.seal(Routes.tapirRoute)
  private val payload = """{"name": "hey", "count": 1}"""

  "The API definition" should {
    "be instantiated" in {
      Get("/prefix/v3/suffix").withHeaders(correlationIdHeader) ~> route ~> check {
        val resp = responseAs[String]
        status shouldEqual StatusCodes.NotFound
        resp shouldEqual """The requested resource could not be found."""
      }
    }
    "find API v1 path but no GET method" in {
      Get("/prefix/v1/suffix").withHeaders(correlationIdHeader) ~> route ~> check {
        status shouldEqual StatusCodes.MethodNotAllowed
      }
    }
    "find API v1 path using `.securityIn(extractFromRequest(_.pathSegments))`" in {
      Post("/prefix/v1/suffix", HttpEntity(ContentTypes.`application/json`, payload)).withHeaders(correlationIdHeader) ~> route ~> check {
        status shouldEqual StatusCodes.OK
      }
    }
    "not recognise security suffix in v1 URL with `.securityIn(extractFromRequest(_.pathSegments))`" in {
      Post("/prefix/v1/suffix/security", HttpEntity(ContentTypes.`application/json`, payload)).withHeaders(correlationIdHeader) ~> route ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }

    // this test fails
    "find unauthorized API v2 path using `.securityIn(paths)`" in {
      Post("/prefix/v2/suffix", HttpEntity(ContentTypes.`application/json`, payload)).withHeaders(correlationIdHeader) ~> route ~> check {
        status shouldEqual StatusCodes.Unauthorized // 404 Not Found returned by tapir
      }
    }

    // this test fails
    "recognise security suffix in v2 path using `.securityIn(paths)`" in {
      Post("/prefix/v2/suffix/security", HttpEntity(ContentTypes.`application/json`, payload)).withHeaders(correlationIdHeader) ~> route ~> check {
        status shouldEqual StatusCodes.Unauthorized // 404 Not Found returned by tapir
      }
    }

  }
}