package Akka_Stream.part3_Graphs

import java.util.Date

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ClosedShape, FanOutShape2, FlowShape, Materializer, UniformFanInShape}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, RunnableGraph, Sink, Source, ZipWith}

object MoreOpenGraphs extends App {
 implicit val system = ActorSystem("MoreOpenGraph")
 implicit val materializer = Materializer

 //Re-learning about graph
// val input = Source(1 to 1000)
// val output = Sink.foreach[Int](x => println(s"Value: $x"))
// val plusFlow = Flow[Int].map(_ + 1)
// val multiFlow = Flow[Int].map(_ * 10)
// val flowGraph = Flow.fromGraph(
//   GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
//    import GraphDSL.Implicits._
//     val plusFlowShape = builder.add(plusFlow)
//     val multiFlowShape = builder.add(multiFlow)
//    plusFlowShape ~> multiFlowShape
//     FlowShape(plusFlowShape.in, multiFlowShape.out)
//   })
// input.via(flowGraph).to(output).run()

 // Start lesion

 //Fan-In Shape
 val max3StaticGraph = GraphDSL.create() { implicit builder =>
  import GraphDSL.Implicits._
   val max1 = builder.add(ZipWith[Int, Int, Int]((a, b) => Math.max(a, b)))
   val max2 = builder.add(ZipWith[Int, Int, Int]((a, b) => Math.max(a, b)))
   max1.out ~> max2.in0
   UniformFanInShape(max2.out, max1.in0, max1.in1, max2.in1)
 }
 val source_1 = Source(1 to 10)
 val source_2 = Source(1 to 10).map(_ => 5)
 val source_3 = Source((1 to 10).reverse)

 val maxSink = Sink.foreach[Int](x => println(s"Max value: $x"))
 val max3RunnableGraph = RunnableGraph.fromGraph(
  GraphDSL.create() { implicit builder =>
   import GraphDSL.Implicits._
   val max3Shape = builder.add(max3StaticGraph)
   source_1 ~> max3Shape.in(0)
   source_2 ~> max3Shape.in(1)
   source_3 ~> max3Shape.in(2)
   max3Shape.out ~> maxSink
    ClosedShape
  })
// max3RunnableGraph.run()
 //Same for fan-out shape
 /**
 Non-uniform fan out shape
  Processing bank transactions
  Txn suspicious if amount > 10000

 Stream component for txns
 - Output 1: Let the transition go through
 - Output 2: Suspicious txn ids
  */

 case class Transaction(id: String, source: String, recipient: String, amount: Int, date: Date)
 val transactionSource = Source(List(
  Transaction("3423412342345124","Quynh", "Lan", 1000, new Date),
  Transaction("5344534523412423", "Ngoc", "Huyen", 50000, new Date),
  Transaction("2124323412323423","Huyen", "Quynh", 2000, new Date)
 ))

 val bankProcessor = Sink.foreach[Transaction](println)
 val suspiciousAnalysisService = Sink.foreach[String](txnID => println(s"Suspicious transaction ID: $txnID"))

 val suspiciousTnxStaticGraph = GraphDSL.create() { implicit builder =>
  import GraphDSL.Implicits._
   val broadcast = builder.add(Broadcast[Transaction](2))
   val suspiciousTxnFilter = builder.add(Flow[Transaction].filter(txn => txn.amount > 10000))
   val txnIdExtractor = builder.add(Flow[Transaction].map[String](txn => txn.id))

   broadcast.out(0) ~> suspiciousTxnFilter ~> txnIdExtractor
   new FanOutShape2(broadcast.in, broadcast.out(1), txnIdExtractor.out)
 }
 val suspiciousTxnRunnableGraph = RunnableGraph.fromGraph(
  GraphDSL.create() { implicit  builder =>
   import GraphDSL.Implicits._
    val suspiciousTxnShape = builder.add(suspiciousTnxStaticGraph)
    transactionSource ~> suspiciousTxnShape.in
    suspiciousTxnShape.out0 ~> bankProcessor
    suspiciousTxnShape.out1 ~> suspiciousAnalysisService
    ClosedShape
  })
 suspiciousTxnRunnableGraph.run()
}
