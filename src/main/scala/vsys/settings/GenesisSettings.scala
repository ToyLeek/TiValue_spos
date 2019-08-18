package vsys.settings

import vsys.blockchain.state.ByteStr

import scala.concurrent.duration._

case class GenesisTransactionSettings(recipient: String, amount: Long, slotId: Int)

case class GenesisSettings(
  blockTimestamp: Long,
  timestamp: Long,
  initialBalance: Long,
  signature: Option[ByteStr],
  transactions: Seq[GenesisTransactionSettings],
  initialMintTime: Long,
  averageBlockDelay: FiniteDuration)

object GenesisSettings {
  val MAINNET = GenesisSettings(1557478816060000000L, 1557478816060000000L, 28670000000000000L,
    ByteStr.decodeBase58("3iuRJ5cqhsJos9iJViU2CrkPEHhyjEJnEMou2pM9sNhkuxwGCafu7TsvCxfUUTpBscNn2fMGbXWFEk8xSpqcoWpf").toOption,
    List(
        GenesisTransactionSettings("tvBLKjXW8MrASXyCdYXaBozdTLDc27Bqdkt",28070000000000000L,-1),
        GenesisTransactionSettings("tvK9LthxPHcwMxxacyCH3u6sm3jpvRJFa4q",100000000000000L,0),
        GenesisTransactionSettings("tvGezfk7kMgfv3qoZWzDjnoLV2p1JgFoTiE",100000000000000L,4),
        GenesisTransactionSettings("tvFwJtFqgk624rdZRs7Ty22LbdkTwqA1Ans",100000000000000L,8),
        GenesisTransactionSettings("tv9v9GyrrM6r6mMEsY4se74egRkFKXpzFNz",100000000000000L,12),
        GenesisTransactionSettings("tv5wDFLNSWf1BTDzmgEDr1GmJ872PtiyNev",100000000000000L,16),
        GenesisTransactionSettings("tvE8faTiHTYsZirq1CkWmJGNn32mrkiuSJu",100000000000000L,20)),
    1557478816000000000L, 60.seconds)

  val TESTNET = GenesisSettings(1557123105006000000L, 1557123105006000000L, Constants.UnitsInVsys * Constants.TotalVsys,
    ByteStr.decodeBase58("5NQXZMQqfzwc2apbt545GCoN5PqgqqRUKYF4d9irkJU4g3niGDXvbL9LyLgT58nTQCEjwkGWMoqcMRUL5KbxLS9E").toOption,
    List(
      GenesisTransactionSettings("u6BpC4w9PfNaFgWYMifZvZZ1PVA3Uu7uGpd", (Constants.UnitsInVsys * Constants.TotalVsys * 0.30).toLong, 0)),
    1557123105000000000L, 60.seconds)
}
