package com.klikix.fundtransfersaga.transfer.entity.transfer

import java.util.UUID

import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.ReplyType

import akka.Done
import play.api.libs.json.Format
import play.api.libs.json.Json
import com.klikix.util.general.JsonFormats._


sealed trait TransferCommand

case object GetTransfer extends TransferCommand with ReplyType[Option[Transfer]] {
  implicit val format: Format[GetTransfer.type] = singletonFormat(GetTransfer)
}


case class StartTransfer(data: TransferData) extends TransferCommand with ReplyType[Option[Transfer]]
  
object StartTransfer {
  implicit val format: Format[StartTransfer] = Json.format
}

case object SetSourceRemoved extends TransferCommand with ReplyType[Done]{
   implicit val format: Format[SetSourceRemoved.type] = singletonFormat(SetSourceRemoved)
}

case class SetSourceRemoveFailed(reason: String) extends TransferCommand with ReplyType[Done]

object SetSourceRemoveFailed {
  implicit val format: Format[SetSourceRemoveFailed] = Json.format
}

case class SetDestinationAddFailed(reason: String) extends TransferCommand with ReplyType[Done]

object SetDestinationAddFailed {
  implicit val format: Format[SetDestinationAddFailed] = Json.format
}


case object SetSourceRemoveRollbacked extends TransferCommand with ReplyType[Done]{
   implicit val format: Format[SetSourceRemoveRollbacked.type] = singletonFormat(SetSourceRemoveRollbacked)
}

case class SetSourceRemoveRollbackFailed(reason: String) extends TransferCommand with ReplyType[Done]

object SetSourceRemoveRollbackFailed {
  implicit val format: Format[SetSourceRemoveRollbackFailed] = Json.format
}

case object SetDestinationAdded extends TransferCommand with ReplyType[Done]{
   implicit val format: Format[SetDestinationAdded.type] = singletonFormat(SetDestinationAdded)
}