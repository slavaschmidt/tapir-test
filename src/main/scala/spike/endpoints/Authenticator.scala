package spike.endpoints

import scala.concurrent.Future

object Authenticator {

  val noSecurity: ((SpikeHeader, Option[String], List[String])) => Future[(Future[AuthResult], SpikeHeader)] = params =>
    Future.successful(Future.successful(AuthResult(None)) -> params._1)

  val security: ((SpikeHeader, Option[String], List[String])) => Future[(Future[AuthResult], SpikeHeader)] = params =>
    Future.successful(Future.successful(AuthResult(params._2.filter(_.length > 5))) -> params._1)
}
