package com.klikix.fundtransfersaga.account.api

import java.time.Instant
import java.util.UUID

import julienrf.json.derived
import play.api.libs.json._
import java.time.OffsetDateTime

sealed trait AccountEvent {
  val accountUid: UUID
}

object AccountEvent {
  implicit val format: Format[AccountEvent] =
    derived.flat.oformat((__ \ "type").format[String])
}

/*
 * Account administration
 */
case class AccountCreated(accountUid: UUID, 
                          ownerUid: UUID, 
                          amount: Int,
                          timestamp: OffsetDateTime
                          ) extends AccountEvent

object AccountCreated {
  implicit val format: Format[AccountCreated] = Json.format
}

case class AccountClosed(accountUid: UUID, 
                         ownerUid: UUID, 
                         amount: Int,
                         timestamp: OffsetDateTime
                         ) extends AccountEvent

object AccountClosed {
  implicit val format: Format[AccountClosed] = Json.format
}

/*
 * Add funds
 */
case class AccountFundsAdded(accountUid: UUID, 
                             transactionUid: UUID, 
                             amountAdded: Int,
                             newAmount: Int,
                             oldAmount: Int,
                             timestamp: OffsetDateTime 
                             ) extends AccountEvent

object AccountFundsAdded {
  implicit val format: Format[AccountFundsAdded] = Json.format
}

case class AccountFundsAddRollbacked(accountUid: UUID, 
                                     transactionUid: UUID, 
                                     amountAdded: Int,
                                     newAmount: Int,
                                     oldAmount: Int,
                                     rollbackReason: String,
                                     timestamp: OffsetDateTime 
                                     ) extends AccountEvent

object AccountFundsAddRollbacked {
  implicit val format: Format[AccountFundsAddRollbacked] = Json.format
}
/*
case class AccountFundsAddRollbackFailed(accountUid: UUID, 
                                         transactionUid: UUID, 
                                         amountAdded: Int,
                                         rollbackReason: String,
                                         reason: String,
                                         timestamp: OffsetDateTime 
                                     ) extends AccountEvent

object AccountFundsAddRollbackFailed {
  implicit val format: Format[AccountFundsAddRollbackFailed] = Json.format
}
*/

/*
 * Remove funds
 */
case class AccountFundsRemoved(accountUid: UUID, 
                               transactionUid: UUID, 
                               amountRemoved: Int,
                               newAmount: Int,
                               oldAmount: Int,
                               timestamp: OffsetDateTime 
                               ) extends AccountEvent

object AccountFundsRemoved {
  implicit val format: Format[AccountFundsRemoved] = Json.format
}
/*
case class AccountFundsRemoveFailed(accountUid: UUID, 
                                    transactionUid: UUID, 
                                    reason: String,
                                    timestamp: OffsetDateTime 
                                    ) extends AccountEvent

object AccountFundsRemoveFailed {
  implicit val format: Format[AccountFundsRemoveFailed] = Json.format
}
*/
case class AccountFundsRemoveRollbacked(accountUid: UUID, 
                                        transactionUid: UUID, 
                                        amountRemoved: Int,
                                        newAmount: Int,
                                        oldAmount: Int,
                                        rollbackReason: String,
                                        timestamp: OffsetDateTime 
                                       ) extends AccountEvent

object AccountFundsRemoveRollbacked {
  implicit val format: Format[AccountFundsRemoveRollbacked] = Json.format
}

/*
case class AccountFundsRemoveRollbackFailed(accountUid: UUID, 
                                            transactionUid: UUID,
                                            amountRemoved: Int,
                                            rollbackReason: String,
                                            reason: String,
                                            timestamp: OffsetDateTime 
                                            ) extends AccountEvent

object AccountFundsRemoveRollbackFailed {
  implicit val format: Format[AccountFundsRemoveRollbackFailed] = Json.format
}

*/



