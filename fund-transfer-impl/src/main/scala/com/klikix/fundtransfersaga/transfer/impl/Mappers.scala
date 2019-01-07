package com.klikix.fundtransfersaga.transfer.impl
import java.util.UUID
import com.klikix.fundtransfersaga.transfer.entity.transfer._
import com.klikix.fundtransfersaga.transfer.api.{TransferData => ApiTransferData,
                                                 Transfer => ApiTransfer,
                                                 TransferStatus => ApiTransferStatus,
                                                 SourceRemoveStarted => TopicApiSourceRemoveStarted,
                                                 SourceRemoveFailed => TopicApiSourceRemoveFailed,
                                                 DestinationAddStarted => TopicApiDestinationAddStarted,
                                                 TransferSuccessful => TopicApiTransferSuccessful,
                                                 SourceRemoveRollbackStarted => TopicApiSourceRemoveRollbackStarted,
                                                 SourceRemoveRollbackFailed => TopicApiSourceRemoveRollbackFailed,
                                                 SourceRemoveRollbacked => TopicApiSourceRemoveRollbacked
                                                 }

object Mappers{
  
  def fromApi(data: ApiTransferData):TransferData = {
    TransferData.apply(data.transactionUid, data.sourceAccountUid, data.destinationAccountUid, data.amount)
  }
  def toApi(data: TransferData):ApiTransferData = {
    ApiTransferData.apply(data.transactionUid, data.sourceAccountUid, data.destinationAccountUid, data.amount)
  }
  def toApi(transfer: Transfer):ApiTransfer = {
    ApiTransfer.apply(toApi(transfer.data), transfer.rollbackReason, transfer.failReason, toApi(transfer.status))
  }
  
  def toApi(status: TransferStatus.Status): ApiTransferStatus.Status = {
    status match {
      case TransferStatus.SourceRemoveStarted => ApiTransferStatus.SourceRemoveStarted 
      case TransferStatus.SourceRemoveFailed => ApiTransferStatus.SourceRemoveFailed 
      case TransferStatus.DestinationAddStarted => ApiTransferStatus.DestinationAddStarted 
      case TransferStatus.SourceRemoveRollbackStarted => ApiTransferStatus.SourceRemoveRollbackStarted 
      case TransferStatus.SourceRemoveRollbackFailed => ApiTransferStatus.SourceRemoveRollbackFailed 
      case TransferStatus.SourceRemoveRollbacked => ApiTransferStatus.SourceRemoveRollbacked 
      case TransferStatus.TransferSuccessful => ApiTransferStatus.TransferSuccessful 
    }
  }
  
  def toApi(event: SourceRemoveStarted): TopicApiSourceRemoveStarted = {
    TopicApiSourceRemoveStarted.apply(toApi(event.data), event.timestamp)
  }
  def toApi(event: SourceRemoveFailed): TopicApiSourceRemoveFailed = {
    TopicApiSourceRemoveFailed.apply(toApi(event.data),event.reason, event.timestamp)
  }
  def toApi(event: DestinationAddStarted): TopicApiDestinationAddStarted = {
    TopicApiDestinationAddStarted.apply(toApi(event.data), event.timestamp)
  }
  def toApi(event: TransferSuccessful): TopicApiTransferSuccessful = {
    TopicApiTransferSuccessful.apply(toApi(event.data), event.timestamp)
  }
  def toApi(event: SourceRemoveRollbackStarted): TopicApiSourceRemoveRollbackStarted = {
    TopicApiSourceRemoveRollbackStarted.apply(toApi(event.data),event.rollbackReason, event.timestamp)
  }
  def toApi(event: SourceRemoveRollbackFailed): TopicApiSourceRemoveRollbackFailed = {
    TopicApiSourceRemoveRollbackFailed.apply(toApi(event.data),event.rollbackReason,event.reason, event.timestamp)
  }
  def toApi(event: SourceRemoveRollbacked): TopicApiSourceRemoveRollbacked = {
    TopicApiSourceRemoveRollbacked.apply(toApi(event.data), event.rollbackReason,event.timestamp)
  }
}