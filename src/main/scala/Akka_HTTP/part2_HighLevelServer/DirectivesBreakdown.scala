package Akka_HTTP.part2_HighLevelServer

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpRequest, StatusCodes}
import akka.http.scaladsl.server.Route
import akka.stream.Materializer

object DirectivesBreakdown extends App{
  implicit val system = ActorSystem("DirectivesBreakdown")
  implicit val materializer = Materializer
  import system.dispatcher
  import akka.http.scaladsl.server.Directives._

  /**
   * Type 01: Filtering directives
   **/
  val simpleHttpMethodRoute = // have method only, not have path
    post { //equivalent directives for g et, put, patch, delete, head, options
      complete(StatusCodes.Forbidden)
    }

  val simplePathMethodRoute: Route =
    path("home"){
      get {
        complete(
          HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          """
            |<html>
            | <body>
            |   Hello from Akka, this is homepage
            | </body>
            |<!html>
            |""".stripMargin
         )
        )
      }
    }

  val complexRoute: Route =
    path("api" / "about"){ // localhost:8080/api/about
      get {
        complete(
          HttpEntity(
            ContentTypes.`text/html(UTF-8)`,
            """
              |<html>
              | <body>
              |   Hello from Akka, this is api/about
              | </body>
              |<!html>
              |""".stripMargin
          )
        )
      }
    }

  val notConfuse: Route =
    path("api/about"){ //IF character / is in "" -> it will be encode to URL like: localhost:8080/api%2Fabout
      get {
        complete(
          HttpEntity(
            ContentTypes.`text/html(UTF-8)`,
            """
              |<html>
              | <body>
              |   Hello from Akka, this is api%2Fabout
              | </body>
              |<!html>
              |""".stripMargin
          )
        )
      }
    }

  val pathEndRoute =
    pathEndOrSingleSlash( // valid for localhost:8080 or localhost:8080/
      complete(StatusCodes.OK)
    )

  /**
   * Type 02: Extraction directives
   **/
 // Something like GET on localhost:8080/api/item/42
  val extractRoute: Route =
    path("api" / "item" / IntNumber) { (itemNumber: Int) =>
      println(s"I have received some items: $itemNumber")
      complete(StatusCodes.OK)
    }

  val multiExtractRoute =
    path("api" / "order" / IntNumber / IntNumber ){ (itemID, prince) =>
      println(s"I have received some items and its prince: $itemID - $prince")
      complete(StatusCodes.OK)
    }

  //Query parameter
  //Like localhost:8080/api/item?id=12
  val queryParameterRoute: Route =
    path("api" / "item" ) {
      //parameter("id") { (itemID: String) =>
      // parameter("id".as[Int]) { (itemID: Int) => //safely convert
      parameter('id.as[Int]) { (itemID: Int) => //get performance
        println(s"User need find item with ID: $itemID")
        complete(StatusCodes.OK)
      }
    }

  val extractRequestRoute =
    extractRequest { (httpRequest: HttpRequest) =>
      println(s"I have received a request: $httpRequest")
      complete(StatusCodes.OK) // OK for all http request
    }


  /**
   * Type 03: Composite directives
   **/
  val simpleNestedRoute =
    path("api" / "item"){
      get {
        complete(StatusCodes.OK)
      }
    }

  val compactSimpleRoute = (path("api" / "item") & get) {
    complete(StatusCodes.OK)
  }

  val compactExtractRequestRoute =
    (path("controlEndpoint") & extractRequest & extractLog) { (request, log) =>
      log.info(s"I have received a request : $request")
      complete(StatusCodes.OK)
  }

  //--- If want api/about and api/aboutUs have same HTML result
  val repeatRoute =
    path("about"){
      complete(StatusCodes.OK)
    } ~
      path("aboutUS") {
        complete(StatusCodes.OK)
      }

  val dryRoute =
    (path("about") | path("aboutUS")) {
      complete(StatusCodes.OK)
    }

  //If want: blog.com/8 and blog.com?id=8
  val blogByIDRoute =
    path(IntNumber) { (blogID: Int) =>
      //Enter logic here
      complete(StatusCodes.OK)
    }

  val blogByQueryParam =
    parameter('id.as[Int]){ (blogID: Int) =>
      //Code logic here
      complete(StatusCodes.OK)
    }

  val combineBlogRequest =
    (path(IntNumber) | parameter('id.as[Int])){ (blogID: Int) => // Must extract same value type
      //Code logic here
      complete(StatusCodes.OK)
    }

  /**
   * Type 04: Actionable directives
   **/
  val completeRoute = complete(StatusCodes.OK)

  val failRoute =
    path("unSupported") {
      failWith(throw new RuntimeException("This page not support long time ! ")) // Return 500
    }

  val routeWithReject =
    path("home"){
      reject // reject - not reject()
    } ~
      path("index"){
        complexRoute // === complete(StatusCodes.OK)
      }
  Http().bindAndHandle(extractRequestRoute, "localhost",8080)

}
