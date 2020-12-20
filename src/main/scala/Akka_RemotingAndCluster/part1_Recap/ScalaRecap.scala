package Akka_RemotingAndCluster.part1_Recap

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}
object ScalaRecap extends App{
  // Take notes:
  /*
  1.
   */
  println("Hello")
  //val
  val aCondition: Boolean = false
  def myFunc(x: Int): Int = {
    x * 2
  }

  import scala.concurrent.ExecutionContext.Implicits.global
  val future = Future {
    20
  }
  future.onComplete {
    case Success(value) => println("Success")
    case Failure(exception) => println("Fail")
  }

}
