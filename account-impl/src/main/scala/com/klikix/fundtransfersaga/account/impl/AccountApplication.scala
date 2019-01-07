package com.klikix.fundtransfersaga.account.impl

import akka.stream.Materializer
import com.klikix.fundtransfersaga.account.api.AccountService
import com.lightbend.lagom.scaladsl.broker.kafka.LagomKafkaComponents
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraPersistenceComponents
import com.lightbend.lagom.scaladsl.server._
import com.softwaremill.macwire._
import com.lightbend.rp.servicediscovery.lagom.scaladsl.LagomServiceLocatorComponents
import play.api.Environment
import play.api.libs.ws.ahc.AhcWSComponents

import scala.concurrent.ExecutionContext
import com.klikix.fundtransfersaga.account.entity.account.AccountEntity

trait AccountComponents extends LagomServerComponents
  with CassandraPersistenceComponents {

  implicit def executionContext: ExecutionContext
  def environment: Environment

  implicit def materializer: Materializer

  override lazy val lagomServer = serverFor[AccountService](wire[AccountServiceImpl])
  lazy val jsonSerializerRegistry = AccountSerializerRegistry

  persistentEntityRegistry.register(wire[AccountEntity])
 
}

abstract class AccountApplication(context: LagomApplicationContext) extends LagomApplication(context)
  with AccountComponents
  with AhcWSComponents
  with LagomKafkaComponents {

}

class AccountApplicationLoader extends LagomApplicationLoader {
  override def loadDevMode(context: LagomApplicationContext): LagomApplication =
    new AccountApplication(context) with LagomDevModeComponents

  override def load(context: LagomApplicationContext): LagomApplication =
    new AccountApplication(context) with LagomServiceLocatorComponents

  override def describeService = Some(readDescriptor[AccountService])
}
