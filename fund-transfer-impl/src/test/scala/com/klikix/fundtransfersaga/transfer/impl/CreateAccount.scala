package com.klikix.fundtransfersaga.transfer.impl

import java.net.URI
import java.util.UUID

import com.klikix.fundtransfersaga.account.api.{
  Account,
  AccountService,
  CreateAccountRequest
}
import com.lightbend.lagom.scaladsl.client.{
  StandaloneLagomClientFactory,
  StaticServiceLocatorComponents
}
import play.api.libs.json.Json
import play.api.libs.ws.ahc.AhcWSComponents

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}

object CreateAccount {

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
      val done = accountService.createAccount.invoke(
        CreateAccountRequest(accountUid = None,
                             ownerUid = UUID.randomUUID,
                             intitalAmount = Some(1000)))
      val account = Await.result(done, Duration.Inf)
      println(Json.prettyPrint(Account.format.writes(account)))
    } finally {
      clientFactory.stop()
    }
  }
}
