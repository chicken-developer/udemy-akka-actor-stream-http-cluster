package Akka_Stream.part5_Advanced

import akka.actor.ActorSystem
import akka.stream.scaladsl.{BroadcastHub, Keep, MergeHub, Sink, Source}
import akka.stream.{KillSwitches, Materializer}

import scala.concurrent.duration._

object DynamicStreamHandling extends App {
  implicit val system = ActorSystem("DynamicStreamHanding")
  implicit val materializer = Materializer

  //#1. Kill Switch
  val killFlow = KillSwitches.single[Int]
  val counter = Source(Stream from 1).throttle(1, 1 second).log("counter")
  val sink = Sink.ignore

//  val killSwitches = counter
//    .viaMat(killFlow)(Keep.right)
//    .to(sink)
//    .run()
//  import system.dispatcher
//  system.scheduler.scheduleOnce(3 second){
//    killSwitches.shutdown()
//  }

  val anotherCounter = Source(Stream.from(1)).throttle(2, 1 second).log("AnotherCounter")
  val sharedKillSwitch = KillSwitches.shared("oneButtonToRuleThemAll")

//  counter.via(sharedKillSwitch.flow).runWith(Sink.ignore)
//  anotherCounter.via(sharedKillSwitch.flow).runWith(Sink.ignore)
//  import system.dispatcher
//  system.scheduler.scheduleOnce(5 second){
//    sharedKillSwitch.shutdown()
//  }

  //#2. MergeHub
//  val dynamicMerge = MergeHub.source[Int]
//  val materializedSink = dynamicMerge.to(Sink.foreach[Int](println)).run()
//  counter.runWith(materializedSink)

  //#3. BroadcastHub
//  val dynamicBroadcast = BroadcastHub.sink[Int]
//  val materializedValue = Source(1 to 100).runWith(dynamicBroadcast)
//  materializedValue.runWith(Sink.ignore)
//  materializedValue.runWith(Sink.foreach[Int](println))

  //Ex: Combine broadcastHub and mergeHub
  //I not understand this lesion now
  //TODO: Need relearn this lesion again

  val merge = MergeHub.source[String]
  val broadcast = BroadcastHub.sink[String]
  val (publisherPort, customerPort) = merge.toMat(broadcast)(Keep.both).run()

  customerPort.runWith(Sink.foreach[String](e => println(s"I received an element: $e")))
  customerPort
    .map(e => e.length)
    .runWith(Sink.foreach[Int](n => println(s"I have receive a number $n")))

  Source(List("Akka", "is", "powerful", "toolkit")).runWith(publisherPort)
  Source(List("I", "love", "Stream")).runWith(publisherPort)

}
