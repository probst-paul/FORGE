package forge.strategy.support;

public class PriceRange {
    private final long lowPriceTicks;
    private final long highPriceTicks;

    public PriceRange(long lowPriceTicks, long highPriceTicks) {
        if (highPriceTicks < lowPriceTicks) {
            throw new IllegalArgumentException("highPriceTicks must be greater than or equal to lowPriceTicks");
        }
        this.lowPriceTicks = lowPriceTicks;
        this.highPriceTicks = highPriceTicks;
    }

    public long getLowPriceTicks() {
        return lowPriceTicks;
    }

    public long getHighPriceTicks() {
        return highPriceTicks;
    }

    public long getRangeTicks() {
        return highPriceTicks - lowPriceTicks;
    }

    public double getLowPrice(double tickSize) {
        validateTickSize(tickSize);
        return lowPriceTicks * tickSize;
    }

    public double getHighPrice(double tickSize) {
        validateTickSize(tickSize);
        return highPriceTicks * tickSize;
    }

    private void validateTickSize(double tickSize) {
        if (tickSize <= 0) {
            throw new IllegalArgumentException("tickSize must be greater than zero");
        }
    }
}
