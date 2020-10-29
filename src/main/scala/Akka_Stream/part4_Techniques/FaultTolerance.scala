package Akka_Stream.part4_Techniques

import java.util.Random

import akka.actor.ActorSystem
import akka.stream.Supervision.{Resume, Stop, Restart}
import akka.stream.{ActorAttributes, Materializer}
import akka.stream.scaladsl.{RestartSource, Sink, Source}

import scala.concurrent.duration._

object FaultTolerance extends App{
  implicit val system = ActorSystem("FaultTolerance")
  implicit val materializer = Materializer

  val faultSource = Source(1 to 10).map(element => if(element == 6) throw new RuntimeException else element)

//  faultSource.log("trackingError").to(Sink.ignore).run() // Thrown new error when count to 6

  //2. Recover with a value
  faultSource.recover{
    case _: RuntimeException => 6
  } .log("Gracefully Terminated")
    .to(Sink.ignore)
//    .run()   // When count to 6 and see an error, recover it with value 6 and terminate stream

  //3. Recover with another source
  val recoverSource = Source(6 to 10)
  faultSource.recoverWithRetries(3, {
    case _: RuntimeException => recoverSource
  }) .log("recoverSource")
    .to(Sink.ignore)
  //  .run() // When count to 6 and see an error, recover it with another source in case and continuous with new source

  //4. Backoff supervision
  val restartSource = RestartSource.onFailuresWithBackoff(
    minBackoff = 1 second,
    maxBackoff = 30 second,
    randomFactor = 0.2,
  )(() => {
    val randomNumber = new Random().nextInt(20)
    Source(1 to 10).map(e => if(e == randomNumber) throw new RuntimeException else e)
  }) .log("RestartBackoff")
    .to(Sink.ignore)
//    .run()

  //5. Supervision strategy
  val numbers = Source(1 to 20).map(e => if(e == 13) throw new RuntimeException("bad number") else e).log("supervision")
  val supervisionNumber = numbers.withAttributes(ActorAttributes.supervisionStrategy{
    //partial function
    /*
    Resume: Skip the fail element
    Stop: Stop the stream
    Restart: Resume + clears internal state
     */
    case _: RuntimeException => Resume
    case _ => Stop
  })
  supervisionNumber.to(Sink.ignore).run()
}
