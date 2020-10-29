package Akka_Stream.part5_Advanced

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Balance, GraphDSL, Merge}
import akka.stream.{Inlet, Materializer, Outlet, Shape}

import scala.collection.immutable

object CustomGraphDSLs extends App {
  implicit val system = ActorSystem("CustomGraphDSL")
  implicit val materializer = Materializer

  case class Balance3x4(in0: Inlet[Int],
                        in1: Inlet[Int],
                        in2: Inlet[Int],
                        out0: Outlet[Int],
                        out1: Outlet[Int],
                        out2: Outlet[Int],
                        out3: Outlet[Int]
                       ) extends Shape{
    override val inlets: immutable.Seq[Inlet[_]] = List(in0, in1, in2)
    override val outlets: immutable.Seq[Outlet[_]] = List(out0, out1, out2, out3)
    override def deepCopy(): Shape = Balance3x4(
      in0.carbonCopy(),
      in1.carbonCopy(),
      in2.carbonCopy(),
      out0.carbonCopy(),
      out1.carbonCopy(),
      out2.carbonCopy(),
      out3.carbonCopy(),
    )
  }

  val balance3x4 = GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._
    val mergeShape = builder.add(Merge[Int](3))
    val balanceShape = builder.add(Balance[Int](4))
    mergeShape ~> balanceShape
    Balance3x4(
      mergeShape.in(0),
      mergeShape.in(1),
      mergeShape.in(2),
      balanceShape.out(0),
      balanceShape.out(1),
      balanceShape.out(2),
      balanceShape.out(3)
    )
  }
}
