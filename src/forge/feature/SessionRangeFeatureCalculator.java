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
        this(new TradingDayClassifier());
    }

    public SessionRangeFeatureCalculator(TradingDayClassifier tradingDayClassifier) {
        if (tradingDayClassifier == null) {
            throw new IllegalArgumentException("tradingDayClassifier is required");
        }
        this.tradingDayClassifier = tradingDayClassifier;
    }

    public List<SessionRangeFeature> calculate(Collection<TradeTick> ticks) {
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
        return new Accumulator();
    }

    public class Accumulator implements TradeTickStreamProcessor {
        private final Map<FeatureKey, RangeAccumulator> accumulators = new HashMap<>();

        @Override
        public void onTick(TradeTick tick) {
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
            this.contractSymbol = contractSymbol;
            this.tradingDay = tradingDay;
        }

        @Override
        public boolean equals(Object other) {
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
            return overnight.hasValues() && firstHour.hasValues() && rth.hasValues();
        }
    }

    private static class TickRange {
        private long lowTicks = Long.MAX_VALUE;
        private long highTicks = Long.MIN_VALUE;

        private void include(long priceTicks) {
            lowTicks = Math.min(lowTicks, priceTicks);
            highTicks = Math.max(highTicks, priceTicks);
        }

        private boolean hasValues() {
            return lowTicks != Long.MAX_VALUE && highTicks != Long.MIN_VALUE;
        }
    }
}
