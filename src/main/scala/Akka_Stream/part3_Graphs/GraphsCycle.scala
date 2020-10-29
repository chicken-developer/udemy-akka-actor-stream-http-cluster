package Akka_Stream.part3_Graphs

import akka.actor.ActorSystem
import akka.stream.{ClosedShape, Materializer, OverflowStrategy, UniformFanInShape}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge, MergePreferred, RunnableGraph, Sink, Source, Zip}

object GraphsCycle extends App {
  implicit val system = ActorSystem("GraphCycle")
  implicit val materializer = Materializer

  val accelerator = GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._
    val sourceShape = builder.add(Source(1 to 100))
    val incrementerFlowShape = builder.add(Flow[Int].map{ x =>
      println(s"Value: $x")
      x + 1
    })
    val mergeShape = builder.add(Merge[Int](2))
    sourceShape ~> mergeShape ~> incrementerFlowShape
                  mergeShape <~ incrementerFlowShape
    ClosedShape
  }
//  RunnableGraph.fromGraph(accelerator).run() // Graph cycle deadlock
  //Solution 1: MergePreferred
  val actualAccelerator = GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._
    val sourceShape = builder.add(Source(1 to 100))
    val incrementerFlowShape = builder.add(Flow[Int].map{ x =>
      println(s"Value: $x")
      x + 1
    })
    val mergeShape = builder.add(MergePreferred[Int](1))
    sourceShape ~> mergeShape ~> incrementerFlowShape
    mergeShape.preferred <~ incrementerFlowShape
    ClosedShape
  }
//  RunnableGraph.fromGraph(actualAccelerator).run() // Will run forever

  //Solution 2: Buffer
  val bufferedRepeater = GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._
    val sourceShape = builder.add(Source(1 to 100))
    val incrementerFlowShape = builder.add(Flow[Int].buffer(10, OverflowStrategy.dropHead).map{ x =>
      println(s"Value: $x")
      x
    })
    val mergeShape = builder.add(Merge[Int](2))
    sourceShape ~> mergeShape ~> incrementerFlowShape
    mergeShape <~ incrementerFlowShape
    ClosedShape 
  }
//  RunnableGraph.fromGraph(bufferedRepeater).run()
  /*
  Cycle risk deadlocking
    - add bounds to the number of elements in the cycle
    boundedness vs liveness
   */
  //Challenge:
  val fibonacciGenerator = GraphDSL.create(){ implicit builder =>
    import GraphDSL.Implicits._
    val zip = builder.add(Zip[BigInt, BigInt])
    val mergerPrefer = builder.add(MergePreferred[(BigInt, BigInt)](1))
    val fiboLogic = builder.add(Flow[(BigInt, BigInt)].map{ pair =>
      val last = pair._1
      val previous = pair._2
      Thread.sleep(100)
      (last + previous, last)
    })
    val broadcast = builder.add(Broadcast[(BigInt, BigInt)] (2))
    val extractLast = builder.add(Flow[(BigInt, BigInt)].map(_._1))
    zip.out ~> mergerPrefer ~> fiboLogic ~> broadcast ~> extractLast
               mergerPrefer       <~        broadcast

    UniformFanInShape(extractLast.out, zip.in0, zip.in1)
  }
  val fiboGraph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit  builder =>
      import GraphDSL.Implicits._
      val source_01Shape = builder.add(Source.single[BigInt](1))
      val source_02Shape = builder add Source.single[BigInt](1)
      val sinkShape = builder.add(Sink.foreach[BigInt](println))
      val fiboGraph = builder.add(fibonacciGenerator)
      source_01Shape ~> fiboGraph.in(0)
      source_02Shape ~> fiboGraph.in(1)
      fiboGraph ~> sinkShape
      ClosedShape
    }
  )
  fiboGraph.run()
}
