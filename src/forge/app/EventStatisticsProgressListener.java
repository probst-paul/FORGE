package forge.app;

public interface EventStatisticsProgressListener {
    EventStatisticsProgressListener NO_OP = progress -> {
    };

    void onProgress(EventStatisticsProgress progress);

    default void onContractProgress(String contractSymbol, EventStatisticsProgress progress) {
        /*
         * Intent: Optionally report progress for one contract inside a larger event-statistics run.
         * Precondition: contractSymbol may identify a selected contract window; progress must use that contract's units.
         * Returns: Nothing.
         * Postcondition: Default listeners ignore scoped progress and existing callers remain compatible.
         */
    }
}
