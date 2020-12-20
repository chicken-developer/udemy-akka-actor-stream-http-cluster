import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.stream.testkit.scaladsl.{TestSink, TestSource}
import akka.testkit.{TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.{Failure, Success}

class TestingStreamSpec extends TestKit(ActorSystem("TestingStreamSpec"))
  with WordSpecLike
  with BeforeAndAfterAll {
    implicit val materializer = Materializer

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  //All my test case:
  "A simple test" must {
    "return a sum of all value in source" in {
      val simpleSource = Source(1 to 10)
      val simpleSink = Sink.fold(0)((a: Int, b: Int) => a + b)
      val sumFuture = simpleSource.toMat(simpleSink)(Keep.right).run()
      val sum = Await.result(sumFuture, 2 second)
      assert(sum == 55)
    }
    "integrate with test actor via materialized value " in {
      import akka.pattern.pipe
      import system.dispatcher
      val simpleSource = Source(1 to 10)
      val simpleSink = Sink.fold(0)((a: Int, b: Int) => a + b)
      val probe = TestProbe()
      simpleSource.toMat(simpleSink)(Keep.right).run().pipeTo(probe.ref)
      probe.expectMsg(55)
    }
    "integrate with actor-based sink" in {
      val simpleSource = Source(1 to 5)
      val simpleFlow = Flow[Int].scan[Int](0)(_ + _)
      val streamUnderTest = simpleSource.via(simpleFlow)

      val probe = TestProbe()
      val probeSink = Sink.actorRef(probe.ref,"Competed",err => println(err.toString))
      streamUnderTest.to(probeSink).run()
      probe.expectMsgAllOf(0, 1, 3, 6, 10, 15)
    }
    "integrate with stream testkit sink" in {
      val simpleSource = Source(1 to 5).map(_ * 2)
      val testSink = TestSink.probe[Int]
      val materializedTestValue = simpleSource.runWith(testSink)
      materializedTestValue
        .request(5) //Importance
        .expectNext(2,4,6,8,10)
        .expectComplete()

    }
    "integrate with stream testkit source" in {
      import system.dispatcher
      val sinkUnderTest = Sink.foreach[Int]{
        case 13 => throw new RuntimeException("Bad look!")
        case _ =>
      }
      val testSource = TestSource.probe[Int]
      val materializedTestValue = testSource.toMat(sinkUnderTest)(Keep.both).run()
      val (testPublisher, resultFeature) = materializedTestValue
      testPublisher
        .sendNext(1)
        .sendNext(5)
        .sendNext(13)
      resultFeature.onComplete{ //TODO: Need learn more, do not understand here now @@@
        case Success(_) => fail("The sink under test should be throw an exception when receive value 13")
        case Failure(_) =>
      }
    }
    "integrate with stream testkit source and sink" in {
      val testSource = TestSource.probe[Int]
      val testSink = TestSink.probe[Int]
      val flowNeedCheck = Flow[Int].map(_ * 2)
      val materializedTestValue = testSource.via(flowNeedCheck).toMat(testSink)(Keep.both).run()
      val (publisher, subscriber) = materializedTestValue
      publisher
        .sendNext(1)
        .sendNext(2)
        .sendNext(5)
        .sendNext(10)
        .sendComplete()

      subscriber
        .request(4)
        .expectNext(2,4,10,20)
        .expectComplete()
    }
  }
}


