package Akka_Stream.part2_Primers

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}

import scala.util.{Failure, Success}

object Materializing extends App{
  //Goal of this file is to learn how to extract meaningful values out of the runing of a stream

  //Components are static until they run
  // A graph is a blueprint for a stream
  //Running a graph called Materializing, results of materializing a graph is called a materialized
  //When materializing a graph, it will materializing one by one components of this graph
  //So each component produces a materialized value when run

  implicit val system = ActorSystem("MaterializingStream")
  implicit val materializer = Materializer
  import  system.dispatcher
  val simpleGraph = Source(1 to 10).to(Sink.foreach(println))
//  val simpleMaterializedValue = simpleGraph.run()

  val source = Source(1 to 10)
  val sink = Sink.reduce[Int]((a, b) => a + b)

  val someFuture = source.runWith(sink)
//  someFuture.onComplete {
//    case Success(value) => println(s"Sum of all elements is $value")
//    case Failure(exception) => println(s"Have error: $exception")
//  }

  //Choosing materialized value
  val simpleSource = Source(1 to 10)
  val simpleFlow = Flow[Int].map(x => x + 1)
  val simpleSink = Sink.foreach(println)
  simpleSource.viaMat(simpleFlow)((sourceMat, flowMat) => flowMat)
  //viaMat => like Source.via(Flow) but can save materialized value if choose Keep method at next
  //sourceMat = materialized value of simple Source
  // flowMat = materialized value of simple source after pass via SimpleFlow
  // (sourceMat, flowMat) => flowMat : Get 2 materialized value, then take flowMat and not take sourceMat
  // === Keep.right
  //Another: Keep.both ; Keep.left ; Keep.none

  val graph = simpleSource.viaMat(simpleFlow)(Keep.right).toMat(simpleSink)(Keep.right)
  //toMat => like Source.to(Sink) but can save materialized value if choose Keep method at next
  //Same like viaMat
  graph.run().onComplete{
    case Success(_) => println("Stream processing finish")
    case Failure(exception) => println(s"Stream processing failure with an exception $exception")
  }

  //Sugar syntax
  val sum = Source(1 to 10).runWith(Sink.reduce[Int](_ + _)) // Source.to(Sink.reduce)(keep.right) -> keep right is default
  Source(1 to 10).runReduce[Int](_ + _)

  //Backwards
  Sink.foreach(println).runWith(Source.single(42)) // Same like: Source.single(42).to(Sink.foreach(println)).run()

  //Both ways
  Flow[Int].map(x => x * 2).runWith(simpleSource, simpleSink)

  /* Exercises:
    Sink.last // Return the last element out of the source
  Compute the total word count of a stream of sentences
  -map , reduce, fold
   */

  val f1 = Source(1 to 10).toMat(Sink.last)(Keep.right).run()
  val f2 = Source(1 to 10).runWith(Sink.last)

  val sentenceSource = Source(List("Akka is awesome", "I love you","The world is very beauty"))
  val wordCountSink = Sink.fold[Int, String](0)((currentCount, sentence) => currentCount + sentence.split(" ").length)
  val g1 = sentenceSource.toMat(wordCountSink)(Keep.right).run()
  val g2 = sentenceSource.runWith(wordCountSink)
  val g3 = sentenceSource.runFold(0)((currentCount, sentence) => currentCount + sentence.split(" ").length)

  val wordCountFlow = Flow[String].fold[Int](0)((currentCount, sentence) => currentCount + sentence.split(" ").length)
  val g4 = sentenceSource.via(wordCountFlow).toMat(Sink.head)(Keep.right).run()
  val g5 = sentenceSource.viaMat(wordCountFlow)(Keep.left).toMat(Sink.head)(Keep.right)
  val g6 = sentenceSource.via(wordCountFlow).runWith(Sink.head)
  val g7 = wordCountFlow.runWith(sentenceSource,Sink.head)._2
}
