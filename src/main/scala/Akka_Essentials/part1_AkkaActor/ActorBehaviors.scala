package Akka_Essentials.part1_AkkaActor

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ActorBehaviors extends App{
  //Started learning
  // I watching video from another computer, atr Udemy course
  // Have error at Scala version !!!
  class SimpleActor extends Actor{

    override def receive: Receive = {
      case "Hi!" => context.sender() ! "Hello, there"
      case message: String => println(s"[$self] I have received a message $message")
      case SayHiTo(ref) => ref ! "Hi!"
      case WirelessPhoneMessage(content,ref) => ref forward (content + "s") // keep the origin sender
    }
  }

  val actorSystem = ActorSystem("ActorBehaviorSystem") // Add actor name in " "
  val simpleActor = actorSystem.actorOf(Props[SimpleActor], "SimpleActor")

  simpleActor ! "hello world"


  //Some actor behaviors
  //1. message can be of any type
  simpleActor ! 25 // Type of number
  case class SpecialMessage(content: String)
  simpleActor ! SpecialMessage("Content from special message")

  //Note. Message must be immutable -> const, let in some other programming language like C++, Swift, C#,...
  //Note. message must be Serializable
  //Note. In practical use case class and case object

  //2. actor have information about their context
  //This actor information: Actor[akka://ActorBehaviorSystem/user/SimpleActor#1134737449]
  //context.self === this( In C++)

  case class SendMessageToYourSelf(content: String)
  simpleActor ! SendMessageToYourSelf("Sent this message for me")

  //3 Actors can reply to messages
  val alice = actorSystem.actorOf(Props[SimpleActor], "alice")
  val bob = actorSystem.actorOf(Props[SimpleActor], "bob")

  case class SayHiTo(ref: ActorRef)
  alice ! SayHiTo(bob)
   //From bob actor: [Actor[akka://ActorBehaviorSystem/user/bob#-1722883505]] I have received a message Hi!

  //4. Dead letters
  alice ! "Hi!" // Reply to "me"

  //5 Forwarding messages
  // Q -> A -> B
  //forwarding = sending a messages with the ORIGINAL sender
  case class WirelessPhoneMessage(content: String, ref: ActorRef)
  alice ! WirelessPhoneMessage("Hi!",bob)
}


