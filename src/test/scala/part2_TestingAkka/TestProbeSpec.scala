package part2_TestingAkka

import org.scalatest.{BeforeAndAfterAll, WordSpecLike}
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}

class TestProbeSpec extends TestKit(ActorSystem("TestProbeSpec"))
  with ImplicitSender
  with WordSpecLike
  with BeforeAndAfterAll {

  //Setup
  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }
  import TestProbeSpec._
  "A Master Actor" should {
    "register a slave" in {
      val master = system.actorOf(Props[Master])
      val slave = TestProbe("slave")

      master ! Register(slave.ref)
      expectMsg(RegistrationAck)
    }

    "send the work to slave actor" in {
      val master = system.actorOf(Props[Master])
      val slave = TestProbe("salve")
      master ! Register(slave.ref)
      expectMsg(RegistrationAck)

      val workloadString = "I love Akka"
      master ! Work(workloadString)

      slave.expectMsg(SlaveWork(workloadString, testActor))
      slave.reply(WorkCompleted(3,testActor))
      expectMsg(Report(3))
    }

    "aggregate data correctly" in {
      val master = system.actorOf(Props[Master])
      val slave = TestProbe("slave")
      master ! Register(slave.ref)
      expectMsg(RegistrationAck)

      val workloadString = "I love Akka"
      master ! Work(workloadString)
      master ! Work(workloadString)

      slave.receiveWhile(){
        case SlaveWork(`workloadString`, `testActor`) => slave.reply(WorkCompleted(3, testActor))
      }

      expectMsg(Report(3))
      expectMsg(Report(6))
    }
  }
}

object TestProbeSpec{
  /*
  word counting actor hierarchy master - slave( boss - worker)
    //Work follow:
    1.master sends the slave the piece of work
    2.slave process the work and replies to master
   */

  case class Work(text: String)
  case class Register(slaveRef: ActorRef)
  case class SlaveWork(text: String, originalRequester: ActorRef)
  case object RegistrationAck
  case class WorkCompleted(count: Int, originalRequester: ActorRef)
  case class Report(totalWordCount: Int)


  class Master extends Actor{
    override def receive: Receive = {
      case Register(slaveRef) =>
        sender() ! RegistrationAck
        context.become(online(slaveRef, 0))
    }
    def online(slaveRef: ActorRef, totalWordCount: Int): Receive ={
      case Work(text) => slaveRef ! SlaveWork(text, sender())
      case WorkCompleted(count, originalRequester) =>
        val newTotalWordCount = totalWordCount + count
        originalRequester ! Report(newTotalWordCount)
        context.become(online(slaveRef, newTotalWordCount))
      case _ =>
    }
  }


}