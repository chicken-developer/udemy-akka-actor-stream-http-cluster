package Akka_Essentials.part3_Faulttolerance

import java.io.File
import akka.actor.SupervisorStrategy.{Escalate, Restart, Resume, Stop}
import scala.concurrent.duration._
import akka.actor.{Actor, ActorLogging, ActorSystem, OneForOneStrategy, Props}
import akka.pattern.{Backoff, BackoffSupervisor}
import scala.concurrent.duration._
import scala.io.Source

object BackoffSupervisorPattern extends App{
  case object ReadFile
  class FileBasePersistentActor extends Actor with ActorLogging {
    var dataSource: Source = null

    override def preStart(): Unit =
      log.info("Persistent actor starting")

    override def postStop(): Unit =
      log.warning("Persistent actor has stopped")

    override def preRestart(reason: Throwable, message: Option[Any]): Unit =
      log.warning("Persistent actor restarting")
    override def receive: Receive = {
      case ReadFile =>
        if(dataSource == null)
          dataSource = Source.fromFile(new File("src/main/resources/textfile/important_data.txt"))
        log.info("I 've just read some IMPORTANT data: " + dataSource.getLines().toList)

    }
  }

  val system = ActorSystem("BackoffSupervisorDemo")
//  val simpleActor = system.actorOf(Props[FileBasePersistentActor], "simpleActor")
//  simpleActor ! ReadFile

  val simpleSupervisorProps = BackoffSupervisor.props(
    Backoff.onFailure(
      Props[FileBasePersistentActor],
      "simpleBackoffActor",
      3 second,
      30 second,
      0.2
    )
  )
  /*
    simpleSupervisor
      - child called simpleBackoffActor (props of type FileBasedPersistentActor)
      - supervision strategy is the default one (restarting on everything)
        - first attempt after 3 seconds
        - next attempt is 2x the previous attempt
   */

  val simpleBackoffSupervisor = system.actorOf(simpleSupervisorProps, "simpleSupervisor")
//  simpleBackoffSupervisor ! ReadFile

  val stopSupervisorProps = BackoffSupervisor.props(
    Backoff.onStop(
      Props[FileBasePersistentActor],
      "stopBackoffActor",
      3 second,
      30 second,
      0.2
    ).withSupervisorStrategy(
      OneForOneStrategy(){
        case _ => Stop
      }
    )
  )
  val stopSupervisor = system.actorOf(stopSupervisorProps, "stopSupervisor")
//  stopSupervisor ! ReadFile

  class EagerFPBActor extends FileBasePersistentActor{
    override def preStart(): Unit = {
      log.info("Eager actor starting")
      dataSource = Source.fromFile(new File("src/main/resources/textfile/important_data.txt"))
    }
  }

//  val eagerFPBActor = system.actorOf(Props[EagerFPBActor], "eagerFPBActor")
// // ActorInitializationException => STOP
  val repeatSupervisorProps = BackoffSupervisor.props(
      Backoff.onStop(
        Props[EagerFPBActor],
        "eagerActor",
        1 second,
        30 second,
        0.1
      )
  )
  /*
    eagerSupervisor
      - child eagerActor
        - will die on start with ActorInitializationException
        - trigger the supervision strategy in eagerSupervisor => STOP eagerActor
      - backoff will kick in after 1 second, 2s, 4, 8, 16
   */
  val repeatedSupervisor = system.actorOf(repeatSupervisorProps, "eagerSupervisor")

}
