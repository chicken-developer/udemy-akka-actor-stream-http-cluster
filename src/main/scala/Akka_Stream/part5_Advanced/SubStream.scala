package Akka_Stream.part5_Advanced

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.{Keep, Sink, Source}

import scala.util.{Failure, Success}

object SubStream extends App {
  implicit val system = ActorSystem("SubStream")
  implicit val materializer = Materializer

  //1. group stream by a certain function
  val wordSource = Source(List("Akka", "is", "interesting", "and", "amazing", "language"))
  val group = wordSource.groupBy(30, {
    case word: String if(word.isEmpty) => '\u0000'
    case word: String => word.toLowerCase().charAt(0)
  })
//  group.to(Sink.fold(0)((count, word) => {
//    val newCount = count + 1
//    println(s"I just receive $word, count is $newCount")
//    newCount
//  }))
//    .run()

  //2. Merge substream back to a super stream
  val textSource = Source(List(
    "Akka is very hard to learn",
    "Akka is a powerful toolkit",
    "I love akka stream",
    "Akka is very amazing"
  ))
  val totalCharCountFuture = textSource
    .groupBy(2, str => str.length % 2)
    .map(_.length)
    .mergeSubstreamsWithParallelism(2) // Number argument here == number of substream
    .toMat(Sink.reduce[Int](_ + _ ))(Keep.right)
    .run()

  import system.dispatcher
  totalCharCountFuture.onComplete{
    case Success(value) => println(s"Total char count $value")
    case Failure(ex) => println(s"Have an exception $ex")
  }

  val text =  "Akka is very hard to learn\n"+
  "Akka is a powerful toolkit\n"+
  "I love akka stream\n"+
  "Akka is very amazing\n"

  val anotherCountFuture = Source(text)
    .splitWhen(c => c == '\n')
    .filter(c => c != '\n')
    .map(_ => 1)
    .mergeSubstreams
    .toMat(Sink.reduce[Int](_ + _))(Keep.right)
    .run()
    .onComplete{
      case Success(value) => println(s"Total words count is: $value")
      case Failure(ex) => println(s"Have an error: $ex")
    } //This case is useful than first case

  //3 - flatten
  val simpleSource = Source(1 to 5)
  simpleSource.flatMapConcat(x => Source(x to 3 * x)).runWith(Sink.foreach(println))
  simpleSource.flatMapMerge(2, x => Source(x to 3 * x)).runWith(Sink.foreach(println))


}
