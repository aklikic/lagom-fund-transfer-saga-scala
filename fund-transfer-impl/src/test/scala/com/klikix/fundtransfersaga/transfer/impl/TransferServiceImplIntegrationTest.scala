package com.klikix.fundtransfersaga.transfer.impl

import java.time.Duration
import java.util.UUID

import akka.stream.scaladsl.Sink
import com.lightbend.lagom.scaladsl.api.AdditionalConfiguration
import com.lightbend.lagom.scaladsl.server.{LagomApplication, LocalServiceLocator}
import com.lightbend.lagom.scaladsl.testkit.{ServiceTest, TestTopicComponents}
import com.typesafe.config.ConfigFactory
import org.scalatest.{AsyncWordSpec, BeforeAndAfterAll, Matchers}
import play.api.libs.ws.ahc.AhcWSComponents

import scala.collection.immutable.Seq
import scala.concurrent.duration._
import scala.concurrent.{Future, Promise}
import com.klikix.fundtransfersaga.account.api.AccountService
import com.klikix.fundtransfersaga.transfer.api._
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraPersistenceComponents
import com.lightbend.lagom.scaladsl.api._


class TransferServiceImplIntegrationTest extends AsyncWordSpec with Matchers with BeforeAndAfterAll {

  
  private val server = ServiceTest.startServer(ServiceTest.defaultSetup.withCassandra(true)) { ctx =>
    new LagomApplication(ctx) with TransferComponents with LocalServiceLocator with AhcWSComponents with TestTopicComponents {
      override def additionalConfiguration: AdditionalConfiguration =
        super.additionalConfiguration ++ ConfigFactory.parseString(
          "cassandra-query-journal.eventual-consistency-delay = 0"
        )
      override lazy val accountService = new AccountServiceStub
    }
  }

  val transferService = server.serviceClient.implement[TransferService]

  import server.materializer

  override def afterAll = server.stop()

  "The Transfer service" should {

    "allow sucessful transfer" in {
     
      val data=TransferData.apply(UUID.randomUUID, UUID.randomUUID, UUID.randomUUID, 1000)
      for {
        created <- startTransfer(data)
        events: Seq[TransferEvent] <- transferService.transferEvents.subscribe.atMostOnceSource
        .filter(_.sourceAccountUid == data.sourceAccountUid)
        .take(3)
        .runWith(Sink.seq)
      } yield {
        created.data.transactionUid should ===(data.transactionUid)
        events.size shouldBe 3
        events.head shouldBe an[SourceRemoveStarted]
        events.drop(1).head shouldBe an[DestinationAddStarted]
        events.drop(2).head shouldBe an[TransferSuccessful]
      }
      
    }
    


    //TODO other test cases
  }

  private def startTransfer(data: TransferData) = {
    transferService.transfer.invoke(StartTransferRequest.apply(data))
  }

  private def getTransfer(transactionUid: UUID) = {
    transferService.getTransfer(transactionUid).invoke
  }

  def awaitSuccess[T](maxDuration: FiniteDuration = 10.seconds, checkEvery: FiniteDuration = 100.milliseconds)(block: => Future[T]): Future[T] = {
    val checkUntil = System.currentTimeMillis() + maxDuration.toMillis

    def doCheck(): Future[T] = {
      block.recoverWith {
        case recheck if checkUntil > System.currentTimeMillis() =>
          val timeout = Promise[T]()
          server.application.actorSystem.scheduler.scheduleOnce(checkEvery) {
            timeout.completeWith(doCheck())
          }(server.executionContext)
          timeout.future
      }
    }

    doCheck()
  }
}
