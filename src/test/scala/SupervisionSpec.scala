import akka.actor.SupervisorStrategy.{Escalate, Restart, Resume, Stop}
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, AllForOneStrategy, OneForOneStrategy, Props, SupervisorStrategy, Terminated}
import akka.testkit.{EventFilter, ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

class SupervisionSpec extends TestKit(ActorSystem("SupervisionSpec"))
  with ImplicitSender
  with WordSpecLike
  with BeforeAndAfterAll{
  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }
  import SupervisionSpec._
  "A Supervisor" must{
    "resume its child in case of minor fault" in {
      val supervisor = system.actorOf(Props[Supervisor])
      supervisor ! Props[FussyWordCounter]
      val child = expectMsgType[ActorRef]

      child ! "I love akka"
      child ! Report
      expectMsg(3)

      child ! "I love akka and all this toolkit like http and spark and stream and cluster and so more so more"
      child ! Report
      expectMsg(3)
    }
    "restart its child in case of empty sentence" in {
      val supervisor = system.actorOf(Props[Supervisor])
      supervisor ! Props[FussyWordCounter]
      val child = expectMsgType[ActorRef]

      child ! "I love akka"
      child ! Report
      expectMsg(3)

      child ! ""
      child ! Report
      expectMsg(0)
    }

    "terminate its child in case of a major error" in {
      val supervisor = system.actorOf(Props[Supervisor])
      supervisor ! Props[FussyWordCounter]
      val child = expectMsgType[ActorRef]

      watch(child)
      child ! "akka is nice"
      val terminated = expectMsgType[Terminated]
      assert(terminated.actor == child)
    }

    "escalate an error when it doesn't know what to do" in{
      val supervisor = system.actorOf(Props[Supervisor])
      supervisor ! Props[FussyWordCounter]
      val child = expectMsgType[ActorRef]

      watch(child)
      child ! 88
      val terminated = expectMsgType[Terminated]
      assert(terminated.actor == child)
    }
  }

  "A kinder supervisor" must {
    "not kill children in case it's restarted or escalates failures" in{
      val supervisor = system.actorOf(Props[NoDeathOnSupervisor])
      supervisor ! Props[FussyWordCounter]
      val child = expectMsgType[ActorRef]

      child ! "Akka is so cool"
      child ! Report
      expectMsg(4)

      child ! 88
      child ! Report
      expectMsg(0)
    }
  }
  "An all-for-one supervisor" must{
    "apply the all-for-one strategy" in {
      val supervisor = system.actorOf(Props[AllForOneSupervisor])
      supervisor ! Props[FussyWordCounter]
      val child = expectMsgType[ActorRef]
      supervisor ! Props[FussyWordCounter]
      val secondChild = expectMsgType[ActorRef]
      secondChild ! "Testing report"
      secondChild ! Report
      expectMsg(2)

      EventFilter[NullPointerException]() intercept{
        child ! ""
      }
      Thread.sleep(400)
      secondChild ! Report
      expectMsg(0)
    }
  }

}

object SupervisionSpec{
  case object Report

  class Supervisor extends Actor{
    override val supervisorStrategy: SupervisorStrategy = OneForOneStrategy(){
      case _: NullPointerException => Restart
      case _: IllegalArgumentException => Stop
      case _: RuntimeException => Resume
      case _: Exception => Escalate
    }
    override def receive: Receive = {
      case props: Props =>
        val childrenRef = context.actorOf(props)
        sender() ! childrenRef
    }
  }

  class NoDeathOnSupervisor extends Supervisor{
    override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
      // Empty
    }
  }

  class AllForOneSupervisor extends Supervisor{
    override val supervisorStrategy = AllForOneStrategy(){
      case _: NullPointerException => Restart
      case _: IllegalArgumentException => Stop
      case _: RuntimeException => Resume
      case _: Exception => Escalate
    }
  }

  class FussyWordCounter extends Actor with ActorLogging{
    var words = 0
    override def receive: Receive = {
      case Report => sender() ! words
      case "" => throw new NullPointerException("sentence is empty")
      case sentence: String =>
        if(sentence.length > 20) throw new RuntimeException("sentence is too long")
        else if(!Character.isUpperCase(sentence(0))) throw new IllegalArgumentException("sentence must start with uppercase")
        else words += sentence.split(" ").length
      case _ => throw new Exception("can only receive string")
    }
  }
}
