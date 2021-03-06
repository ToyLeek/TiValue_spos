package vsys.utils

import java.net.{InetAddress, SocketTimeoutException}

import org.apache.commons.net.ntp.NTPUDPClient
import monix.eval.Task
import monix.execution.Scheduler
import monix.execution.schedulers.SchedulerService

import scala.concurrent.duration.DurationInt

trait Time {
  def correctedTime(): Long
  def getTimestamp() : Long
}

class TimeImpl extends Time with ScorexLogging with AutoCloseable {
  private val offsetPanicThreshold = 1000000L
  private val ExpirationTimeout = 120000L
  private val RetryDelay = 10.seconds
  private val ResponseTimeout = 10.seconds
  private val NtpServer = "pool.ntp.org"

  private implicit val scheduler: SchedulerService = Scheduler.singleThread(name = "time-impl")

  private var cntTime = 0L

  def correctedTime(): Long = {
    //CALCULATE CORRECTED TIME
    val cnt = System.currentTimeMillis() * 1000000L + System.nanoTime() % 1000000L + offset
    cntTime = if (cnt <= cntTime && cntTime - cnt <= 1000000L) cnt + 1000000L else cnt
    cntTime
  }

  private var txTime: Long = 0

  def getTimestamp: Long = {
    //guarantee the cnt_SystemTime > last_Timestamp+1
    txTime = Math.max(correctedTime(), txTime + 1)
    txTime
  }

  private val client = new NTPUDPClient()
  client.setDefaultTimeout(ResponseTimeout.toMillis.toInt)

  @volatile private var offset = 0L
  private val updateTask: Task[Unit] = {
    def newOffsetTask: Task[Option[(InetAddress, java.lang.Long)]] = Task {
      try {
        client.open()
        val info = client.getTime(InetAddress.getByName(NtpServer))
        info.computeDetails()
        Option(info.getOffset).map { offset =>
          if (Math.abs(offset) > offsetPanicThreshold) throw new Exception("Offset is suspiciously large") else (info.getAddress, offset * 1000000L)
        }
      } catch {
        case _: SocketTimeoutException =>
          None
        case t: Throwable =>
          log.warn("Problems with NTP: ", t)
          None
      } finally {
        client.close()
      }
    }

    newOffsetTask.flatMap {
      case None if !scheduler.isShutdown => updateTask.delayExecution(RetryDelay)
      case Some((server, newOffset)) if !scheduler.isShutdown =>
        log.trace(s"Adjusting time with $newOffset nanoseconds, source: ${server.getHostAddress}.")
        offset = newOffset
        val cntSysTime = correctedTime() / 1000000L
        val nextUpdateTime = (ExpirationTimeout - cntSysTime % ExpirationTimeout).toInt.milliseconds + 500.milliseconds
        // to avoid the miner mint time
        updateTask.delayExecution(nextUpdateTime)
      case _ => Task.unit
    }
  }

  private val taskHandle = updateTask.runAsyncLogErr

  override def close(): Unit = {
    log.info("Shutting down Time")
    taskHandle.cancel()
    scheduler.shutdown()
  }
}

object NTP extends TimeImpl
