package com.klikix.fundtransfersaga.transfer.api

import java.time.{Duration, Instant}
import java.util.UUID

import com.klikix.util.general.JsonFormats._
import com.lightbend.lagom.scaladsl.api.deser.PathParamSerializer
import play.api.libs.json.{Format, Json}

case class StartTransferRequest(
  data: TransferData
)

object StartTransferRequest {
  implicit val format: Format[StartTransferRequest] = Json.format
}

/*
 * Transfer metadata
 */

case class Transfer(
  data: TransferData,
  rollbackReason: Option[String],
  failReason: Option[String],
  status: TransferStatus.Value
)

object Transfer {
  implicit val format: Format[Transfer] = Json.format
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

  implicit val format: Format[Value] = enumFormat(this)
}

