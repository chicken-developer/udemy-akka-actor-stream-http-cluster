package Akka_Essentials.part0_Recap

object ImplicitRecap extends App{
  // 1. Implicit val
  implicit val implicitNumber: Int = 50
  def printNumber(implicit number: Int): Unit = {
    println(s"Receive number: $number")
  }
  printNumber

  //2. Implicit defs
  case class Person(name : String){
    def sayHello():Unit = println(s"Hi, my name is $name")
  }
  implicit def fromNumberToPerson(personID: Int): Person = {
     Person("PersonID" + personID.toString)
  }
  implicit def fromStringToPerson(personName: String): Person = Person(personName)

  25.sayHello()
  "Bob".sayHello()

  //3. Implicit classes
  implicit class Animal(name: String){
    //Using class, not case class
    def eat(): Unit = println(s"Hi, i am $name and i want to eat now ")
  }
  "Dog".eat()

  //Recreate implicit arguments
  implicit val inverseOrdering: Ordering[Int] = Ordering.fromLessThan(_ > _)
  List(1,2,3).sorted // List(3,2,1) (sorted method need an implicit Ordering - default is < )
}

