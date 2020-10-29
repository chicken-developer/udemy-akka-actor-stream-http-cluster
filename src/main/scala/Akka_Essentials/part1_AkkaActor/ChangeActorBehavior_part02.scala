package Akka_Essentials.part1_AkkaActor

import Akka_Essentials.part1_AkkaActor.ChangeActorBehavior_part02.Mom.{MomStart, VEGETABLE}
import akka.actor.{Actor, ActorRef, ActorSystem, Props, actorRef2Scala}

object ChangeActorBehavior_part02 extends App{

  object FussyKid{
    case class KidAccept()
    case class KidReject()
    val HAPPY = "happy"
    val SAD = "sad"
  }
  class FussyKid extends Actor{
    import FussyKid._
    import Mom._
    var state = HAPPY // MUTABLE variable here
    override def receive: Receive = {
      case Food(VEGETABLE) => state = SAD
      case Food(CHOCOLATE) => state = HAPPY
      case Ask(_) =>
        if( state == HAPPY) sender() ! KidAccept
        else sender() ! KidReject
    }
  }

  class StatelessFussyKid extends Actor{
    import FussyKid._
    import Mom._
    override def receive: Receive = HappyReceive
    def HappyReceive: Receive ={
      case Food(VEGETABLE) => context.become(SadReceive)
      case Food(CHOCOLATE) => // Still happy
      case Ask(_) => sender() ! KidAccept
    }

    def SadReceive: Receive ={
      case Food(VEGETABLE) =>
        //Some change here
        context.become(SadReceive, false)
      case Food(CHOCOLATE) => context.unbecome() //Reverting to previous behavior
      case Ask(_) => sender() ! KidReject
    }
  }

  object Mom{
    case class MomStart(kidRef: ActorRef)
    case class Food(food: String)
    case class Ask(question: String)
    val VEGETABLE = "vegetable"
    val CHOCOLATE = "chocolate"
  }
  class Mom extends Actor{
    import Mom._
    import FussyKid._
    override def receive: Receive = {
      case MomStart(kidRef) =>
        kidRef ! Food(VEGETABLE)
        kidRef ! Food(CHOCOLATE)
        kidRef ! Ask("Again, do you want to play ?")
      case KidReject => println("Why you sad my lovely son")
      case KidAccept => println("Are you happy now ?")
    }
  }
  val actorSystem = ActorSystem("ChangeActorBehavior_part02")
  val fussyKid = actorSystem.actorOf(Props[FussyKid])
  val statelessFussyKid = actorSystem.actorOf(Props[StatelessFussyKid])
  val mom = actorSystem.actorOf(Props[Mom])
//
//  mom ! MomStart(statelessFussyKid)

  //1. context.become(sadReceive, false) -> context.become can have one or two parameters
  /*
  IF context.become(sadReceive, true) -> normal, replace -> like
  state = happyReceive
  state->replace(sadReceive)

  IF context.become(sadReiceive, false) -> push to stack like:
  state = happyReceive
  state.push(sadReceive)
  When call to an actor, akka call top receive.If stack receive empty, akka call receive function
   */


  //// Exercises
  //1. Recreate  the Counter Actor with context.become and NO MUTABLE STATE
  object Counter{
    //Message of this object
    case object Increment
    case object Decrement
    case object Print
  }
  class Counter extends Actor{
    import Counter._
    override def receive: Receive = counterReceive(0)
    def counterReceive(currentCounter: Int): Receive ={
      case Increment =>
        println(s"Current counter increment: $currentCounter")
        context.become(counterReceive(currentCounter + 1))
      case Decrement =>
        println(s"Current counter decrement: $currentCounter")
        context.become(counterReceive(currentCounter - 1))
      case Print => println(s"Current counter is: $currentCounter")
    }
  }
  import Counter._
  val counter = actorSystem.actorOf(Props[Counter])
  (1 to 5).foreach(_ => counter ! Increment)
  (1 to 3).foreach(_ => counter ! Decrement)
  counter ! Print
  //If you want to rewrite a stateful actor to a stateless actor
  //You need to rewrite it's mutable state into the parameters of the received handler that you want to support


  //2. Create a vote system
  case class Vote(candidate: String)
  case object VoteStatusRequest
  case class VoteStatusReply(candidate: Option[String])
  class Citizen extends Actor{
    override def receive: Receive = {
      case Vote(c) => context.become(Voted(c))
      case VoteStatusRequest => sender() ! VoteStatusReply(None)
    }
    def Voted(candidate: String): Receive ={
      case VoteStatusRequest => sender() ! VoteStatusReply(Some(candidate))
    }
  }

  case class AggregateVotes(citizens: Set[ActorRef])
  class VoteAggregator extends Actor{

    override def receive: Receive = awaitingCommand
    def awaitingCommand: Receive ={
      case AggregateVotes(citizens) =>
        citizens.foreach(citizenRef => citizenRef ! VoteStatusRequest)
        context.become(awaitingStatuses(citizens,Map()))
    }

    def awaitingStatuses(stillWaiting: Set[ActorRef], currentStats: Map[String, Int]): Receive ={
      case VoteStatusReply(None) =>
        //A citizen hasn't vote yet
        sender() ! VoteStatusRequest
      case VoteStatusReply(Some(candidate)) =>
        val newStillWaiting = stillWaiting - sender()
        val currentVoteOfCandidate = currentStats.getOrElse(candidate, 0)
        val newStats = currentStats + (candidate -> (currentVoteOfCandidate + 1))
        if(newStillWaiting.isEmpty){
          println(s"[Aggregator] poll stats: $newStats")
        } else {
          context.become(awaitingStatuses(newStillWaiting,newStats))
        }

    }


  }

  val alice = actorSystem.actorOf(Props[Citizen])
  val bob = actorSystem.actorOf(Props[Citizen])
  val susan = actorSystem.actorOf(Props[Citizen])
  val natalie = actorSystem.actorOf(Props[Citizen])

  alice ! Vote("Martin")
  bob ! Vote("Martin")
  susan ! Vote("Trump")
  natalie ! Vote("Mark")

  val voteAggregator = actorSystem.actorOf(Props[VoteAggregator])
  voteAggregator ! AggregateVotes(Set(alice,bob,susan, natalie))
}
