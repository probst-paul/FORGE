package forge.target;

public class TargetResult {
    private final long targetPriceTicks;
    private final long stopPriceTicks;

    public TargetResult(long targetPriceTicks, long stopPriceTicks) {
        if (targetPriceTicks <= 0) {
            throw new IllegalArgumentException("targetPriceTicks must be greater than zero");
        }
        if (stopPriceTicks <= 0) {
            throw new IllegalArgumentException("stopPriceTicks must be greater than zero");
        }

        this.targetPriceTicks = targetPriceTicks;
        this.stopPriceTicks = stopPriceTicks;
    }

    public long getTargetPriceTicks() {
        return targetPriceTicks;
    }

    public long getStopPriceTicks() {
        return stopPriceTicks;
    }

    public double getTargetPrice(double tickSize) {
        validateTickSize(tickSize);
        return targetPriceTicks * tickSize;
    }

    public double getStopPrice(double tickSize) {
        validateTickSize(tickSize);
        return stopPriceTicks * tickSize;
    }

    private void validateTickSize(double tickSize) {
        if (tickSize <= 0) {
            throw new IllegalArgumentException("tickSize must be greater than zero");
        }
    }

    @Override
    public String toString() {
        return "TargetResult{" +
                "targetPriceTicks=" + targetPriceTicks +
                ", stopPriceTicks=" + stopPriceTicks +
                '}';
    }
}
