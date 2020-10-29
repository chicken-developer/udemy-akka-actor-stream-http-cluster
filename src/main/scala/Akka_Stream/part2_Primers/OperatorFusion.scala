package Akka_Stream.part2_Primers

import akka.actor.{Actor, ActorSystem, Props}
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}

import scala.util.{Failure, Success}

object OperatorFusion extends App{
  implicit val system = ActorSystem("OperatorFusion")
  implicit val materizalizer = Materializer

  val simpleSource = Source(1 to 1000)
  val simpleFlow = Flow[Int].map(_ * 2)
  val simpleFlow_02 = Flow[Int].map(_ + 1)
  val simpleSink = Sink.foreach[Int](println)

  //This run on the same actor - akka stream based on Akka Actor
//  simpleSource.via(simpleFlow).via(simpleFlow_02).to(simpleSink).run()
  //operator/ component FUSION

  //"equivalent behavior
  class SimpleActor extends Actor{
    override def receive: Receive = {
      case x: Int =>
        //Flow operations
        val x2 = x * 2
        val y = x2 + 1
        println(y)
    }
  }
  val simpleActor = system.actorOf(Props[SimpleActor])
//  (1 to 1000).foreach(simpleActor ! _)
  // Same like simpleSource.via(simpleFlow).via(simpleFlow_02).to(simpleSink).run()

  //Complex flows
  val complexFlow = Flow[Int].map {x =>
    Thread.sleep(1000)
    x * 2
  }
  val complexFlow_02 = Flow[Int].map {x =>
    Thread.sleep(1000)
    x + 1
  }

//  simpleSource.via(complexFlow).via(complexFlow_02).to(simpleSink).run()

  //Async boundary
//  simpleSource.via(complexFlow).async // Runs on one actor
//    .via(complexFlow_02).async // Runs on another actor
//    .to(simpleSink)
//    .run()

  Source(1 to 3)
    .map(element => { println(s"Elements A: $element"); element}).async
    .map(element => { println(s"Elements B: $element"); element}).async
    .map(element => { println(s"Elements C: $element"); element}).async
    .runWith(Sink.ignore)
}
