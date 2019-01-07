package com.klikix.fundtransfersaga.transfer.impl

import com.lightbend.lagom.scaladsl.playjson.{JsonSerializerRegistry, JsonSerializer}
import com.klikix.fundtransfersaga.transfer.entity.transfer._


object TransferSerializerRegistry extends JsonSerializerRegistry {
  override def serializers = List(
    JsonSerializer[Transfer],
    JsonSerializer[TransferData],

    JsonSerializer[StartTransfer],
    JsonSerializer[SetSourceRemoved.type],
    JsonSerializer[SetSourceRemoveFailed],
    JsonSerializer[SetDestinationAddFailed],
    JsonSerializer[SetSourceRemoveRollbacked.type],
    JsonSerializer[SetSourceRemoveRollbackFailed],
    JsonSerializer[SetDestinationAdded.type],

    JsonSerializer[SourceRemoveStarted],
    JsonSerializer[SourceRemoveFailed],
    JsonSerializer[DestinationAddStarted],
    JsonSerializer[SourceRemoveRollbackStarted],
    JsonSerializer[SourceRemoveRollbacked],
    JsonSerializer[SourceRemoveRollbackFailed],
    JsonSerializer[TransferSuccessful]
  )
}
