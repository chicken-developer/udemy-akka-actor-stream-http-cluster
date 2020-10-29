package Akka_Stream.part4_Techniques

import akka.Done
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.stream.{CompletionStrategy, Materializer, OverflowStrategy}
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.Timeout

import scala.concurrent.duration._
object IntegratingWithActor extends App {
  implicit val system = ActorSystem("IntegratingWithActor")
  implicit val materializer = Materializer
  
  class SimpleActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case str: String =>
        log.info(s"Receive a string: $str")
        sender() ! s"$str$str"
      case number: Int =>
        log.info(s"Receive a number: $number")
        sender() ! (number * 2)
      case _ => log.info("Have error here")
    }
  }
  val simpleActor = system.actorOf(Props[SimpleActor], "simpleActor")
  val numberSource = Source(1 to 10)

  //Actor as flow
  implicit val timeOut = Timeout(2 seconds)
  val actorBasedFlow = Flow[Int].ask[Int](parallelism = 4)(simpleActor)

//  numberSource.via(actorBasedFlow).to(Sink.ignore).run()
//  numberSource.ask[Int](parallelism = 4)(simpleActor).to(Sink.ignore).run()

  //Actor as a source
  val actorPoweredSource: Source[Int, ActorRef] = Source.actorRef(
    completionMatcher = {
      case Done =>
        // complete stream immediately if we send it Done
        CompletionStrategy.immediately
    },
    // never fail the stream because of a message
    failureMatcher = PartialFunction.empty,
    bufferSize = 100,
    overflowStrategy = OverflowStrategy.dropHead)

  val materializedActorRef: ActorRef = actorPoweredSource.to(Sink.foreach[Int](number => println(s"Got number: $number"))).run()
  materializedActorRef ! 10
  //Terminated stream
  materializedActorRef ! akka.actor.Status.Success("Completed mission")

  //Actor as a destination/ sink
  // Need:
    /* - An init message
       - An ack message to confirm reception
       - A complete message
       - A function fo generate a message in case the stream throws an exception
     */

  case object StreamInit
  case object StreamAck
  case object StreamComplete
  case class StreamFail(ex: Throwable)

  class DestinationActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case StreamInit =>
        log.info("Stream Initialize")
        sender() ! StreamAck
      case StreamComplete =>
        log.info("Stream complete")
        context.stop(self)
      case StreamFail(ex) =>
        log.warning(s"Stream fail with an exception: $ex")
      case message =>
        log.info(s"Receive a special message: $message")
        sender() ! StreamAck
    }
  }
  val destinationActor = system.actorOf(Props[DestinationActor], "destinationActor")
  val actorPoweredSink = Sink.actorRefWithBackpressure[Int](
    destinationActor,
    onInitMessage = StreamInit,
    ackMessage = StreamAck,
    onCompleteMessage = StreamComplete,
    onFailureMessage = throwable => StreamFail(throwable)
  )
  Source(1 to 10).to(actorPoweredSink).run()
}
