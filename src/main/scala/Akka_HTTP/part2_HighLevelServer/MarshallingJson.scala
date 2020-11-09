package Akka_HTTP.part2_HighLevelServer

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import spray.json._
import akka.stream.Materializer
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.duration._

case object GameAreaMap{
  case object GetAllPlayers
  case class GetPlayer(nickname: String)
  case class GetPlayerByClass(characterClass: String)
  case class AddPlayer(player: Player)
  case class RemovePlayer(player: Player)
  case object OperationSuccess
}

case class Player(nickname: String, characterClass: String, level: Int)

class GameArea extends Actor with ActorLogging{
  import GameAreaMap._
  var players: Map[String, Player] = Map[String, Player]()
  override def receive: Receive = {
    case GetAllPlayers =>
      log.info("Getting all player...")
      sender() ! players.values.toList
    case GetPlayer(nickname) =>
      log.info(s"Getting player with nickname: $nickname")
//      sender() ! players.values.filter(_.nickname == nickname)
      sender() ! players.get(nickname)
    case GetPlayerByClass(characterClass) =>
      log.info(s"Getting player with class : $characterClass")
      sender() ! players.values.toList.filter(_.characterClass == characterClass)
    case AddPlayer(player) =>
      log.info(s"Try to add player: $player")
      players = players + (player.nickname -> player)
      sender() ! OperationSuccess
    case RemovePlayer(player) =>
      log.info(s"Try to remove player: $player")
      players - player.nickname
      sender() ! OperationSuccess
  }
}

trait PlayerJsonProtocol extends DefaultJsonProtocol{
  implicit val format: RootJsonFormat[Player] = jsonFormat3(Player)
}

object MarshallingJson extends App
  with PlayerJsonProtocol
  with SprayJsonSupport {
  implicit val system = ActorSystem("MarshallingJson")
  implicit val materializer = Materializer

  import system.dispatcher
  import GameAreaMap._
  val myGameAreaMap = system.actorOf(Props[GameArea],"myGameAreaMap")
  val playersList = List (
    Player("paradise", "Barbarian", 180),
    Player("killer_07", "Devil", 180),
    Player("tanker_man_001", "Barbarian",120),
    Player("devil_may_cry","Devil",180),
    Player("saveTheWorld","Angel",160)
  )
  playersList.foreach{ player =>
    myGameAreaMap ! AddPlayer(player)
  }
//  def addPlayer(player: Player) = myGameAreaMap ! AddPlayer(player)
//  playersList.map(addPlayer)

  /*
  Exercise: -> All return types are json
    GET /api/player -> return all players
    GET /api/player/(nickname) -> return player with nickname
    GET /api/player?nickname=x -> Same
    GET /api/player/class/(characterClass) -> return all players with characterClass
    POST /api/player with Json payload -> Add player from payload to map
    DELETE /api/player with Json payload -> Remove player from map
   */

  implicit val timeout = Timeout(2 seconds)
  val gameAreaMapRoute =
    pathPrefix("api" / "player"){
      get {
        (parameter('nickname.as[String]) | path(Segment)){ (nickname: String) =>
          complete((myGameAreaMap ? GetPlayer(nickname)).mapTo[Option[Player]])
        } ~
          path("class" / Segment){ (characterClass: String) =>
            val playerByClassFuture = (myGameAreaMap ? GetPlayerByClass(characterClass)).mapTo[List[Player]]
            complete(playerByClassFuture)
              }
          } ~
          pathEndOrSingleSlash {
            complete((myGameAreaMap ? GetAllPlayers).mapTo[List[Player]])
          }
      } ~
        post {
         entity(as[Player]){ player =>
           complete((myGameAreaMap ? AddPlayer(player)).map(_ => StatusCodes.OK))
         }
        } ~
        delete {
          entity(as[Player]){ player =>
            complete((myGameAreaMap ? RemovePlayer(player)).map(_ => StatusCodes.OK))
          }
        }


  Http().bindAndHandle(gameAreaMapRoute, "localhost", 8080)
}
