package Akka_Essentials.part1_AkkaActor

import Akka_Essentials.part1_AkkaActor.ActorBehaviors_Excercise.Person.LiveTheLife
import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ActorBehaviors_Excercise extends App {

  // Counter excercise
  object Counter {

    case object Increment

    case object Decrement

    case object Print

  }

  class Counter extends Actor {

    import Counter._

    var count = 0

    override def receive: Receive = {
      case Increment => count += 1
      case Decrement => count -= 1
      case Print => println(s"[Counter] Current count is : $count")
    }
  }

  import Counter._

  val system = ActorSystem("CountSystem")
  val count = system.actorOf(Props[Counter], "Counter")
//  (1 to 5).foreach(_ => count ! Increment)
//  (1 to 2).foreach(_ => count ! Decrement)
//  count ! Print

  //Bank accout excercise
  object BankAccount {

    case class Deposit(amount: Int)

    case class Withdraw(amount: Int)

    case object Statement

    case class TransactionSuccess(message: String)

    case class TransactionFailure(reason: String)

  }

  class BankAccount extends Actor {

    import BankAccount._

    var funds = 0

    override def receive: Receive = {
      case Deposit(amount) =>
        if (amount < 0) sender() ! TransactionFailure(s"Fail to deposit with:, $amount")
        else {
          funds += amount
          sender() ! TransactionSuccess(s"Transfer success, $amount")
        }

      case Withdraw(amount) =>
        if(amount < 0) sender() ! TransactionFailure(s"Fail to withdraw with < 0 $amount")
        else
          if (funds < amount) sender() ! TransactionFailure(s"Fail to withdraw > balance, $amount")
        else {
          funds -= amount
          sender() ! TransactionSuccess(s"Withdraw success with $amount")
        }

      case Statement => sender() ! s"Current balance: $funds"
    }
  }

  object Person{
    case class LiveTheLife(account: ActorRef)
  }

  class Person extends Actor{
    import Person._
    import BankAccount._
    override def receive: Receive = {
      case LiveTheLife(account) => //Implicit here
        account ! Deposit(-1)
        account ! Deposit(1000000)
        account ! Deposit(2000)
        account ! Withdraw(6000000)
        account ! Withdraw(50000)
        account ! Statement
      case message => println(message.toString)

    }
  }

  val account = system.actorOf(Props[BankAccount], "bankAccount")
  val person = system.actorOf(Props[Person], "BillGate")

  person ! LiveTheLife(account)
}
