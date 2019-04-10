# lagom-fund-transfer-saga-scala
Fund transfer saga is a show case project for implementing Saga pattern in Lagom scala.

## Running locally

1) Install & run Kafka, which requires Zookeeper

2) Install & run Cassandra

3) `sbt runAll`

4) To invoke services, use the tests as described below.

- Create an account

    ```
    sbt
    project fundTransferImpl
    test:run
    ```
    
    Select the number corresponding to `com.klikix.fundtransfersaga.transfer.impl.CreateAccount`
    
    The output will show the account created, something like this:
    
    ```
    {
      "accountUid" : "7c3e64a0-5bdb-11e9-a186-fd0aaca5d8ce",
      "ownerUid" : "67ff8c8a-d71d-43ee-bdcb-da96072e4632",
      "amount" : 1000,
      "status" : "Created"
    }
    ```
    
- Query an account

    ```
    sbt
    project fundTransferImpl
    test:run <account UUID>
    ```
    
    Select the number corresponding to `com.klikix.fundtransfersaga.transfer.impl.QueryAccount`
    
    The output will show the account balance, something like this:
    
    ```
    {
      "accountUid" : "7c3e64a0-5bdb-11e9-a186-fd0aaca5d8ce",
      "ownerUid" : "67ff8c8a-d71d-43ee-bdcb-da96072e4632",
      "amount" : 1000,
      "status" : "Created"
    }
    ```
    
- Transfer funds between accounts

    ```
    sbt
    project fundTransferImpl
    test:run <source account UUID> <destination account UUID> <amount>
    ```
    
    Select the number corresponding to `com.klikix.fundtransfersaga.transfer.impl.TransferFunds`
    
    The output will show something like this:
    
    ```
    {
      "data" : {
        "transactionUid" : "b77407dc-8b00-43d9-abaa-8ce414e70990",
        "sourceAccountUid" : "7c3e64a0-5bdb-11e9-a186-fd0aaca5d8ce",
        "destinationAccountUid" : "a0390e50-5bdb-11e9-a186-fd0aaca5d8ce",
        "amount" : 42
      },
      "status" : "SourceRemoveStarted"
    }
    ```
 
- Query the status of a fund transfer

    ```
    sbt
    project fundTransferImpl
    test:run <transaction UUID>
    ```
    
    Select the number corresponding to `com.klikix.fundtransfersaga.transfer.impl.GetTransfer`
    
    The output will show something like this:
    
    ```
    {
      "data" : {
        "transactionUid" : "b77407dc-8b00-43d9-abaa-8ce414e70990",
        "sourceAccountUid" : "7c3e64a0-5bdb-11e9-a186-fd0aaca5d8ce",
        "destinationAccountUid" : "a0390e50-5bdb-11e9-a186-fd0aaca5d8ce",
        "amount" : 42
      },
      "status" : "TransferSuccessful"
    }
    ```

