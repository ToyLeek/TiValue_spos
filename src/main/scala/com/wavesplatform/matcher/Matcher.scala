package com.wavesplatform.matcher

import java.io.File

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.stream.ActorMaterializer
import vsys.blockchain.UtxPool
import com.wavesplatform.matcher.api.MatcherApiRoute
import com.wavesplatform.matcher.market.{MatcherActor, MatcherTransactionWriter, OrderHistoryActor}
import vsys.settings.{BlockchainSettings, RestAPISettings}
import vsys.blockchain.state.reader.StateReader
import io.netty.channel.group.ChannelGroup
import vsys.api.http.CompositeHttpService
import vsys.blockchain.history.History
import vsys.utils.ScorexLogging
import vsys.wallet.Wallet

import scala.concurrent.Await
import scala.concurrent.duration._

class Matcher(actorSystem: ActorSystem,
              wallet: Wallet,
              utx: UtxPool,
              allChannels: ChannelGroup,
              stateReader: StateReader,
              history: History,
              blockchainSettings: BlockchainSettings,
              restAPISettings: RestAPISettings, matcherSettings: MatcherSettings) extends ScorexLogging {
  lazy val matcherApiRoutes = Seq(
    MatcherApiRoute(wallet, stateReader, matcher, orderHistory, txWriter, restAPISettings, matcherSettings)
  )

  lazy val matcherApiTypes: Set[Class[_]] = Set(
    classOf[MatcherApiRoute]
  )

  lazy val matcher: ActorRef = actorSystem.actorOf(MatcherActor.props(orderHistory, stateReader, wallet, utx, allChannels,
    matcherSettings, history, blockchainSettings.functionalitySettings), MatcherActor.name)

  lazy val orderHistory: ActorRef = actorSystem.actorOf(OrderHistoryActor.props(matcherSettings, utx, wallet),
    OrderHistoryActor.name)

  lazy val txWriter: ActorRef = actorSystem.actorOf(MatcherTransactionWriter.props(matcherSettings),
    MatcherTransactionWriter.name)

  @volatile var matcherServerBinding: ServerBinding = _

  def shutdownMatcher(): Unit = {
    Await.result(matcherServerBinding.unbind(), 10.seconds)
  }

  private def checkDirectory(directory: File): Unit = if (!directory.exists()) {
    log.error(s"Failed to create directory '${directory.getPath}'")
    sys.exit(1)
  }

  def runMatcher() {
    val journalDir = new File(matcherSettings.journalDataDir)
    val snapshotDir = new File(matcherSettings.snapshotsDataDir)
    journalDir.mkdirs()
    snapshotDir.mkdirs()

    checkDirectory(journalDir)
    checkDirectory(snapshotDir)

    log.info(s"Starting matcher on: ${matcherSettings.bindAddress}:${matcherSettings.port} ...")

    implicit val as = actorSystem
    implicit val materializer = ActorMaterializer()

    val combinedRoute = CompositeHttpService(actorSystem, matcherApiTypes, matcherApiRoutes, restAPISettings).compositeRoute
    matcherServerBinding = Await.result(Http().bindAndHandle(combinedRoute, matcherSettings.bindAddress,
      matcherSettings.port), 5.seconds)

    log.info(s"Matcher bound to ${matcherServerBinding.localAddress} ")
  }

}
