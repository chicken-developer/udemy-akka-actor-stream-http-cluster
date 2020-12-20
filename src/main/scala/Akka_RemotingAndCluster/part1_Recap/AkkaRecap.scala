package Akka_RemotingAndCluster.part1_Recap

import akka.actor.{Actor, ActorSystem, Props}

object AkkaRecap extends App {
  class SimpleActor extends Actor {
    override def receive: Receive = {
      case message => println(s"I received a message: $message")
    }
  }

  val actorSystem = ActorSystem("SimpleActor")
  val simpleActor = actorSystem.actorOf(Props[SimpleActor], "simpleActor")
  simpleActor ! "simple message"
}
