package vsys.blockchain.transaction.contract

import com.google.common.primitives.{Bytes, Ints, Longs, Shorts}
import vsys.blockchain.state.ByteStr
import play.api.libs.json.{JsObject, Json}
import vsys.account._
import scorex.crypto.encode.Base58
import vsys.utils.serialization.{BytesSerializable, Deser}
import vsys.blockchain.transaction.TransactionParser._
import vsys.blockchain.transaction.{AssetId, ValidationError}
import vsys.account.ContractAccount
import vsys.blockchain.contract.{Contract, DataEntry}
import vsys.blockchain.transaction.proof._
import vsys.blockchain.transaction.ProvenTransaction

import scala.util.{Failure, Success, Try}

case class RegisterContractTransaction private(contract: Contract,
                                               data: Seq[DataEntry],
                                               description: String,
                                               fee: Long,
                                               feeScale: Short,
                                               timestamp: Long,
                                               proofs: Proofs)
  extends ProvenTransaction {

  override val transactionType: TransactionType.Value = TransactionType.RegisterContractTransaction

  lazy val contractId: ContractAccount = ContractAccount.fromId(id)

  lazy val toSign: Array[Byte] = Bytes.concat(
    Array(transactionType.id.toByte),
    BytesSerializable.arrayWithSize(contract.bytes.arr),
    Deser.serializeArray(DataEntry.serializeArrays(data)),
    Deser.serializeArray(Deser.serilizeString(description)),
    Longs.toByteArray(fee),
    Shorts.toByteArray(feeScale),
    Longs.toByteArray(timestamp))

  override lazy val json: JsObject = jsonBase() ++ Json.obj(
    "contractId" ->  contractId.address,
    "contract" -> Json.obj("languageCode" ->Deser.deserilizeString(contract.languageCode),
      "languageVersion" -> Ints.fromByteArray(contract.languageVersion),
      "triggers" -> contract.trigger.map(p => Base58.encode(p)),
      "descriptors" -> contract.descriptor.map(p => Base58.encode(p)),
      "stateVariables" -> contract.stateVar.map(p => Base58.encode(p)),
      "textual" -> Json.obj("triggers" -> Base58.encode(contract.textual.head),
        "descriptors" -> Base58.encode(contract.textual(1)),
        "stateVariables" -> Base58.encode(contract.textual.last))),
    "initData" -> Base58.encode(DataEntry.serializeArrays(data)),
    "description" -> description,
    "fee" -> fee,
    "feeScale" -> feeScale,
    "timestamp" -> timestamp
  )

  override val assetFee: (Option[AssetId], Long, Short) = (None, fee, 100)
  override lazy val bytes: Array[Byte] = Bytes.concat(toSign, proofs.bytes)

}

object RegisterContractTransaction {

  val MaxDescriptionSize = 140
  val MinDescriptionSize = 0

  def parseTail(bytes: Array[Byte]): Try[RegisterContractTransaction] = Try {
    val (contractBytes, contractEnd) = Deser.parseArraySize(bytes, 0)
    (for {
      contract <- Contract.fromBytes(contractBytes)
      (dataBytes, dataEnd) = Deser.parseArraySize(bytes, contractEnd)
      data <- DataEntry.parseArrays(dataBytes)
      (descriptionBytes, descriptionEnd) = Deser.parseArraySize(bytes, dataEnd)
      description = Deser.deserilizeString(descriptionBytes)
      fee = Longs.fromByteArray(bytes.slice(descriptionEnd, descriptionEnd + 8))
      feeScale = Shorts.fromByteArray(bytes.slice(descriptionEnd + 8, descriptionEnd + 10))
      timestamp = Longs.fromByteArray(bytes.slice(descriptionEnd + 10, descriptionEnd + 18))
      proofs <- Proofs.fromBytes(bytes.slice(descriptionEnd + 18, bytes.length))
      tx <- RegisterContractTransaction.createWithProof(contract, data, description, fee, feeScale, timestamp, proofs)
    } yield tx).fold(left => Failure(new Exception(left.toString)), right => Success(right))
  }.flatten

  def createWithProof(contract: Contract,
                      data: Seq[DataEntry],
                      description: String,
                      fee: Long,
                      feeScale: Short,
                      timestamp: Long,
                      proofs: Proofs): Either[ValidationError, RegisterContractTransaction] =
    if ((Deser.serilizeString(description).length > MaxDescriptionSize) || !Deser.validUTF8(description)) {
      Left(ValidationError.InvalidUTF8String("contractDescription"))
    } else if(fee <= 0) {
      Left(ValidationError.InsufficientFee)
    } else if (feeScale != DefaultFeeScale) {
      Left(ValidationError.WrongFeeScale(feeScale))
    } else {
      Right(RegisterContractTransaction(contract, data, description, fee, feeScale, timestamp, proofs))
    }

  def create(sender: PrivateKeyAccount,
             contract: Contract,
             data: Seq[DataEntry],
             description: String,
             fee: Long,
             feeScale: Short,
             timestamp: Long): Either[ValidationError, RegisterContractTransaction] = for {
    unsigned <- createWithProof(contract, data, description, fee, feeScale, timestamp, Proofs.empty)
    proofs <- Proofs.create(List(EllipticCurve25519Proof.createProof(unsigned.toSign, sender).bytes))
    tx <- createWithProof(contract, data, description, fee, feeScale, timestamp, proofs)
  } yield tx


  def create(sender: PublicKeyAccount,
             contract: Contract,
             data: Seq[DataEntry],
             description: String,
             fee: Long,
             feeScale: Short,
             timestamp: Long,
             signature: ByteStr): Either[ValidationError, RegisterContractTransaction] = for {
    proofs <- Proofs.create(List(EllipticCurve25519Proof.buildProof(sender, signature).bytes))
    tx <- createWithProof(contract, data, description, fee, feeScale, timestamp, proofs)
  } yield tx
}
