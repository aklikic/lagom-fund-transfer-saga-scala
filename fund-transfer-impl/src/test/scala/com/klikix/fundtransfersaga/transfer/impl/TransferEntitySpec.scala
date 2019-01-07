package com.klikix.fundtransfersaga.transfer.impl

import java.time.Duration
import java.util.UUID

import akka.actor.ActorSystem
import akka.serialization.SerializationExtension
import akka.testkit.TestKit


import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.InvalidCommandException
import com.lightbend.lagom.scaladsl.playjson.JsonSerializerRegistry
import com.lightbend.lagom.scaladsl.testkit.PersistentEntityTestDriver
import com.klikix.fundtransfersaga.transfer.entity.transfer._
import com.klikix.fundtransfersaga.transfer.impl.TransferSerializerRegistry

import java.time.OffsetDateTime
import org.scalatest._

class TransferEntitySpec extends WordSpec with Matchers with BeforeAndAfterAll with OptionValues {
  private val system = ActorSystem("test", JsonSerializerRegistry.actorSystemSetupFor(TransferSerializerRegistry))

  override def afterAll = {
    TestKit.shutdownActorSystem(system)
  }

  val transactionUid=UUID.randomUUID
  val sourceAccountUid=UUID.randomUUID
  val destinationAccountUid=UUID.randomUUID
  val amount=1000
  val data=TransferData.apply(transactionUid, sourceAccountUid, destinationAccountUid, amount);
  

  private def withDriver[T](block: PersistentEntityTestDriver[TransferCommand, TransferEvent, Option[Transfer]] => T): T = {
    val driver = new PersistentEntityTestDriver(system, new TransferEntity, sourceAccountUid.toString)
    try {
      block(driver)
    } finally {
      driver.getAllIssues shouldBe empty
    }
  }

  "The Transfer entity" should {
    
     "allow transaction finish" in withDriver { driver =>
      val outcome = driver.run(StartTransfer.apply(data),SetSourceRemoved,SetDestinationAdded)
      outcome.events.size shouldBe 3
      outcome.events.head shouldBe an[SourceRemoveStarted]
      outcome.events.drop(1).head shouldBe an[DestinationAddStarted]
      outcome.events.drop(2).head shouldBe an[TransferSuccessful]
      outcome.state.get.status shouldBe TransferStatus.TransferSuccessful
    }

   //TODO other test cases
  }
}
