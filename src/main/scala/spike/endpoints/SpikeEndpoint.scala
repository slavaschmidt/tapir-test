package spike.endpoints

import spike.endpoints.EndpointDefinitions.epV2
import sttp.tapir.docs.openapi.OpenAPIDocsInterpreter
import sttp.tapir.openapi.{Info, OpenAPI}
import sttp.tapir.openapi.circe.yaml._

object SpikeEndpoint {
  private val info = Info(title = "Spike", version = "0.1", description = Option("Spike"))
  val openApi: OpenAPI = OpenAPIDocsInterpreter().toOpenAPI(List(epV2).map(_.tag("spike")), info)
  val yml: String = openApi.toYaml
}
