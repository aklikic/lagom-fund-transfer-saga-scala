package com.klikix.fundtransfersaga.transfer.impl

import java.net.URI
import java.util.UUID

import akka.NotUsed
import com.klikix.fundtransfersaga.transfer.api.{Transfer, TransferService}
import com.lightbend.lagom.scaladsl.client.{StandaloneLagomClientFactory, StaticServiceLocatorComponents}
import play.api.libs.json.Json
import play.api.libs.ws.ahc.AhcWSComponents

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}

object GetTransfer {

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
      if (args.length != 1) {
        println("Usage: GetTransfer <transaction UUID>")
        System.exit(-1)
      }

      val transactionUid = args(0)

      val done = transferService.getTransfer(UUID.fromString(transactionUid)).invoke(NotUsed)
      val transfer = Await.result(done, Duration.Inf)
      println(Json.prettyPrint(Transfer.format.writes(transfer)))
    } finally {
      clientFactory.stop()
    }
  }
}
