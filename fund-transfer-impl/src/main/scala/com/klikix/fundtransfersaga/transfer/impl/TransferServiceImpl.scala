package com.klikix.fundtransfersaga.transfer.impl

import java.util.UUID

import akka.persistence.query.Offset
import com.datastax.driver.core.utils.UUIDs
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.transport.{Forbidden, NotFound}
import com.lightbend.lagom.scaladsl.broker.TopicProducer
import com.lightbend.lagom.scaladsl.persistence.{EventStreamElement, PersistentEntityRegistry}
import com.lightbend.lagom.scaladsl.server.ServerServiceCall

import scala.concurrent.{ExecutionContext, Future}
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import akka.Done
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity._
import com.lightbend.lagom.scaladsl.api.transport._
import com.typesafe.scalalogging.LazyLogging
import com.klikix.fundtransfersaga.transfer.api.{TransferEvent => TopicApiTransferEvent}
import com.klikix.fundtransfersaga.transfer.impl.Mappers._
import com.klikix.fundtransfersaga.transfer.entity.transfer._
import com.klikix.fundtransfersaga.transfer.api.TransferService
import com.klikix.fundtransfersaga.transfer.entity.transfer.view.TransferRepository

class TransferServiceImpl(registry: PersistentEntityRegistry, transferRepository: TransferRepository)(implicit ec: ExecutionContext) extends TransferService with LazyLogging {
  
  def transfer = { request =>
    logger.info(s"transfer: $request")
    val data = fromApi(request.data)
    entityRef(data.sourceAccountUid)
    .ask(StartTransfer.apply(data))
    .transform(transfer => toApi(transfer.get),
               transformFailed _)
    
  }
  
  def getTransfer(transactionUid: UUID) = { request =>
    logger.info(s"getTransfer: $transactionUid")
    transferRepository.getTransfer(transactionUid)
    .map{
      case Some(transfer) => toApi(transfer)
      case None => throw NotFound(s"Transfer for $transactionUid not found")
    }
  }
  
  override def transferEvents = TopicProducer.taggedStreamWithOffset(TransferEvent.Tag.allTags.toList) { (tag, offset) =>
    registry.eventStream(tag, offset).filter{_.event.isPublic }.mapAsync(1)(convertEvent)
  }
  
  private def convertEvent(eventStreamElement: EventStreamElement[TransferEvent]): Future[(TopicApiTransferEvent, Offset)] = {
    logger.debug(s"convertEvent: {}",eventStreamElement.event.getClass)
    eventStreamElement match {
      case EventStreamElement(_, event: SourceRemoveStarted, offset) => Future(toApi(event.asInstanceOf[SourceRemoveStarted]),offset)
      case EventStreamElement(_, event: SourceRemoveFailed, offset) => Future(toApi(event.asInstanceOf[SourceRemoveFailed]),offset)
      case EventStreamElement(_, event: DestinationAddStarted, offset) => Future(toApi(event.asInstanceOf[DestinationAddStarted]),offset)
      case EventStreamElement(_, event: TransferSuccessful, offset) => Future(toApi(event.asInstanceOf[TransferSuccessful]),offset)
      case EventStreamElement(_, event: SourceRemoveRollbackStarted, offset) => Future(toApi(event.asInstanceOf[SourceRemoveRollbackStarted]),offset)
      case EventStreamElement(_, event: SourceRemoveRollbackFailed, offset) => Future(toApi(event.asInstanceOf[SourceRemoveRollbackFailed]),offset)
      case EventStreamElement(_, event: SourceRemoveRollbacked, offset) => Future(toApi(event.asInstanceOf[SourceRemoveRollbacked]),offset)
    }
  }
 
  private def transformFailed(ex: Throwable): Throwable = {
    logger.error("Error: $ex")
    ex match {
      case e @ (_: InvalidCommandException | _:UnhandledCommandException)  => BadRequest(e.getMessage)
      case _ => ex
    }
  }

  private def entityRef(sourceAccountUid: UUID) = entityRefString(sourceAccountUid.toString)

  private def entityRefString(sourceAccountUid: String) = registry.refFor[TransferEntity](sourceAccountUid)

}
