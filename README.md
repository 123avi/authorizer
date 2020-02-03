# Authorizer
The Authorizer application authorizes a transaction for a specific account following a set of predefined rules.

The application aggregates all transaction in 2 minutes interval, once the aggregation interval completes the transactions
will be executed according to the time defined in transaction

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

#### Packages :
##### domain 
1. violations - a package contains  the value objects that represents the different violations
2. Authorizer - Aggregate represents the Account life cicle states) and executes the operations according to the logic
Authorizer states :
* *waitForInit* - the initial state, waits for Init account command , once the account is initilize with active-card true it fires 
timer accourding to transaction interval (2 minutes,can be configured) and change the state to run or to inactiveAccount 
(depends on the active-card argument)
* *run* - accepts transactions and checks validation of the operation
* *inactiveAccount* - incase the active-card:false, this state will response with violation "card-not-active" in case of transaction
##### protocol
This package contains the json conventions protocol

#### utils 
package that contains helpers for handling the configuration and datetime conventions gracefully

The application accepts the input and emits the output as json

#### Publisher - simple actor that simply receives a message and emits it to the stdout as a json

Akka actors are low resource and enables to achieve async and scalable operations easly.
In case the code will need to support many accounts I would use persistent actors and event sourcing approach. 
I didn't use it in this case because the application should use only the memory as a storage.
