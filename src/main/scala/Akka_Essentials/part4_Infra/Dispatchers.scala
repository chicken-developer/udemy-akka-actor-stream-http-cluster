package Akka_Essentials.part4_Infra

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import com.typesafe.config.ConfigFactory

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

object Dispatchers extends App {
  class Counter extends Actor with ActorLogging{
    var count: Int = 0
    override def receive: Receive = {
      case message =>
        count += 1
        log.info(s"[$count] $message")
    }
  }
  val system = ActorSystem("DispatcherDemo")//, ConfigFactory.load().getConfig("dispatcherDemo"))

  //Method 01: Programmatic/ In code
  val simpleCounters = for(i <- 1 to 10) yield system.actorOf(Props[Counter].withDispatcher("my-dispatcher"),s"counter_$i")
//  val r = new Random()
//  for(i <- 1 to 1000){
//    simpleCounters(r.nextInt(10)) ! i
//  }

  //Method 2: From config
  val quynhActor = system.actorOf(Props[Counter], "nguyenmanhquynh")

  /**
   * Dispatchers implement the ExecutionContext trait
   */

  class DBActor extends Actor with ActorLogging{
  // implicit val excutionContext: ExecutionContext = context.dispatcher - default
    //=> That will make all actors in system wait
    //#Solution 1 : context.system.dispatchers.lookup("my-dispatcher")
  implicit val excutionContext: ExecutionContext = context.system.dispatchers.lookup("my-dispatcher")
    override def receive: Receive = {
      case message => Future {
        // Wait on resources
        Thread.sleep(5000)
        log.info(s"Success: $message")
      }
    }
  }

  val dBActor = system.actorOf(Props[DBActor], "dbActor")
//  dBActor ! "Mean of the life is love"
  val nonBlockingActor = system.actorOf(Props[Counter],"nonblockingActor")
  for(i <- 1 to 1000){
    val message = s"Counting one, two, three...and $i"
    dBActor ! message
    nonBlockingActor ! message
  }

}
