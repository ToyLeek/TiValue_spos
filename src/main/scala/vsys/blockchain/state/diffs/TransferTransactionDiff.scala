package vsys.blockchain.state.diffs

import cats.implicits._
import vsys.settings.FunctionalitySettings
import vsys.blockchain.state._
import vsys.blockchain.state.reader.StateReader
import vsys.account.Address
import vsys.blockchain.transaction.ValidationError
import vsys.blockchain.transaction.ValidationError.GenericError
import vsys.blockchain.transaction.assets.TransferTransaction

import scala.util.Right

object TransferTransactionDiff {
  def apply(state: StateReader, s: FunctionalitySettings, blockTime: Long, height: Int)(tx: TransferTransaction): Either[ValidationError, Diff] = {
    val sender = Address.fromPublicKey(tx.sender.publicKey)

    val isInvalidEi = for {
      recipient <- state.resolveAliasEi(tx.recipient)
      portfolios = (
        tx.assetId match {
          case None => Map(sender -> Portfolio(-tx.amount, LeaseInfo.empty, Map.empty)).combine(
            Map(recipient -> Portfolio(tx.amount, LeaseInfo.empty, Map.empty))
          )
          case Some(aid) =>
            Map(sender -> Portfolio(0, LeaseInfo.empty, Map(aid -> -tx.amount))).combine(
              Map(recipient -> Portfolio(0, LeaseInfo.empty, Map(aid -> tx.amount)))
            )
        }).combine(
        tx.feeAssetId match {
          case None => Map(sender -> Portfolio(-tx.fee, LeaseInfo.empty, Map.empty))
          case Some(aid) =>
            Map(sender -> Portfolio(0, LeaseInfo.empty, Map(aid -> -tx.fee)))
        }
      )
      assetIssued = tx.assetId match {
        case None => true
        case Some(aid) => state.assetInfo(aid).isDefined
      }
      feeAssetIssued = tx.feeAssetId match {
        case None => true
        case Some(aid) => state.assetInfo(aid).isDefined
      }
    } yield (portfolios, !(assetIssued && feeAssetIssued))

    isInvalidEi match {
      case Left(e) => Left(e)
      case Right((portfolios, invalid)) =>
        if (invalid)
          Left(GenericError(s"Unissued assets are not allowed")) 
        else  
          Right(Diff(height = height,
            tx = tx,
            portfolios = portfolios,
            chargedFee = tx.fee))
    }
  }
}