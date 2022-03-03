package spike.endpoints

import akka.actor.ActorSystem
import sttp.tapir.server.akkahttp.AkkaHttpServerOptions

class AkkaHttpOptions(implicit as: ActorSystem) {
  val requestInterceptor = new StrictEntityRequestInterceptor

  val customServerOptions: AkkaHttpServerOptions = AkkaHttpServerOptions.customInterceptors
    .addInterceptor(requestInterceptor)
    .options
}

