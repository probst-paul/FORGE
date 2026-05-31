package forge.event;

import forge.engine.MarketContext;

import java.util.Objects;

public class PriceCrossoverEvent implements MarketEvent {
    private final EventDirection direction;
    private final long priceThresholdTicks;

    public PriceCrossoverEvent() {
        /*
         * Intent: Create a default event instance for catalog/reflection discovery.
         * Precondition: None.
         * Returns: A constructed PriceCrossoverEvent instance.
         * Postcondition: Default event is inert unless configured with a reachable threshold.
         */
        this(EventDirection.LONG, Long.MAX_VALUE);
    }

    public PriceCrossoverEvent(EventDirection direction, long priceThresholdTicks) {
        /*
         * Intent: Create a price crossover event for long or short threshold checks.
         * Precondition: Direction must be LONG or SHORT and threshold must be a positive tick price.
         * Returns: A constructed PriceCrossoverEvent instance.
         * Postcondition: Event can evaluate ticks against the configured threshold.
         */
        if (direction != EventDirection.LONG && direction != EventDirection.SHORT) {
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
         * Intent: Provide the stable config/display name for this event.
         * Precondition: None.
         * Returns: PriceCrossover.
         * Postcondition: Event state is unchanged.
         */
        return "PriceCrossover";
    }

    @Override
    public EventResult evaluate(MarketContext marketContext) {
        /*
         * Intent: Decide whether the current market price has crossed the configured threshold.
         * Precondition: Market context must contain the latest price in ticks.
         * Returns: Conditioned result with direction when crossed, otherwise notConditioned.
         * Postcondition: Condition and market context state are unchanged.
         */
        Objects.requireNonNull(marketContext, "marketContext is required");
        long lastPriceTicks = marketContext.getLastPriceTicks();
        boolean crossed = direction == EventDirection.LONG
                ? lastPriceTicks >= priceThresholdTicks
                : lastPriceTicks <= priceThresholdTicks;
        if (!crossed) {
            return EventResult.notConditioned();
        }
        return EventResult.conditioned(direction);
    }

    public EventDirection getDirection() {
        return direction;
    }

    public long getPriceThresholdTicks() {
        return priceThresholdTicks;
    }
}
