package com.klikix.fundtransfersaga.transfer.entity.transfer

import java.util.UUID

import com.klikix.util.general.JsonFormats.enumFormat

import play.api.libs.json.Format
import play.api.libs.json.Json
  

case class Transfer(
  data: TransferData,
  rollbackReason: Option[String],
  failReason: Option[String],
  status: TransferStatus.Value
){
  def sourceRemoveFailed(failReason: String) = {
    copy(
      status = TransferStatus.SourceRemoveFailed,
      failReason = Some(failReason)
    )
  }
  def destinationAddStarted() = {
    copy(
      status = TransferStatus.DestinationAddStarted 
    )
  }
  def sourceRemoveRollbackStarted(rollbackReason: String) = {
    copy(
      status = TransferStatus.SourceRemoveRollbackStarted,
      rollbackReason = Some(rollbackReason)
    )
  }
  def sourceRemoveRollbackFailed(failReason: String) = {
    copy(
      status = TransferStatus.SourceRemoveRollbackFailed,
      failReason = Some(failReason)
    )
  }
  def sourceRemoveRollbacked() = {
    copy(
      status = TransferStatus.SourceRemoveRollbacked 
    )
  }
  def transferSuccessful() = {
    copy(
      status = TransferStatus.TransferSuccessful 
    )
  }
}

object Transfer {
  implicit val format: Format[Transfer] = Json.format
  def create (data: TransferData) = Transfer(data,None,None,TransferStatus.SourceRemoveStarted)
}

case class TransferData(
  transactionUid: UUID,
  sourceAccountUid: UUID,
  destinationAccountUid: UUID,
  amount: Int
)

object TransferData {
  implicit val format: Format[TransferData] = Json.format
}

object TransferStatus extends Enumeration {
  val SourceRemoveStarted, 
      SourceRemoveFailed, 
      DestinationAddStarted,
      SourceRemoveRollbackStarted,
      SourceRemoveRollbackFailed,
      SourceRemoveRollbacked,
      TransferSuccessful
      = Value
  type Status = Value

  implicit val format: Format[Value] = enumFormat(TransferStatus)
}