package Akka_Essentials.part1_AkkaActor

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ChildActorEx extends App {
  //Distributed Word Counting
  object WordCounterMaster{
    case class Initialize(nChildren: Int)
    case class WordCountTask(id: Int, text: String)
    case class WordCountReply(id: Int, count: Int)
  }
  class WordCounterMaster extends Actor{
    import WordCounterMaster._
    override def receive: Receive = {
      case Initialize(nChildren) =>
        val childrenRefs = for( i <- 1 to nChildren) yield
          context.actorOf(Props[WordCounterWorker],s"wordCounterWorker_$i")
        context.become(withChildren(childrenRefs, 0, 0, Map()))
    }
    def withChildren(childrenRefs: Seq[ActorRef], currentChildIndex: Int, currentTaskID: Int, requestMap: Map[Int, ActorRef]): Receive ={
      case text: String =>
        println(s"[Master] I have received: $text - I will sent it to child $currentChildIndex")
        val originSender = sender()
        val task = WordCountTask(currentTaskID,text)
        val childRef = childrenRefs(currentChildIndex)
        childRef ! task
        val nextChildIndex: Int = (currentChildIndex + 1) % childrenRefs.length
        val newTaskID: Int = currentTaskID + 1
        val newRequestMap = requestMap + (currentTaskID -> originSender)
        context.become(withChildren(childrenRefs, nextChildIndex, newTaskID, newRequestMap))
      case WordCountReply(id, count) =>
        println(s"[Master] I have received a reply for task id $id with word count: $count")
        val originalSender = requestMap(id)
        originalSender ! count
        context.become(withChildren(childrenRefs, currentChildIndex, currentTaskID, requestMap - id))
    }
  }

  class WordCounterWorker extends Actor{
    import WordCounterMaster._
    override def receive: Receive = {
      case WordCountTask(id, text) =>
        println(s"${self.path} I have received a task $id with $text")
        sender() ! WordCountReply(id, text.split(" ").length)
    }
  }
 class TestActor extends Actor{
   import WordCounterMaster._
   override def receive: Receive = {
     case "go" =>
       val master = context.actorOf(Props[WordCounterMaster], "master")
       master ! Initialize(3)
       val texts = List("I love scala", "Akka is best toolkit","I love programming", "hello world", "hi")
       texts.foreach( text => master ! text)
     case count: Int =>
       println(s"[TestActor] I received a reply: $count")
   }
 }
  val system = ActorSystem("ChildActorEx")
  val testActor = system.actorOf(Props[TestActor])
  testActor ! "go"

}
