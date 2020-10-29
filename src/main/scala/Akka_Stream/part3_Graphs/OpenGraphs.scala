package Akka_Stream.part3_Graphs

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ClosedShape, FlowShape, Materializer, SinkShape, SourceShape}
import akka.stream.scaladsl.{Broadcast, Concat, Flow, GraphDSL, Merge, RunnableGraph, Sink, Source, Zip}

object OpenGraphs extends App{
  implicit val system = ActorSystem("OpenGraphs")
  implicit val materializer = Materializer

  val firstSource = Source(1 to 10)
  val secondSource = Source(40 to 1000)

  //Default graph
//  val graph = RunnableGraph.fromGraph(
//    GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
//      import GraphDSL.Implicits._
//
//      ClosedShape
//    }
//  )

  //
  val sourceGraph = Source.fromGraph(
    GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._
      val concat = builder.add(Concat[Int](2))
      firstSource ~> concat
      secondSource ~> concat
      SourceShape(concat.out)
    }
  )
//  sourceGraph.to(Sink.foreach[Int](println)).run()

  val sink1 = Sink.foreach[Int](x => println(s"Value from sink 1 : $x"))
  val sink2 = Sink.foreach[Int](x => println(s"Value from sink 2 : $x"))
  val sinkGraph = Sink.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._
      val broadcast = builder.add(Broadcast[Int](2))
      broadcast ~> sink1
      broadcast ~> sink2
      SinkShape(broadcast.in)
    }
  )

//  firstSource.to(sinkGraph).run()

  //Ex_01:
  val exSource = Source(1 to 10)
  val plus_Flow = Flow[Int].map(x => x + 1)
  val multi_Flow = Flow[Int].map(x => x * 10)
  val exSink = Sink.foreach(println)

  //My ans
//  val flowGraph = Flow.fromGraph(
//    GraphDSL.create() { implicit builder =>
//      import GraphDSL.Implicits._
//      val broadcast = builder.add(Broadcast[Int](2))
//      val merge = builder.add(Merge[Int](2))
//      exSource ~> plus_Flow ~> broadcast ~> merge
//      exSource ~> multi_Flow ~> broadcast ~> merge
//      FlowShape(broadcast.in, merge.out)
//    }
//  )
//  exSource.via(flowGraph).to(exSink).run()

  //Correct ans
  val flowGraph_02 = Flow.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._
      val plus_FlowShape = builder.add(plus_Flow)
      val multi_FlowShape = builder.add(multi_Flow)
      plus_FlowShape ~> multi_FlowShape
      FlowShape(plus_FlowShape.in, multi_FlowShape.out)
      //All operators on SHAPES, not Component
    }
  )
  exSource.via(flowGraph_02).to(exSink).run()

  /**
  Exercise: flow from a sink and a source?
   */
  def fromSinkAndSource[A, B](sink: Sink[A, _], source: Source[B, _]): Flow[A, B, _] =
  // step 1
    Flow.fromGraph(
      GraphDSL.create() { implicit builder =>
        // step 2: declare the SHAPES
        val sourceShape = builder.add(source)
        val sinkShape = builder.add(sink)
        FlowShape(sinkShape.in, sourceShape.out)
      }
    )

  val f = Flow.fromSinkAndSourceCoupled(Sink.foreach[String](println), Source(1 to 10))
}
