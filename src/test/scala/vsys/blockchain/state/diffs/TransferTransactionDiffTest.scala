package vsys.blockchain.state.diffs

import cats._
import vsys.blockchain.transaction.TransactionGen
import vsys.blockchain.state._
import org.scalacheck.{Gen, Shrink}
import org.scalatest.prop.{GeneratorDrivenPropertyChecks, PropertyChecks}
import org.scalatest.{Ignore, Matchers, PropSpec}
import vsys.account.Address
import vsys.blockchain.block.TestBlock
import vsys.blockchain.transaction.GenesisTransaction
import vsys.blockchain.transaction.assets.{IssueTransaction, TransferTransaction}

@Ignore
class TransferTransactionDiffTest extends PropSpec with PropertyChecks with GeneratorDrivenPropertyChecks with Matchers with TransactionGen {

  private implicit def noShrink[A]: Shrink[A] = Shrink(_ => Stream.empty)

  val preconditionsAndTransfer: Gen[(GenesisTransaction, IssueTransaction, IssueTransaction, TransferTransaction)] = for {
    master <- accountGen
    recepient <- otherAccountGen(candidate = master)
    ts <- positiveIntGen
    genesis: GenesisTransaction = GenesisTransaction.create(master, ENOUGH_AMT, -1, ts).right.get
    issue1: IssueTransaction <- issueReissueBurnGeneratorP(ENOUGH_AMT, master).map(_._1)
    issue2: IssueTransaction <- issueReissueBurnGeneratorP(ENOUGH_AMT, master).map(_._1)
    maybeAsset <- Gen.option(issue1)
    maybeAsset2 <- Gen.option(issue2)
    maybeFeeAsset <- Gen.oneOf(maybeAsset, maybeAsset2)
    transfer <- transferGeneratorP(master, recepient, maybeAsset.map(_.id), maybeFeeAsset.map(_.id))
  } yield (genesis, issue1, issue2, transfer)

  property("transfers assets to recipient preserving vsys invariant") {
    forAll(preconditionsAndTransfer) { case ((genesis, issue1, issue2, transfer)) =>
      assertDiffAndState(Seq(TestBlock.create(Seq(genesis, issue1, issue2))), TestBlock.create(Seq(transfer))) { case (totalDiff, newState) =>
        val totalPortfolioDiff = Monoid.combineAll(totalDiff.txsDiff.portfolios.values)
        totalPortfolioDiff.balance shouldBe 0
        totalPortfolioDiff.effectiveBalance shouldBe 0
        totalPortfolioDiff.assets.values.foreach(_ shouldBe 0)

        val recipient: Address = transfer.recipient.asInstanceOf[Address]
        val recipientPortfolio = newState.accountPortfolio(recipient)
        if (transfer.sender.toAddress != recipient) {
          transfer.assetId match {
            case Some(aid) => recipientPortfolio shouldBe Portfolio(0, LeaseInfo.empty, Map(aid -> transfer.amount))
            case None => recipientPortfolio shouldBe Portfolio(transfer.amount, LeaseInfo.empty, Map.empty)
          }
        }
      }
    }
  }
}
