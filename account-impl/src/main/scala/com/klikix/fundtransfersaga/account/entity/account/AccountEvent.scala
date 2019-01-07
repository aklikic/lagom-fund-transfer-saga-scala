package com.klikix.fundtransfersaga.account.entity.account

import java.time.OffsetDateTime
import java.util.UUID

import com.lightbend.lagom.scaladsl.persistence.AggregateEvent
import com.lightbend.lagom.scaladsl.persistence.AggregateEventTag

import play.api.libs.json.Format
import play.api.libs.json.Json

sealed trait AccountEvent extends AggregateEvent[AccountEvent] {
  override def aggregateTag = AccountEvent.Tag
  var isPublic = true //override per event is not public
}

object AccountEvent {
  val NumShards = 3
  val Tag = AggregateEventTag.sharded[AccountEvent](NumShards)
}

/*
 * Account management
 */
case class AccountCreated(account: Account,
                          timestamp: OffsetDateTime 
                          ) extends AccountEvent

object AccountCreated {
  implicit val format: Format[AccountCreated] = Json.format
}

case class AccountClosed(account: Account,
                         timestamp: OffsetDateTime 
                         ) extends AccountEvent

object AccountClosed {
  implicit val format: Format[AccountClosed] = Json.format
}


/*
 * Add funds
 */
case class FundsAdded(transactionUid: UUID, 
                      amountAdded: Int,
                      newAmount: Int,
                      oldAmount: Int,
                      timestamp: OffsetDateTime 
                      ) extends AccountEvent

object FundsAdded {
  implicit val format: Format[FundsAdded] = Json.format
}

case class FundsAddRollbacked(transactionUid: UUID, 
                              amountAdded: Int,
                              newAmount: Int,
                              oldAmount: Int,
                              rollbackReason: String,
                              timestamp: OffsetDateTime 
                              ) extends AccountEvent

object FundsAddRollbacked {
  implicit val format: Format[FundsAddRollbacked] = Json.format
}
/*
case class FundsAddRollbackFailed(transactionUid: UUID, 
                                  amountAdded: Int,
                                  rollbackReason: String,
                                  reason: String,
                                  timestamp: OffsetDateTime 
                                  ) extends AccountEvent

object FundsAddRollbackFailed {
  implicit val format: Format[FundsAddRollbackFailed] = Json.format
}
*/

/*
 * Remove funds
 */
case class FundsRemoved(transactionUid: UUID, 
                        amountRemoved: Int,
                        newAmount: Int,
                        oldAmount: Int,
                        timestamp: OffsetDateTime 
                        ) extends AccountEvent

object FundsRemoved {
  implicit val format: Format[FundsRemoved] = Json.format
}
/*
case class FundsRemoveFailed(transactionUid: UUID, 
                             amountToRemove: Int,
                             amount: Int,
                             reason: String,
                             timestamp: OffsetDateTime 
                             ) extends AccountEvent

object FundsRemoveFailed {
  implicit val format: Format[FundsRemoveFailed] = Json.format
}
*/
case class FundsRemoveRollbacked(transactionUid: UUID, 
                                 amountRemoved: Int,
                                 newAmount: Int,
                                 oldAmount: Int,
                                 rollbackReason: String,
                                 timestamp: OffsetDateTime 
                                 ) extends AccountEvent

object FundsRemoveRollbacked {
  implicit val format: Format[FundsRemoveRollbacked] = Json.format
}

/*
case class FundsRemoveRollbackFailed(transactionUid: UUID, 
                                     amountRemoved: Int,
                                     rollbackReason: String,
                                     reason: String,
                                     timestamp: OffsetDateTime 
                                     ) extends AccountEvent

object FundsRemoveRollbackFailed {
  implicit val format: Format[FundsRemoveRollbackFailed] = Json.format
}
*/
