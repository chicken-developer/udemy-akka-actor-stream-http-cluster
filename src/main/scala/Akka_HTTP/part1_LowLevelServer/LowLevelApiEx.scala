package Akka_HTTP.part1_LowLevelServer

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri._
import akka.http.scaladsl.model.headers.Location
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, HttpResponse, StatusCodes, Uri}
import akka.stream.Materializer
import akka.stream.scaladsl.Flow

object LowLevelApiEx extends App{
  implicit val system = ActorSystem("LowLevelApiEx")
  implicit val materializer = Materializer

  val requestHandler: Flow[HttpRequest, HttpResponse, _] = Flow[HttpRequest].map {
        //First ex
    case HttpRequest(HttpMethods.GET, Path("/about"), _, _, _) =>
      HttpResponse(
        StatusCodes.OK, //HTTP code: 200
        entity = HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          """
            |<html>
            | <body>
            |     This website power by Akka Toolkit !
            | </body>
            |</html>
            |""".stripMargin
        )
      )

    case HttpRequest(HttpMethods.GET,Path("/"), _, _, _) =>
      HttpResponse(
//        StatusCodes.OK, => That is default
        entity = HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          """
            |<html>
            | <body>
            |     Hello, this is Front Door
            | </body>
            |</html>
            |""".stripMargin
        )
      )

      //Navigation to another page
    case HttpRequest(HttpMethods.GET, Path("/search"), _, _, _) =>
      HttpResponse(
        StatusCodes.Found,
        headers = List(Location("http://google.com"))
      )

    case request: HttpRequest =>
      request.discardEntityBytes()
      HttpResponse(
        StatusCodes.NotFound, //HTTP code: 404
        entity = HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          """
            |<html>
            | <body>
            |     The resource not found !
            | </body>
            |</html>
            |""".stripMargin
        )
      )
  }

//  Http().bindAndHandle(requestHandler, "localhost",8388)
  //Shut down the server
  import system.dispatcher
  val bindingFuture = Http().bindAndHandle(requestHandler, "localhost", 8388)
  bindingFuture.flatMap(binding => binding.unbind())
    .onComplete(_ => system.terminate())
}
