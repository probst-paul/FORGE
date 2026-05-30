package forge.app;

public class BacktestProgress {
    private final long processedTicks;
    private final long totalTicks;

    public BacktestProgress(long processedTicks, long totalTicks) {
        /*
         * Intent: Capture backtest tick-processing state for CLI and GUI progress displays.
         * Precondition: Processed and total tick counts must be nonnegative; processed cannot exceed total.
         * Returns: A constructed BacktestProgress instance.
         * Postcondition: Progress counts are valid and immutable.
         */
        if (processedTicks < 0) {
            throw new IllegalArgumentException("processedTicks cannot be negative");
        }
        if (totalTicks < 0) {
            throw new IllegalArgumentException("totalTicks cannot be negative");
        }
        if (processedTicks > totalTicks) {
            throw new IllegalArgumentException("processedTicks cannot exceed totalTicks");
        }
        this.processedTicks = processedTicks;
        this.totalTicks = totalTicks;
    }

    public long getProcessedTicks() {
        return processedTicks;
    }

    public long getTotalTicks() {
        return totalTicks;
    }

    /*
     * Intent: Convert processed/total ticks into a normalized completion ratio.
     * Precondition: BacktestProgress has validated nonnegative counts.
     * Returns: A value from 0.0 through 1.0, treating zero-total work as complete.
     * Postcondition: Object state is unchanged.
     */
    public double getCompletionRatio() {
        if (totalTicks == 0) {
            return 1.0;
        }
        return (double) processedTicks / totalTicks;
    }

    /*
     * Intent: Convert the completion ratio into a rounded whole-number percentage.
     * Precondition: BacktestProgress has validated nonnegative counts.
     * Returns: Integer percent from 0 through 100.
     * Postcondition: Object state is unchanged.
     */
    public int getCompletionPercent() {
        return (int) Math.round(getCompletionRatio() * 100.0);
    }
}
