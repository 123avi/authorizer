package nb.utils

object AuthorizerConfig extends ConfigUtils {
  lazy val maxTransactionFrequency  = getInt("authorizer.max-transactions-frequency", 3)
  lazy val transactionInterval = getMinutes("authorizer.transactions-interval-minutes", 2).toSeconds

}
