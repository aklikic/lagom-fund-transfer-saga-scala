package com.klikix.fundtransfersaga.transfer.impl

import com.klikix.fundtransfersaga.account.api.AccountService
import com.typesafe.scalalogging.LazyLogging
import com.lightbend.lagom.scaladsl.server.ServerServiceCall
import java.util.UUID
import akka.Done
import scala.concurrent.Future
import scala.concurrent.ExecutionContext

class AccountServiceStub(implicit ec: ExecutionContext) extends AccountService with LazyLogging{
    override def createAccount = ServerServiceCall { request =>
      logger.info(s"createAccount: $request")
      throw new NotImplementedError
    }
    
    override def closeAccount(accountUid: UUID) = { _ =>
      logger.info(s"closeAccount $accountUid")
      throw new NotImplementedError
    }
    
    override def getAccount(accountUid: UUID) = {_ =>
      logger.info(s"getAccount $accountUid")
      throw new NotImplementedError
    }
    
    override def addFunds(accountUid: UUID) = { request =>
      logger.info(s"addFunds $accountUid :$request")
      Future(Done)
    }
    
    override def addFundsRollback(accountUid: UUID) = { request =>
      logger.info(s"addFundsRollback $accountUid")
      Future(Done)
    }
    
    override def removeFunds(accountUid: UUID) = { request =>
      logger.info(s"removeFunds $accountUid")
      Future(Done)
    }
    
    override def removeFundsRollback(accountUid: UUID) = { request =>
      logger.info(s"removeFundsRollback $accountUid")
      Future(Done)
    }
    
    def accountEvents = {
      null
    }
  
}