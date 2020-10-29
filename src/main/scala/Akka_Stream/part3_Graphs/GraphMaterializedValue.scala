package Akka_Stream.part3_Graphs

import akka.actor.ActorSystem
import akka.stream.{FlowShape, Materializer, SinkShape}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Keep, Sink, Source}

import scala.concurrent.Future
import scala.util.{Failure, Success}

object GraphMaterializedValue extends App{
  implicit val system = ActorSystem("GraphMaterializedValue")
  implicit val materializer = Materializer

  val source = Source(List("Akka", "is", "VERY", "powerful", "Toolkit","for", "build", "distributed", "System"))
  val printer = Sink.foreach(println)
  val counter = Sink.fold[Int, String](0)((counter, _) => counter + 1)

  val complexWordGraph = Sink.fromGraph(
    GraphDSL.create(counter) { implicit builder => counterShape =>
      import GraphDSL.Implicits._
      val broadcast = builder.add(Broadcast[String](2))
      val lowercaseFilter = builder.add(Flow[String].filter(word => word == word.toLowerCase))
      val shortStringFilter = builder.add(Flow[String].filter(_.length < 5))

      broadcast ~> lowercaseFilter ~> printer
      broadcast ~> shortStringFilter ~> counterShape
      SinkShape(broadcast.in)
    }) //If add first parameter into GraphDSL.create, must a a higher order function part after builder

  val moreParameterOFComplexWordGraph = Sink.fromGraph(
  GraphDSL.create(printer, counter)((printerMatValue, counterMatValue) => counterMatValue) { implicit builder => (printerShape, counterShape) =>
    import GraphDSL.Implicits._
    val broadcast = builder.add(Broadcast[String](2))
    val lowercaseFilter = builder.add(Flow[String].filter(word => word == word.toLowerCase))
    val shortStringFilter = builder.add(Flow[String].filter(_.length < 5))

    broadcast ~> lowercaseFilter ~> printerShape
    broadcast ~> shortStringFilter ~> counterShape
    SinkShape(broadcast.in)
  })
  import system.dispatcher
  val shortStringCounterFilter = source.toMat(moreParameterOFComplexWordGraph)(Keep.right).run()
  shortStringCounterFilter.onComplete{
    case Success(value) => println(s"Result: $value")
    case Failure(exception) => println(s"Have error here: ${exception.toString}")
  }

  def enhanceFlow[A, B](flow: Flow[A, B, _]): Flow[A, B, Future[Int]] = {
    val counterSink = Sink.fold[Int, B](0)((count, _) => count + 1)
    Flow.fromGraph(
      GraphDSL.create(counterSink) { implicit builder => counterSinkShape =>
        import GraphDSL.Implicits._
        val broadcast = builder.add(Broadcast[B](2))
        val originalFlowShape = builder.add(flow)
        originalFlowShape ~> broadcast ~> counterSinkShape
        FlowShape(originalFlowShape.in, broadcast.out(1))
      })
  }
  val simpleSource = Source(1 to 8)
  val simpleFlow = Flow[Int].map(x => x)
  val simpleSink = Sink.ignore

  val enhancedFlowCountFuture = simpleSource.viaMat(enhanceFlow(simpleFlow))(Keep.right).toMat(simpleSink)(Keep.left).run()
  enhancedFlowCountFuture.onComplete{
    case Success(value) => println(s"Result through from counter: $value")
    case _ => println("Have some error here")
  }


}
