package Akka_Essentials.part1_AkkaActor

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ChildActor extends App {
  //Actor can create other actor
  object Parent{
    case class CreateChild(childName: String)
    case class Tell(message: String)
  }
  class Parent extends Actor{
    import Parent._
    override def receive: Receive = {
      case CreateChild(childName) =>
        println(s"${self.path} Creating a child")
        val childRef = context.actorOf(Props[Child],childName)
        context.become(withChild(childRef))
    }
    def withChild(childRef: ActorRef): Receive ={
      case Tell(message) =>
        if(childRef != null) childRef forward message
    }
  }

  class Child extends Actor{
    override def receive: Receive ={
      case message => println(s"${self.path} I got: $message")
    }
  }

  val system = ActorSystem("ParentChildSystem")
  val parent = system.actorOf(Props[Parent], "parent")
//  val child = system.actorOf(Props[Child], "child")
  import Parent._
  parent ! CreateChild("newChild")
  val child = system.actorSelection("/user/ParentChildSystem/newChild")
  parent ! Tell("hey kid")
  child ! "Hello"


}
