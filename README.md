# Authorizer
The Authorizer application authorizes a transaction for a specific account following a set of predefined rules.

## The application:
Appication is written in Scala and based on Akka actors.

### Building executable 
Use *sbt* build tool https://www.scala-sbt.org/
run the command: sbt clean assembly or sbt assembly 

### running the Application 
 java -jar $PATH_TO_JAR/authorizer-assembly-1.0.jar

### Project structure 
Domain Driven Design is the main design approach hence using a ubiquitous language trough the spec document given by Nubank and 
naming the application entities 

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
This package contains the json converntions protocol 

#### utils 
package that contains helpers for handling the configuration and datetime convensions gracefully 

The appliction accepts the input and emits the output as json 


Akka actors are low resource and enables to achive async and scalable operations easly.
In case the code will need to support many accounts I would use persistent actors and event sourcing approach. 
I didn't use it in this case because the application should use only the memory as a storage.
