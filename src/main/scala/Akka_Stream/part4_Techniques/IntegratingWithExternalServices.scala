package Akka_Stream.part4_Techniques

import java.util.Date

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import akka.util.Timeout

import scala.concurrent.Future
import scala.concurrent.duration._

object IntegratingWithExternalServices extends App {
  implicit val system = ActorSystem("IntegratingWithExternalServices")
  implicit val materializer = Materializer
  // import system.dispatcher // Not recommend for mapAsync
  implicit val dispatcher = system.dispatchers.lookup("my-dispatcher")

  case class PagerEvent(application: String, description: String, date: Date)
  val eventList = List(
    PagerEvent("AkkaStream","A new source was detected", new Date),
    PagerEvent("AkkaHTTP","Server is not responding", new Date),
    PagerEvent("AkkaHTTP","Server is restarting", new Date),
    PagerEvent("AkkaCluster","Need more money to learn continues", new Date)
  )
  val eventSource = Source(eventList)
  object PagerService {
    private val engineers = List("Martine", "Donal Trump", "Mickey", "Harry Potter")
    private val emails = Map(
      "Martine" -> "martine@gmail.com",
      "Donal Trump" -> "trumptrumptrump@gmail.com",
      "Mickey" -> "mickey@mouse.com",
      "Harry Potter" -> "potter@mail.com"
    )
    def processEvent(pagerEvent: PagerEvent)= Future {
      val engineerIndex = (pagerEvent.date.toInstant.getEpochSecond / (24 * 3600)) % engineers.length
      val engineer = engineers(engineerIndex.toInt)
      val engineerEmail = emails(engineer)

      println(s"Sending to engineer $engineerEmail a high priority notification $pagerEvent")
      Thread.sleep(1000)
      //Return engineer email paged
      engineerEmail
    }
  } // Finished object

  val infraEvents = eventSource.filter(_.application == "AkkaHTTP")
  //parallelism input parameters is how many future can run at the same time
  val pagedEngineerEmails = infraEvents.mapAsync(4)(event => PagerService.processEvent(event))
  //=> Guarantee the relative order. If not want, code will like:
  //  val pagedEngineerEmails_DontGuarantee = infraEvents.mapAsyncUnordered(4)(event => PagerService.processEvent(event))
  val outputSink = Sink.foreach[String](email => println(s"Successfully sent to email: $email"))
//  pagedEngineerEmails.to(outputSink).run()

  class PageActor extends Actor with ActorLogging {
    private val engineers = List("Martine", "Donal Trump", "Mickey", "Harry Potter")
    private val emails = Map(
      "Martine" -> "martine@gmail.com",
      "Donal Trump" -> "trumptrumptrump@gmail.com",
      "Mickey" -> "mickey@mouse.com",
      "Harry Potter" -> "potter@mail.com"
    )
    private def processEvent(pagerEvent: PagerEvent)=  {
      val engineerIndex = (pagerEvent.date.toInstant.getEpochSecond / (24 * 3600)) % engineers.length
      val engineer = engineers(engineerIndex.toInt)
      val engineerEmail = emails(engineer)

      log.info(s"Sending to engineer $engineerEmail a high priority notification $pagerEvent")
      Thread.sleep(1000)
      engineerEmail
    }
    override def receive: Receive = {
      case pagerEvent: PagerEvent =>
        sender() ! processEvent(pagerEvent)
    }
  }
  import akka.pattern.ask
  implicit val timeOut =Timeout(3 second)
  val pagerActor = system.actorOf(Props[PageActor],"pageActor")
  val alertNativePagerEvent = infraEvents.mapAsync(parallelism = 4)(event => (pagerActor ? event).mapTo[String])
  alertNativePagerEvent.to(outputSink).run()
}
