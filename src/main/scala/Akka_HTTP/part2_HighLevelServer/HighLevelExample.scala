package Akka_HTTP.part2_HighLevelServer

import akka.http.scaladsl.server.Directives._
import Akka_HTTP.part1_LowLevelServer.{Guitar, GuitarDB, GuitarStoreJsonProtocol}
import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.pattern.ask
import akka.stream.Materializer
import akka.util.Timeout

import scala.concurrent.Future
import scala.concurrent.duration._

object HighLevelExample extends App with GuitarStoreJsonProtocol {
  implicit val system = ActorSystem("HighLevelExample")
  implicit val materializer = Materializer
  implicit val timeout = Timeout(3 seconds)
  import GuitarDB._
  import system.dispatcher
  /*
    *Endpoint
      GET api/guitars
      GET api/guitar?id=x
      GET api/guitar/x
      GET api/guitar/inventory?inStock = true
   * */
  val guitarDB = system.actorOf(Props[GuitarDB],"GuitarDB")
  val guitarList = List(
    Guitar("Fender_01","Stratocaster_01"),
    Guitar("Fender_02","Stratocaster_02"),
    Guitar("Fender_03","Stratocaster_03"),
    Guitar("Fender_04","Stratocaster_04"),
    Guitar("Fender_05","Stratocaster_05"),
    Guitar("Fender_06","Stratocaster_06")
  )
  guitarList.foreach{ guitar =>
    guitarDB ! CreateGuitar(guitar)
  }

  import spray.json._
  val guitarServerRoute =
    path("api" / "guitar") { //GET api/guitar?id=x
      parameter('id.as[Int]){(guitarID: Int) =>
          get {
            val guitarFuture: Future[Option[Guitar]] = (guitarDB ? FindGuitarByID(guitarID)).mapTo[Option[Guitar]]
            val entityGuitar = guitarFuture.map{ guitarID =>
              HttpEntity(
                ContentTypes.`application/json`,
                guitarID.toJson.prettyPrint
              )
            }
            complete(entityGuitar)
          }
        } ~ //GET api/guitar/x
      path(IntNumber){ (guitarId: Int) =>
          get {
            val guitarFuture: Future[Option[Guitar]] = (guitarDB ? FindGuitarByID(guitarId)).mapTo[Option[Guitar]]
            val entityGuitar = guitarFuture.map { guitar =>
              HttpEntity(
                ContentTypes.`application/json`,
                guitar.toJson.prettyPrint
              )
            }
            complete(entityGuitar)
          }
        } ~ // GET api/guitar/inventory?inStock = true
          path("inventory"){
            parameter('inStock.as[Boolean]){(isInStock: Boolean) =>
              val guitarsFuture: Future[Option[Guitar]] = (guitarDB ? FindGuitarsInStock(isInStock)).mapTo[Option[Guitar]]
              // SandBox
              //End of SandBox
              val entitiesGuitar = guitarsFuture.map { guitar =>
                HttpEntity(
                  ContentTypes.`application/json`,
                  guitar.toJson.prettyPrint
                )
              }
              complete(entitiesGuitar)
            }
          } ~  //GET api/guitars
          get {
            val guitarFuture: Future[List[Guitar]] = (guitarDB ? FindAllGuitars).mapTo[List[Guitar]]
            val entityFuture = guitarFuture.map{ guitars =>
              HttpEntity(
                ContentTypes.`application/json`,
                guitars.toJson.prettyPrint
              )
            }
            complete(entityFuture)
          }

    }
  def toHttpEntity(payload: String) = HttpEntity(ContentTypes.`application/json`, payload)

  val simplifiedGuitarServerRoute =
    (pathPrefix("api" / "guitar") & get) {
      path("inventory") {
        parameter('inStock.as[Boolean]) { inStock =>
          complete(
            (guitarDB ? FindGuitarsInStock(inStock))
              .mapTo[List[Guitar]]
              .map(_.toJson.prettyPrint)
              .map(toHttpEntity)
          )
        }
      } ~
        (path(IntNumber) | parameter('id.as[Int])) { guitarId =>
          complete(
            (guitarDB ? FindGuitarByID(guitarId))
              .mapTo[Option[Guitar]]
              .map(_.toJson.prettyPrint)
              .map(toHttpEntity)
          )
        } ~
        pathEndOrSingleSlash {
          complete(
            (guitarDB ? FindAllGuitars)
              .mapTo[List[Guitar]]
              .map(_.toJson.prettyPrint)
              .map(toHttpEntity)
          )
        }
    }

  Http().bindAndHandle(simplifiedGuitarServerRoute, "localhost", 8080)
}
