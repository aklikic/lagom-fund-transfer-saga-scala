package com.klikix.fundtransfersaga.account.api

import java.util.UUID

import akka.{Done, NotUsed}
import com.klikix.util._
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.transport.Method
import com.lightbend.lagom.scaladsl.api.{Service, ServiceCall}
import com.lightbend.lagom.scaladsl.api.broker.kafka.PartitionKeyStrategy
import com.lightbend.lagom.scaladsl.api.broker.kafka.KafkaProperties
import com.klikix.util.security.SecurityHeaderFilter
import akka.stream.scaladsl.Source

trait AccountService extends Service {
 
  def createAccount: ServiceCall[CreateAccountRequest, Account]
  
  def closeAccount(accountUid: UUID): ServiceCall[NotUsed,Done]
  
  def getAccount(accountUid: UUID): ServiceCall[NotUsed,Account]
 
  def addFunds(accountUid: UUID): ServiceCall[AddFundsRequest,Done]
  
  def addFundsRollback(accountUid: UUID): ServiceCall[AddFundsRollbackRequest,Done]
  
  def removeFunds(accountUid: UUID): ServiceCall[RemoveFundsRequest,Done]
  
  def removeFundsRollback(accountUid: UUID): ServiceCall[RemoveFundsRollbackRequest,Done]
  
  def accountStream(userUid: UUID): ServiceCall[Source[AccountStreamAlive,NotUsed],Source[AccountEvent,NotUsed]]
  
  def accountEvents: Topic[AccountEvent]

  final override def descriptor = {
    import Service._

    named("account-service").withCalls(
      pathCall("/api/accounts/stream/:userUid", accountStream _),
      restCall(Method.POST,"/api/accounts", createAccount),
      restCall(Method.DELETE, "/api/accounts/:accountUid", closeAccount _),
      restCall(Method.GET, "/api/accounts/:accountUid", getAccount _),
      restCall(Method.PUT, "/api/accounts/:accountUid/funds_add", addFunds _),
      restCall(Method.DELETE, "/api/accounts/:accountUid/funds_add", addFundsRollback _),
      restCall(Method.PUT, "/api/accounts/:accountUid/funds_remove", removeFunds _),
      restCall(Method.DELETE, "/api/accounts/:accountUid/funds_remove", removeFundsRollback _)
    ).withTopics(
      topic("Account", this.accountEvents)
        .addProperty(
          KafkaProperties.partitionKeyStrategy,
          PartitionKeyStrategy[AccountEvent](_.accountUid.toString())
      )
    ).withAutoAcl(true)
  }
}
