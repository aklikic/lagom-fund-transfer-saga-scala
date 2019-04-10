package com.klikix.fundtransfersaga.transfer.impl

import java.net.URI
import java.util.UUID

import com.klikix.fundtransfersaga.transfer.api.{StartTransferRequest, Transfer, TransferData, TransferService}
import com.lightbend.lagom.scaladsl.client.{StandaloneLagomClientFactory, StaticServiceLocatorComponents}
import play.api.libs.json.Json
import play.api.libs.ws.ahc.AhcWSComponents

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.Duration

object TransferFunds {

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

    val transferService = clientFactory.serviceClient.implement[TransferService]

    try {
      if (args.length != 3) {
        println("Usage: TransferFunds <source account UUID> <destination account UUID> <transfer amount>")
        System.exit(-1)
      }

      val transferFrom = args(0)
      val transferTo = args(1)
      val transferAmount = args(2)

      val done = transferService.transfer.invoke(StartTransferRequest(
        TransferData(
          transactionUid = UUID.randomUUID,
          sourceAccountUid = UUID.fromString(transferFrom),
          destinationAccountUid = UUID.fromString(transferTo),
          amount = Integer.parseInt(transferAmount))
      ))

      val transfer = Await.result(done, Duration.Inf)
      println(Json.prettyPrint(Transfer.format.writes(transfer)))
    } finally {
      clientFactory.stop()
    }

  }
}
