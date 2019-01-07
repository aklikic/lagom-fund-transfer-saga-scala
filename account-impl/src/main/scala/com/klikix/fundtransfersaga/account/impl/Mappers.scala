package com.klikix.fundtransfersaga.account.impl

import com.klikix.fundtransfersaga.account.api.CreateAccountRequest
import com.klikix.fundtransfersaga.account.api.{Account => ApiAccount, 
                                                AccountStatus => ApiAccountStatus, 
                                                AccountCreated => TopicApiAccountCreated,
                                                AccountClosed => TopicApiAccountClosed,
                                                AccountFundsAdded => TopicApiAccountFundsAdded,
                                                AccountFundsAddRollbacked => TopicApiAccountFundsAddRollbacked,
                                                AccountFundsRemoved => TopicApiAccountFundsRemoved,
                                                AccountFundsRemoveRollbacked => TopicApiAccountFundsRemoveRollbacked
                                                }
import com.klikix.fundtransfersaga.account.entity.account.{Account => ImplAccount, 
                                                           AccountStatus => ImplAccountStatus}
import java.util.UUID
import com.klikix.fundtransfersaga.account.entity.account._

object Mappers{
  
  def fromApi(request: CreateAccountRequest): ImplAccount = {
    ImplAccount.create(request.ownerUid,request.intitalAmount.getOrElse(0))
  }
  
  def toApi(accountUid: UUID, account: ImplAccount): ApiAccount = {
    ApiAccount.apply(accountUid,account.ownerUid,account.amount,toApi(account.status))
  }
  
  def toApi(status: ImplAccountStatus.Status): ApiAccountStatus.Status = {
    status match {
      case ImplAccountStatus.Created => ApiAccountStatus.Created 
      case ImplAccountStatus.Closed => ApiAccountStatus.Closed 
    }
  }
  
  def toApi(accountUid: UUID, event: AccountCreated): TopicApiAccountCreated = {
    TopicApiAccountCreated.apply(accountUid, event.account.ownerUid, event.account.amount, event.timestamp)
  }
  def toApi(accountUid: UUID, event: AccountClosed): TopicApiAccountClosed = {
    TopicApiAccountClosed.apply(accountUid, event.account.ownerUid, event.account.amount, event.timestamp)
  }
  def toApi(accountUid: UUID, event: FundsAdded): TopicApiAccountFundsAdded = {
    TopicApiAccountFundsAdded.apply(accountUid, event.transactionUid, event.amountAdded, event.newAmount, event.oldAmount, event.timestamp)
  }
  def toApi(accountUid: UUID, event: FundsAddRollbacked): TopicApiAccountFundsAddRollbacked = {
    TopicApiAccountFundsAddRollbacked.apply(accountUid, event.transactionUid, event.amountAdded, event.newAmount, event.oldAmount, event.rollbackReason, event.timestamp)
  }
  def toApi(accountUid: UUID, event: FundsRemoved): TopicApiAccountFundsRemoved = {
    TopicApiAccountFundsRemoved.apply(accountUid, event.transactionUid, event.amountRemoved, event.newAmount, event.oldAmount, event.timestamp)
  }
  def toApi(accountUid: UUID, event: FundsRemoveRollbacked): TopicApiAccountFundsRemoveRollbacked = {
    TopicApiAccountFundsRemoveRollbacked.apply(accountUid, event.transactionUid, event.amountRemoved, event.newAmount, event.oldAmount, event.rollbackReason, event.timestamp)
  }
}