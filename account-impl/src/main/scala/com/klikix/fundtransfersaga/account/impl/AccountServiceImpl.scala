package com.klikix.fundtransfersaga.account.impl

import java.util.UUID

import akka.persistence.query.Offset
import com.datastax.driver.core.utils.UUIDs
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.transport.{Forbidden, NotFound}
import com.lightbend.lagom.scaladsl.broker.TopicProducer
import com.lightbend.lagom.scaladsl.persistence.{EventStreamElement, PersistentEntityRegistry}
import com.lightbend.lagom.scaladsl.server.ServerServiceCall

import scala.concurrent.{ExecutionContext, Future}
import com.klikix.fundtransfersaga.account.api._
import com.klikix.fundtransfersaga.account.api.{AccountEvent => TopicApiAccountEvent}
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import com.klikix.fundtransfersaga.account.entity.account.AccountEntity
import com.klikix.fundtransfersaga.account.entity.account.CreateAccount
import akka.Done
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.InvalidCommandException
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.UnhandledCommandException
import com.lightbend.lagom.scaladsl.api.transport.BadRequest
import com.lightbend.lagom.scaladsl.api.transport.TransportException
import com.klikix.fundtransfersaga.account.impl.Mappers._
import com.lightbend.lagom.scaladsl.api.transport.TransportException._
import com.lightbend.lagom.scaladsl.api.transport.TransportErrorCode
import com.lightbend.lagom.scaladsl.api.transport.ExceptionMessage
import com.klikix.fundtransfersaga.account.entity.account.CloseAccount
import com.klikix.fundtransfersaga.account.entity.account.GetAccount
import com.klikix.fundtransfersaga.account.entity.account.AddFunds
import com.typesafe.scalalogging.LazyLogging
import com.klikix.fundtransfersaga.account.entity.account.AddFundsRollback
import com.klikix.fundtransfersaga.account.entity.account.RemoveFunds
import com.klikix.fundtransfersaga.account.entity.account.RemoveFundsRollback
import com.klikix.fundtransfersaga.account.entity.account.AccountEvent
import com.klikix.fundtransfersaga.account.entity.account.AccountCreated
import com.klikix.fundtransfersaga.account.entity.account.AccountClosed
import com.klikix.fundtransfersaga.account.entity.account.FundsAdded
import com.klikix.fundtransfersaga.account.entity.account.FundsAddRollbacked
import com.klikix.fundtransfersaga.account.entity.account.FundsRemoved
import com.klikix.fundtransfersaga.account.entity.account.FundsRemoveRollbacked
import akka.stream.scaladsl.Source
import akka.stream.OverflowStrategy
import com.klikix.fundtransfersaga.transfer.impl.AccountSubscriberActor
import com.lightbend.lagom.scaladsl.pubsub.PubSubRegistry
import akka.actor.ActorSystem
import akka.stream.scaladsl.Sink
import akka.stream.Materializer
import akka.NotUsed

class AccountServiceImpl(registry: PersistentEntityRegistry, pubSub: PubSubRegistry, system: ActorSystem)(implicit ec: ExecutionContext,materializer: Materializer) extends AccountService with LazyLogging {
  
  override def createAccount = ServerServiceCall { request =>
    logger.info(s"createAccount: $request")
    val accountUid = request.accountUid.getOrElse(UUIDs.timeBased)
    val account = fromApi(request)
    entityRef(accountUid)
    .ask(CreateAccount.apply(account))
    .transform(d => toApi(accountUid, account),
               transformFailed _)
  }
  
  override def closeAccount(accountUid: UUID) = { _ =>
    logger.info(s"closeAccount $accountUid")
    entityRef(accountUid)
    .ask(CloseAccount)
    .transform(d=>d,
               transformFailed _)
  }
  
  override def getAccount(accountUid: UUID) = {_ =>
    logger.info(s"getAccount $accountUid")
    entityRef(accountUid)
    .ask(GetAccount)
    .map {
      case Some(account) => toApi(accountUid, account)
      case None => throw NotFound(s"Account $accountUid not found")
    }.transform(d=>d,
                transformFailed _)
  }
  
