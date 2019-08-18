package vsys.api.http.assets

import play.api.libs.json._


case class BurnRequest(sender: String, assetId: String, quantity: Long, fee: Long)

object BurnRequest {
  implicit val burnFormat: Format[BurnRequest] = Json.format
}
