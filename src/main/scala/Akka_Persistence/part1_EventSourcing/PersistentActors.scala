package Akka_Persistence.part1_EventSourcing

import akka.actor.ActorLogging
import akka.persistence.{PersistentActor, Recovery}

object PersistentActors extends App{
  class Accountant extends PersistentActor with ActorLogging {
    override def persistenceId: String = "simple-accountant"

    override def receiveCommand: Receive = ???

    override def receiveRecover: Receive = ???
  }
}
