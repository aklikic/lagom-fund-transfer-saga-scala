package com.klikix.fundtransfersaga.transfer.entity.transfer

import java.time.{Duration, Instant}
import java.util.UUID

import akka.Done
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.ReplyType
import com.lightbend.lagom.scaladsl.persistence.{AggregateEvent, AggregateEventTag, PersistentEntity}
import play.api.libs.json.{Format, Json}
import com.klikix.util.general.JsonFormats._
import java.time.OffsetDateTime
import com.klikix.fundtransfersaga.transfer.entity.transfer._



class TransferEntity extends PersistentEntity {
  override type Command = TransferCommand
  override type Event = TransferEvent
  override type State = Option[Transfer]

  override def initialState: Option[Transfer] = None

  override def behavior: Behavior = {
    case None => notCreated
    case Some(transfer) if transfer.status == TransferStatus.SourceRemoveStarted => sourceRemoveStarted(transfer)
    case Some(transfer) if transfer.status == TransferStatus.SourceRemoveFailed => sourceRemoveFailed(transfer)
    case Some(transfer) if transfer.status == TransferStatus.DestinationAddStarted => destinationAddStarted(transfer)
    case Some(transfer) if transfer.status == TransferStatus.TransferSuccessful => transferSuccessful(transfer)
    case Some(transfer) if transfer.status == TransferStatus.SourceRemoveRollbackStarted => sourceRemoveRollbackStarted(transfer)
    case Some(transfer) if transfer.status == TransferStatus.SourceRemoveRollbacked => sourceRemoveRollbacked(transfer)
    case Some(transfer) if transfer.status == TransferStatus.SourceRemoveRollbackFailed => sourceRemoveRollbackFailed(transfer)
  }
 
  private val notCreated = {
    Actions()
    .onCommand[StartTransfer, Option[Transfer]] {
      case (StartTransfer(data), ctx, state) =>
        ctx.thenPersist(SourceRemoveStarted.apply(data,OffsetDateTime.now))(_ => ctx.reply(Some(Transfer.create(data))))
    }.onEvent {
      case (SourceRemoveStarted(data, _), state) => Some(Transfer.create(data))
    }.orElse(other(Some("Transfer does not exist")))
  }
  
  private def sourceRemoveStarted (transfer: Transfer) = {
    Actions()
    .onCommand[SetSourceRemoved.type,Done]{
      case (_,ctx,_) => 
        ctx.thenPersist(DestinationAddStarted.apply(transfer.data,OffsetDateTime.now))(_ => ctx.reply(Done))
    }.onCommand[SetSourceRemoveFailed,Done]{
      case (SetSourceRemoveFailed(reason),ctx,_) => 
        ctx.thenPersist(SourceRemoveFailed.apply(transfer.data,reason,OffsetDateTime.now))(_ => ctx.reply(Done))
    }.onEvent {
      case (DestinationAddStarted(_, _), state) => state.map(_.destinationAddStarted)
      case (SourceRemoveFailed(_,reason, _), state) => state.map(_.sourceRemoveFailed(reason))
    }.orElse(other(Some("In transfer [SourceRemoveStarted]")))
  }
  
  private def destinationAddStarted (transfer: Transfer) = {
    Actions()
    .onCommand[SetDestinationAdded.type,Done]{
      case (_,ctx,_) => 
        ctx.thenPersist(TransferSuccessful.apply(transfer.data,OffsetDateTime.now))(_ => ctx.reply(Done))
    }.onCommand[SetDestinationAddFailed,Done]{
      case (SetDestinationAddFailed(reason),ctx,_) => 
        ctx.thenPersist(SourceRemoveRollbackStarted.apply(transfer.data,reason,OffsetDateTime.now))(_ => ctx.reply(Done))
    }.onEvent {
      case (TransferSuccessful(_, _), state) => state.map(_.transferSuccessful)
      case (SourceRemoveRollbackStarted(_,reason, _), state) => state.map(_.sourceRemoveRollbackStarted(reason))
    }.orElse(other(Some("In transfer [DestinationAddStarted]")))
  }
  private def sourceRemoveFailed (transfer: Transfer) = {
    other(Some("Finished [SourceRemoveFailed]"))
  }
  private def transferSuccessful (transfer: Transfer) = {
    other(Some("Finished [TransferSuccessful]"))
  }
  private def sourceRemoveRollbackStarted (transfer: Transfer) = {
    Actions()
    .onCommand[SetSourceRemoveRollbacked.type,Done]{
      case (_,ctx,_) => 
        ctx.thenPersist(SourceRemoveRollbacked.apply(transfer.data,transfer.rollbackReason.getOrElse(null),OffsetDateTime.now))(_ => ctx.reply(Done))
    }.onCommand[SetSourceRemoveRollbackFailed,Done]{
      case (SetSourceRemoveRollbackFailed(reason),ctx,_) => 
        ctx.thenPersist(SourceRemoveRollbackFailed.apply(transfer.data,transfer.rollbackReason.getOrElse(null),reason,OffsetDateTime.now))(_ => ctx.reply(Done))
    }.onEvent {
      case (SourceRemoveRollbacked(_,_, _), state) => state.map(_.sourceRemoveRollbacked)
      case (SourceRemoveRollbackFailed(_,_,reason, _), state) => state.map(_.sourceRemoveRollbackFailed(reason))
    }.orElse(other(Some("In transfer [SourceRemoveRollbackStarted]")))
  }
  private def sourceRemoveRollbacked (transfer: Transfer) = {
    other(Some("Finished [SourceRemoveRollbacked]"))
  }
  private def sourceRemoveRollbackFailed (transfer: Transfer) = {
    other(Some("Finished [SourceRemoveRollbackFailed]"))
  }
  
  private def other (maybeMessage: Option[String]) = { 
    val message = maybeMessage.getOrElse("N/A");
    Actions()
    .onReadOnlyCommand[GetTransfer.type, Option[Transfer]] {
      case (GetTransfer, ctx, state) => ctx.reply(state)
    }.onReadOnlyCommand[StartTransfer, Option[Transfer]] {
      case (_, ctx, _) => ctx.invalidCommand(message)
    }.onReadOnlyCommand[SetSourceRemoved.type, Done] {
      case (_, ctx, _) => ctx.invalidCommand(message)
    }.onReadOnlyCommand[SetSourceRemoveFailed, Done] {
      case (_, ctx, _) => ctx.invalidCommand(message)
    }.onReadOnlyCommand[SetDestinationAddFailed, Done] {
      case (_, ctx, _) => ctx.invalidCommand(message)
    }.onReadOnlyCommand[SetSourceRemoveRollbacked.type, Done] {
      case (_, ctx, _) => ctx.invalidCommand(message)
    }.onReadOnlyCommand[SetSourceRemoveRollbackFailed, Done] {
      case (_, ctx, _) => ctx.invalidCommand(message)
    }.onReadOnlyCommand[SetDestinationAdded.type, Done] {
      case (_, ctx, _) => ctx.invalidCommand(message)
    }
  }
  
  private def entityUid(): UUID = {
    UUID.fromString(entityId)
  }

}
