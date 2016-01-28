import akka.actor.{Props, ActorSystem}
import akka.http.scaladsl.model.ws.{TextMessage, Message}
import akka.http.scaladsl.server.Directives
import akka.stream.{OverflowStrategy, Materializer}
import akka.stream.scaladsl.{Source, Flow, Sink}
import DispatchActor._

class DispatchService(implicit fm: Materializer, system: ActorSystem) extends Directives {
  // Currently only one DispatchActor per service
  // This actor is what holds a reference to all the other actors subscribed
  lazy val dispatchActor = system.actorOf(Props[DispatchActor])

  // Wrapping our dispatchActor in a sink, all elements in stream will be sent to this actor
  // We instruct the sink send an Unsubscribe message when the stream terminates
  def dispatchInSink(sender: String) = Sink.actorRef[DispatchEvent](dispatchActor, Unsubscribe(sender))

  // Constructs and returns a flow that takes a received message, dispatches it to all subscribed actors
  def dispatchActorFlow(sender: String): Flow[ReceivedMessage, ReceivedMessage, Unit] = {
    val in = Flow[ReceivedMessage].to(dispatchInSink(sender))

    // When the flow is materialized for the first time (websocket connect) we create an actor for that flow
    // The newly created ActorRef is sent to our DispatchActor in a subscribe message so the DispatchActor will
    // broadcast new messages to this actor
    val out = Source.actorRef[ReceivedMessage](1, OverflowStrategy.fail)
      .mapMaterializedValue(subscriberActorRef => dispatchActor ! Subscribe(sender, subscriberActorRef))

    Flow.fromSinkAndSource(in, out)
  }

  // The flow from beginning to end to be passed into handleWebsocketMessages
  def websocketDispatchFlow(sender: String): Flow[Message, Message, Unit] =
    Flow[Message]
      // First we convert the TextMessage to a ReceivedMessage
      .collect { case TextMessage.Strict(msg) => ReceivedMessage(sender, msg) }
      // Then we send the message to the dispatch actor which fans it out
      .via(dispatchActorFlow(sender))
      // The message is converted back to a TextMessage for serialization across the socket
      .map { case ReceivedMessage(from, msg) => TextMessage.Strict(s"$from: $msg") }

  def route =
    (get & path("chat") & parameter('name)) { name =>
      handleWebsocketMessages(websocketDispatchFlow(sender = name))
    }
}