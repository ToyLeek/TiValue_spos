package vsys.wallet

import org.scalatest.{FunSuite, Matchers}
import vsys.settings.WalletSettings

class WalletSpecification extends FunSuite with Matchers {

  private val walletSize = 6
  val w = Wallet(WalletSettings(None, "cookies", Option("4kCz3Zkuz9gR2Ls85p9jA7V3fRM8Xnd7dK1cCJQMUimfiosp5fn8Q1qWWAfdMcXWEEigvfusZTLfJ5tp8RaurRwe")))

  test("wallet - acc creation") {
    w.generateNewAccounts(walletSize)

    w.privateKeyAccounts.size shouldBe walletSize
    w.privateKeyAccounts.map(_.address) shouldBe Seq("u6EWabPewf2YzVGdyoES4Jkqixmyw3DMevo", "u6Sa86UUzLXVSMaWXSu3vo2tsREd29QoEL4", "u6DqJNHc6ChPA7X2Hc6s7YT2UohM9cgyBJL", "u6NuDiYu92gYaMZVeMcKXEij5LHFybAKWrB", "u6G3i5jAHNtPQQs5NZFSbpALQ4LXd7Y1iXj", "u6PZfjkT7c6KHHGXRK6x8mCKrvrS8fPTPLF")
  }

  test("wallet - acc deletion") {

    val head = w.privateKeyAccounts.head
    w.deleteAccount(head)
    assert(w.privateKeyAccounts.size == walletSize - 1)

    w.deleteAccount(w.privateKeyAccounts.head)
    assert(w.privateKeyAccounts.size == walletSize - 2)

    w.privateKeyAccounts.foreach(w.deleteAccount)

    assert(w.privateKeyAccounts.isEmpty)
  }

  test("reopening") {

    val walletFile = Some(vsys.createTestTemporaryFile("wallet", ".dat"))

    val w = Wallet(WalletSettings(walletFile, "cookies", Option("4kCz3Zkuz9gR2Ls85p9jA7V3fRM8Xnd7dK1cCJQMUimfiosp5fn8Q1qWWAfdMcXWEEigvfusZTLfJ5tp8RaurRwe")))
    w.generateNewAccounts(6)
    val nonce = w.nonce

    val w2 = Wallet(WalletSettings(walletFile, "cookies", None))
    w2.privateKeyAccounts.head.address should not be null
    w2.nonce shouldBe nonce
  }
}