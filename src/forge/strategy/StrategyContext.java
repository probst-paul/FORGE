package forge.strategy;

import forge.data.market.TradeTick;
import forge.engine.MarketContext;
import forge.condition.MarketConditionOccurrence;
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
    private final List<MarketConditionOccurrence> currentEvents;

    public StrategyContext(
            MarketContext marketContext,
            TradeTick currentTick,
            TradingDayContext tradingDayContext,
            TpoPeriod tpoPeriod,
            SessionRangeFeature sessionRangeFeature,
            List<MarketConditionOccurrence> currentEvents
    ) {
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

    public List<MarketConditionOccurrence> getCurrentEvents() {
        return currentEvents;
    }

    public boolean hasOpenPosition() {
        return marketContext.hasOpenPosition();
    }

    private List<MarketConditionOccurrence> normalizeEvents(List<MarketConditionOccurrence> events) {
        if (events == null || events.isEmpty()) {
            return Collections.emptyList();
        }
        List<MarketConditionOccurrence> normalized = new ArrayList<>();
        for (MarketConditionOccurrence event : events) {
            if (event != null) {
                normalized.add(event);
            }
        }
        return Collections.unmodifiableList(normalized);
    }
}
