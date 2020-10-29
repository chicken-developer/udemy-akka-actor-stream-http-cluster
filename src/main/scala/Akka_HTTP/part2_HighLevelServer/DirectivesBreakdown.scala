package Akka_HTTP.part2_HighLevelServer

import akka.actor.ActorSystem
import akka.stream.Materializer

object DirectivesBreakdown extends App{
  implicit val system = ActorSystem("DirectivesBreakdown")
  implicit val materializer = Materializer
  import system.dispatcher
  import akka.http.scaladsl.server.Directives._

  /**
   * Type 01: Filtering directives
   **/
  val simpleHttpMethodRoute =
    post { 
      
    }
  
   
}
