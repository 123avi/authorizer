package avi.utils

object AuthorizerConfig extends ConfigUtils {
  lazy val maxTransaction  = getInt("authorizer.max-transactions", 3)
  lazy val transactionInterval = getMinutes("authorizer.transactions-interval-minutes", 2)

}
