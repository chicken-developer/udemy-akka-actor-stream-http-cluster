package Akka_HTTP.part2_HighLevelServer

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.stream.Materializer
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

case class Person(pin: Int, name: String)
import spray.json._

trait PersonJsonProtocol extends DefaultJsonProtocol{
  implicit val personJson = jsonFormat2(Person)
}

/*
  Exercise:
    GET /api/people
    GET /api/people/pin
    GET /api/people?pin=x
    POST /api/people with JSon payload

 */
object HighLevelExercise extends App with PersonJsonProtocol {
  val system = ActorSystem("HighLevelExercise")
  val materializer = Materializer
  import system.dispatcher

  val people = List(
    Person(1, "Alice"),
    Person(2, "John"),
    Person(3, "Jimmy"),
    Person(4, "Lily"),
    Person(5, "Pretty")
  )

  /*
    Exercise:
      GET /api/people
      GET /api/people/pin
      GET /api/people?pin=x
      POST /api/people with JSon payload

   */
  def toHttpEntity(payload: String): HttpEntity.Strict = HttpEntity(ContentTypes.`application/json`, payload)
  val personServerRoute: Route =
    pathPrefix("api" / "people"){
      get {
        (path(IntNumber) | parameter('pin.as[Int])){ (pin: Int) =>
          //TODO 1:
          complete(
            people.find(_.pin == pin)
              .map(_.toJson.prettyPrint)
              .map(toHttpEntity)
          )
        } ~
          pathEndOrSingleSlash{
            complete(
              HttpEntity(
                ContentTypes.`application/json`,
                people.toJson.prettyPrint
              )
            )
          }
      } ~
        (post & pathEndOrSingleSlash & extractRequest & extractLog) {(request, log) =>
          //TODO 3:
          reject
        }
    }
  println(people.toJson.prettyPrint)

}
