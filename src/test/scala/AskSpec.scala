import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.pattern.{ask, pipe}
import akka.testkit.{ImplicitSender, TestKit}
import akka.util.Timeout
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.{Failure, Success}

class AskSpec extends TestKit(ActorSystem("AskSpec"))
  with ImplicitSender
  with WordSpecLike
  with BeforeAndAfterAll{
  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  import AskSpec._
  import AuthManager._
  "An authenticator" must {
    authenticatorTestSuite(Props[AuthManager])
  }

  "A pipeAuthenticator" must {
    authenticatorTestSuite(Props[PipedAuthManager])
  }
  def authenticatorTestSuite(props: Props): Unit ={
    "fail to authenticate a non-registered user" in {
      val authManager = system.actorOf(props)
      authManager ! Authenticate("quynh","123")
      expectMsg(AuthFailure(AUTH_FAILURE_USERNAME_NOT_FOUND))
    }
    "fail to authenticate if invalid password" in {
      val authManager = system.actorOf(props)
      authManager ! RegisterUser("quynh","123")
      authManager ! Authenticate("quynh","12344")
      expectMsg(AuthFailure(AUTH_FAILURE_PASSWORD_INCORRECT))
    }
    "success to authenticate if valid username and  password" in {
      val authManager = system.actorOf(props)
      authManager ! RegisterUser("quynh","123")
      authManager ! Authenticate("quynh","123")
      expectMsg(AuthSuccess)
    }
  }
}

object AskSpec {
  case class Read(key: String)
  case class Write(key: String, value: String)
  class KeyValueActor extends Actor with ActorLogging {
    override def receive: Receive = online(Map())
    def online(kv: Map[String, String]): Receive ={
      case Read(key) =>
        log.info(s"Try to read value from key: $key")
        sender() ! kv.get(key)
      case Write(key, value) =>
        log.info(s"Writing value: [$value] for the key: [$key]")
        context.become(online(kv + (key -> value)))
    }
  }

  case class RegisterUser(username: String, password: String)
  case class Authenticate(username: String, password: String)
  case object AuthSuccess
  case class AuthFailure(error: String)
  object AuthManager {
    val AUTH_FAILURE_USERNAME_NOT_FOUND = "Username not found"
    val AUTH_FAILURE_PASSWORD_INCORRECT = "Password incorrect"
    val AUTH_FAILURE_SYSTEM_ERROR = "System Error"
  }
  class AuthManager extends Actor with ActorLogging {
    import AuthManager._
    // timeout and executionContext must have for handle future from ask ?
    implicit val timeout: Timeout = Timeout( 1 second)
    implicit val executionContext: ExecutionContext = context.dispatcher

    protected val authDb = context.actorOf(Props[KeyValueActor])
    override def receive: Receive = {
      case RegisterUser(username, password) => authDb ! Write(username,password)
      case Authenticate(username, password) => handleAuthentication(username, password)

    }
    def handleAuthentication(username: String, password: String): Unit ={
      //Ask the actor -> will return a future
      //Important: make original sender
      val originalSender = sender()
      val future = authDb ? Read(username)
      //Handle future return from Actor
      future.onComplete{
        // NEVER CALL METHODS ON THE ACTOR INSTANCE OR ACCESS MUTABLE STATE IN ONCOMPLETE
        case Success(None) => originalSender ! AuthFailure(AUTH_FAILURE_USERNAME_NOT_FOUND)
        case Success(Some(dbPassword)) =>
          if(dbPassword == password) originalSender ! AuthSuccess
          else originalSender ! AuthFailure(AUTH_FAILURE_PASSWORD_INCORRECT)
        case Failure(_) => AuthFailure(AUTH_FAILURE_SYSTEM_ERROR)
      }
    }
  }

  class PipedAuthManager extends AuthManager {
    import AuthManager._

    override def handleAuthentication(username: String, password: String): Unit = {
      val future = authDb ? Read(username) // Return Future[Any]
      val passwordFuture = future.mapTo[Option[String]] // Now type is Future[Option[String]]
      val respondFuture = passwordFuture.map {
        case None => AuthFailure(AUTH_FAILURE_USERNAME_NOT_FOUND)
        case Some(dbPassword) =>
          if( dbPassword == password) AuthSuccess
          else AuthFailure(AUTH_FAILURE_PASSWORD_INCORRECT)
      } // Future[Any] will be completed with the response I will send back
      respondFuture.pipeTo(sender()) // When the future completed, send the response to the actorRef in argument list, in this case in sender()
    }
  }
}

