package Akka_HTTP.part1_LowLevelServer

import Akka_HTTP.part1_LowLevelServer.GuitarDB.{CreateGuitar, FindGuitarByID}
import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, HttpResponse, StatusCodes, Uri}
import akka.pattern.ask
import akka.stream.Materializer
import akka.util.Timeout
import spray.json._

import scala.concurrent.duration._
import scala.concurrent.Future

case class Guitar(make: String, model: String, quantity: Int = 0)

object GuitarDB{
  case class CreateGuitar(guitar: Guitar)
  case class GuitarCreated(id: Int)
  case class FindGuitarByID(id: Int)
  case class FindGuitarsInStock(inStock: Boolean)
  case class AddQuantity(id: Int, quantity: Int)
  case object FindAllGuitars
}
class GuitarDB extends Actor with ActorLogging{
  import GuitarDB._
  var guitars: Map[Int, Guitar] = Map()
  var currentGuitarID: Int = 0

  override def receive: Receive = {
    case FindAllGuitars =>
      log.info("Searching for all guitar..")
      sender() ! guitars.values.toList
    case FindGuitarByID( id) =>
      log.info("Searching guitar by ID")
      sender() ! guitars.get( id)
    case CreateGuitar(guitar) =>
      log.info(s"Creating guitar $guitar with id $currentGuitarID")
      guitars = guitars + (currentGuitarID -> guitar)
      sender() ! GuitarCreated(currentGuitarID)
      currentGuitarID += 1
    case AddQuantity(id, quantity) =>
      log.info(s"Trying to add $quantity items from guitar ID: $id")
      val guitar: Option[Guitar] = guitars.get(id)
      val newGuitar: Option[Guitar] =guitar.map{
        case Guitar(make, model, q) => Guitar(make, model, quantity + q)
      }
      newGuitar.foreach(guitar => guitars = guitars + (id -> guitar))
      sender() ! newGuitar
    case FindGuitarsInStock(inStock) =>
      log.info(s"Searching for all guitar result ${if(inStock) "in" else "out of"} stock")
      if(inStock) sender() ! guitars.values.filter(_.quantity > 0 )
      else sender() ! guitars.values.filter(_.quantity == 0)

  }
}
trait GuitarStoreJsonProtocol extends DefaultJsonProtocol {
  implicit val guitarFormat = jsonFormat3(Guitar) //Using jsonFormat2 because class have 2 agrs, if more, using another like jsonFormat3, jsonFormat4,...
}
object LowLevelRest extends App with GuitarStoreJsonProtocol {
  implicit val system = ActorSystem("LowLevelRest")
  implicit val materializer = Materializer
  import system.dispatcher

/*
localhost:8080/api/guitar
GET: Return all guitar for the client
POST: Insert guitar from client into database
 USING: JSON - MARSHALLING
 */

  //Marshalling
//  val simpleGuitar = Guitar("Fender","Stratocaster")
//  println(simpleGuitar.toJson.prettyPrint)
//
//  //Unmarshalling
//  val jsonGuitar =
//    """
//      |{
//      |  "make": "Fender",
//      |  "model": "Stratocaster"
//      |}
//      |""".stripMargin
//  println(jsonGuitar.parseJson.convertTo[Guitar])

  /*
  Server code
   */
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
  implicit val timeout = Timeout(2 second)
  def getGuitar(query: Query): Future[HttpResponse] ={
    val guitarID = query.get("id").map(_.toInt)
    guitarID match {
      case None => Future(HttpResponse(StatusCodes.NotFound))
      case Some(id: Int ) =>
        val guitarFuture = (guitarDB ? FindGuitarByID(id)).mapTo[Option[Guitar]]
        guitarFuture.map{
          case None =>
            HttpResponse(StatusCodes.NotFound)
          case Some(guitar) =>
            HttpResponse(
              entity = HttpEntity(
                ContentTypes.`application/json`,
                guitar.toJson.prettyPrint
              )
            )
        }
    }
  }
  import GuitarDB._
//  implicit val defaultTimeout = Timeout(2 second)
  val requestHandler: HttpRequest => Future[HttpResponse] = {
    case HttpRequest(HttpMethods.POST, uri@Uri.Path("/api/guitar/inventory"), _, _, _) =>
      val query = uri.query()
      val guitarId: Option[Int] = query.get("id").map(_.toInt)
      val guitarQuantity: Option[Int] = query.get("quantity").map(_.toInt)
      val validGuitarResponseFuture: Option[Future[HttpResponse]] = for {
        id <- guitarId
        quantity <- guitarQuantity
      } yield {
        val newGuitarFuture: Future[Option[Guitar]] = (guitarDB ? AddQuantity(id, quantity)).mapTo[Option[Guitar]]
        newGuitarFuture.map(_ => HttpResponse(StatusCodes.OK))
      }
      validGuitarResponseFuture.getOrElse(Future(HttpResponse(StatusCodes.BadRequest)))

    case HttpRequest(HttpMethods.GET, uri@Uri.Path("/api/guitar/inventory"), headers, entity, protocol) =>
      val query = uri.query()
      val inStockOption = query.get("inStock").map(_.toBoolean)
      inStockOption match {
        case Some(inStock) =>
          val guitarsFuture: Future[List[Guitar]] = (guitarDB ? FindGuitarsInStock(inStock)).mapTo[List[Guitar]]
          guitarsFuture.map{ guitars =>
            HttpResponse(
              entity = HttpEntity(
                ContentTypes.`application/json`,
                guitars.toJson.prettyPrint
              )
            )
          }
        case None => Future(HttpResponse(StatusCodes.BadRequest))
      }

    case HttpRequest(HttpMethods.GET, Uri.Path("/api/guitar"), _, _, _) =>
      println("Client require all guitars")
      val guitarFuture: Future[List[Guitar]] =  (guitarDB ? FindAllGuitars).mapTo[List[Guitar]]
      guitarFuture.map{ guitar =>
        HttpResponse(
          entity = HttpEntity(
            ContentTypes.`application/json`,
            guitar.toJson.prettyPrint
          )
        )
      }

    case HttpRequest(HttpMethods.POST, uri@Uri.Path("/api/guitar"), _, entity, _) =>

      val query = uri.query() //query object <=> Map[String, String]
      if(query.isEmpty){
        val strictEntityFuture = entity.toStrict(3 seconds)
        strictEntityFuture.flatMap{ strictData =>
          val dataJson = strictData.data.utf8String
          val guitar = dataJson.parseJson.convertTo[Guitar]

          val guitarCreatedFuture: Future[GuitarCreated] = (guitarDB ? CreateGuitar(guitar)).mapTo[GuitarCreated]
          guitarCreatedFuture.map { guitarCreated =>
            HttpResponse(
              StatusCodes.OK
            )
          }
        }
      }else{
        //Fetch guitar
        //localhost:8080/api/guitar?id=45
        getGuitar(query)
      }

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
  Http().bindAndHandleAsync(requestHandler,"localhost",8089)
}
