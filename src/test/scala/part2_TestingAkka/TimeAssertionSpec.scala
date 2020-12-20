package part2_TestingAkka

import part2_TestingAkka.TimeAssertionSpec.{WorkResult, WorkerActor}
import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

import scala.concurrent.duration._
import scala.util.Random

class TimeAssertionSpec extends TestKit(ActorSystem("TimeAssertionSpec", ConfigFactory.load().getConfig("specialTimedAssertionsConfig")))
  with ImplicitSender
  with WordSpecLike
  with BeforeAndAfterAll{
  //Setup
  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "A worker actor" must{
    val workerActor = system.actorOf(Props[WorkerActor])

    "reply with the meaning of life in a timely manner" in {
      within(300 millis, 1 second){
        workerActor ! "work"

        expectMsg(WorkResult(42))
      }
    }

    "reply with valid work at a reasonable cadence" in {
      within(1 second){
        workerActor ! "workSequence"
        val results: Seq[Int] = receiveWhile[Int](max = 2 second, idle = 500 millis, messages = 10){
          case WorkResult(result) => result
        }
        assert(results.sum > 5)
      }
    }

    "replay to a test probe in a timely manner" in {
      within(1 second){
        val probe = TestProbe()
        probe.send(workerActor, "work")
        probe.expectMsg(WorkResult(42))
      }
    }
  }

}

object TimeAssertionSpec {
  case class WorkResult(result: Int)
  class WorkerActor extends Actor{
    override def receive: Receive = {
      case "work" =>
        Thread.sleep(500)
        sender() ! WorkResult(42)
      case "workSequence" =>
        val rand = new Random()
        for(_  <- 1 to 10){
          Thread.sleep(rand.nextInt(50))
          sender() ! WorkResult(1)
        }
    }

  }
}
