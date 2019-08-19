package vsys.settings

import com.typesafe.config.ConfigFactory
import vsys.blockchain.state.ByteStr
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.duration._

class BlockchainSettingsSpecification extends FlatSpec with Matchers {
  "BlockchainSettings" should "read custom values" in {
    val config = loadConfig(ConfigFactory.parseString(
      """vsys {
        |  directory = "/tv"
        |  blockchain {
        |    minimum-in-memory-diff-blocks = 1
        |    type = CUSTOM
        |    custom {
        |      address-scheme-character = "C"
        |      functionality {
        |        allow-temporary-negative-until = 1
        |        allow-invalid-payment-transactions-by-timestamp = 2
        |        require-sorted-transactions-after = 3
        |        generation-balance-depth-from-50-to-1000-after-height = 4
        |        minimal-generating-balance-after = 5
        |        allow-transactions-from-future-until = 6
        |        allow-unissued-assets-until = 7
        |        allow-burn-transaction-after = 8
        |        allow-lease-transaction-after = 10
        |        allow-exchange-transaction-after = 11
        |        allow-invalid-reissue-in-same-block-until-timestamp = 12
        |        allow-createalias-transaction-after = 13
        |        allow-multiple-lease-cancel-transaction-until-timestamp = 14
        |        reset-effective-balances-at-height = 15
        |        allow-leased-balance-transfer-until = 17
        |        allow-contract-transaction-after-height = 0
        |        num-of-slots = 5
        |        minting-speed = 5
        |      }
        |      genesis {
        |        timestamp = 1460678400000
        |        block-timestamp = 1460678400000
        |        signature = "BASE58BLKSGNATURE"
        |        initial-balance = 100000000000000
        |        initial-mint-time = 1529885280000000000
        |        average-block-delay = 60s
        |        transactions = [
        |          {recipient = "BASE58ADDRESS1", amount = 50000000000001, slot-id = -1},
        |          {recipient = "BASE58ADDRESS2", amount = 49999999999999, slot-id = -1}
        |        ]
        |      }
        |    }
        |    state {
        |      tx-type-account-tx-ids = on
        |    }
        |  }
        |}""".stripMargin))
    val settings = BlockchainSettings.fromConfig(config)

    //not snapshot
    settings.minimumInMemoryDiffSize should be(1)
    settings.addressSchemeCharacter should be('C')
    settings.functionalitySettings.numOfSlots should be (5)
    settings.functionalitySettings.mintingSpeed should be (5)
    settings.functionalitySettings.allowContractTransactionAfterHeight should be (0)
    settings.genesisSettings.blockTimestamp should be(1460678400000L)
    settings.genesisSettings.timestamp should be(1460678400000L)
    settings.genesisSettings.signature should be(ByteStr.decodeBase58("BASE58BLKSGNATURE").toOption)
    settings.genesisSettings.initialBalance should be(100000000000000L)
    settings.genesisSettings.initialMintTime should be(1529885280000000000L)
    settings.genesisSettings.averageBlockDelay should be(60.seconds)
    settings.genesisSettings.transactions should be(Seq(
      GenesisTransactionSettings("BASE58ADDRESS1", 50000000000001L, -1),
      GenesisTransactionSettings("BASE58ADDRESS2", 49999999999999L, -1)))
    settings.stateSettings.txTypeAccountTxIds should be (true)
  }

  it should "read testnet settings" in {
    val config = loadConfig(ConfigFactory.parseString(
      """vsys {
        |  directory = "/tv"
        |  blockchain {
        |    minimum-in-memory-diff-blocks = 1
        |    type = TESTNET
        |    state {
        |      tx-type-account-tx-ids = off
        |    }
        |  }
        |}""".stripMargin))
    val settings = BlockchainSettings.fromConfig(config)

    settings.minimumInMemoryDiffSize should be(1)
    settings.addressSchemeCharacter should be('T')
    settings.functionalitySettings.numOfSlots should be (60)
    settings.functionalitySettings.mintingSpeed should be (1)
    settings.functionalitySettings.allowContractTransactionAfterHeight should be (430000) // same as the setting
    settings.genesisSettings.blockTimestamp should be(1557123105006000000L)
    settings.genesisSettings.timestamp should be(1557123105006000000L)
    settings.genesisSettings.averageBlockDelay should be(60.seconds)
    settings.genesisSettings.signature should be(ByteStr.decodeBase58("5NQXZMQqfzwc2apbt545GCoN5PqgqqRUKYF4d9irkJU4g3niGDXvbL9LyLgT58nTQCEjwkGWMoqcMRUL5KbxLS9E").toOption)
    settings.genesisSettings.initialBalance should be(300000000000000000L)

    settings.genesisSettings.transactions should be(Seq(
        GenesisTransactionSettings("u6BpC4w9PfNaFgWYMifZvZZ1PVA3Uu7uGpd",(Constants.UnitsInVsys * Constants.TotalVsys * 0.30).toLong, 0)
    ))

    settings.stateSettings.txTypeAccountTxIds should be (false)
  }

  it should "read mainnet settings" in {
    val config = loadConfig(ConfigFactory.parseString(
      """vsys {
        |  directory = "/tv"
        |  blockchain {
        |    minimum-in-memory-diff-blocks = 1
        |    type = MAINNET
        |    state {
        |      tx-type-account-tx-ids = off
        |    }
        |  }
        |}""".stripMargin))
    val settings = BlockchainSettings.fromConfig(config)

    settings.minimumInMemoryDiffSize should be(1)
    settings.addressSchemeCharacter should be(';')
    settings.functionalitySettings.numOfSlots should be (60)
    settings.functionalitySettings.mintingSpeed should be (1)
    settings.functionalitySettings.allowContractTransactionAfterHeight should be (1800000) // same as the setting
    settings.genesisSettings.blockTimestamp should be(1557478816060000000L)
    settings.genesisSettings.timestamp should be(1557478816060000000L)
    settings.genesisSettings.signature should be(ByteStr.decodeBase58("3iuRJ5cqhsJos9iJViU2CrkPEHhyjEJnEMou2pM9sNhkuxwGCafu7TsvCxfUUTpBscNn2fMGbXWFEk8xSpqcoWpf").toOption)
    settings.genesisSettings.initialBalance should be(28670000000000000L) //changed the total initialBalance in default setting
    settings.genesisSettings.transactions should be(Seq(
        GenesisTransactionSettings("tvBLKjXW8MrASXyCdYXaBozdTLDc27Bqdkt",28070000000000000L,-1),
        GenesisTransactionSettings("tvK9LthxPHcwMxxacyCH3u6sm3jpvRJFa4q",100000000000000L,0),
        GenesisTransactionSettings("tvGezfk7kMgfv3qoZWzDjnoLV2p1JgFoTiE",100000000000000L,4),
        GenesisTransactionSettings("tvFwJtFqgk624rdZRs7Ty22LbdkTwqA1Ans",100000000000000L,8),
        GenesisTransactionSettings("tv9v9GyrrM6r6mMEsY4se74egRkFKXpzFNz",100000000000000L,12),
        GenesisTransactionSettings("tv5wDFLNSWf1BTDzmgEDr1GmJ872PtiyNev",100000000000000L,16),
        GenesisTransactionSettings("tvE8faTiHTYsZirq1CkWmJGNn32mrkiuSJu",100000000000000L,20)))
    
    settings.stateSettings.txTypeAccountTxIds should be (false)
  }
}
