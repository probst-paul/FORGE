package forge.stop;

import java.util.Objects;

public class StopResult {
    private final boolean stopped;
    private final StopReason reason;
    private final long exitPriceTicks;

    private StopResult(boolean stopped, StopReason reason, long exitPriceTicks) {
        this.stopped = stopped;
        this.reason = Objects.requireNonNull(reason, "reason is required");
        if (!stopped && reason != StopReason.NONE) {
            throw new IllegalArgumentException("reason must be NONE when stop condition is false");
        }
        if (stopped && reason == StopReason.NONE) {
            throw new IllegalArgumentException("reason must be PRICE or TIME when stop condition is true");
        }
        if (stopped && exitPriceTicks <= 0) {
            throw new IllegalArgumentException("exitPriceTicks must be greater than zero when stop condition is true");
        }
        if (!stopped && exitPriceTicks != 0) {
            throw new IllegalArgumentException("exitPriceTicks must be zero when stop condition is false");
        }
        this.exitPriceTicks = exitPriceTicks;
    }

    public static StopResult stopped(StopReason reason, long exitPriceTicks) {
        return new StopResult(true, reason, exitPriceTicks);
    }

    public static StopResult notStopped() {
        return new StopResult(false, StopReason.NONE, 0);
    }

    public boolean isStopped() {
        return stopped;
    }

    public StopReason getReason() {
        return reason;
    }

    public long getExitPriceTicks() {
        return exitPriceTicks;
    }
}
