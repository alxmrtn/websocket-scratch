
import java.io.File

import akka.actor.ActorSystem
import akka.http.javadsl.model.ws.Message
import akka.stream.scaladsl._
import akka.stream.{ActorMaterializer, ClosedShape}
import akka.util.ByteString

import scala.util.{Failure, Success}

object StdInFlow {
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("Sys")
    import system.dispatcher
    implicit val materializer = ActorMaterializer()

    // take in from stdin
    val stdinSource = Source.fromIterator(io.Source.stdin.getLines)

    // dump to console
    val consoleSink = Sink.foreach[String](println)

    // clear the file every time we run
    val file = new File("target/echo.txt")
    file.delete()
    file.createNewFile()

    // also dump to file
    val fileSink = Flow[String]
      .map(i => ByteString(i.toString + "\n"))
      .toMat(FileIO.toFile(file))((_, bytesWritten) => bytesWritten)

    // idk how the fuck this works
    val graph = GraphDSL.create(consoleSink, fileSink)((_, file) => file) { implicit builder => (console, file) =>
      import GraphDSL.Implicits._

      // would this need to be dynamic in our case?
      val broadcast = builder.add(Broadcast[String](2))

      stdinSource ~> broadcast.in

      broadcast.out(0) ~> file
      broadcast.out(1) ~> console

      ClosedShape
    }

    // and then we do it
    println("Enter anything: \n")
    val materialized = RunnableGraph.fromGraph(graph).run()

    materialized.onComplete {
      case Success(_) =>
        system.shutdown()
      case Failure(e) =>
        println(s"Failure: ${e.getMessage}")
        system.shutdown()
    }
  }
}