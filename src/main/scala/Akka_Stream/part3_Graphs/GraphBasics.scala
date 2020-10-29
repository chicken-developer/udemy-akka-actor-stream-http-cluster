package Akka_Stream.part3_Graphs

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.client.RequestBuilding.WithTransformation
import akka.stream.{ClosedShape, Materializer}
import akka.stream.scaladsl.{Balance, Broadcast, Flow, GraphDSL, Merge, RunnableGraph, Sink, Source, Zip}

object GraphBasics extends App {
  implicit val system = ActorSystem("GraphBasic")
  implicit val materializer = Materializer

  //Learning
  //  val input = Source(1 to 1000)
  //  val increment = Flow[Int].map{ x =>
  //    x + 1
  //  }
  //  val multi = Flow[Int].map { x =>
  //    x * 2
  //  }
  //  val output = Sink.foreach[(Int, Int)](println)
  //  //Step 1 : Setting up graph
  //  val graph = RunnableGraph.fromGraph(
  //    GraphDSL.create(){ implicit  builder: GraphDSL.Builder[NotUsed] =>
  //      import  GraphDSL.Implicits._
  //      //Step 2: Add broadcast and zip components
  //      //Careful when import library
  //      val broadcast = builder.add( Broadcast[Int](2)) //Fan - out
  //      val zip = builder.add(Zip[Int, Int])
  //      input ~> broadcast // Mean: input feeds into broadcast
  //      broadcast.out(0) ~> increment ~> zip.in0
  //      broadcast.out(1) ~> multi ~> zip.in1
  //      zip.out ~> output
  //      ClosedShape
  //    } // Return ClosedShape
  //  ) // Runnable
  //  graph.run()
  //


  //Ex_01:
//  val mySource = Source(1 to 1000)
//  val firstOutput = Sink.foreach[Int] { x =>
//    println(s"First output: $x")
//  }
//  val secondOutput = Sink.foreach[Int] { x =>
//    println(s"Second output: $x")
//  }
//  val myGraph = RunnableGraph.fromGraph(
//    GraphDSL.create(){ implicit builder: GraphDSL.Builder[NotUsed] =>
//      import GraphDSL.Implicits._
//      val broadcast = builder.add(Broadcast[Int](2))
////      mySource ~> broadcast
////      broadcast.out(0) ~> firstOutput
////      broadcast.out(1) ~> secondOutput
//
//      //Short way -> implicit
//      mySource ~> broadcast ~> firstOutput
//                  broadcast ~> secondOutput
//      ClosedShape
//    }
//  )
//  myGraph.run()


  //Ex_02 - My answer
//  val fastSource = Source(1 to 100).via(Flow[Int].map{ x =>
//    x * 2 //Return 2,4,6,8,10...
//  })
//  val slowSource = Source(1 to 100).via(Flow[Int].map{ x =>
//    Thread.sleep(1000) // Simulating so long compute
//    x * 2 + 1 //Return 1,3,5,7,9,...
//  })
//
//  // Try source
//  fastSource.to(Sink.foreach(println)).run()
//
//  val testOutput = Sink.foreach[(Int, Int)](println)
//  val firstSink = Sink.foreach[Int] { x =>
//    println(s"First sink: $x")
//  }
//  val secondSink = Sink.foreach[Int] { x =>
//    println(s"Second sink: $x")
//  }
//  val complexGraph = RunnableGraph.fromGraph(
//    GraphDSL.create() { implicit  builder: GraphDSL.Builder[NotUsed] =>
//      import GraphDSL.Implicits._
//      //2 source -> 2 port -> merge -> 1 port -> balance -> 2 sink
//      val zip = builder.add(Zip[Int, Int])
//      val inputBroadcast = builder.add(Broadcast[Int](2))
////      val outputBroadcast = builder.add(Broadcast[Int](2))
//      fastSource ~> inputBroadcast
//      slowSource ~> inputBroadcast
//      inputBroadcast.out(0) ~> zip.in0
//      inputBroadcast.out(1) ~> zip.in1
//      zip.out ~> testOutput
//      ClosedShape
//    }
//  )

  //Ex2 - Correct answer
  import scala.concurrent.duration._
  val input = Source(1 to 1000)
  val fastSource = input.throttle(5, 1 second)
  val slowSource = input.throttle(2, 1 second)

  val sink1 = Sink.foreach[Int]( x => println(s"Sink 01: $x"))
  val sink2 = Sink.foreach[Int]( x => println(s"Sink 02: $x"))

  val countSink_1 = Sink.fold[Int, Int](0)((count, _) => {
    println(s"Sink 1: $count")
    count + 1
  })
  val countSink_2 = Sink.fold[Int, Int](0)((count, _) => {
    println(s"Sink 2: $count")
    count + 1
  })
  val balanceGraph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._ //Very important
      val merge = builder.add(Merge[Int](2))
      val balance = builder.add(Balance[Int](2))
      fastSource ~> merge ~> balance ~> sink1
      slowSource ~> merge
      balance ~> sink2
      ClosedShape
    }
  )
  balanceGraph.run()
}
