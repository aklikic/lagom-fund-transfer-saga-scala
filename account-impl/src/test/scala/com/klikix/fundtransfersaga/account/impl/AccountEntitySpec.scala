package com.klikix.fundtransfersaga.account.impl

import java.time.Duration
import java.util.UUID

import akka.actor.ActorSystem
import akka.serialization.SerializationExtension
import akka.testkit.TestKit


import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.InvalidCommandException
import com.lightbend.lagom.scaladsl.playjson.JsonSerializerRegistry
import com.lightbend.lagom.scaladsl.testkit.PersistentEntityTestDriver
import org.scalatest.{BeforeAndAfterAll, Matchers, OptionValues, WordSpec}
import com.klikix.fundtransfersaga.account.impl.AccountSerializerRegistry
import com.klikix.fundtransfersaga.account.entity.account._
import java.time.OffsetDateTime
import com.lightbend.lagom.scaladsl.pubsub.PubSubRegistry
import com.lightbend.lagom.internal.scaladsl.PubSubRegistryImpl

class AccountEntitySpec extends WordSpec with Matchers with BeforeAndAfterAll with OptionValues {
  private val system = ActorSystem("test", JsonSerializerRegistry.actorSystemSetupFor(AccountSerializerRegistry))
  
  override def afterAll = {
    TestKit.shutdownActorSystem(system)
  }

  private val accountUid = UUID.randomUUID
  private val ownerUid = UUID.randomUUID
  private val amount = 1000
  private val account = Account.apply(ownerUid, amount, AccountStatus.Created)

  private def withDriver[T](block: PersistentEntityTestDriver[AccountCommand, AccountEvent, Option[Account]] => T): T = {
    val driver = new PersistentEntityTestDriver(system, new AccountEntity(null), accountUid.toString)
    try {
      block(driver)
    } finally {
      driver.getAllIssues shouldBe empty
    }
  }

  "The account entity" should {

    "allow creating an account" in withDriver { driver =>
      val outcome = driver.run(CreateAccount.apply(account))
      outcome.events.size shouldBe 1
      outcome.events.head shouldBe an[AccountCreated]
      outcome.state.get.status shouldBe AccountStatus.Created
    }
    
    "allow closing an account" in withDriver { driver =>
      val outcome = driver.run(CreateAccount.apply(account),CloseAccount)
      outcome.events.size shouldBe 2
      outcome.events.head shouldBe an[AccountCreated]
      outcome.events.drop(1).head shouldBe an[AccountClosed]
      outcome.state.get.status shouldBe AccountStatus.Closed
    }

   //TODO other test cases
  }
}
