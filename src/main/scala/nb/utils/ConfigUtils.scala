package nb.utils

import com.typesafe.config.ConfigFactory

import scala.concurrent.duration.{FiniteDuration, TimeUnit}

trait ConfigUtils {

  val config = ConfigFactory.load()

  private def getDuration(key: String, default: => Long)(
    tu: TimeUnit): FiniteDuration = {
    val d = get(key, config.getLong).getOrElse(default)
    FiniteDuration(d, tu)
  }

  def getConfigPath(key: String): Option[String] =
    Option(key).filter(config.hasPath)

  private def get[T](key: String, f: String => T): Option[T] = {
    getConfigPath(key).map(f)
  }
  def getInt(key: String, default: => Int = 0): Int =
    get(key, config.getInt).getOrElse(default)

  def getMinutes(key: String, default: => Long): FiniteDuration = {
    getDuration(key, default)(scala.concurrent.duration.MINUTES)
  }
}
