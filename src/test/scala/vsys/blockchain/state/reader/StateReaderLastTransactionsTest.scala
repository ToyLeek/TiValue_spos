package vsys.blockchain.state.reader

import vsys.blockchain.transaction.TransactionGen
import vsys.blockchain.state.diffs._
import org.scalacheck.{Gen, Shrink}
import org.scalatest.prop.{GeneratorDrivenPropertyChecks, PropertyChecks}
import org.scalatest.{Matchers, PropSpec}
import vsys.blockchain.block.TestBlock
import vsys.blockchain.transaction.{GenesisTransaction, PaymentTransaction, Transaction}
import vsys.blockchain.transaction.{ProcessedTransaction, TransactionStatus}
import vsys.blockchain.transaction.proof.EllipticCurve25519Proof

class StateReaderLastTransactionsTest extends PropSpec with PropertyChecks with GeneratorDrivenPropertyChecks with Matchers with TransactionGen {

  private implicit def noShrink[A]: Shrink[A] = Shrink(_ => Stream.empty)

  val preconditionsAndPayment: Gen[(Seq[Transaction], PaymentTransaction)] = for {
    master <- accountGen
    recipient <- accountGen
    ts <- timestampGen
    genesis: GenesisTransaction = GenesisTransaction.create(master, ENOUGH_AMT, -1, ts).right.get
    transfer1: PaymentTransaction <- paymentGeneratorP(ts + 1, master, recipient)
    transfer2: PaymentTransaction <- paymentGeneratorP(ts + 2, master, recipient)
    preconditions: Seq[Transaction] = Seq(genesis, transfer1, transfer2)

    transfer3: PaymentTransaction <- paymentGeneratorP(ts + 3, master, recipient)
  } yield (preconditions, transfer3)

  private def txToProcessedTx(tx: Transaction): ProcessedTransaction =
    ProcessedTransaction(TransactionStatus.Success, tx.transactionFee, tx)

  property("accountTransactions sort results by 'fresh head' rule") {
    forAll(preconditionsAndPayment) { case ((pre, payment: PaymentTransaction)) =>
      assertDiffAndState(Seq(TestBlock.create(pre)), TestBlock.create(Seq(payment))) { (blockDiff, newState) =>

        val sender = EllipticCurve25519Proof.fromBytes(payment.proofs.proofs.head.bytes.arr).toOption.get.publicKey
        newState.accountTransactions(sender, 1, 0)._2.map{case (h,tx) => tx} shouldBe Seq(txToProcessedTx(payment))
        val g = pre.head
        val tx1 = pre(1)
        val tx2 = pre(2)
        newState.accountTransactions(sender, 3, 0)._2.map{case (h,tx) => tx} shouldBe Seq(txToProcessedTx(payment), txToProcessedTx(tx2), txToProcessedTx(tx1))
        newState.accountTransactions(sender, 10, 0)._2.map{case (h,tx) => tx} shouldBe Seq(txToProcessedTx(payment), txToProcessedTx(tx2), txToProcessedTx(tx1), txToProcessedTx(g))
      }
    }
  }
}
