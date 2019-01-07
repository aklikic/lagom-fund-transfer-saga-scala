package com.klikix.fundtransfersaga.transfer.entity.transfer.view

import java.util.UUID

import akka.Done
import akka.stream.Materializer
import com.datastax.driver.core._
import com.lightbend.lagom.scaladsl.persistence.ReadSideProcessor
import com.lightbend.lagom.scaladsl.persistence.cassandra.{CassandraReadSide, CassandraSession}
import akka.persistence.cassandra.ListenableFutureConverter

import collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future, Promise}
import com.klikix.fundtransfersaga.transfer.entity.transfer._
import com.lightbend.lagom.scaladsl.persistence.ReadSideProcessor
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraReadSide
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import com.klikix.fundtransfersaga.transfer.entity.transfer.TransferEvent
import com.datastax.driver.core.PreparedStatement
import com.lightbend.lagom.scaladsl.persistence.ReadSideProcessor
import scala.collection.immutable.List
import play.api.libs.json.Json
import com.klikix.util.general.JsonFormats._
import play.api.libs.json.Json._
import com.typesafe.scalalogging.LazyLogging


class TransferRepository(session: CassandraSession)(implicit ec: ExecutionContext, mat: Materializer) {

  def getTransfer(transactionUid: UUID):Future[Option[Transfer]] = {
    session.selectOne(s"SELECT * FROM transfer WHERE transaction_uid=?", transactionUid)
    .map {_.map(transferFromRow _)}
  }
  
  private def transferFromRow(row: Row):Transfer = {
    val info = row.getString("info")
    val jsonValue = parse(info)
    fromJson[Transfer](jsonValue).asOpt.getOrElse(null)
  }
}

class TransferEventProcessor(session: CassandraSession, readSide: CassandraReadSide)(implicit ec: ExecutionContext)
    extends ReadSideProcessor[TransferEvent] with LazyLogging{

  private val insertSourceAccountUidByTransactionUidPromise = Promise[PreparedStatement]
  private def insertSourceAccountUidByTransactionUidCreator: Future[PreparedStatement] = insertSourceAccountUidByTransactionUidPromise.future

  def buildHandler = {
    readSide.builder[TransferEvent]("transferEventOffset")
        .setGlobalPrepare(() => createTables())
        .setPrepare(_ => prepareStatements())
        .setEventHandler[SourceRemoveStarted](e => insertTransfer(e.event.data, None, None, TransferStatus.SourceRemoveStarted))
        .setEventHandler[SourceRemoveFailed](e => insertTransfer(e.event.data, None, Some(e.event.reason), TransferStatus.SourceRemoveFailed))
        .setEventHandler[DestinationAddStarted](e => insertTransfer(e.event.data, None, None, TransferStatus.DestinationAddStarted))
        .setEventHandler[TransferSuccessful](e => insertTransfer(e.event.data, None, None, TransferStatus.TransferSuccessful))
        .setEventHandler[SourceRemoveRollbackStarted](e => insertTransfer(e.event.data, Some(e.event.rollbackReason), None, TransferStatus.SourceRemoveRollbackStarted))
        .setEventHandler[SourceRemoveRollbackFailed](e => insertTransfer(e.event.data, Some(e.event.rollbackReason), Some(e.event.reason), TransferStatus.SourceRemoveRollbackFailed))
        .setEventHandler[SourceRemoveRollbacked](e => insertTransfer(e.event.data, Some(e.event.rollbackReason), None, TransferStatus.SourceRemoveRollbacked))
        .build
  }

  def aggregateTags = TransferEvent.Tag.allTags

  private def createTables() = {
    for {
      _ <- session.executeCreateTable(s"""
        CREATE TABLE IF NOT EXISTS transfer (
          transaction_uid UUID PRIMARY KEY,
          info TEXT
        )
      """)
    } yield Done
  }

  private def prepareStatements() = {

    val insertSourceAccountUidByTransactionUidCreatorFuture = session.prepare(s"""
        INSERT INTO transfer(
          transaction_uid,
          info
        ) VALUES (?, ?)
      """)
    insertSourceAccountUidByTransactionUidPromise.completeWith(insertSourceAccountUidByTransactionUidCreatorFuture)

    
    for {
      _ <- insertSourceAccountUidByTransactionUidCreatorFuture
    } yield Done
  }
  
  private def insertTransfer(data: TransferData,rollbackReason:Option[String], failReason: Option[String], status: TransferStatus.Value) = {
    val transfer=Transfer.apply(data, rollbackReason, failReason, status)
    for {
      creator <- insertSourceAccountUidByTransactionUidCreator.map{ _.bind(transfer.data.transactionUid,toJson(transfer).toString)}  
    } yield List(creator)
    
  }
}