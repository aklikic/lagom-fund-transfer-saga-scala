package com.klikix.fundtransfersaga.transfer.api

import java.util.UUID

import akka.{Done, NotUsed}
import com.klikix.util._
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.transport.Method
import com.lightbend.lagom.scaladsl.api.{Service, ServiceCall}
import com.lightbend.lagom.scaladsl.api.broker.kafka.PartitionKeyStrategy
import com.lightbend.lagom.scaladsl.api.broker.kafka.KafkaProperties
import com.klikix.util.security.SecurityHeaderFilter

trait TransferService extends Service {
 
  def transfer: ServiceCall[StartTransferRequest, Transfer]
  def getTransfer(transactionUid: UUID): ServiceCall[NotUsed,Transfer]
  def transferEvents: Topic[TransferEvent]
 
  final override def descriptor = {
    import Service._

    named("fund-transfer-service").withCalls(
      restCall(Method.POST,"/api/fund_transfers", transfer),
      restCall(Method.GET, "/api/fund_transfers/:transactionUid", getTransfer _)
    ).withTopics(
      topic("Fund-Transfer", this.transferEvents)
        .addProperty(
          KafkaProperties.partitionKeyStrategy,
          PartitionKeyStrategy[TransferEvent](_.sourceAccountUid.toString())
      )
    ).withAutoAcl(true)
  }
}
