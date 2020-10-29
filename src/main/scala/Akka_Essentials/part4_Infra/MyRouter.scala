package Akka_Essentials.part4_Infra

import akka.actor.{Actor, ActorLogging, ActorSystem, Props, Terminated}
import akka.routing.{ActorRefRoutee, Broadcast, BroadcastRoutingLogic, ConsistentHashingRoutingLogic, FromConfig, RandomRoutingLogic, RoundRobinGroup, RoundRobinPool, RoundRobinRoutingLogic, Router, ScatterGatherFirstCompletedRoutingLogic, SmallestMailboxRoutingLogic, TailChoppingRoutingLogic}
import com.typesafe.config.ConfigFactory

object MyRouter extends App {
  class Master extends Actor{
    // Step 1: Create routes
    private val slaves = for(slaveIndex <- 1 to 5) yield {
      val slave = context.actorOf(Props[Slave], s"slave_$slaveIndex")
      context.watch(slave)

      ActorRefRoutee(slave)
    }
    //Step 2: Define router
    private val router = Router(RoundRobinRoutingLogic(), slaves)
//    // Some Router routing logic:
//    RoundRobinRoutingLogic //Send next message by Cycle 1-2-3-1-2-3-1-2-3
//    RandomRoutingLogic  //Sent next message by Random
//    SmallestMailboxRoutingLogic // Sent next message to the actor with the fewest messages in the queue
//    BroadcastRoutingLogic // Send the same message to all the routines
//    ScatterGatherFirstCompletedRoutingLogic // Send the message to every one and waits for the first reply. All next replies are discarded
//    TailChoppingRoutingLogic // Forwards the next message to each actor sequentially until the first reply was received and all the other replies are discarded
//    ConsistentHashingRoutingLogic // All the messages with the same hash get to the same actor


    override def receive: Receive = {
      //Step 4: handle the termination/ lifecycle of the routes
      case Terminated(ref) =>
        router.removeRoutee(ref)
        val newSlave = context.actorOf(Props[Slave])
        context.watch(newSlave)
        router.addRoutee(newSlave)

      //Step 3: Route message
      case message =>
        router.route(message, sender())
    }
  }

  class Slave extends Actor with ActorLogging{
    override def receive: Receive = {
      case message => log.info(message.toString)
    }
  }

  val system = ActorSystem("MyRouterDemo",ConfigFactory.load().getConfig("routersDemo"))
  val master = system.actorOf(Props[Master])
//  for(index <- 1 to 12) {
//    master ! s"Hello the world: $index"
//  }


  //Method 2 : A router with its own child
  // => POOL Router
  //2.1 Programmatically( in code)
  val poolMaster = system.actorOf(RoundRobinPool(5).props(Props[Slave]), "simplePoolMaster")
  //All routing login( Round, Random, Smallest,... have same poo; is RoundRobinPool)
//  for(index <- 1 to 14){
//    poolMaster ! s"${index} Hello word"
//  }

  //2.2 From configuration
  val poolMaster_02 = system.actorOf(FromConfig.props(Props[Slave]), "poolMaster_02")
  //actor name must match with name from application.conf
//  for(index <- 1 to 14){
//    poolMaster_02 ! s"${index} Hello word"
//  }


  //Method 3: Router with actors created elsewhere
  //==> GROUP Router

  //In another part of my application
  //Somebody create for me some actors
  val slaveList = (1 to 5).map(i => system.actorOf(Props[Slave], s"slave_${i}")).toList
  // Need their paths
  val slavePaths = slaveList.map(slaveRef => slaveRef.path.toString)
  //3.1 In the code
  val groupMaster = system.actorOf(RoundRobinGroup(slavePaths).props())
//  for(index <- 1 to 14){
//    groupMaster ! s"${index} Hello word"
//  }
  //3.2 From configuration
  val groupMaster_02 = system.actorOf(FromConfig.props(), "groupMaster_02")
  //Again, actor name must match with name from application.conf
  for(index <- 1 to 14){
    groupMaster_02 ! s"${index} Hello word"
  }

  //Special messages
  groupMaster_02 ! Broadcast("Hello every slave") // This will sent to all child actor of routes
  // PoisonPill and Kill are NOT routed
  // AddRoutee, Remove, Get handled only by the routing actor
}
