package Akka_Stream.part3_Graphs

import akka.actor.ActorSystem
import akka.stream.{BidiShape, ClosedShape, Materializer}
import akka.stream.scaladsl.{Flow, GraphDSL, RunnableGraph, Sink, Source}

object BidirectionalFlows extends App {
  implicit val system = ActorSystem("BidirectionalFlows")
  implicit val materializer = Materializer

  def encrypt(index: Int)(source: String) = source.map(c => (c + index).toChar)
  def decrypt(index: Int)(source: String) = source.map(c => (c - index).toChar)

  val bidiCryptStaticGraph = GraphDSL.create(){ implicit  builder =>
    val encryptShape = builder.add(Flow[String].map(encrypt(3)))
    val decryptShape = builder.add(Flow[String].map(decrypt(3)))
//    BidiShape(encryptShape.in,encryptShape.out, decryptShape.in, decryptShape.out)
    BidiShape.fromFlows(encryptShape, decryptShape)
  }

  val unencryptedString = List("Akka","is", "very", "powerful", "toolkit")
  val unencryptedSource = Source(unencryptedString)
  val encryptSource = Source(unencryptedString.map(encrypt(3)))

  val cryptoBidiGraph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit  builder =>
      import GraphDSL.Implicits._
      val unencryptSourceShape = builder.add(unencryptedSource)
      val encryptSourceShape = builder.add(encryptSource)
      val bidi = builder.add(bidiCryptStaticGraph)
      val encryptSinkShape = builder.add(Sink.foreach[String](str => println(s"Encrypt string: $str")))
      val decryptSinkShape = builder.add(Sink.foreach[String](str => println(s"Decrypt string: $str")))

      unencryptSourceShape ~> bidi.in1 ; bidi.out1 ~> encryptSinkShape
      decryptSinkShape <~ bidi.out2 ; bidi.in2 <~ encryptSourceShape // 1 and 2, not have 0; <~ not  ~>
      ClosedShape
    })
  cryptoBidiGraph.run()
}
