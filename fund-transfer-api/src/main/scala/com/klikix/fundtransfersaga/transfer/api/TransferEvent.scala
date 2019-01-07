package com.klikix.fundtransfersaga.transfer.api

import java.time.Instant
import java.util.UUID

import julienrf.json.derived
import play.api.libs.json._
import java.time.OffsetDateTime

sealed trait TransferEvent {
  val sourceAccountUid: UUID
}

object TransferEvent {
  implicit val format: Format[TransferEvent] =
    derived.flat.oformat((__ \ "type").format[String])
}

case class SourceRemoveStarted(data: TransferData,
                               timestamp: OffsetDateTime
                               ) extends TransferEvent {val sourceAccountUid=data.sourceAccountUid}

object SourceRemoveStarted {
  implicit val format: Format[SourceRemoveStarted] = Json.format
}

case class SourceRemoveFailed(data: TransferData,
                              reason: String,
                              timestamp: OffsetDateTime
                              ) extends TransferEvent {val sourceAccountUid=data.sourceAccountUid}

object SourceRemoveFailed {
  implicit val format: Format[SourceRemoveFailed] = Json.format
}

case class DestinationAddStarted(data: TransferData,
                                 timestamp: OffsetDateTime
                                 ) extends TransferEvent {val sourceAccountUid=data.sourceAccountUid}

object DestinationAddStarted {
  implicit val format: Format[DestinationAddStarted] = Json.format
}

case class SourceRemoveRollbackStarted(data: TransferData,
                                       rollbackReason: String,
                                       timestamp: OffsetDateTime
                                       ) extends TransferEvent {val sourceAccountUid=data.sourceAccountUid}

object SourceRemoveRollbackStarted {
  implicit val format: Format[SourceRemoveRollbackStarted] = Json.format
}

case class SourceRemoveRollbackFailed(data: TransferData,
                                      rollbackReason: String,
                                      reason: String,
                                      timestamp: OffsetDateTime
                                      ) extends TransferEvent {val sourceAccountUid=data.sourceAccountUid}

object SourceRemoveRollbackFailed {
  implicit val format: Format[SourceRemoveRollbackFailed] = Json.format
}

case class SourceRemoveRollbacked(data: TransferData,
                                  rollbackReason: String,
                                  timestamp: OffsetDateTime
                                  ) extends TransferEvent {val sourceAccountUid=data.sourceAccountUid}

object SourceRemoveRollbacked {
  implicit val format: Format[SourceRemoveRollbacked] = Json.format
}

case class TransferSuccessful(data: TransferData,
                             timestamp: OffsetDateTime
                             ) extends TransferEvent {val sourceAccountUid=data.sourceAccountUid}

object TransferSuccessful {
  implicit val format: Format[TransferSuccessful] = Json.format
}