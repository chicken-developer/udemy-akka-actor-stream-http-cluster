package Akka_RemotingAndCluster.part2_AkkaRemoting

import akka.actor.{Actor, ActorLogging}

class SimpleActor extends Actor with ActorLogging {
  override def receive: Receive = {
    case message: String =>
      log.info(s"Received a message from ${sender()} : $message")
  }
}
