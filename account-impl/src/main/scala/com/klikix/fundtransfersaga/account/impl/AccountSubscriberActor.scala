package com.klikix.fundtransfersaga.transfer.impl

import akka.actor._
import akka.event.Logging
import java.util.UUID
import akka.stream.scaladsl.Flow
import scala.concurrent.Future
import akka.Done
import akka.util.Timeout
import java.util.concurrent.TimeUnit
import java.time.Duration
import akka.stream.scaladsl._
import com.klikix.fundtransfersaga.account.api._

import com.lightbend.lagom.scaladsl.pubsub._
import akka.stream.QueueOfferResult
import scala.concurrent.ExecutionContext
import akka.pattern.pipe
import akka.actor.ActorRef
import scala.concurrent.duration._
import akka.cluster.pubsub._
import com.klikix.fundtransfersaga.account.impl.PubSub._

object AccountSubscriberActor{
  def props(pubSub: PubSubRegistry, downstream: SourceQueueWithComplete[AccountEvent], connectionUid: UUID, userUid: UUID): Props = Props(new AccountSubscriberActor(pubSub,downstream,connectionUid,userUid))
  case object Disconnect
  case class SendStatus(replyTo: ActorRef,error: Option[String])
  object SendStatus
  case object ConnectionCheck
}


class AccountSubscriberActor(pubSub: PubSubRegistry, downstream: SourceQueueWithComplete[AccountEvent], connectionUid: UUID, userUid: UUID) extends Actor with ActorLogging{
  val logPrefix = s"[$userUid-$connectionUid] "
  import AccountSubscriberActor._
  implicit val ec: ExecutionContext = context.dispatcher
  //implicit val timeout: Timeout =  Timeout.apply(1, TimeUnit.SECONDS)
  //pubSub.refFor(topicAll).subscriber.ask[Done](self).to(Sink.ignore)
  import DistributedPubSubMediator.{ Subscribe, SubscribeAck, Unsubscribe }
  val mediator = DistributedPubSub(context.system).mediator
  mediator ! Subscribe(topicAll.name, self)
  
  val connectionCheckTicket = context.system.scheduler.schedule(Duration.ofSeconds(10), Duration.ofSeconds(10), self, ConnectionCheck, context.dispatcher, self)
  val connectionUnavailableTreshold = FiniteDuration.apply(30, TimeUnit.SECONDS)
  var connectionUnavailableDeadline : Deadline = connectionUnavailableTreshold.fromNow
  
  override def preStart(){
    log.debug("{}Connected",logPrefix)
    
  }
  override def postStop(){
    log.debug("{}Disconnected",logPrefix)
    connectionCheckTicket.cancel
    mediator ! Unsubscribe(topicAll.name, self)
  }
  
  def receive = {
    case AccountStreamAlive(_) => log.debug("{}Alive received",logPrefix)
                                  connectionUnavailableDeadline = connectionUnavailableTreshold.fromNow
    case ConnectionCheck => if(connectionUnavailableDeadline.isOverdue){
                              log.error("{}Connection not valid. Disconnecting!",logPrefix)
                              disconnect
                            }
    case event : AccountEvent => log.debug("{}Event received:{}",logPrefix,event.getClass)
                                 downstream.offer(event)
                                           .map {
                                              case QueueOfferResult.Enqueued => SendStatus(sender,None)
                                              case QueueOfferResult.Failure(ex) => SendStatus(sender,Some(s"$ex.getMessage"))
                                              case _ => SendStatus(sender, Some("Error"))
                                            } pipeTo self
    case SendStatus(replyTo, None) => log.debug("{}Message sent",logPrefix)
                                      replyTo ! Done
    case SendStatus(replyTo, Some(error)) => log.error("{}Error: {}",logPrefix,error)
                                             disconnect    
    case _ : SubscribeAck =>  log.debug("{}Subscribed to pubsub",logPrefix)
    case Disconnect => log.debug("{}Client disconnected!",logPrefix)
                       disconnect
    case unknown => log.error("{}Unknown message {}",logPrefix ,unknown.getClass)
  }
  
  
  private def disconnect() = {
    context.stop(self)
  }
}

