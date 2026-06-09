package forge.app;

public interface BacktestProgressListener {
    BacktestProgressListener NO_OP = progress -> {
    };

    void onProgress(BacktestProgress progress);

    default void onContractProgress(String contractSymbol, BacktestProgress progress) {
        /*
         * Intent: Optionally report progress for one contract inside a larger backtest run.
         * Precondition: contractSymbol may identify a selected contract window; progress must use that contract's units.
         * Returns: Nothing.
         * Postcondition: Default listeners ignore scoped progress and existing callers remain compatible.
         */
    }
}
