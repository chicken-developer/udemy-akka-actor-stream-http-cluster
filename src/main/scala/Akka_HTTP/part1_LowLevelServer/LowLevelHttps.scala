package Akka_HTTP.part1_LowLevelServer

import java.io.InputStream
import java.security.{KeyStore, SecureRandom}

import Akka_HTTP.part1_LowLevelServer.LowLevelHttps.getClass
import akka.actor.ActorSystem
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, HttpResponse, StatusCodes, Uri}
import akka.http.scaladsl.{ConnectionContext, Http, HttpsConnectionContext}
import akka.stream.Materializer
import akka.stream.scaladsl.Flow
import javax.net.ssl.{KeyManagerFactory, SSLContext, TrustManagerFactory}

object HttpsContext{
  //Step 1: Create Keystore object
  val keyStore: KeyStore = KeyStore.getInstance("PKCS12")
  val storeFile: InputStream = getClass.getClassLoader.getResourceAsStream("keystore.pkcs12")
  //Another way: val storeFile: InputStream = new FileInputStream(new File("src/main/resources/keystore.pkcs12"))
  val password: Array[Char] = "akka-https".toCharArray
  keyStore.load(storeFile, password)

  //Step 2: Initialize a key manager
  val keyManagerFactory: KeyManagerFactory = KeyManagerFactory.getInstance("SunX509") //PKI: public key infrastructure
  keyManagerFactory.init(keyStore,password)

  //Step 3: Initialize a trust manager
  val trustManagerFactory: TrustManagerFactory = TrustManagerFactory.getInstance("SunX509")
  trustManagerFactory.init(keyStore)

  //Step 4: Initialize a sll context
  val sllContext: SSLContext = SSLContext.getInstance("TLS")
  sllContext.init(keyManagerFactory.getKeyManagers, trustManagerFactory.getTrustManagers, new SecureRandom)

  //Step 5: Return the https connection context
  val httpsConnectionContext: HttpsConnectionContext = ConnectionContext.https(sllContext)
}

object LowLevelHttps extends App {
  implicit val system = ActorSystem("LowLevelHttps")
  implicit val materializer = Materializer


  val streamBasedAsyncRequestHandler: Flow[HttpRequest, HttpResponse, _] = Flow[HttpRequest].map {
    case HttpRequest(HttpMethods.GET, Uri.Path("/"), _, _, _) =>
      HttpResponse(
        StatusCodes.OK, //HTTP code: 200
        entity = HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          """
            |<html>
            | <body>
            |     Hello from Akka HTTP HomePage Stream!
            | </body>
            |</html>
            |""".stripMargin
        )
      )
    case request: HttpRequest =>
      request.discardEntityBytes()
      HttpResponse(
        StatusCodes.NotFound, //HTTP code: 404
        entity = HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          """
            |<html>
            | <body>
            |     The resource not found !
            | </body>
            |</html>
            |""".stripMargin
        )
      )
  }

  val httpsBinding =   Http().bindAndHandle(streamBasedAsyncRequestHandler, "localhost",8082, HttpsContext.httpsConnectionContext)

}
