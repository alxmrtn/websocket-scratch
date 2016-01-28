import akka.actor.{Terminated, Actor, ActorRef}

object DispatchActor {
  trait DispatchEvent
  case class Subscribe(sender: String, subscriber: ActorRef) extends DispatchEvent
  case class Unsubscribe(sender: String) extends DispatchEvent
  case class ReceivedMessage(sender: String, msg: String) extends DispatchEvent
}

class DispatchActor extends Actor {
  import DispatchActor._
  // The "state" of this actor, a mutable, changing set of ActorRefs, each representing a websocket connection
  var subscribers = Set.empty[(String, ActorRef)]

  // For each of the subscribers, send the passed message
  def broadcast(msg: ReceivedMessage) = subscribers.foreach(_._2 ! msg)

  def receive: Receive = {
    // On a Subscribe message, register the ActorRef and User in our state
    case Subscribe(name, subscriber) =>
      subscribers = subscribers + ((name, subscriber))
    // On an Unsubscribe message, remove the ActorRef from the state
    case Unsubscribe(name) =>
      subscribers = subscribers.filterNot(_._1 == name)
    // This would be sent by the system, and covered by our Unsubscribe case
    // but good to implement to prevent sending dead letters
    case Terminated(subscriber) =>
      subscribers = subscribers.filterNot(_._2 == subscriber)
    // On a received message, broadcast it to all of the registered Actors
    case msg: ReceivedMessage =>
      broadcast(msg)
  }
}