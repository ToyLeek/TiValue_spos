package vsys.blockchain.transaction

import com.google.common.primitives.{Bytes, Longs}
import vsys.blockchain.state.ByteStr
import play.api.libs.json.{JsObject, Json}
import vsys.account._
import vsys.utils.crypto.EllipticCurveImpl
import vsys.utils.crypto.hash.FastCryptographicHash
import vsys.utils.serialization.{BytesSerializable, Deser}
import vsys.blockchain.transaction.TransactionParser._

import scala.util.{Failure, Success, Try}


case class CreateAliasTransaction private(sender: PublicKeyAccount,
                                          alias: Alias,
                                          fee: Long,
                                          timestamp: Long,
                                          signature: ByteStr)
  extends SignedTransaction {

  override val transactionType: TransactionType.Value = TransactionType.CreateAliasTransaction

  override lazy val id: ByteStr = ByteStr(FastCryptographicHash(transactionType.id.toByte +: alias.bytes.arr))

  lazy val toSign: Array[Byte] = Bytes.concat(
    Array(transactionType.id.toByte),
    sender.publicKey,
    BytesSerializable.arrayWithSize(alias.bytes.arr),
    Longs.toByteArray(fee),
    Longs.toByteArray(timestamp))

  override lazy val json: JsObject = jsonBase() ++ Json.obj(
    "alias" -> alias.name,
    "fee" -> fee,
    "timestamp" -> timestamp
  )

  // TODO
  // add feeScale in assetFee, need to change 100 later
  override val assetFee: (Option[AssetId], Long, Short) = (None, fee, 100)
  override lazy val bytes: Array[Byte] = Bytes.concat(toSign, signature.arr)

}

object CreateAliasTransaction {

  def parseTail(bytes: Array[Byte]): Try[CreateAliasTransaction] = Try {
    import EllipticCurveImpl._
    val sender = PublicKeyAccount(bytes.slice(0, KeyLength))
    val (aliasBytes, aliasEnd) = Deser.parseArraySize(bytes, KeyLength)
    (for {
      alias <- Alias.fromBytes(aliasBytes)
      fee = Longs.fromByteArray(bytes.slice(aliasEnd, aliasEnd + 8))
      timestamp = Longs.fromByteArray(bytes.slice(aliasEnd + 8, aliasEnd + 16))
      signature = ByteStr(bytes.slice(aliasEnd + 16, aliasEnd + 16 + SignatureLength))
      tx <- CreateAliasTransaction.create(sender, alias, fee, timestamp, signature)
    } yield tx).fold(left => Failure(new Exception(left.toString)), right => Success(right))
  }.flatten

  def create(sender: PublicKeyAccount,
             alias: Alias,
             fee: Long,
             timestamp: Long,
             signature: ByteStr): Either[ValidationError, CreateAliasTransaction] =
    if (fee <= 0) {
      Left(ValidationError.InsufficientFee)
    } else {
      Right(CreateAliasTransaction(sender, alias, fee, timestamp, signature))
    }

  def create(sender: PrivateKeyAccount,
             alias: Alias,
             fee: Long,
             timestamp: Long): Either[ValidationError, CreateAliasTransaction] = {
    create(sender, alias, fee, timestamp, ByteStr.empty).right.map { unsigned =>
      unsigned.copy(signature = ByteStr(EllipticCurveImpl.sign(sender, unsigned.toSign)))
    }
  }
}
