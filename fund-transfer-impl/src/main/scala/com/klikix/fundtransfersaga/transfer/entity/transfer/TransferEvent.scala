package com.klikix.fundtransfersaga.transfer.entity.transfer

import java.time.OffsetDateTime
import java.util.UUID

import com.lightbend.lagom.scaladsl.persistence.AggregateEvent
import com.lightbend.lagom.scaladsl.persistence.AggregateEventTag

import play.api.libs.json.Format
import play.api.libs.json.Json

sealed trait TransferEvent extends AggregateEvent[TransferEvent] {
  override def aggregateTag = TransferEvent.Tag
  var isPublic = true //override per event is not public
}

object TransferEvent {
  val NumShards = 3
  val Tag = AggregateEventTag.sharded[TransferEvent](NumShards)
}

case class SourceRemoveStarted(data: TransferData,
                               timestamp: OffsetDateTime 
                              ) extends TransferEvent

object SourceRemoveStarted {
  implicit val format: Format[SourceRemoveStarted] = Json.format
}

case class SourceRemoveFailed(data: TransferData,
                              reason: String,
                              timestamp: OffsetDateTime 
                              ) extends TransferEvent

object SourceRemoveFailed {
  implicit val format: Format[SourceRemoveFailed] = Json.format
}

case class DestinationAddStarted(data: TransferData,
                                 timestamp: OffsetDateTime 
                                 ) extends TransferEvent

object DestinationAddStarted {
  implicit val format: Format[DestinationAddStarted] = Json.format
}

case class SourceRemoveRollbackStarted(data: TransferData,
                                       rollbackReason: String,
                                       timestamp: OffsetDateTime 
                                       ) extends TransferEvent

object SourceRemoveRollbackStarted {
  implicit val format: Format[SourceRemoveRollbackStarted] = Json.format
}

case class SourceRemoveRollbackFailed(data: TransferData,
                                      rollbackReason: String,
                                      reason: String,
                                      timestamp: OffsetDateTime 
                                      ) extends TransferEvent

object SourceRemoveRollbackFailed {
  implicit val format: Format[SourceRemoveRollbackFailed] = Json.format
}

case class SourceRemoveRollbacked(data: TransferData,
                                  rollbackReason: String,
                                  timestamp: OffsetDateTime 
                                  ) extends TransferEvent

object SourceRemoveRollbacked {
  implicit val format: Format[SourceRemoveRollbacked] = Json.format
}

case class TransferSuccessful(data: TransferData,
                             timestamp: OffsetDateTime 
                            ) extends TransferEvent

object TransferSuccessful {
  implicit val format: Format[TransferSuccessful] = Json.format
}


