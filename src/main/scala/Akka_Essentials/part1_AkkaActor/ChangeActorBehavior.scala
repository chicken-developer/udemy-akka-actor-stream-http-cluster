package Akka_Essentials.part1_AkkaActor

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ChangeActorBehavior extends App{

  object FussyKid{
    case class KidAccept()
    case class KidReject()
    val HAPPY = "happy"
    val SAD = "sad"
  }
  class FussyKid extends Actor{
    import FussyKid._
    import Mom._
    var state = HAPPY
    override def receive: Receive = {
      case Food(VEGETABLE) => state = SAD
      case Food(CHOCOLATE) => state = HAPPY
      case Ask(_) =>
        if(state == SAD) sender() ! KidReject
        else sender() ! KidAccept
    }
  }

  object Mom{
    case class MomStart(kidRef: ActorRef)
    case class Food(foodName: String)
    case class Ask(question: String) // Something like Do you like it ?
    val VEGETABLE = "veggies"
    val CHOCOLATE = "chocolate"
  }
  class Mom extends Actor{
    import Mom._
    import FussyKid._
    override def receive: Receive = {
      case MomStart(kidRef) =>
        kidRef ! Food(VEGETABLE)
        kidRef ! Ask("Do you want to play ?")
        kidRef ! Food(CHOCOLATE)
        kidRef ! Ask("Do you want to play ?")
      case KidAccept => println("My kid happy")
      case KidReject => println(s"My kid not happy")
    }
  }

  val actorSystem = ActorSystem("ChangeActorBehaviorSystem")
  val son = actorSystem.actorOf(Props[FussyKid], "fussyKid")
  val mom = actorSystem.actorOf(Props[Mom], "mom")

  import Mom._
  mom ! MomStart(son)
}
