package forge.strategy;

import forge.data.market.TradeTick;
import forge.engine.MarketContext;
import forge.event.MarketEventOccurrence;
import forge.feature.SessionRangeFeature;
import forge.feature.TpoPeriod;
import forge.feature.TradingDayContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class StrategyContext {
    private final MarketContext marketContext;
    private final TradeTick currentTick;
    private final TradingDayContext tradingDayContext;
    private final TpoPeriod tpoPeriod;
    private final SessionRangeFeature sessionRangeFeature;
    private final List<MarketEventOccurrence> currentEvents;

    public StrategyContext(
            MarketContext marketContext,
            TradeTick currentTick,
            TradingDayContext tradingDayContext,
            TpoPeriod tpoPeriod,
            SessionRangeFeature sessionRangeFeature,
            List<MarketEventOccurrence> currentEvents
    ) {
        /*
         * Intent: Package all per-tick strategy inputs into one immutable context.
         * Precondition: Core context, tick, trading-day context, and TPO period must be present.
         * Returns: Constructed StrategyContext.
         * Postcondition: Current events are normalized into an immutable non-null list.
         */
        this.marketContext = Objects.requireNonNull(marketContext, "marketContext is required");
        this.currentTick = Objects.requireNonNull(currentTick, "currentTick is required");
        this.tradingDayContext = Objects.requireNonNull(tradingDayContext, "tradingDayContext is required");
        this.tpoPeriod = Objects.requireNonNull(tpoPeriod, "tpoPeriod is required");
        this.sessionRangeFeature = sessionRangeFeature;
        this.currentEvents = normalizeEvents(currentEvents);
    }

    public MarketContext getMarketContext() {
        return marketContext;
    }

    public TradeTick getCurrentTick() {
        return currentTick;
    }

    public TradingDayContext getTradingDayContext() {
        return tradingDayContext;
    }

    public TpoPeriod getTpoPeriod() {
        return tpoPeriod;
    }

    public Optional<SessionRangeFeature> getSessionRangeFeature() {
        return Optional.ofNullable(sessionRangeFeature);
    }

    public List<MarketEventOccurrence> getCurrentEvents() {
        return currentEvents;
    }

    public boolean hasOpenPosition() {
        return marketContext.hasOpenPosition();
    }

    private List<MarketEventOccurrence> normalizeEvents(List<MarketEventOccurrence> events) {
        /*
         * Intent: Convert optional current-event input into a safe immutable list.
         * Precondition: events may be null and may contain null entries.
         * Returns: Empty list or unmodifiable list of non-null events.
         * Postcondition: Strategy code can iterate events without null-list checks.
         */
        if (events == null || events.isEmpty()) {
            return Collections.emptyList();
        }
        List<MarketEventOccurrence> normalized = new ArrayList<>();
        for (MarketEventOccurrence event : events) {
            if (event != null) {
                normalized.add(event);
            }
        }
        return Collections.unmodifiableList(normalized);
    }
}
