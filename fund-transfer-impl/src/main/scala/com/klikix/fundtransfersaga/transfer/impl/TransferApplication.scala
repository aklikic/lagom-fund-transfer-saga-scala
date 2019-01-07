package com.klikix.fundtransfersaga.transfer.impl

import akka.stream.Materializer
import com.klikix.fundtransfersaga.transfer.api.TransferService
import com.lightbend.lagom.scaladsl.broker.kafka.LagomKafkaComponents
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraPersistenceComponents
import com.lightbend.lagom.scaladsl.server._
import com.softwaremill.macwire._
import com.lightbend.rp.servicediscovery.lagom.scaladsl.LagomServiceLocatorComponents
import play.api.Environment
import play.api.libs.ws.ahc.AhcWSComponents

import scala.concurrent.ExecutionContext
import com.klikix.fundtransfersaga.transfer.entity.transfer._
import com.klikix.fundtransfersaga.transfer.entity.transfer.view.TransferEventProcessor
import com.klikix.fundtransfersaga.transfer.entity.transfer.view.TransferRepository
import com.klikix.fundtransfersaga.account.api.AccountService
import com.klikix.fundtransfersaga.transfer.entity.transfer.view.TransferSagaReadSideProcessor
import com.lightbend.lagom.scaladsl.client._
import com.lightbend.lagom.scaladsl.api._

trait TransferComponents extends LagomServerComponents
  with CassandraPersistenceComponents with LagomServiceClientComponents with LagomConfigComponent {

  implicit def executionContext: ExecutionContext
  def environment: Environment

  implicit def materializer: Materializer

  override lazy val lagomServer = serverFor[TransferService](wire[TransferServiceImpl])
  lazy val jsonSerializerRegistry = TransferSerializerRegistry
  lazy val transferRepository = wire[TransferRepository]
  
  persistentEntityRegistry.register(wire[TransferEntity])
  readSide.register(wire[TransferEventProcessor])
  lazy val accountService = serviceClient.implement[AccountService]
  readSide.register(wire[TransferSagaReadSideProcessor])
  
 
}

abstract class TransferApplication(context: LagomApplicationContext) extends LagomApplication(context)
  with TransferComponents
  with AhcWSComponents
  with LagomKafkaComponents {
  
}

class TransferApplicationLoader extends LagomApplicationLoader {
  override def loadDevMode(context: LagomApplicationContext): LagomApplication =
    new TransferApplication(context) with LagomDevModeComponents

  override def load(context: LagomApplicationContext): LagomApplication =
    new TransferApplication(context) with LagomServiceLocatorComponents

  override def describeService = Some(readDescriptor[TransferService])
}
