package Akka_HTTP.part2_HighLevelServer

import Akka_HTTP.part1_LowLevelServer.HttpsContext
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.ContentNegotiator.Alternative.ContentType
import akka.http.scaladsl.server.{Route, RouteConcatenation}
import akka.stream.Materializer

object HighLevelIntro extends App {
  implicit val system = ActorSystem("HighLevelIntro")
  implicit val materializer = Materializer

  import  system.dispatcher

  //1. First concept: Directives
  import akka.http.scaladsl.server.Directives._
  val simpleRoute: Route = {
    path("home"){
      complete(StatusCodes.OK)
    }
  }
  val getRoutePath: Route = {
    path("home"){
      get{
        complete(StatusCodes.OK)
      }
    }
  }

  //2. Chaining directives with ~
  val chainedRoute: Route =
    path("myEndPoint"){
      
      get{
        complete(StatusCodes.OK)
      } ~ // ~ meaning otherwise - very important
      // because if not have ~, but have multi expression like get, post,...
      // scala only return last expression, in this case that is post method.
        post{
          complete(StatusCodes.Forbidden)
        }
    } ~ // can using with path or route
      path("home"){
        complete(
          HttpEntity(
            ContentTypes.`text/html(UTF-8)`,
              """
              |<html>
              | <body>
              |   Hello from high level API
              | <body>
              |</html>
              |""".stripMargin
          )
        )
      }// Routing tree
  Http().bindAndHandle(simpleRoute, "localhost",8080, HttpsContext.httpsConnectionContext)
}
