package forge.condition;

import forge.data.market.TradeTick;
import forge.data.market.TradeTickStreamProcessor;
import forge.feature.SessionRangeFeatureCalculator;
import forge.feature.SessionRangeFeature;
import forge.feature.TradingDayClassifier;
import forge.feature.TradingDayContext;
import forge.feature.TradingSession;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
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

        Accumulator accumulator = new Accumulator(featureByKey);
        for (TradeTick tick : ticks) {
            accumulator.onTick(tick);
        }
        return accumulator.getEvents();
    }

    public Accumulator newAccumulator(Collection<SessionRangeFeature> sessionRangeFeatures) {
        if (sessionRangeFeatures == null) {
            throw new IllegalArgumentException("sessionRangeFeatures is required");
        }

        Map<EventKey, SessionRangeFeature> featureByKey = new HashMap<>();
        for (SessionRangeFeature feature : sessionRangeFeatures) {
            if (feature == null) {
                continue;
            }
            featureByKey.put(new EventKey(feature.getContractSymbol(), feature.getSessionDate()), feature);
        }
        return new Accumulator(featureByKey);
    }

    public LiveAccumulator newLiveAccumulator(SessionRangeFeatureCalculator.Accumulator sessionRangeAccumulator) {
        if (sessionRangeAccumulator == null) {
            throw new IllegalArgumentException("sessionRangeAccumulator is required");
        }
        return new LiveAccumulator(sessionRangeAccumulator);
    }

    public class Accumulator implements TradeTickStreamProcessor {
        private final Map<EventKey, SessionRangeFeature> featureByKey;
        private final List<MarketConditionOccurrence> events = new ArrayList<>();
        private final Map<EventKey, MarketConditionOccurrence> firstEventByKey = new HashMap<>();

        private Accumulator(Map<EventKey, SessionRangeFeature> featureByKey) {
            this.featureByKey = featureByKey;
        }

        @Override
        public void onTick(TradeTick tick) {
            if (tick == null) {
                return;
            }
            TradingDayContext context = tradingDayClassifier.classify(tick.getTradeDateTime());
            if (context.getSession() != TradingSession.RTH) {
                return;
            }

            EventKey key = new EventKey(tick.getContractSymbol(), context.getTradingDay());
            if (firstEventByKey.containsKey(key)) {
                return;
            }

            SessionRangeFeature feature = featureByKey.get(key);
            if (feature == null) {
                return;
            }

            MarketConditionOccurrence event = detectBreach(feature, tick, context.getTradingDay());
            if (event != null) {
                firstEventByKey.put(key, event);
                events.add(event);
            }
        }

        public List<MarketConditionOccurrence> getEvents() {
            return events;
        }
    }

    public class LiveAccumulator implements TradeTickStreamProcessor {
        private final SessionRangeFeatureCalculator.Accumulator sessionRangeAccumulator;
        private final List<MarketConditionOccurrence> events = new ArrayList<>();
        private final Map<EventKey, MarketConditionOccurrence> firstEventByKey = new HashMap<>();

        private LiveAccumulator(SessionRangeFeatureCalculator.Accumulator sessionRangeAccumulator) {
            this.sessionRangeAccumulator = sessionRangeAccumulator;
        }

        @Override
        public void onTick(TradeTick tick) {
            if (tick == null) {
                return;
            }
            TradingDayContext context = tradingDayClassifier.classify(tick.getTradeDateTime());
            if (context.getSession() != TradingSession.RTH) {
                return;
            }

            EventKey key = new EventKey(tick.getContractSymbol(), context.getTradingDay());
            if (firstEventByKey.containsKey(key)
                    || !sessionRangeAccumulator.hasFirstHourRange(tick.getContractSymbol(), context.getTradingDay())) {
                return;
            }

            MarketConditionOccurrence event = detectBreach(
                    tick,
                    context.getTradingDay(),
                    sessionRangeAccumulator.getFirstHourLowTicks(tick.getContractSymbol(), context.getTradingDay()),
                    sessionRangeAccumulator.getFirstHourHighTicks(tick.getContractSymbol(), context.getTradingDay())
            );
            if (event != null) {
                firstEventByKey.put(key, event);
                events.add(event);
            }
        }

        public List<MarketConditionOccurrence> getEvents() {
            return events;
        }
    }

    private MarketConditionOccurrence detectBreach(SessionRangeFeature feature, TradeTick tick, LocalDate sessionDate) {
        return detectBreach(tick, sessionDate, feature.getFirstHourLowTicks(), feature.getFirstHourHighTicks());
    }

    private MarketConditionOccurrence detectBreach(
            TradeTick tick,
            LocalDate sessionDate,
            long firstHourLowTicks,
            long firstHourHighTicks
    ) {
        if (tick.getPriceTicks() >= firstHourHighTicks) {
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
        if (tick.getPriceTicks() <= firstHourLowTicks) {
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
