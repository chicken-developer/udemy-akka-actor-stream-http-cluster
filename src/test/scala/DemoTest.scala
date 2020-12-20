
import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.testkit.{EventFilter, ImplicitSender, TestKit}
import com.typesafe.config.ConfigFactory

import org.scalatest.{BeforeAndAfterAll, WordSpecLike}
//Can import on test only...

class InterceptingLongsSpec extends  TestKit(ActorSystem("InterceptingLongsSpec",ConfigFactory.load().getConfig("interceptingLogMessages")))
  with ImplicitSender
  with WordSpecLike
  with BeforeAndAfterAll{

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }


  import InterceptingLongsSpec._
  val item = "Red Bull"
  val creditCard = "1234-1234-1234-1234"
  val invalidCreditCard = "0000-0000-0000-0001"
  "A check out flow" must{
    "correctly log the dispatch of an order" in{
      EventFilter.info(pattern = s"Order ID [0-9]+ for item $item has been dispatch", occurrences = 1) intercept {
        //Test code here
        val checkOutRef = system.actorOf(Props[CheckoutActor])
        checkOutRef ! Checkout(item, creditCard)
      }
    }

    "freak out if the payment is denied" in {
      EventFilter[RuntimeException](occurrences = 1) intercept{
        val checkOutRef = system.actorOf(Props[CheckoutActor])
        checkOutRef ! Checkout(item, invalidCreditCard)
      }
    }
  }

}

object InterceptingLongsSpec{
  case class Checkout(item: String, creditCard: String)
  case class AuthorizeCard(creditCard: String)
  case object PaymentAccept
  case object PaymentDenied
  case class DispatchOrder(item: String)
  case object OrderConfirmed

  class CheckoutActor extends Actor{
    private val paymentManager = context.actorOf(Props[PaymentManager])
    private val fulFillmentManager = context.actorOf(Props[FulFillmentManager])
    override def receive: Receive = awaitingCheckout

    def awaitingCheckout: Receive ={
      case Checkout(item, creditCard) =>
        paymentManager ! AuthorizeCard(creditCard)
        context.become(pendingPayment(item))
    }
    def pendingPayment(item: String):Receive ={
      case PaymentAccept =>
        fulFillmentManager ! DispatchOrder(item)
        context.become(pendingFulFillment(item))
      case PaymentDenied =>
        throw new RuntimeException("I can't handle this anymore")
    }

    def pendingFulFillment(item: String): Receive ={
      case OrderConfirmed => context.become(awaitingCheckout)
    }
  }

  class PaymentManager extends Actor{
    override def receive: Receive = {
      case AuthorizeCard(creditCard) =>
        if(creditCard.startsWith("0")) sender() ! PaymentDenied
        else {
          sender() ! PaymentAccept
        }
    }
  }

  class FulFillmentManager extends Actor with ActorLogging{
    var orderID = 8
    override def receive: Receive = {
      case DispatchOrder(item) =>
        orderID += 1
        log.info(s"Order ID $orderID for item $item has been dispatch")
        sender() ! OrderConfirmed
    }
  }

}
