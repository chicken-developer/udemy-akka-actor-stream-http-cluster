package Akka_Essentials.part5_Patterns

import akka.actor.{Actor, ActorLogging, ActorSystem, Props, Stash}

object StashDemo extends App{
/*
  ResourceActor
   - open => it can receive read/ write requests to the resource
   - otherwise it will postpone( delay, pause) all read/ write requests until the state is open

   Default ResourceActor state is Close
    - Open => switch to Open state
    - Read/ Write resource is postpone
   ResourceActor is Open State
    - Read/ Write are handled
    - Close => Switch to Close state
    []
 */

  case object Open
  case object Close
  case object Read
  case class Write(data: String)

  //. Step 1: Mix-in the Stash trait( class MyActor extends Actor with Stash)
  class ResourceActor extends Actor with ActorLogging with Stash {
    private var innerData: String = ""

    override def receive: Receive = close
    def close: Receive = {
      case Open =>
        log.info("Opening resource")
        //. Step 3: unstashAll when switch the message handler
        unstashAll()
        context.become(open)
      case message =>
        log.info(s"Stashing $message because can't handle it while in close state")
        //. Step 2: Stash away what you can't handle
        stash()
    }
    def open: Receive ={
      case Read =>
        log.info(s"Reading data: $innerData")
      case Write(data) =>
        log.info("I' m writing data")
        innerData = data
      case Close =>
        log.info("Closing resource")
        unstashAll()
        context.become(close)
      case message =>
        log.info(s"Stashing $message because can't handle it while in open state")
        //. Step 2: Stash away what you can't handle
        stash()
    }
  }

  val system = ActorSystem("StashDemo")
  val resourceActor = system.actorOf(Props[ResourceActor])

  resourceActor ! Read // Stashed
  resourceActor ! Open // Switch to open -> println to console: Reading data: ""
  resourceActor ! Open // Stash
  resourceActor ! Write("Important data") //Still open -> println to console I' m writing data
  resourceActor ! Close // Switch to close, but in close stash open -> so open again
  resourceActor ! Read // println to console: Reading data: "Important data"


}
