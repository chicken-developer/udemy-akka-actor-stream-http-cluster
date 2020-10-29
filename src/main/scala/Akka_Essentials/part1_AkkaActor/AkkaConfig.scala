package Akka_Essentials.part1_AkkaActor

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import com.typesafe.config.ConfigFactory

object AkkaConfig extends App{

  class SimpleLoggingActor extends Actor with ActorLogging{
    override def receive: Receive = {
      case message => log.info(message.toString)
    }
  }
 //1. Inline config
val configString =
  """
    | akka {
    |   loglevel = "DEBUG"
    | }
    """.stripMargin   // loglevel --- not logLevel
  val config = ConfigFactory.parseString(configString)
  val system = ActorSystem("ConfigDemo", ConfigFactory.load(config))
  val simpleActor = system.actorOf(Props[SimpleLoggingActor])
  simpleActor ! "Remember me, i am inline config"


  //2. File config - create application.conf file at src/main/resources/ -> default config file location
  val defaultConfigSystem = ActorSystem("DefaultConfigFileDemo")
  val defaultConfigActor = defaultConfigSystem.actorOf(Props[SimpleLoggingActor])
  defaultConfigActor ! "Remember me, i am file config"

  //3. Separate config in the same file
//  val specialConfig = ConfigFactory.load().getConfig("demoConfig") // Have error here
//  val specialConfigSystem = ActorSystem("SpecialConfigDemo", specialConfig)
//  val specialActor= specialConfigSystem.actorOf(Props[SimpleLoggingActor])
//  specialActor ! "Remember me, i am special config" // TODO: Fix error when using separate config
  /*
    demoConfig {
      akka {
          loglevel = INFO
      }
    }
   */


  // TODO: Fix error when load file config, all log levels are INFO
  //4. Separate config in another file
  val secretFileConfig = ConfigFactory.load("secretFolder/secretConfig.conf")
  println(s"Separate config log level: ${secretFileConfig.getString("akka.loglevel")}")

  //5. Another file config format: .conf  .json
  val jsonFileConfig = ConfigFactory.load("json/jsonConfig.json")
  println(s"Json config log level: ${jsonFileConfig.getString("akka.loglevel")}")

  val propsFileConfig = ConfigFactory.load("props/propsConfig.properties")
//  println(s"Props config log level: ${propsFileConfig.getString("my.simpleProperty")}")
  println(s"Props config log level: ${propsFileConfig.getString("akka.loglevel")}")

}
