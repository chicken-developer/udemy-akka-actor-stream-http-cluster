package Akka_Stream.part2_Primers


import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Sink, Source}

import scala.concurrent.Future

object FirstPrinciples extends App {
  implicit val system: ActorSystem = ActorSystem("FirstPrinciples")
  implicit val materializer: Materializer.type = Materializer

  //Source
  val source = Source(1 to 10)
  //Sink
  val sink = Sink.foreach[Int](println)
  //Graph - to connect source and sink
  val graph = source.to(sink)
  //  graph.run() // graph will not do any thing after call run method

  //Flows transform elements
  val flow = Flow[Int].map(x => x * 2)
  val sourceWithFlow = source.via(flow)
  val flowWithSink = flow.to(sink) // source --> flow --> flow --> sink
  val graphWithFlow = sourceWithFlow.to(sink)
  //  graphWithFlow.run()

  //  sourceWithFlow.to(sink).run()
  //  source.to(flowWithSink).run()
  //  source.via(flow).to(sink).run()

  //Note:
  //Source can emit any kind of object must immutable and serializable - like message of akka actor
  //But null are not allow
  //  val illegalSource = Source.single[String](null)
  //  illegalSource.to(Sink.foreach[String](println)).run()

  //Various kinds of sources:
  val finiteSource = Source.single(1)
  val anotherFiniteSource = Source(List(1,2,3,4))
  val emptySource = Source.empty[Int]
  val infiniteSource = Source(Stream from 1 )
  import scala.concurrent.ExecutionContext.Implicits.global
  val futureSource = Source.future(Future(40))

  //Sink
  val theMostBoringSink = Sink.ignore
  val foreachSink = Sink.foreach[String](println)
  val headSink = Sink.head[Int] //Retrieves head and then closed stream
  val foldSink = Sink.fold[Int, Int](0)((a, b) => a + b)

  //Flow
  //Usually using mapped to collection operators
  val mapFlow = Flow[Int].map(x => x * 2) // Have filter, drop, but NOT have flatMap
  val takeFlow = Flow[Int].take(5) //Take 5 first element Also closed stream

  //Source -> FLow -> Flow -> ... -> Flow -> Sink(Handle Result - map, fold,..).
  //Short syntax
  val mapSource = Source(1 to 10).map(x => x * 2)
  //Same like Source(1 to 10).via(Flow[Int].map( x => x * 2))

  //Run stream directly
  //  mapSource.runForeach(println)
  //-----

  //Exercises
  val names = List("Alice", "Bob", "Daniel", "Martine", "EdogawaConan", "Aphelios", "Invoker")
  val nameSource = Source(names)
  val longNameFlow = Flow[String].filter(name => name.length > 5)
  val take2Flow = Flow[String].take(4)
  val nameSink = Sink.foreach[String](println)

  //Run graph
  nameSource.via(longNameFlow).via(take2Flow).to(nameSink).run()
  nameSource.filter(_.length > 5).take(2).runForeach(println)

}