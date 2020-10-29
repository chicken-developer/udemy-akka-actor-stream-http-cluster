package Akka_Stream.part2_Primers

import akka.actor.ActorSystem
import akka.stream.{Materializer, OverflowStrategy}
import akka.stream.scaladsl.{Flow, Sink, Source}

object BackPressureBasics extends App{
  implicit val system = ActorSystem("BackPressureBasic")
  implicit val materializer = Materializer

  val fastSource = Source(1 to 1000)
  val slowSink = Sink.foreach[Int]{ x =>
    Thread.sleep(1000)
    println(s"In Sink: $x")
  }
  // fastSource.to(slowSink).run()
  // Not have backpressure

  val normalFlow = Flow[Int].map{ x =>
    println(s"In queued: $x")
    x + 1
  }

  fastSource.async
    .via(normalFlow).async
    .to(slowSink).async
//    .run()
  /*
  reactions to backpressure (in order):
  - try to slow down if possible
  - buffer elements until there's more demand
  - drop down elements from the buffer if it overflows
  - tear down/kill the whole stream (failure)
  */
  val bufferFlow  = normalFlow.buffer(10, overflowStrategy = OverflowStrategy.dropHead)
  fastSource.async
    .via(bufferFlow).async
    .to(slowSink)
//    .run()
  /*
    1-16: nobody is backpressured
    17-26: flow will buffer, flow will start dropping at the next element
    26-1000: flow will always drop the oldest element
      => 991-1000 => 992 - 1001 => sink
   */

  /*
    overflow strategies:
    - drop head = oldest
    - drop tail = newest
    - drop new = exact element to be added = keeps the buffer
    - drop the entire buffer
    - backpressure signal
    - fail
   */

  // throttling
  import scala.concurrent.duration._
  fastSource.throttle(10, 1 second).runWith(Sink.foreach(println))


}
