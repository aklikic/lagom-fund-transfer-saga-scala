package com.klikix.fundtransfersaga.account.impl

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
import com.klikix.fundtransfersaga.account.api.CreateAccountRequest
import com.klikix.fundtransfersaga.account.api.AccountStatus
import com.klikix.fundtransfersaga.account.api.AccountEvent
import com.klikix.fundtransfersaga.account.api.AccountCreated
import com.klikix.fundtransfersaga.account.api.AccountClosed
import akka.stream.scaladsl.Source
import com.klikix.fundtransfersaga.account.api.AccountStreamAlive
import java.util.concurrent.TimeUnit
import akka.NotUsed
import akka.stream.testkit.scaladsl.TestSink

class AccountServiceImplIntegrationTest extends AsyncWordSpec with Matchers with BeforeAndAfterAll {

  private val server = ServiceTest.startServer(ServiceTest.defaultSetup.withCassandra(true)) { ctx =>
    new LagomApplication(ctx) with AccountComponents with LocalServiceLocator with AhcWSComponents with TestTopicComponents {
      override def additionalConfiguration: AdditionalConfiguration =
        super.additionalConfiguration ++ ConfigFactory.parseString(
          "cassandra-query-journal.eventual-consistency-delay = 0"
        )
    }
  }

  val accountService = server.serviceClient.implement[AccountService]

  import server.materializer

  override def afterAll = server.stop()

  "The Account service" should {

    "allow creating accounts" in {
      val ownerUid = UUID.randomUUID
      val accountUid = UUID .randomUUID
      val initialAmount = 0
      for {
        created <- createAccount(ownerUid, initialAmount)
        retrieved <- getAccount(created.accountUid)
        events: Seq[AccountEvent] <- accountService.accountEvents.subscribe.atMostOnceSource
        .filter(_.accountUid == created.accountUid)
        .take(1)
        .runWith(Sink.seq)
      } yield {
        created.accountUid should ===(retrieved.accountUid)
        events.size shouldBe 1
        events.head shouldBe an[AccountCreated]
      }
      
    }
    
    "allow closing accounts" in {
      val ownerUid = UUID.randomUUID
      val accountUid = UUID .randomUUID
      val initialAmount = 0
      for {
        created <- createAccount(ownerUid, initialAmount) 
        closed <- closeAccount(created.accountUid)
        retrieved <- getAccount(created.accountUid)
        events: Seq[AccountEvent] <- accountService.accountEvents.subscribe.atMostOnceSource
        .filter(_.accountUid == created.accountUid)
        .take(2)
        .runWith(Sink.seq)
      } yield {
        retrieved.status should ===(AccountStatus.Closed)
        events.size shouldBe 2
        events.head shouldBe an[AccountCreated]
        events.drop(1).head shouldBe an[AccountClosed]
      }
    }
    
    "receive account events over stream" in {
      val userUid = UUID.randomUUID
      val ownerUid = UUID.randomUUID
      val accountUid = UUID .randomUUID
      val initialAmount = 0
      val input : Source[AccountStreamAlive,NotUsed] = Source.single(AccountStreamAlive(true)).concat(Source.maybe)
      accountService.accountStream(userUid).invoke(input).flatMap{ output =>
        val probe = output.runWith(TestSink.probe(server.actorSystem))
        for {
          created <- createAccount(ownerUid, initialAmount)
          closed <- closeAccount(created.accountUid)
          retrieved <- getAccount(created.accountUid)
          events: Seq[AccountEvent] <- accountService.accountEvents.subscribe.atMostOnceSource
          .filter(_.accountUid == created.accountUid)
          .take(2)
          .runWith(Sink.seq)
          
        } yield {
          retrieved.status should ===(AccountStatus.Closed)
          events.size shouldBe 2
          events.head shouldBe an[AccountCreated]
          events.drop(1).head shouldBe an[AccountClosed]
          probe.request(2)
          probe.expectNext() shouldBe an[AccountCreated]
          probe.expectNext() shouldBe an[AccountClosed]
          probe.cancel
          succeed
        }
      }  
      
    }

    //TODO other test cases
  }
  
  

  private def createAccount(ownerUid: UUID, initialAmount: Int) = {
    accountService.createAccount.invoke(CreateAccountRequest.apply(None, ownerUid, Some(initialAmount)))
  }
  
  private def closeAccount(accountUid: UUID) = {
    accountService.closeAccount(accountUid).invoke
  }

  private def getAccount(accountUid: UUID) = {
    accountService.getAccount(accountUid).invoke
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
