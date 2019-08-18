package vsys.blockchain.state

import java.util.concurrent.locks.ReentrantReadWriteLock

import vsys.blockchain.history.HistoryWriterImpl
import vsys.settings.FunctionalitySettings
import vsys.blockchain.state.reader.{CompositeStateReader, StateReader}
import vsys.blockchain.block.Block
import vsys.settings.TestFunctionalitySettings
import vsys.blockchain.history.History
import vsys.blockchain.transaction.{Transaction, ValidationError}
import vsys.blockchain.contract.ExecutionContext
import vsys.blockchain.state.opcdiffs.{OpcDiff, OpcFuncDiffer}
import vsys.blockchain.transaction.contract.{ExecuteContractFunctionTransaction, RegisterContractTransaction}
import vsys.blockchain.history.db
import vsys.settings.TestStateSettings

package object diffs {

  private val lock = new ReentrantReadWriteLock()

  def newState(): StateWriterImpl = new StateWriterImpl(StateStorage(db, dropExisting = true), lock, TestStateSettings.AllOn)

  def newHistory(): History = new HistoryWriterImpl(db, lock, true)

  val ENOUGH_AMT: Long = Long.MaxValue / 3

  def assertDiffEi(preconditions: Seq[Block], block: Block, fs: FunctionalitySettings = TestFunctionalitySettings.Enabled)(assertion: Either[ValidationError, BlockDiff] => Unit): Unit = {
    val differ: (StateReader,  Block) => Either[ValidationError, BlockDiff] = (s, b) => BlockDiffer.fromBlock(fs, s, None)(b)

    val state = newState()

    preconditions.foreach { precondition =>
      val preconditionDiff = differ(state,  precondition).explicitGet()
      state.applyBlockDiff(preconditionDiff)
    }
    val totalDiff1 = differ(state,  block)
    assertion(totalDiff1)

    val preconditionDiff = BlockDiffer.unsafeDiffMany(fs, newState(), None)(preconditions)
    val compositeState = new CompositeStateReader(newState(), preconditionDiff)
    val totalDiff2 = differ(compositeState, block)
    assertion(totalDiff2)
  }

  def assertDiffAndState(preconditions: Seq[Block], block: Block, fs: FunctionalitySettings = TestFunctionalitySettings.Enabled)(assertion: (BlockDiff, StateReader) => Unit): Unit = {
    val differ: (StateReader, Block) => Either[ValidationError, BlockDiff] = (s, b) => BlockDiffer.fromBlock(fs, s, None)(b)

    val state = newState()
    preconditions.foreach { precondition =>
      val preconditionDiff = differ(state,  precondition).explicitGet()
      state.applyBlockDiff(preconditionDiff)
    }
    val totalDiff1 = differ(state,  block).explicitGet()
    state.applyBlockDiff(totalDiff1)
    assertion(totalDiff1, state)

    val preconditionDiff = BlockDiffer.unsafeDiffMany(fs, newState(), None)(preconditions)
    val compositeState = new CompositeStateReader(newState(), preconditionDiff)
    val totalDiff2 = differ(compositeState,  block).explicitGet()
    assertion(totalDiff2, new CompositeStateReader(compositeState, totalDiff2))
  }

  def produce(errorMessage: String): ProduceError = new ProduceError(errorMessage)

  def assertOpcFuncDifferEi(height: Int, tx: Transaction)(assertion: Either[ValidationError, OpcDiff] => Unit): Unit = {
    val state = newState()

    tx match {
      case tx: RegisterContractTransaction
      => assertion(OpcFuncDiffer(ExecutionContext.fromRegConTx(state, height, tx).right.get)(tx.data))
      case tx: ExecuteContractFunctionTransaction
      => assertion(OpcFuncDiffer(ExecutionContext.fromExeConTx(state, height, tx).right.get)(tx.data))
    }
  }
}
