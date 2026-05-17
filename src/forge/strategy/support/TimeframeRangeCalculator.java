package forge.strategy.support;

import forge.data.market.TradeTick;

import java.time.Instant;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

public class TimeframeRangeCalculator {
    public Optional<PriceRange> calculatePriceRange(
            Collection<TradeTick> ticks,
            Instant startInclusive,
            Instant endExclusive
    ) {
        Objects.requireNonNull(ticks, "ticks is required");
        Objects.requireNonNull(startInclusive, "startInclusive is required");
        Objects.requireNonNull(endExclusive, "endExclusive is required");
        if (!endExclusive.isAfter(startInclusive)) {
            throw new IllegalArgumentException("endExclusive must be after startInclusive");
        }

        Long lowPriceTicks = null;
        Long highPriceTicks = null;
        for (TradeTick tick : ticks) {
            if (tick == null || !isInsideTimeframe(tick, startInclusive, endExclusive)) {
                continue;
            }

            long priceTicks = tick.getPriceTicks();
            if (lowPriceTicks == null || priceTicks < lowPriceTicks) {
                lowPriceTicks = priceTicks;
            }
            if (highPriceTicks == null || priceTicks > highPriceTicks) {
                highPriceTicks = priceTicks;
            }
        }

        if (lowPriceTicks == null) {
            return Optional.empty();
        }
        return Optional.of(new PriceRange(lowPriceTicks, highPriceTicks));
    }

    private boolean isInsideTimeframe(TradeTick tick, Instant startInclusive, Instant endExclusive) {
        Instant timestamp = tick.getTradeDateTime();
        return !timestamp.isBefore(startInclusive) && timestamp.isBefore(endExclusive);
    }
}
