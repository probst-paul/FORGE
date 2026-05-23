package forge.condition;

import forge.data.market.TradeTick;
import forge.feature.SessionRangeFeature;
import forge.feature.TradingDayClassifier;
import forge.feature.TradingDayContext;
import forge.feature.TradingSession;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirstHourBreachConditionDetector implements ConditionDetector {
    private final TradingDayClassifier tradingDayClassifier;

    public FirstHourBreachConditionDetector() {
        this(new TradingDayClassifier());
    }

    public FirstHourBreachConditionDetector(TradingDayClassifier tradingDayClassifier) {
        if (tradingDayClassifier == null) {
            throw new IllegalArgumentException("tradingDayClassifier is required");
        }
        this.tradingDayClassifier = tradingDayClassifier;
    }

    @Override
    public ConditionDefinition getDefinition() {
        return new FirstHourBreachCondition();
    }

    public List<MarketConditionOccurrence> detect(
            Collection<SessionRangeFeature> sessionRangeFeatures,
            Collection<TradeTick> ticks
    ) {
        if (sessionRangeFeatures == null) {
            throw new IllegalArgumentException("sessionRangeFeatures is required");
        }
        if (ticks == null) {
            throw new IllegalArgumentException("ticks is required");
        }

        Map<EventKey, SessionRangeFeature> featureByKey = new HashMap<>();
        for (SessionRangeFeature feature : sessionRangeFeatures) {
            if (feature == null) {
                continue;
            }
            featureByKey.put(new EventKey(feature.getContractSymbol(), feature.getSessionDate()), feature);
        }

        List<TradeTick> sortedTicks = new ArrayList<>();
        for (TradeTick tick : ticks) {
            if (tick != null) {
                sortedTicks.add(tick);
            }
        }
        sortedTicks.sort(Comparator
                .comparing(TradeTick::getTradeDateTime)
                .thenComparingLong(TradeTick::getScidRecordIndex));

        List<MarketConditionOccurrence> events = new ArrayList<>();
        Map<EventKey, MarketConditionOccurrence> firstEventByKey = new HashMap<>();
        for (TradeTick tick : sortedTicks) {
            TradingDayContext context = tradingDayClassifier.classify(tick.getTradeDateTime());
            if (context.getSession() != TradingSession.RTH) {
                continue;
            }

            EventKey key = new EventKey(tick.getContractSymbol(), context.getTradingDay());
            if (firstEventByKey.containsKey(key)) {
                continue;
            }

            SessionRangeFeature feature = featureByKey.get(key);
            if (feature == null) {
                continue;
            }

            MarketConditionOccurrence event = detectBreach(feature, tick, context.getTradingDay());
            if (event != null) {
                firstEventByKey.put(key, event);
                events.add(event);
            }
        }

        return events;
    }

    private MarketConditionOccurrence detectBreach(SessionRangeFeature feature, TradeTick tick, LocalDate sessionDate) {
        if (tick.getPriceTicks() >= feature.getFirstHourHighTicks()) {
            return new MarketConditionOccurrence(
                    tick.getContractSymbol(),
                    sessionDate,
                    FirstHourBreachCondition.EVENT_NAME,
                    FirstHourBreachCondition.EVENT_VERSION,
                    ConditionSide.LONG,
                    tick.getTradeDateTime(),
                    tick.getPriceTicks()
            );
        }
        if (tick.getPriceTicks() <= feature.getFirstHourLowTicks()) {
            return new MarketConditionOccurrence(
                    tick.getContractSymbol(),
                    sessionDate,
                    FirstHourBreachCondition.EVENT_NAME,
                    FirstHourBreachCondition.EVENT_VERSION,
                    ConditionSide.SHORT,
                    tick.getTradeDateTime(),
                    tick.getPriceTicks()
            );
        }
        return null;
    }

    private static class EventKey {
        private final String contractSymbol;
        private final LocalDate sessionDate;

        private EventKey(String contractSymbol, LocalDate sessionDate) {
            this.contractSymbol = contractSymbol;
            this.sessionDate = sessionDate;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof EventKey)) {
                return false;
            }
            EventKey that = (EventKey) other;
            return contractSymbol.equals(that.contractSymbol) && sessionDate.equals(that.sessionDate);
        }

        @Override
        public int hashCode() {
            int result = contractSymbol.hashCode();
            result = 31 * result + sessionDate.hashCode();
            return result;
        }
    }
}
