package vsys.blockchain.state

import vsys.account.Address

object Hash {
  def accountPortfolios(accountPortfolios: Map[Address, Portfolio]): Int = {
    def accountPortfolioHash(accP: (Address, Portfolio)): Int = {
      val (account, portfolio) = accP
      val h = account.hashCode() + portfolio.balance.hashCode() + portfolio.leaseInfo.hashCode()
      portfolio.assets.foldLeft(h) {
        case (h, (acc, balance)) =>
          h + acc.hashCode() + balance.hashCode()
      }
    }

    accountPortfolios.foldLeft(0) { case (hash, (account, portfolio)) =>
      hash + accountPortfolioHash((account, portfolio))
    }
  }
}
