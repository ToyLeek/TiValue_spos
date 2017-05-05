package scorex.api.http

import javax.ws.rs.Path

import akka.http.scaladsl.server.Route
import com.wavesplatform.settings.RestAPISettings
import io.swagger.annotations._
import scorex.api.http.assets.TransferRequest
import scorex.transaction.{NewTransactionHandler, TransactionFactory}
import scorex.utils.Time
import scorex.wallet.Wallet

@Path("/payment")
@Api(value = "/payment")
@Deprecated
case class PaymentApiRoute(settings: RestAPISettings, wallet: Wallet, newTxHandler: NewTransactionHandler, time: Time) extends ApiRoute {

  override lazy val route = payment

  @Deprecated
  @ApiOperation(value = "Send payment. Deprecated: use /assets/transfer instead",
    notes = "Send payment to another wallet. Deprecated: use /assets/transfer instead",
    httpMethod = "POST",
    produces = "application/json",
    consumes = "application/json")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      name = "body",
      value = "Json with data",
      required = true,
      paramType = "body",
      dataType = "scorex.api.http.assets.PaymentRequest",
      defaultValue = "{\n\t\"amount\":400,\n\t\"fee\":1,\n\t\"sender\":\"senderId\",\n\t\"recipient\":\"recipientId\"\n}"
    )
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Json with response or error")
  ))
  def payment: Route = (path("payment") & post & withAuth) {
    json[TransferRequest] { p =>
      TransactionFactory.transferAsset(p, wallet, newTxHandler, time).map(_.json)
    }
  }
}