  override def addFunds(accountUid: UUID) = { request =>
    logger.info(s"addFunds $accountUid :$request")
    entityRef(accountUid)
    .ask(AddFunds.apply(request.transactionUid,request.amountToAdd))
    .transform(d => d,
               transformFailed _)
  }
  
  override def addFundsRollback(accountUid: UUID) = { request =>
    logger.info(s"addFundsRollback $accountUid")
    entityRef(accountUid)
    .ask(AddFundsRollback.apply(request.transactionUid,
                          request.amountAdded,
                          request.rollbackReason))
    .transform(d=>d,
               transformFailed _) 
  }
  
  override def removeFunds(accountUid: UUID) = { request =>
    logger.info(s"removeFunds $accountUid")
    entityRef(accountUid)
    .ask(RemoveFunds.apply(request.transactionUid,
                            request.amountToRemove))
    .transform(d=>d,
               transformFailed _) 
  }
  
  override def removeFundsRollback(accountUid: UUID) = { request =>
    logger.info(s"removeFundsRollback $accountUid")
    entityRef(accountUid)
    .ask(RemoveFundsRollback.apply(request.transactionUid,
                                    request.amountRemoved,
                                    request.rollbackReason))
    .transform(d=>d,
               transformFailed _) 
  }
  
  override def accountStream(userUid: UUID) = { upstream =>
    logger.debug("accountStream")
    val downstream = Source.queue[TopicApiAccountEvent](100, OverflowStrategy.fail)
    val result = downstream.mapMaterializedValue(downstreamSourceQueue =>{
      val connectionUid = UUID.randomUUID
      val subscriberActor = system.actorOf(AccountSubscriberActor.props(pubSub, downstreamSourceQueue, connectionUid, userUid),
                                           s"AccountSubscriber-$connectionUid")
      upstream.runWith(Sink.actorRef(subscriberActor, AccountSubscriberActor.Disconnect))
      NotUsed
    })
    Future(result)
  }
  
  private def transformFailed(ex: Throwable): Throwable = {
    logger.error("Error: $ex")
     ex match {
      case e @ (_: InvalidCommandException | _:UnhandledCommandException)  => BadRequest(e.getMessage)
      case _ => ex
    }
  }
  
  override def accountEvents = TopicProducer.taggedStreamWithOffset(AccountEvent.Tag.allTags.toList) { (tag, offset) =>
    registry.eventStream(tag, offset).filter{_.event.isPublic }.mapAsync(1)(convertEvent)
  }
  
  private def convertEvent(eventStreamElement: EventStreamElement[AccountEvent]): Future[(TopicApiAccountEvent, Offset)] = {
    eventStreamElement match {
      case EventStreamElement(accountUid, event: AccountCreated, offset) => Future(toApi(UUID.fromString(accountUid), event.asInstanceOf[AccountCreated]),offset)
      case EventStreamElement(accountUid, event: AccountClosed, offset) => Future(toApi(UUID.fromString(accountUid), event.asInstanceOf[AccountClosed]),offset)
      case EventStreamElement(accountUid, event: FundsAdded, offset) => Future(toApi(UUID.fromString(accountUid), event.asInstanceOf[FundsAdded]),offset)
      case EventStreamElement(accountUid, event: FundsAddRollbacked, offset) => Future(toApi(UUID.fromString(accountUid), event.asInstanceOf[FundsAddRollbacked]),offset)
      case EventStreamElement(accountUid, event: FundsRemoved, offset) => Future(toApi(UUID.fromString(accountUid), event.asInstanceOf[FundsRemoved]),offset)
      case EventStreamElement(accountUid, event: FundsRemoveRollbacked, offset) => Future(toApi(UUID.fromString(accountUid), event.asInstanceOf[FundsRemoveRollbacked]),offset)
    }
  }

  private def entityRef(accountUid: UUID) = entityRefString(accountUid.toString)

  private def entityRefString(accountUid: String) = registry.refFor[AccountEntity](accountUid)

}
