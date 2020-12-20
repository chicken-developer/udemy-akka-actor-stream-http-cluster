package part2_TestingAkka

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{CallingThreadDispatcher, TestActorRef, TestProbe}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

import scala.concurrent.duration._

class SynchronousTestingSpec extends WordSpecLike with BeforeAndAfterAll{
  implicit val system = ActorSystem("SynchronousTestingSpec")
  override def afterAll(): Unit = {
    system.terminate()
  }
  import  SynchronousTestingSpec._
  "A counter" must{
    "synchronously increase its counter" in {
      val counter = TestActorRef[Counter](Props[Counter])
      counter ! Inc
      assert(counter.underlyingActor.counter == 1)
    }

    "synchronously increase its counter at the acll of the receive function" in {
      val counter = TestActorRef[Counter](Props[Counter])
      counter.receive(Inc)
      assert(counter.underlyingActor.counter == 1)
    }

    "work on tge calling thread dispatcher" in {
      val counter = system.actorOf(Props[Counter].withDispatcher(CallingThreadDispatcher.Id))
      val probe = TestProbe()
      probe.send(counter, Read)
      probe.expectMsg(Duration.Zero, 0)
    }

  }

}

object SynchronousTestingSpec{
  case object Inc
  case object Dec
  case object Read

  class Counter extends Actor{
    var counter = 0
    override def receive: Receive = {
      case Inc => counter += 1
      case Dec => counter -= 1
      case Read => sender() ! counter
    }
  }
}
