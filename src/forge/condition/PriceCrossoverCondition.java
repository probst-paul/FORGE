package forge.condition;

import forge.engine.MarketContext;

import java.util.Objects;

public class PriceCrossoverCondition implements MarketCondition {
    private final ConditionDirection direction;
    private final long priceThresholdTicks;

    public PriceCrossoverCondition() {
        this(ConditionDirection.LONG, Long.MAX_VALUE);
    }

    public PriceCrossoverCondition(ConditionDirection direction, long priceThresholdTicks) {
        if (direction != ConditionDirection.LONG && direction != ConditionDirection.SHORT) {
            throw new IllegalArgumentException("direction must be LONG or SHORT");
        }
        if (priceThresholdTicks <= 0) {
            throw new IllegalArgumentException("priceThresholdTicks must be greater than zero");
        }
        this.direction = direction;
        this.priceThresholdTicks = priceThresholdTicks;
    }

    @Override
    public String getName() {
        return "PriceCrossover";
    }

    @Override
    public ConditionResult evaluate(MarketContext marketContext) {
        Objects.requireNonNull(marketContext, "marketContext is required");
        long lastPriceTicks = marketContext.getLastPriceTicks();
        boolean crossed = direction == ConditionDirection.LONG
                ? lastPriceTicks >= priceThresholdTicks
                : lastPriceTicks <= priceThresholdTicks;
        if (!crossed) {
            return ConditionResult.notConditioned();
        }
        return ConditionResult.conditioned(direction);
    }

    public ConditionDirection getDirection() {
        return direction;
    }

    public long getPriceThresholdTicks() {
        return priceThresholdTicks;
    }
}
