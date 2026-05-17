package forge.app;

public class BacktestProgress {
    private final long processedTicks;
    private final long totalTicks;

    public BacktestProgress(long processedTicks, long totalTicks) {
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

    public double getCompletionRatio() {
        if (totalTicks == 0) {
            return 1.0;
        }
        return (double) processedTicks / totalTicks;
    }

    public int getCompletionPercent() {
        return (int) Math.round(getCompletionRatio() * 100.0);
    }
}
