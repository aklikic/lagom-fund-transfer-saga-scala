package com.klikix.fundtransfersaga.account.impl

import com.klikix.fundtransfersaga.account.api.{AccountEvent => ApiAccountEvent}
import com.klikix.fundtransfersaga.account.entity.account._
import java.util.UUID
import com.klikix.fundtransfersaga.account.impl.Mappers._
import com.lightbend.lagom.scaladsl.pubsub.PubSubRef
import com.klikix.fundtransfersaga.account.entity.account.AccountClosed
import com.klikix.fundtransfersaga.account.entity.account.AccountCreated
import com.klikix.fundtransfersaga.account.entity.account.AccountEvent
import com.klikix.fundtransfersaga.account.entity.account.FundsAddRollbacked
import com.klikix.fundtransfersaga.account.entity.account.FundsAdded
import com.klikix.fundtransfersaga.account.entity.account.FundsRemoveRollbacked
import com.klikix.fundtransfersaga.account.entity.account.FundsRemoved
import com.lightbend.lagom.scaladsl.pubsub.TopicId
import com.typesafe.scalalogging.LazyLogging

object PubSub extends LazyLogging{
  def topicAll: TopicId[ApiAccountEvent] = {
    TopicId[ApiAccountEvent]("all")
  }
  def publish (accountUid: UUID, evt: AccountEvent, publishedTopic: PubSubRef[ApiAccountEvent]):Unit = {
    val apiEvent: ApiAccountEvent = 
    evt match {
      case event: AccountCreated => toApi(accountUid, event.asInstanceOf[AccountCreated])
      case event: AccountClosed => toApi(accountUid, event.asInstanceOf[AccountClosed])
      case event: FundsAdded => toApi(accountUid, event.asInstanceOf[FundsAdded])
      case event: FundsAddRollbacked => toApi(accountUid, event.asInstanceOf[FundsAddRollbacked])
      case event: FundsRemoved => toApi(accountUid, event.asInstanceOf[FundsRemoved])
      case event: FundsRemoveRollbacked => toApi(accountUid, event.asInstanceOf[FundsRemoveRollbacked])
    }
    logger.debug("pubSub event: {}",apiEvent.getClass)
    publishedTopic.publish(apiEvent)
  }
}