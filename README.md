# Authorizer App
The Authorizer application authorizes a transaction for a specific account following a set of predefined rules.

The application aggregates all transactions within 2 minutes interval, once the aggregation interval completes, the transactions
will be executed according to the time defined in transaction.

## The application:
Appication is written in [Scala](https://docs.scala-lang.org/) and based on [Akka actors](https://doc.akka.io/docs/akka/current/actors.html) .

### running the Application 
 java -jar $PATH_TO_JAR/authorizer-assembly-1.0.jar

For graceful stop use the command **stop** as input or simply *CTL+C*

### Building executable
Use *sbt* build tool <https://www.scala-sbt.org/>

run the command: *sbt clean assembly* or *sbt assembly*


### Project structure 
Domain Driven Design is the main design approach hence using a ubiquitous language trough the spec document given by Nubank and 
naming the application entities

#### Tests
All rules defined in the spec are covered in *AthorizerSpec.scala*

### domain entities:
 
#### Authorizer 
 Aggregate the Account life states, and simulates state machine of the account life cycle  and executes the operations according to the logic

**Authorizer states** :
* *waitForInit* - the initial state, waits for Init account command , once the account is initilize with active-card true it fires 
timer accourding to transaction interval (2 minutes,can be configured) and change the state to run or to inactiveAccount 
(depends on the active-card argument)
* *run* - accepts transactions and checks validation of the operation
* *inactiveAccount* - in case the active-card:false, this state will respond with violation "card-not-active" in case of transaction

#### violations
contains the value objects that represents the different violations
#### Operations
contains the value objects that represents the different operations and responses

### protocol
This package contains the json conventions protocol
The application accepts the input and emits the output as json

#### utils 
package that contains helpers for handling the configuration and datetime conventions gracefully

#### Publisher - simple actor that simply receives a message and emits it to the stdout as a json

## Remarks
* Akka actors are low resource and enables to achieve async and scalable operations easily.
* Each actor is minimized to maintain single responsibility althogh it might be a little overhead or over engineering I do believe that it is correct in a real life apps.   
* In order to keep it simple I didn't use persisted actor or other persistence storage and maintained a single account with in memory state as requested.
