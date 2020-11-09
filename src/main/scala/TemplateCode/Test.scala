//package TemplateCode
//
//object Test extends App {
//  println("hello Akka")
//  val crazyList = List("Akka", "Toolkit", List("Hee", "Lee"))
//  val crazyWithMap = crazyList.map{ name =>
//    name.toString.toUpperCase() // Type of result: List[String]
//  }
//  val crazyWithFlatMap = crazyList.flatMap{ name =>
//    name.toString.toUpperCase() // Type of result: List[Char]
//  }
//  val crazyWithFold = crazyList.fold{ name: Char =>
//    name.toString.toUpperCase
//  }
//  println(crazyWithMap)
//  println(crazyWithFlatMap)
//}
