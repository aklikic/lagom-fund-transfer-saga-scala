package com.klikix.fundtransfersaga.account.entity.account

import java.util.UUID

import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.ReplyType

import akka.Done
import play.api.libs.json.Format
import play.api.libs.json.Json
import com.klikix.util.general.JsonFormats._


sealed trait AccountCommand

case object GetAccount extends AccountCommand with ReplyType[Option[Account]] {
  implicit val format: Format[GetAccount.type] = singletonFormat(GetAccount)
}

/*
 * Account administration
 */

case class CreateAccount(account: Account) extends AccountCommand with ReplyType[Done]
  
object CreateAccount {
  implicit val format: Format[CreateAccount] = Json.format
}

case object CloseAccount extends AccountCommand with ReplyType[Done]{
   implicit val format: Format[CloseAccount.type] = singletonFormat(CloseAccount)
}
  
/*
 * Add funds
 */
case class AddFunds(transactionUid: UUID,
                    amountToAdd: Int
                    ) extends AccountCommand with ReplyType[Done]

object AddFunds {
  implicit val format: Format[AddFunds] = Json.format
}

case class AddFundsRollback(transactionUid: UUID,
                            amountAdded: Int,
                            rollbackReason: String
                           ) extends AccountCommand with ReplyType[Done]

object AddFundsRollback {
  implicit val format: Format[AddFundsRollback] = Json.format
}


/*
 * Remove funds
 */
case class RemoveFunds(transactionUid: UUID,
                       amountToRemove: Int
                      ) extends AccountCommand with ReplyType[Done]

object RemoveFunds {
  implicit val format: Format[RemoveFunds] = Json.format
}

case class RemoveFundsRollback(transactionUid: UUID,
                               amountRemoved: Int,
                               rollbackReason: String
                               ) extends AccountCommand with ReplyType[Done]

object RemoveFundsRollback {
  implicit val format: Format[RemoveFundsRollback] = Json.format
}