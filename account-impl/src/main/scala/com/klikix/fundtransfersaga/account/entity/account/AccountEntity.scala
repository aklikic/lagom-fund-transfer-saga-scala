package com.klikix.fundtransfersaga.account.entity.account

import java.time.{Duration, Instant}
import java.util.UUID

import akka.Done
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.ReplyType
import com.lightbend.lagom.scaladsl.persistence.{AggregateEvent, AggregateEventTag, PersistentEntity}
import play.api.libs.json.{Format, Json}
import com.klikix.util.general.JsonFormats._
import java.time.OffsetDateTime
import com.klikix.fundtransfersaga.account.entity.account._
import com.lightbend.lagom.scaladsl.pubsub._
import com.klikix.fundtransfersaga.account.impl.Mappers._
import com.klikix.fundtransfersaga.account.api.{AccountEvent => ApiAccountEvent}
import com.klikix.fundtransfersaga.account.impl.PubSub._
import com.typesafe.scalalogging.LazyLogging


class AccountEntity(pubSubRegistry: PubSubRegistry) extends PersistentEntity with LazyLogging {

  private val publishedTopic = pubSubRegistry.refFor(topicAll)
  override type Command = AccountCommand
  override type Event = AccountEvent
  override type State = Option[Account]

  override def initialState: Option[Account] = None

  override def behavior: Behavior = {
    case None => notCreated
    case Some(account) if account.status == AccountStatus.Created => created(account)
    case Some(account) if account.status == AccountStatus.Closed => closed(account)
  }
 
  private val notCreated = {
    Actions()
    .onCommand[CreateAccount, Done] {
      case (CreateAccount(account), ctx, state) =>
        ctx.thenPersist(AccountCreated.apply(account,OffsetDateTime.now))(evt => {pubSub(evt) 
                                                                                  ctx.reply(Done)})
    }.onEvent {
      case (AccountCreated(account, _), state) => Some(account)
    }.orElse(other(Some("Account does not exist")))
  }
  
  private def created (account: Account) = {
    Actions()
    .onReadOnlyCommand[CreateAccount, Done] {
      case (_, ctx, _) => ctx.reply(Done)
    }.onCommand[AddFunds,Done]{
      case (AddFunds(transactionUid,amountToAdd),ctx,_) => 
        val newAccount = account.addFunds(amountToAdd)
        ctx.thenPersist(FundsAdded.apply(transactionUid,amountToAdd,newAccount.amount,account.amount,OffsetDateTime.now))(evt => {pubSub(evt) 
                                                                                                                                  ctx.reply(Done)})
    }.onCommand[AddFundsRollback,Done]{
      case (AddFundsRollback(transactionUid,amountAdded,rollbackReason),ctx,state) => 
        val newAccount = account.addFundsRollback(amountAdded) 
        if(newAccount.amount<0){
          ctx.invalidCommand("After add rollback funds can not be less then 0")
          ctx.done
        }
        ctx.thenPersist(FundsAddRollbacked.apply(transactionUid,amountAdded,newAccount.amount,state.get.amount,rollbackReason,OffsetDateTime.now))(evt => {pubSub(evt) 
                                                                                                                                                           ctx.reply(Done)})
    }.onCommand[RemoveFunds,Done]{
      case (RemoveFunds(transactionUid,amountToRemove),ctx,_) => 
        val newAccount = account.removeFunds(amountToRemove)
        if(newAccount.amount<0){
          ctx.invalidCommand("Insufficient funds")
          ctx.done
        }
        ctx.thenPersist(FundsRemoved.apply(transactionUid,amountToRemove,newAccount.amount,account.amount,OffsetDateTime.now))(evt => {pubSub(evt) 
                                                                                                                                       ctx.reply(Done)})
    }.onCommand[RemoveFundsRollback,Done]{
      case (RemoveFundsRollback(transactionUid,amountRemoved,rollbackReason),ctx,_) => 
        val newAccount = account.removeFundsRollback(amountRemoved) 
        ctx.thenPersist(FundsRemoveRollbacked.apply(transactionUid,amountRemoved,newAccount.amount,account.amount,rollbackReason,OffsetDateTime.now))(evt => {pubSub(evt) 
                                                                                                                                                              ctx.reply(Done)})
    
    }.onCommand[CloseAccount.type,Done]{
      case (_,ctx,_) => 
        if(account.amount!=0){
          ctx.invalidCommand("Need to empty funds before closing account")
          ctx.done
        }
        val closedAccount = account.close
        ctx.thenPersist(AccountClosed.apply(closedAccount,OffsetDateTime.now))(evt => {pubSub(evt) 
                                                                                       ctx.reply(Done)})
    }.onEvent {
      case (FundsAdded( _, amountToAdd, _, _, _), state) => state.map(_.addFunds(amountToAdd))
      case (FundsAddRollbacked( _, amountAdded, _, _, _, _), state) => state.map(_.addFundsRollback(amountAdded))
      case (FundsRemoved( _, amountToRemove, _, _, _), state) => state.map(_.removeFunds(amountToRemove))
      case (FundsRemoveRollbacked( _, amountRemoved, _, _, _, _), state) => state.map(_.removeFundsRollback(amountRemoved))
      case (AccountClosed( _, _), state) => state.map(_.close)
    }.orElse(other(None))
  }
  
   private def closed (account: Account) = {
    Actions()
    .onReadOnlyCommand[CloseAccount.type, Done] {
      case (_, ctx, _) => ctx.reply(Done)
    }.orElse(other(Some("Account is closed")))
  }
   
  private def other (maybeMessage: Option[String]) = { 
    val message = maybeMessage.getOrElse("N/A");
    Actions()
    .onReadOnlyCommand[CreateAccount, Done] {
      case (_, ctx, _) => ctx.invalidCommand(message)
    }.onReadOnlyCommand[AddFunds, Done] {
      case (_, ctx, _) => ctx.invalidCommand(message)
    }.onReadOnlyCommand[AddFundsRollback, Done] {
      case (_, ctx, _) => ctx.invalidCommand(message)
    }.onReadOnlyCommand[RemoveFunds, Done] {
      case (_, ctx, _) => ctx.invalidCommand(message)
    }.onReadOnlyCommand[RemoveFundsRollback, Done] {
      case (_, ctx, _) => ctx.invalidCommand(message)
    }.onReadOnlyCommand[CloseAccount.type, Done] {
      case (_, ctx, _) => ctx.invalidCommand(message)
    }.onReadOnlyCommand[GetAccount.type, Option[Account]] {
      case (GetAccount, ctx, state) => ctx.reply(state)
    }
  }

  private def pubSub(evt: AccountEvent):Unit = {
    publish(entityUid(), evt, publishedTopic)
  }
  
  private def entityUid(): UUID = {
    UUID.fromString(entityId)
  }

}
