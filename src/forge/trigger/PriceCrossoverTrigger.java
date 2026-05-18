package forge.trigger;

import forge.engine.MarketContext;

import java.util.Objects;

public class PriceCrossoverTrigger implements TradeTrigger {
    private final TriggerDirection direction;
    private final long priceThresholdTicks;

    public PriceCrossoverTrigger() {
        this(TriggerDirection.LONG, Long.MAX_VALUE);
    }

    public PriceCrossoverTrigger(TriggerDirection direction, long priceThresholdTicks) {
        if (direction != TriggerDirection.LONG && direction != TriggerDirection.SHORT) {
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
    public TriggerResult evaluate(MarketContext marketContext) {
        Objects.requireNonNull(marketContext, "marketContext is required");
        long lastPriceTicks = marketContext.getLastPriceTicks();
        boolean crossed = direction == TriggerDirection.LONG
                ? lastPriceTicks >= priceThresholdTicks
                : lastPriceTicks <= priceThresholdTicks;
        if (!crossed) {
            return TriggerResult.notTriggered();
        }
        return TriggerResult.triggered(direction);
    }

    public TriggerDirection getDirection() {
        return direction;
    }

    public long getPriceThresholdTicks() {
        return priceThresholdTicks;
    }
}
