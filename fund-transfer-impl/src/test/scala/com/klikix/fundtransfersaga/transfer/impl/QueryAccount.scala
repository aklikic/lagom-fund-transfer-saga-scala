package com.klikix.fundtransfersaga.transfer.impl

import java.net.URI
import java.util.UUID

import akka.NotUsed
import com.klikix.fundtransfersaga.account.api.{Account, AccountService}
import com.lightbend.lagom.scaladsl.client.{StandaloneLagomClientFactory, StaticServiceLocatorComponents}
import play.api.libs.json.Json
import play.api.libs.ws.ahc.AhcWSComponents

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.Duration

object QueryAccount {

  def main(
            args: Array[String]
          ): Unit = {

    val clientFactory =
      new StandaloneLagomClientFactory(
        "CreateAccount",
        classOf[StandaloneLagomClientFactory].getClassLoader)
        with StaticServiceLocatorComponents with AhcWSComponents {
        override def staticServiceUri = URI.create("http://localhost:9000")
      }
    implicit val ec: ExecutionContext = clientFactory.executionContext

    val accountService = clientFactory.serviceClient.implement[AccountService]

    try {
      if (args.length != 1) {
        println("Usage: QueryAccount <account UUID>")
        System.exit(-1)
      }

      val accountUid = args(0)
      val done = accountService.getAccount(UUID.fromString(accountUid)).invoke(NotUsed)
      val account = Await.result(done, Duration.Inf)
      println(Json.prettyPrint(Account.format.writes(account)))

    } finally {
      clientFactory.stop()
    }
  }
}
