package com.klikix.fundtransfersaga.account.api

import java.time.{Duration, Instant}
import java.util.UUID

import com.klikix.util.general.JsonFormats._
import com.lightbend.lagom.scaladsl.api.deser.PathParamSerializer
import play.api.libs.json.{Format, Json}

case class CreateAccountRequest(
  accountUid: Option[UUID],
  ownerUid: UUID,
  intitalAmount: Option[Int]
){}

object CreateAccountRequest {
  implicit val format: Format[CreateAccountRequest] = Json.format
}

/*
 * Add funds
 */

case class AddFundsRequest(
  transactionUid: UUID,
  amountToAdd: Int
){}

object AddFundsRequest {
  implicit val format: Format[AddFundsRequest] = Json.format
}

case class AddFundsRollbackRequest(
  transactionUid: UUID,
  amountAdded: Int,
  rollbackReason: String
){}

object AddFundsRollbackRequest {
  implicit val format: Format[AddFundsRollbackRequest] = Json.format
}

/*
 * Remove funds
 */
case class RemoveFundsRequest(
  transactionUid: UUID,
  amountToRemove: Int
){}

object RemoveFundsRequest {
  implicit val format: Format[RemoveFundsRequest] = Json.format
}

case class RemoveFundsRollbackRequest(
  transactionUid: UUID,
  amountRemoved: Int,
  rollbackReason: String
){}

object RemoveFundsRollbackRequest {
  implicit val format: Format[RemoveFundsRollbackRequest] = Json.format
}

/*
 * Account metadata
 */

case class Account(
  accountUid: UUID,
  ownerUid: UUID,
  amount: Int,
  status: AccountStatus.Status
){}

object Account {
  implicit val format: Format[Account] = Json.format

  def create(owner: UUID, initialAmount: Int) = Account(UUID.randomUUID(),owner, initialAmount, AccountStatus.Created)
}

object AccountStatus extends Enumeration {
  val Created,Closed = Value
  type Status = Value

  implicit val format: Format[Value] = enumFormat(this)
}
