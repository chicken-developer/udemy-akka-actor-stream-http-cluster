package Akka_Stream.part4_Techniques

import java.util.Date

import akka.actor.{ActorSystem, actorRef2Scala}
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Sink, Source}

object AdvancedBackpressure extends App{
  implicit val system = ActorSystem("AdvancedBackpressure")
  implicit val materializer = Materializer

  val controlledFlow = Flow[Int].map(_ * 2)

  case class PagerEvent(description: String, date: Date, nInstance: Int = 1)
  case class Notification(email: String, pagerEvent: PagerEvent)

  val events = List(
    PagerEvent("Akka Stream starting at localhost:8080", new Date),
    PagerEvent("Akka Stream stopped", new Date),
    PagerEvent("Akka HTTP version is running", new Date)
  )
  val eventSource = Source(events)
  val onCallEngineer = "masterteam.vn@gmail.com"
  def sendEmail(notification: Notification) = {
    println(s"Dear ${notification.email}, you have received an event: ${notification.pagerEvent}")
  }
  val notificationSink = Flow[PagerEvent].map(event => Notification(onCallEngineer, event))
    .to(Sink.foreach[Notification](sendEmail))

//  eventSource.to(notificationSink).run()

  def sendEmailSlowly(notification: Notification) = {
    Thread.sleep(1000)
    println(s"Dear ${notification.email}, you have received an event: ${notification.pagerEvent}")
  }
  val aggregateNotificationFlow = Flow[PagerEvent]
    .conflate((event_01, event_02) => {
      val nInstance = event_01.nInstance + event_02.nInstance
      PagerEvent(s"You have $nInstance need to attention",new Date, nInstance)
    })
    .map(resourceEvent => Notification(onCallEngineer, resourceEvent))
//    eventSource.via(aggregateNotificationFlow).async.to(Sink.foreach[Notification](sendEmailSlowly)).run()

  import scala.concurrent.duration._
  val slowlySource = Source(Stream.from(1)).throttle(1, 1 second)
  val hungrySink = Sink.foreach[Int](println)
  val extrapolator = Flow[Int].extrapolate(element => Iterator.from(element))
  val repeater = Flow[Int].extrapolate(element => Iterator.continually(element))
//  slowlySource.via(extrapolator).to(hungrySink).run()
  slowlySource.via(repeater).to(hungrySink).run()
 //try this:  Flow[Int].expand()
}
