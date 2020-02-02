package nb.domain.violations

sealed abstract class Violation(val asString: String)

object Violation {

  case object AccountAlreadyInitialized extends Violation("account-already-initialized")

  case object AccountNotInitialized extends Violation("account-not-initialized")

  case object CardNotActive extends Violation("card-not-active")

  case object InsufficientLimit extends Violation("insufficient-limit")

  case object HighFrequencySmallInterval extends Violation("high-frequency-small-interval")

  case object DoubledTransaction extends Violation("doubled-transaction")

  def values: Set[Violation] = Set(AccountAlreadyInitialized, AccountNotInitialized, CardNotActive, InsufficientLimit,
    HighFrequencySmallInterval, DoubledTransaction)

}