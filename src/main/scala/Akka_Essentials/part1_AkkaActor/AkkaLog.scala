package Akka_Essentials.part1_AkkaActor

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.event.Logging

object AkkaLog extends App{
  //#1: Explicit logging
  class SimpleActorWithExplicitLogger extends Actor {
    var logger = Logging(context.system, this)
    override def receive: Receive = {
          /*
          Level 1: Debug
          Level 2: Info
          Level 3: Warning
          Level 4: Error
          * */
      case message => logger.info(message.toString)
    }
  }
  val system = ActorSystem("AkkaLog")
  val loggingDemo = system.actorOf(Props[SimpleActorWithExplicitLogger])
  loggingDemo ! "Logging simple message"

  //#2 Actor Logging
  class ActorWithLogging extends Actor with ActorLogging{
    override def receive: Receive = {
      case (a, b) => log.info("Two things: {} and {} ", a, b) //=> In practical using so much - remember it
      case message => log.info(message.toString)
    }
  }

  val simpleActor = system.actorOf(Props[ActorWithLogging])
  simpleActor ! "Logging simple message by extending a trait"
  simpleActor ! (24,"Hello")
}
