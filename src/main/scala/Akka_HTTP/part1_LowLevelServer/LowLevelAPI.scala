package Akka_HTTP.part1_LowLevelServer

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.IncomingConnection
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, HttpResponse, StatusCodes, Uri}
import akka.http.scaladsl.server.ContentNegotiator.Alternative.ContentType
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Sink}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object LowLevelAPI extends App {
  implicit val system = ActorSystem("LowLevelAPI")
  implicit val materializer = Materializer
import system.dispatcher

 val serverSource = Http().bind("localhost",8000)
  val connectionSink = Sink.foreach[IncomingConnection]{ connection =>
    println(s"Accepted incoming connection from ${connection.remoteAddress}")
  }
  val serverBindingFuture = serverSource.to(connectionSink).run()
  serverBindingFuture.onComplete{
    case Success(binding) =>
      println("Server binding successful")
      binding.terminate(2 second)
    case Failure(ex) =>
      println(s"Server binding fail: $ex")
  }

  //Method_01: Synchronously serve HTTP responses
  val requestHandler: HttpRequest => HttpResponse ={
    case HttpRequest(HttpMethods.GET, _, _, _, _) =>
      HttpResponse(
        StatusCodes.OK, //HTTP code: 200
        entity = HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          """
            |<html>
            | <body>
            |     Hello from Akka HTTP!
            | </body>
            |</html>
            |""".stripMargin
        )
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
            |     Hello from Akka HTTP!
            | </body>
            |</html>
            |""".stripMargin
        )
      )
  }

  //  val httpSyncConnectionHandler = Sink.foreach[IncomingConnection]{ connection =>
  //    connection.handleWithSyncHandler(requestHandler)
  //  }
  //  Http().bind("localhost",8080).runWith(httpSyncConnectionHandler)
  //Short version
  Http().bindAndHandleSync(requestHandler, "localhost",8080)

  //Method_02: Asynchronously serve HTTP responses via Future
  val asyncRequestHandler: HttpRequest => Future[HttpResponse] ={
    case HttpRequest(HttpMethods.GET, Uri.Path("/home"), _, _, _) =>
      Future(HttpResponse(
        StatusCodes.OK, //HTTP code: 200
        entity = HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          """
            |<html>
            | <body>
            |     Hello from Akka HTTP HomePage!
            | </body>
            |</html>
            |""".stripMargin
        )
      ))
    case request: HttpRequest =>
      request.discardEntityBytes()
      Future(HttpResponse(
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
      ))
  }
  Http().bindAndHandleAsync(asyncRequestHandler,"localhost",8081)

  //Method_03: Asynchronously serve HTTP responses via Akka Stream
  val streamBasedAsyncRequestHandler: Flow[HttpRequest, HttpResponse, _] = Flow[HttpRequest].map {
    case HttpRequest(HttpMethods.GET, Uri.Path("/home"), _, _, _) =>
      HttpResponse(
        StatusCodes.OK, //HTTP code: 200
        entity = HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          """
            |<html>
            | <body>
            |     Hello from Akka HTTP HomePage Stream!
            | </body>
            |</html>
            |""".stripMargin
        )
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

//  Http().bind("localhost",8082).runForeach{ connection =>
//    connection.handleWith(streamBasedAsyncRequestHandler)
//  }
  Http().bindAndHandle(streamBasedAsyncRequestHandler, "localhost",8082)
}
