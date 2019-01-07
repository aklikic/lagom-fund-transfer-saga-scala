package com.klikix.fundtransfersaga.account.entity.account

import java.util.UUID

import com.klikix.util.general.JsonFormats.enumFormat

import play.api.libs.json.Format
import play.api.libs.json.Json
  
object AccountStatus extends Enumeration {
  val Created,Closed = Value
  type Status = Value
  
  implicit val format: Format[Status] = enumFormat(AccountStatus)
}

case class Account(
  ownerUid: UUID,
  amount: Int,
  status: AccountStatus.Status
) {
  
  def addFunds(amountToAdd: Int) = {
    copy(
      amount=amount+amountToAdd
    )
  }
  
  def addFundsRollback(amountAdded: Int) = removeFunds(amountAdded)

  def removeFunds(amountToRemove: Int) = {
    copy(
      amount=amount-amountToRemove
    )
  }
  
  def removeFundsRollback(amountRemoved: Int) = addFunds(amountRemoved)
  
  def close() = {
    copy(
      status=AccountStatus.Closed
    )
  }
}

object Account {
  implicit val format: Format[Account] = Json.format
  def create(ownerUid: UUID,
             amount: Int) = Account(ownerUid,amount,AccountStatus.Created)
}