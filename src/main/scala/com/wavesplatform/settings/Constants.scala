package com.wavesplatform.settings

import com.wavesplatform.Version
import scorex.utils.ScorexLogging

/**
  * System constants here.
  */

object Constants extends ScorexLogging {
  val ApplicationName = "TiValue"
  val AgentName = s"TV Core v${Version.VersionString}"

  val UnitsInVsys = 100000000L
  val TotalVsys = 10000000000L // unuse in mainnet
}
