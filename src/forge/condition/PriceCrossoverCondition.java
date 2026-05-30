package forge.condition;

import forge.engine.MarketContext;

import java.util.Objects;

public class PriceCrossoverCondition implements MarketCondition {
    private final ConditionDirection direction;
    private final long priceThresholdTicks;

    public PriceCrossoverCondition() {
        /*
         * Intent: Create a default condition instance for catalog/reflection discovery.
         * Precondition: None.
         * Returns: A constructed PriceCrossoverCondition instance.
         * Postcondition: Default condition is inert unless configured with a reachable threshold.
         */
        this(ConditionDirection.LONG, Long.MAX_VALUE);
    }

    public PriceCrossoverCondition(ConditionDirection direction, long priceThresholdTicks) {
        /*
         * Intent: Create a price crossover condition for long or short threshold checks.
         * Precondition: Direction must be LONG or SHORT and threshold must be a positive tick price.
         * Returns: A constructed PriceCrossoverCondition instance.
         * Postcondition: Condition can evaluate ticks against the configured threshold.
         */
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
        /*
         * Intent: Provide the stable config/display name for this condition.
         * Precondition: None.
         * Returns: PriceCrossover.
         * Postcondition: Condition state is unchanged.
         */
        return "PriceCrossover";
    }

    @Override
    public ConditionResult evaluate(MarketContext marketContext) {
        /*
         * Intent: Decide whether the current market price has crossed the configured threshold.
         * Precondition: Market context must contain the latest price in ticks.
         * Returns: Conditioned result with direction when crossed, otherwise notConditioned.
         * Postcondition: Condition and market context state are unchanged.
         */
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
