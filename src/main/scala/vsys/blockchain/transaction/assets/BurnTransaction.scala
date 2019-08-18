package vsys.blockchain.transaction.assets

import com.google.common.primitives.{Bytes, Longs}
import vsys.blockchain.state.ByteStr
import play.api.libs.json.{JsObject, Json}
import vsys.account.{PrivateKeyAccount, PublicKeyAccount}
import vsys.utils.crypto.EllipticCurveImpl
import vsys.blockchain.transaction.TransactionParser._
import vsys.blockchain.transaction.{ValidationError, _}

import scala.util.{Failure, Success, Try}

case class BurnTransaction private(sender: PublicKeyAccount,
                                   assetId: ByteStr,
                                   amount: Long,
                                   fee: Long,
                                   timestamp: Long,
                                   signature: ByteStr)
  extends SignedTransaction {

  override val transactionType: TransactionType.Value = TransactionType.BurnTransaction

  lazy val toSign: Array[Byte] = Bytes.concat(Array(transactionType.id.toByte),
    sender.publicKey,
    assetId.arr,
    Longs.toByteArray(amount),
    Longs.toByteArray(fee),
    Longs.toByteArray(timestamp))

  override lazy val json: JsObject = jsonBase() ++ Json.obj(
    "assetId" -> assetId.base58,
    "amount" -> amount,
    "fee" -> fee
  )

  // TODO
  // add feeScale in assetFee, need to change 100 later
  override val assetFee: (Option[AssetId], Long, Short) = (None, fee, 100)

  override lazy val bytes: Array[Byte] = Bytes.concat(toSign, signature.arr)

}


object BurnTransaction {

  def parseBytes(bytes: Array[Byte]): Try[BurnTransaction] = Try {
    require(bytes.head == TransactionType.BurnTransaction.id)
    parseTail(bytes.tail).get
  }

  def parseTail(bytes: Array[Byte]): Try[BurnTransaction] = Try {
    import EllipticCurveImpl._
    val sender = PublicKeyAccount(bytes.slice(0, KeyLength))
    val assetId = ByteStr(bytes.slice(KeyLength, KeyLength + AssetIdLength))
    val quantityStart = KeyLength + AssetIdLength

    val quantity = Longs.fromByteArray(bytes.slice(quantityStart, quantityStart + 8))
    val fee = Longs.fromByteArray(bytes.slice(quantityStart + 8, quantityStart + 16))
    val timestamp = Longs.fromByteArray(bytes.slice(quantityStart + 16, quantityStart + 24))
    val signature = ByteStr(bytes.slice(quantityStart + 24, quantityStart + 24 + SignatureLength))
    BurnTransaction
      .create(sender, assetId, quantity, fee, timestamp, signature)
      .fold(left => Failure(new Exception(left.toString)), right => Success(right))
  }.flatten

  def create(sender: PublicKeyAccount,
             assetId: ByteStr,
             quantity: Long,
             fee: Long,
             timestamp: Long,
             signature: ByteStr): Either[ValidationError, BurnTransaction] =
    if (quantity < 0) {
      Left(ValidationError.NegativeAmount)
    } else if (fee <= 0) {
      Left(ValidationError.InsufficientFee)
    } else {
      Right(BurnTransaction(sender, assetId, quantity, fee, timestamp, signature))
    }

  def create(sender: PrivateKeyAccount,
             assetId: ByteStr,
             quantity: Long,
             fee: Long,
             timestamp: Long): Either[ValidationError, BurnTransaction] =
    create(sender, assetId, quantity, fee, timestamp, ByteStr.empty).right.map { unverified =>
      unverified.copy(signature = ByteStr(EllipticCurveImpl.sign(sender, unverified.toSign)))
    }
}
