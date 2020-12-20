package part2_TestingAkka

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

import scala.concurrent.duration._
import scala.util.Random

class BasicSpec extends TestKit(ActorSystem("BasicSpec"))
  with ImplicitSender
  with WordSpecLike
  with BeforeAndAfterAll {
  //Should make class name like ClassName + Spec(Ex: AkkaBasicSpec, ClassSpec)
  //Is CLASS, not OBJECT

  //Setup
  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  import BasicSpec._
  "A simple actor" should {
    "don't send any message" in {
      val echoActor = system.actorOf(Props[SimpleActor])
      val message = "hello tester"
      echoActor ! message

      expectMsg(message) // akka.test.single-expect-default
    }
  }

  "A black hole actor" should{
    "send back the message" in {
      val backHole = system.actorOf(Props[BlackHole])
      val message = "hello tester"
      backHole ! message

      expectNoMessage(1 second)
    }
  }

  "A TestLab actor" should{
    val testLabActor = system.actorOf(Props[TestLab])
    "return upper case message" in {
      testLabActor ! "Hello Akka"
      val resultMessage = expectMsgType[String]
      assert(resultMessage == "HELLO AKKA")
    }

    "reply after greeting" in {
      testLabActor ! "GREETING"
      expectMsgAnyOf("hi","hello") // Like or
    }

    "reply after ask favorite task are scala and akka" in {
      testLabActor ! "favoriteTech"
      expectMsgAllOf("Scala","Akka") // Like and
    }

    "reply after ask favorite task with another way" in {
      testLabActor ! "favoriteTech"
      val message = receiveN(2) // Seq[Any]
      assert(Seq("Akka","Scala") == message)
    }

    "reply after ask favorite task with a fancy way" in {
      testLabActor ! "favoriteTech"
     expectMsgPF(){
       case "Scala" => // Only care that the PF is define
       case "Akka" =>
     }
    }

  }

}

object BasicSpec{
  class SimpleActor extends Actor{
    override def receive: Receive = {
      case message => sender() ! message
    }
  }
  class BlackHole extends Actor{
    override def receive: Receive = Actor.emptyBehavior
    /*
    Like:
     override def receive: Receive = {
      case _ =>
    }
     */
  }
  class TestLab extends Actor{
    val random = new Random()
    override def receive: Receive = {
      case "GREETING" =>
        if(random.nextBoolean()) sender() ! "hi"
        else sender() ! "hello"
      case "favoriteTech" =>
        sender() ! "Akka"
        sender() ! "Scala"
      case message: String => sender() ! message.toUpperCase
    }
  }
}