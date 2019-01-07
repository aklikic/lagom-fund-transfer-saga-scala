package com.klikix.fundtransfersaga.transfer.entity.transfer.view

import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraReadSide
import scala.concurrent.ExecutionContext
import com.lightbend.lagom.scaladsl.persistence._
import com.klikix.fundtransfersaga.account.api._
import java.util.UUID
import com.klikix.fundtransfersaga.transfer.entity.transfer._
import com.klikix.fundtransfersaga.account.api.RemoveFundsRequest
import com.typesafe.scalalogging.LazyLogging
import com.lightbend.lagom.scaladsl.api.transport._
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity._
import akka.Done
import com.datastax.driver.core.BoundStatement

class TransferSagaReadSideProcessor(readSide: CassandraReadSide, registry: PersistentEntityRegistry, accountService: AccountService)(implicit ec: ExecutionContext)
    extends ReadSideProcessor[TransferEvent] with LazyLogging{
  
  def buildHandler = {
    readSide.builder[TransferEvent]("transferSagaOffset")
        .setEventHandler[SourceRemoveStarted](e => handleSourceRemoveStarted(e.event))
        .setEventHandler[DestinationAddStarted](e => handleDestinationAddStarted(e.event))
        .setEventHandler[SourceRemoveRollbackStarted](e => handleSourceRemoveRollbackStarted(e.event))
        .build
  }
  
  private def handleSourceRemoveStarted(event: SourceRemoveStarted) = {
    logger.debug(s"handleSourceRemoveStarted: {}",event.data)
    accountService.removeFunds(event.data.sourceAccountUid).invoke(RemoveFundsRequest.apply(event.data.transactionUid, event.data.amount))
    .recoverWith{ 
      case e @ (_: NotFound | _: BadRequest) => entityRef(event.data.sourceAccountUid).ask(SetSourceRemoveFailed.apply(e.getMessage)).recover(recoverFailedCommand)
      case e => throw e //needs to retry in this case
    }.transformWith(
      _ => entityRef(event.data.sourceAccountUid).ask(SetSourceRemoved).recover(recoverFailedCommand)
    ).map(_ => ok)
  }
  
  private def handleDestinationAddStarted(event: DestinationAddStarted) = {
    logger.debug(s"handleDestinationAddStarted: {}",event.data)
    accountService.addFunds(event.data.destinationAccountUid).invoke(AddFundsRequest.apply(event.data.transactionUid, event.data.amount))
    .recoverWith{ 
      case e @ (_: NotFound | _: BadRequest) => entityRef(event.data.destinationAccountUid).ask(SetDestinationAddFailed.apply(e.getMessage)).recover(recoverFailedCommand)
      case e => throw e //needs to retry in this case
    }.transformWith(
      _ => entityRef(event.data.sourceAccountUid).ask(SetDestinationAdded).recover(recoverFailedCommand)
    ).map(_ => ok)
  }
  
  private def handleSourceRemoveRollbackStarted(event: SourceRemoveRollbackStarted) = {
    logger.debug(s"handleSourceRemoveRollbackStarted: {}",event.data)
    accountService.removeFundsRollback(event.data.sourceAccountUid).invoke(RemoveFundsRollbackRequest.apply(event.data.transactionUid, event.data.amount, event.rollbackReason))
    .recoverWith{ 
      case e @ (_: NotFound | _: BadRequest) => entityRef(event.data.sourceAccountUid).ask(SetSourceRemoveRollbackFailed.apply(e.getMessage)).recover(recoverFailedCommand)
      case e => throw e //needs to retry in this case
    }.transformWith(
      _ => entityRef(event.data.sourceAccountUid).ask(SetSourceRemoveRollbacked).recover(recoverFailedCommand)
    ).map(_ => ok)
  }
  
  val recoverFailedCommand: PartialFunction[Throwable, Done] = {
     
     case e @ (_: InvalidCommandException | _: UnhandledCommandException) => Done
     case e => throw e
  }
  
  val ok: List[BoundStatement] = List()
  
  private def entityRef(sourceAccountUid: UUID) = registry.refFor[TransferEntity](sourceAccountUid.toString)

  def aggregateTags = TransferEvent.Tag.allTags
}