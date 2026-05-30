package forge.feature;

import forge.data.market.TradeTick;
import forge.data.market.TradeTickStreamProcessor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SessionRangeFeatureCalculator {
    private final TradingDayClassifier tradingDayClassifier;

    public SessionRangeFeatureCalculator() {
        /*
         * Intent: Create the session range calculator with the default trading-day classifier.
         * Precondition: Default classifier dependencies must be available.
         * Returns: A constructed SessionRangeFeatureCalculator instance.
         * Postcondition: Calculator can group ticks into overnight, first-hour, and RTH ranges.
         */
        this(new TradingDayClassifier());
    }

    public SessionRangeFeatureCalculator(TradingDayClassifier tradingDayClassifier) {
        /*
         * Intent: Create the session range calculator with an explicit trading-day classifier.
         * Precondition: Classifier must not be null.
         * Returns: A constructed SessionRangeFeatureCalculator instance.
         * Postcondition: All tick session assignment uses the supplied classifier.
         */
        if (tradingDayClassifier == null) {
            throw new IllegalArgumentException("tradingDayClassifier is required");
        }
        this.tradingDayClassifier = tradingDayClassifier;
    }

    public List<SessionRangeFeature> calculate(Collection<TradeTick> ticks) {
        /*
         * Intent: Calculate complete session range features from an in-memory tick collection.
         * Precondition: Tick collection must be non-null and should be in chronological order for consistency.
         * Returns: Complete session range features sorted by contract and session date.
         * Postcondition: Source tick collection is not modified.
         */
        if (ticks == null) {
            throw new IllegalArgumentException("ticks is required");
        }

        Accumulator accumulator = newAccumulator();
        for (TradeTick tick : ticks) {
            accumulator.onTick(tick);
        }
        return accumulator.getFeatures();
    }

    public Accumulator newAccumulator() {
        /*
         * Intent: Create a streaming accumulator for session range features.
         * Precondition: None.
         * Returns: New Accumulator ready to consume ordered ticks.
         * Postcondition: No ticks have been processed yet.
         */
        return new Accumulator();
    }

    public class Accumulator implements TradeTickStreamProcessor {
        private final Map<FeatureKey, RangeAccumulator> accumulators = new HashMap<>();

        @Override
        public void onTick(TradeTick tick) {
            /*
             * Intent: Add one tick's price to the correct trading-day/session ranges.
             * Precondition: Tick may be null; non-null ticks should be strategy-usable and timestamped.
             * Returns: Nothing.
             * Postcondition: The matching contract/session accumulator may have updated lows/highs.
             */
            if (tick == null) {
                return;
            }

            TradingDayContext context = tradingDayClassifier.classify(tick.getTradeDateTime());
            FeatureKey key = new FeatureKey(tick.getContractSymbol(), context.getTradingDay());
            RangeAccumulator accumulator = accumulators.computeIfAbsent(key, unused -> new RangeAccumulator());

            if (context.isOvernight()) {
                accumulator.overnight.include(tick.getPriceTicks());
            }
            if (context.isFirstHour()) {
                accumulator.firstHour.include(tick.getPriceTicks());
            }
            if (context.isRth()) {
                accumulator.rth.include(tick.getPriceTicks());
            }
        }

        public List<SessionRangeFeature> getFeatures() {
            /*
             * Intent: Convert accumulated complete session ranges into feature results.
             * Precondition: Accumulator may contain partial or complete session ranges.
             * Returns: Sorted list of complete SessionRangeFeature values.
             * Postcondition: Partial sessions are skipped and accumulator state is unchanged.
             */
            List<SessionRangeFeature> features = new ArrayList<>();
            for (Map.Entry<FeatureKey, RangeAccumulator> entry : accumulators.entrySet()) {
                RangeAccumulator accumulator = entry.getValue();
                if (!accumulator.hasCompleteRanges()) {
                    continue;
                }

                FeatureKey key = entry.getKey();
                features.add(new SessionRangeFeature(
                        key.contractSymbol,
                        key.tradingDay,
                        accumulator.overnight.lowTicks,
                        accumulator.overnight.highTicks,
                        accumulator.firstHour.lowTicks,
                        accumulator.firstHour.highTicks,
                        accumulator.rth.lowTicks,
                        accumulator.rth.highTicks
                ));
            }

            features.sort(Comparator
                .comparing(SessionRangeFeature::getContractSymbol)
                .thenComparing(SessionRangeFeature::getSessionDate));
            return features;
        }

        public boolean hasFirstHourRange(String contractSymbol, LocalDate tradingDay) {
            /*
             * Intent: Check whether a first-hour range is available during live streaming.
             * Precondition: Contract symbol and trading day should identify a potential accumulator entry.
             * Returns: True when first-hour low/high values have been observed.
             * Postcondition: Accumulator state is unchanged.
             */
            RangeAccumulator accumulator = accumulators.get(new FeatureKey(contractSymbol, tradingDay));
            return accumulator != null && accumulator.firstHour.hasValues();
        }

        public long getFirstHourLowTicks(String contractSymbol, LocalDate tradingDay) {
            RangeAccumulator accumulator = requireFirstHourRange(contractSymbol, tradingDay);
            return accumulator.firstHour.lowTicks;
        }

        public long getFirstHourHighTicks(String contractSymbol, LocalDate tradingDay) {
            RangeAccumulator accumulator = requireFirstHourRange(contractSymbol, tradingDay);
            return accumulator.firstHour.highTicks;
        }

        private RangeAccumulator requireFirstHourRange(String contractSymbol, LocalDate tradingDay) {
            /*
             * Intent: Resolve first-hour range data or fail with a clear session-specific message.
             * Precondition: Contract symbol and trading day must identify a completed first-hour range.
             * Returns: RangeAccumulator containing first-hour low/high values.
             * Postcondition: Accumulator state is unchanged.
             */
            RangeAccumulator accumulator = accumulators.get(new FeatureKey(contractSymbol, tradingDay));
            if (accumulator == null || !accumulator.firstHour.hasValues()) {
                throw new IllegalStateException("First-hour range is not available for " + contractSymbol + " " + tradingDay);
            }
            return accumulator;
        }
    }

    private static class FeatureKey {
        private final String contractSymbol;
        private final LocalDate tradingDay;

        private FeatureKey(String contractSymbol, LocalDate tradingDay) {
            /*
             * Intent: Create a stable map key for one contract's trading day.
             * Precondition: Contract symbol and trading day should be non-null.
             * Returns: A constructed FeatureKey instance.
             * Postcondition: Key fields are immutable.
             */
            this.contractSymbol = contractSymbol;
            this.tradingDay = tradingDay;
        }

        @Override
        public boolean equals(Object other) {
            /*
             * Intent: Compare feature keys by contract symbol and trading day.
             * Precondition: Other object may be any type.
             * Returns: True when both keys identify the same contract/trading day.
             * Postcondition: Neither object is modified.
             */
            if (this == other) {
                return true;
            }
            if (!(other instanceof FeatureKey)) {
                return false;
            }
            FeatureKey that = (FeatureKey) other;
            return contractSymbol.equals(that.contractSymbol) && tradingDay.equals(that.tradingDay);
        }

        @Override
        public int hashCode() {
            /*
             * Intent: Produce a hash code consistent with FeatureKey equality.
             * Precondition: Key fields must be non-null.
             * Returns: Hash code for map/set lookup.
             * Postcondition: Key state is unchanged.
             */
            int result = contractSymbol.hashCode();
            result = 31 * result + tradingDay.hashCode();
            return result;
        }
    }

    private static class RangeAccumulator {
        private final TickRange overnight = new TickRange();
        private final TickRange firstHour = new TickRange();
        private final TickRange rth = new TickRange();

        private boolean hasCompleteRanges() {
            /*
             * Intent: Determine whether overnight, first-hour, and RTH ranges are all available.
             * Precondition: Range accumulator must have processed zero or more ticks.
             * Returns: True when all required session ranges contain values.
             * Postcondition: Range accumulator state is unchanged.
             */
            return overnight.hasValues() && firstHour.hasValues() && rth.hasValues();
        }
    }

    private static class TickRange {
        private long lowTicks = Long.MAX_VALUE;
        private long highTicks = Long.MIN_VALUE;

        private void include(long priceTicks) {
            /*
             * Intent: Expand the low/high range to include one tick price.
             * Precondition: Price ticks should be a positive normalized tick price.
             * Returns: Nothing.
             * Postcondition: Low and high bounds include the supplied price.
             */
            lowTicks = Math.min(lowTicks, priceTicks);
            highTicks = Math.max(highTicks, priceTicks);
        }

        private boolean hasValues() {
            /*
             * Intent: Check whether at least one price has been included.
             * Precondition: None.
             * Returns: True when low/high have moved from sentinel values.
             * Postcondition: TickRange state is unchanged.
             */
            return lowTicks != Long.MAX_VALUE && highTicks != Long.MIN_VALUE;
        }
    }
}
