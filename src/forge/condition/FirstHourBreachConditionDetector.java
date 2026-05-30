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
        /*
         * Intent: Create the detector with the default trading-day classifier.
         * Precondition: Default classifier dependencies must be available.
         * Returns: A constructed FirstHourBreachConditionDetector instance.
         * Postcondition: Detector can classify ticks into trading sessions and days.
         */
        this(new TradingDayClassifier());
    }

    public FirstHourBreachConditionDetector(TradingDayClassifier tradingDayClassifier) {
        /*
         * Intent: Create the detector with an explicit trading-day classifier.
         * Precondition: Classifier must not be null.
         * Returns: A constructed FirstHourBreachConditionDetector instance.
         * Postcondition: All tick/session classification uses the supplied classifier.
         */
        if (tradingDayClassifier == null) {
            throw new IllegalArgumentException("tradingDayClassifier is required");
        }
        this.tradingDayClassifier = tradingDayClassifier;
    }

    @Override
    public ConditionDefinition getDefinition() {
        /*
         * Intent: Identify the market condition produced by this detector.
         * Precondition: None.
         * Returns: First-hour breach condition definition.
         * Postcondition: Detector state is unchanged.
         */
        return new FirstHourBreachCondition();
    }

    public List<MarketConditionOccurrence> detect(
            Collection<SessionRangeFeature> sessionRangeFeatures,
            Collection<TradeTick> ticks
    ) {
        /*
         * Intent: Batch-detect first-hour breach occurrences from prebuilt session ranges and ticks.
         * Precondition: Collections must be non-null; ticks should be in market-time order for deterministic first breach selection.
         * Returns: First breach occurrence per contract/session where price crosses the first-hour range.
         * Postcondition: Source collections are not modified.
         */
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
        /*
         * Intent: Prepare streaming first-hour breach detection using already-built session range features.
         * Precondition: Session range features must be non-null.
         * Returns: Accumulator ready to consume ordered trade ticks.
         * Postcondition: Feature lookup is indexed by contract/session for efficient tick processing.
         */
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
        /*
         * Intent: Prepare streaming breach detection that shares a live session-range accumulator.
         * Precondition: Session range accumulator must be non-null.
         * Returns: LiveAccumulator ready to consume ordered trade ticks.
         * Postcondition: Breaches are detected only after first-hour range data is available for a session.
         */
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
            /*
             * Intent: Create an accumulator with fast session range lookup.
             * Precondition: Feature map should be keyed by contract symbol and trading day.
             * Returns: A constructed Accumulator instance.
             * Postcondition: No ticks have been processed and no events are recorded.
             */
            this.featureByKey = featureByKey;
        }

        @Override
        public void onTick(TradeTick tick) {
            /*
             * Intent: Process one ordered trade tick and record the first RTH breach for its contract/session.
             * Precondition: Tick may be null; non-null ticks should arrive in chronological order.
             * Returns: Nothing.
             * Postcondition: At most one new occurrence is recorded for the tick's contract/session.
             */
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
            /*
             * Intent: Expose detected market condition occurrences accumulated so far.
             * Precondition: None.
             * Returns: Mutable list of recorded occurrences.
             * Postcondition: Accumulator state is not changed by this accessor.
             */
            return events;
        }
    }

    public class LiveAccumulator implements TradeTickStreamProcessor {
        private final SessionRangeFeatureCalculator.Accumulator sessionRangeAccumulator;
        private final List<MarketConditionOccurrence> events = new ArrayList<>();
        private final Map<EventKey, MarketConditionOccurrence> firstEventByKey = new HashMap<>();

        private LiveAccumulator(SessionRangeFeatureCalculator.Accumulator sessionRangeAccumulator) {
            /*
             * Intent: Create a live accumulator tied to an in-progress session range calculation.
             * Precondition: Session range accumulator must be ready to receive the same ordered tick stream.
             * Returns: A constructed LiveAccumulator instance.
             * Postcondition: No breach events are recorded yet.
             */
            this.sessionRangeAccumulator = sessionRangeAccumulator;
        }

        @Override
        public void onTick(TradeTick tick) {
            /*
             * Intent: Process one ordered trade tick using live-built first-hour ranges.
             * Precondition: Tick may be null; non-null ticks should arrive in chronological order.
             * Returns: Nothing.
             * Postcondition: First breach is recorded only when the first-hour range exists for that session.
             */
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
            /*
             * Intent: Expose live-detected market condition occurrences accumulated so far.
             * Precondition: None.
             * Returns: Mutable list of recorded occurrences.
             * Postcondition: Live accumulator state is not changed by this accessor.
             */
            return events;
        }
    }

    private MarketConditionOccurrence detectBreach(SessionRangeFeature feature, TradeTick tick, LocalDate sessionDate) {
        /*
         * Intent: Detect a breach using the range values stored on a session feature.
         * Precondition: Feature, tick, and session date should refer to the same contract/session.
         * Returns: Occurrence when price breaches the range, otherwise null.
         * Postcondition: Inputs are unchanged.
         */
        return detectBreach(tick, sessionDate, feature.getFirstHourLowTicks(), feature.getFirstHourHighTicks());
    }

    private MarketConditionOccurrence detectBreach(
            TradeTick tick,
            LocalDate sessionDate,
            long firstHourLowTicks,
            long firstHourHighTicks
    ) {
        /*
         * Intent: Convert a single tick into a long/short first-hour breach occurrence when thresholds are crossed.
         * Precondition: Tick and session date must be non-null; low/high thresholds must be valid tick prices.
         * Returns: LONG occurrence, SHORT occurrence, or null when no breach occurred.
         * Postcondition: No detector state is changed by this pure threshold check.
         */
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
            /*
             * Intent: Create a stable map key for one contract's trading session.
             * Precondition: Contract symbol and session date should be non-null.
             * Returns: A constructed EventKey instance.
             * Postcondition: Key fields are immutable.
             */
            this.contractSymbol = contractSymbol;
            this.sessionDate = sessionDate;
        }

        @Override
        public boolean equals(Object other) {
            /*
             * Intent: Compare event keys by contract symbol and trading session date.
             * Precondition: Other object may be any type.
             * Returns: True when both keys identify the same contract/session.
             * Postcondition: Neither object is modified.
             */
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
            /*
             * Intent: Produce a hash code consistent with EventKey equality.
             * Precondition: Key fields must be non-null.
             * Returns: Hash code for map/set lookup.
             * Postcondition: Key state is unchanged.
             */
            int result = contractSymbol.hashCode();
            result = 31 * result + sessionDate.hashCode();
            return result;
        }
    }
}
