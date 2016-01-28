import akka.actor._
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer

import scala.util.{Failure, Success}

object Server extends App {
  // Standard init
  implicit val system = ActorSystem()
  import system.dispatcher
  implicit val materializer = ActorMaterializer()

  val config = system.settings.config
  val interface = config.getString("server.interface")
  val port = config.getInt("server.port")

  // Create our service and bind it
  val service = new DispatchService
  val binding = Http().bindAndHandle(service.route, interface, port)

  // Log the result of the binding...
  binding.onComplete {
    case Success(binding) ⇒
      val localAddress = binding.localAddress
      println(s"Server is listening on ${localAddress.getHostName}:${localAddress.getPort}")
    case Failure(e) ⇒
      println(s"Binding failed with ${e.getMessage}")
      system.shutdown()
  }
}