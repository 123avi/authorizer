package avi.domain

import avi.domain.violations.Violation

object Operations {
  sealed trait Operation
  //{"account": {"active-card": true, "available-limit": 100}}
  final case class Account(activeCard: Boolean, availableLimit: Double)
  final case class InitOperation(account: Account) extends Operation

  //{"transaction": {"merchant": "Burger King", "amount": 20, "time":"2019-02-13T10:00:00.000Z"}}
  final case class Transaction(merchant: String, amount: Double, time: String)
  final case class TransactionOperation(transaction: Transaction) extends Operation

  sealed trait OperationResponse{ def violations: Set[Violation] }
  final case class InitOperationResponse(account: Account, violations: Set[Violation]) extends OperationResponse
  final case class TransactionResponse(transaction: Transaction, violations: Set[Violation]) extends OperationResponse
}
