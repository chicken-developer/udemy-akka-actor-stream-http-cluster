package Akka_Essentials.part4_Infra

import akka.actor.{Actor, ActorLogging, ActorSystem, PoisonPill, Props}
import akka.dispatch.{ControlMessage, PriorityGenerator, UnboundedPriorityMailbox}
import com.typesafe.config.{Config, ConfigFactory}

object Mailboxes extends App{
  val system = ActorSystem("MailboxDemo", ConfigFactory.load().getConfig("mailboxesDemo"))

  class SimpleActor extends Actor with ActorLogging{
    override def receive: Receive = {
      case message =>
        log.info(message.toString)
    }
  }
  //Interesting 1: Customize mailbox
  //Step 1: Mailbox definitions
  class SupportTicketPriorityMailbox(settings: ActorSystem.Settings, config: Config)
        extends UnboundedPriorityMailbox( // That class for definition Mailbox, not Actor
          PriorityGenerator{
            case message: String if message.startsWith("[P0]") => 0
              //0 is level of priority
            case message: String if message.startsWith("[P1]") => 1
            case message: String if message.startsWith("[P2]") => 2
            case message: String if message.startsWith("[P3]") => 3
            case _ => 4
          }
        )
  //Step 2: make it know in config - do in application.conf
  //Step 3: Attach the dispatcher

  val supportTicketLogger = system.actorOf(Props[SimpleActor].withDispatcher("support-ticket-dispatcher"))
//  supportTicketLogger ! PoisonPill
//  supportTicketLogger ! "[P3] this would be nice to have"
//  supportTicketLogger ! "[P2] this should be do at second time"
//  supportTicketLogger ! "[P1] this must do NOW"
//  supportTicketLogger ! "[P4] can do this latter"


  //Interesting 2:

  //Step 1 : Mark important messages as control messages
  case object ManagementTicket extends ControlMessage

  //Step 2: Configure who gets the mailbox
  /*
    1. make the actor attach to mail box
   */
  //Method 1
  val controlAwareActor = system.actorOf(Props[SimpleActor].withMailbox("control-mailbox"))
//  controlAwareActor ! "[P3] this would be nice to have"
//  controlAwareActor ! "[P0] this should be do at second time"
//  controlAwareActor ! "[P1] this must do NOW"
//  controlAwareActor ! ManagementTicket

  // Method 2: Using deployment config - view in config file
  val altControlAwareActor = system.actorOf(Props[SimpleActor], "altControlAwareActor")
  altControlAwareActor ! "[P3] this would be nice to have"
  altControlAwareActor ! "[P0] this should be do at second time"
  altControlAwareActor ! "[P1] this must do NOW"
  altControlAwareActor ! ManagementTicket
}
