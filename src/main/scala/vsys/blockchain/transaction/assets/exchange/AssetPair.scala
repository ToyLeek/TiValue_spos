package vsys.blockchain.transaction.assets.exchange

import vsys.blockchain.state.ByteStr
import io.swagger.annotations.ApiModelProperty
import play.api.libs.json.{JsObject, Json}
import vsys.blockchain.transaction._
import vsys.blockchain.transaction.assets.exchange.Order.assetIdBytes
import vsys.blockchain.transaction.assets.exchange.Validation.booleanOperators

import scala.util.{Success, Try}

case class AssetPair(@ApiModelProperty(dataType = "java.lang.String") amountAsset: Option[AssetId],
                     @ApiModelProperty(dataType = "java.lang.String") priceAsset: Option[AssetId]) {
  @ApiModelProperty(hidden = true)
  lazy val priceAssetStr: String = priceAsset.map(_.base58).getOrElse(AssetPair.VSYSName)
  @ApiModelProperty(hidden = true)
  lazy val amountAssetStr: String = amountAsset.map(_.base58).getOrElse(AssetPair.VSYSName)

  override def toString: String = key

  def key: String = amountAssetStr + "-" + priceAssetStr

  def isValid: Validation = {
    (amountAsset != priceAsset) :| "Invalid AssetPair"
  }

  def bytes: Array[Byte] = assetIdBytes(amountAsset) ++ assetIdBytes(priceAsset)

  def json: JsObject = Json.obj(
    "amountAsset" -> amountAsset.map(_.base58),
    "priceAsset" -> priceAsset.map(_.base58)
  )

  override def hashCode(): Int = toString.hashCode()

  override def equals(obj: Any): Boolean = {
    obj match {
      case other: AssetPair =>
        toString == other.toString
      case _ => false
    }
  }
}

object AssetPair {
  val VSYSName = "VSYS"

  private def extractAssetId(a: String): Try[Option[AssetId]] = a match {
    case `VSYSName` => Success(None)
    case other => ByteStr.decodeBase58(other).map(Option(_))
  }

  def createAssetPair(amountAsset: String, priceAsset: String): Try[AssetPair] =
    for {
      a1 <- extractAssetId(amountAsset)
      a2 <- extractAssetId(priceAsset)
    } yield AssetPair(a1, a2)
}
