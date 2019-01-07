package com.klikix.fundtransfersaga.account.impl

import com.lightbend.lagom.scaladsl.playjson.{JsonSerializerRegistry, JsonSerializer}
import com.klikix.fundtransfersaga.account.entity.account._

object AccountSerializerRegistry extends JsonSerializerRegistry {
  override def serializers = List(
    JsonSerializer[Account],

    JsonSerializer[CreateAccount],
    JsonSerializer[CloseAccount.type],
    JsonSerializer[GetAccount.type],
    JsonSerializer[AddFunds],
    JsonSerializer[AddFundsRollback],
    JsonSerializer[RemoveFunds],
    JsonSerializer[RemoveFundsRollback],

    JsonSerializer[AccountCreated],
    JsonSerializer[AccountClosed],
    JsonSerializer[FundsAdded],
    JsonSerializer[FundsAddRollbacked],
    //JsonSerializer[FundsAddRollbackFailed],
    JsonSerializer[FundsRemoved],
    //JsonSerializer[FundsRemoveFailed],
    JsonSerializer[FundsRemoveRollbacked]//,
    //JsonSerializer[FundsRemoveRollbackFailed]
  )
}
