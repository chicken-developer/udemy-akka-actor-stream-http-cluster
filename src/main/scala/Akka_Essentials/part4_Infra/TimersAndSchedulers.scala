package Akka_Essentials.part4_Infra

import akka.actor.{Actor, ActorLogging, ActorSystem, Cancellable, Props, Timers}

import scala.concurrent.duration._

object TimersAndSchedulers extends App{
  class SimpleActor extends Actor with ActorLogging{
    override def receive: Receive = {
      case message => log.info(message.toString)
    }
  }
  val system = ActorSystem("TimerAndSchedulersSystem")
  val simpleActor = system.actorOf(Props[SimpleActor], "simpleActor")
  system.log.info("Scheduling reminder for simpleActor") // same like case _ => log.info(message)

  // implicit val executionContext = system.dispatcher
//   import system.dispatcher
//  system.scheduler.scheduleOnce(1 second){
//    simpleActor ! "reminder"
  // } // (system.dispatcher) // If have implicit val or import, don't need this line

//  val routine: Cancellable = system.scheduler.schedule(1 second,2 second){
//    simpleActor ! "heartbeat"
    // 1 second is  time start action after system start
    // 2 second is time start action after previous action shutdown
    // It will repeat forever
    // Add ": Callable" will enable this scheduler can be cancel by this name
  //}
//  system.scheduler.scheduleOnce(5 second){
//    routine.cancel()
//  }

  //Exercise
//  import system.dispatcher
//  class SimpleSystemActor extends Actor with ActorLogging{
//   def createTimeOutWindows(): Cancellable = {
//     context.system.scheduler.scheduleOnce(1 second){
//       self ! "timeout"
//     }
//   }
//    var scheduler = createTimeOutWindows()
//    override def receive: Receive = {
//      case "timeout" =>
//        log.info("Stopping myself")
//        context.stop(self)
//      case message =>
//        log.info(s"Received a message: $message, still live here")
//        scheduler.cancel()
//        scheduler = createTimeOutWindows()
//    }
//  }
//
//  val simpleSystemActor = system.actorOf(Props[SimpleSystemActor])
//  system.scheduler.scheduleOnce(250 milli){
//    simpleSystemActor ! "message here very important"
//  }
//  system.scheduler.scheduleOnce(2 second){
//    simpleSystemActor ! "message here very important"
//  }
//  system.scheduler.scheduleOnce(5 second){
//    simpleSystemActor ! "message here very important"
//  }

  /*
  Scheduler is very hard to maintain
  ==> Timer
   */
  case object TimerKey // For management timer likes: start, stop, restart,...
  case object Start // message want to send
  case object Reminder // another message
  case object Stop
  class TimerBasedHeartbeatActor extends Actor with ActorLogging with Timers{
    timers.startSingleTimer(TimerKey,Start, 500 milli)

    override def receive: Receive = {
      case Start =>
        log.info("Bootstrapping")
        timers.startPeriodicTimer(TimerKey, Reminder, 1 second)
        // 500 milli is  time start timerActor after system start
        // 1 second is time start timerAction after previous timerAction shutdown
        // Timer automatic canceled its self after call next
      case Reminder =>
        log.info("I still alive")
      case Stop =>
        log.warning("Stopping myself")
        timers.cancel(TimerKey)
        context.stop(self)
    }
  }

  val timerBasedHeartbeatActor = system.actorOf(Props[TimerBasedHeartbeatActor])
  system.scheduler.scheduleOnce(5 second){
    timerBasedHeartbeatActor ! Stop
  }(system.dispatcher)
}
