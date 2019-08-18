package vsys.api.http.alias

import com.typesafe.config.ConfigFactory
import io.netty.channel.group.ChannelGroup
import org.scalamock.scalatest.PathMockFactory
import org.scalatest.prop.PropertyChecks
import play.api.libs.json.Json._
import play.api.libs.json._
import vsys.api.http._
import vsys.api.http.ApiMarshallers._
import vsys.blockchain.UtxPool
import vsys.blockchain.state.diffs.TransactionDiffer.TransactionValidationError
import vsys.blockchain.transaction.ValidationError.GenericError
import vsys.blockchain.transaction.Transaction
import vsys.settings.RestAPISettings


class AliasBroadcastRouteSpec extends RouteSpec("/alias/broadcast/") with RequestGen with PathMockFactory with PropertyChecks {
  private val settings = RestAPISettings.fromConfig(ConfigFactory.load())
  private val utx = stub[UtxPool]
  private val allChannels = stub[ChannelGroup]

  (utx.putIfNew _).when(*).onCall((t: Transaction) => Left(TransactionValidationError(GenericError("foo"), t))).anyNumberOfTimes()

  "returns StateCheckFiled" - {
    val route = AliasBroadcastApiRoute(settings, utx, allChannels).route

    def posting(url: String, v: JsValue): RouteTestResult = Post(routePath(url), v) ~> route

    "when state validation fails" in {
      forAll(createAliasGen) { (t: Transaction) =>
        posting("create", t.json) should produce(StateCheckFailed(t, "foo"))
      }
    }
  }

  "returns appropriate error code when validation fails for" - {
    val route = AliasBroadcastApiRoute(settings, utx, allChannels).route

    "create alias transaction" in forAll(createAliasReq) { req =>
      import vsys.api.http.alias.SignedCreateAliasRequest.broadcastAliasRequestReadsFormat

      def posting(v: JsValue): RouteTestResult = Post(routePath("create"), v) ~> route

      forAll(invalidBase58) { s => posting(toJson(req.copy(senderPublicKey = s))) should produce(InvalidAddress) }
      forAll(nonPositiveLong) { q => posting(toJson(req.copy(fee = q))) should produce(InsufficientFee) }
      forAll(invalidAliasStringByLength) { q =>
        val obj = toJson(req).as[JsObject] ++ Json.obj("alias" -> JsString(q))
        posting(obj) should produce(CustomValidationError(s"Alias '$q' length should be between 4 and 30"))
      }
    }
  }
}
