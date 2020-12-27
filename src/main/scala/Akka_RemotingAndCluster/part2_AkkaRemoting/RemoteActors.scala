package Akka_RemotingAndCluster.part2_AkkaRemoting

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory

object RemoteActors extends App {
  val localSystem = ActorSystem("Localsystem", ConfigFactory.load("part2_Remoting/remoteActors.conf"))
  val remoteSystem = ActorSystem("RemoteSystem", ConfigFactory.load("part2_Remoting/remoteActors.conf").getConfig("remoteSystem"))

  val localSimpleActor = localSystem.actorOf(Props[SimpleActor], "simpleLocalActor")
  val remoteSimpleActor = remoteSystem.actorOf(Props[SimpleActor], "remoteSimpleActor")

  localSimpleActor ! "Hello local actor"
  remoteSimpleActor ! "Hello remote actor"
}
