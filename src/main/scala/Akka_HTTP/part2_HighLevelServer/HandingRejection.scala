package Akka_HTTP.part2_HighLevelServer

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.stream.Materializer
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Rejection, RejectionHandler}

object HandingRejection extends App{
  implicit val system = ActorSystem("HandingRejection")
  implicit val materialier = Materializer

  val simpleRoute =
    path("api" / "myEndpoint") {
      get {
        complete(StatusCodes.OK)
      } ~
        parameter('id){ _ =>
          complete(StatusCodes.OK)
        }
    }

  val badRequestHandler : RejectionHandler = { rejections: Seq[Rejection] =>
    println(s"I have received some rejection: $rejections")
    Some(complete(StatusCodes.BadRequest))
  }
  val forbiddenRequestHandler : RejectionHandler = { rejections: Seq[Rejection] =>
    println(s"I have received some rejection: $rejections")
    Some(complete(StatusCodes.Forbidden))
  }

}
