package forge.strategy;

import forge.engine.MarketContext;
import forge.execution.OrderRequest;
import forge.execution.OrderSide;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpeningRangeContinuationStrategyTest {
    private static final ZoneId CENTRAL_TIME = ZoneId.of("America/Chicago");

    @Nested
    class Constructor {
        @Test
        void storesQuantityAndName() {
            OpeningRangeContinuationStrategy strategy = new OpeningRangeContinuationStrategy(2);

            assertEquals("OpeningRangeContinuation", strategy.getName());
            assertEquals(2, strategy.getQuantity());
            assertEquals(OpeningRangeContinuationStrategy.ExitStyle.RANGE, strategy.getExitStyle());
            assertEquals(2.0, strategy.getRewardRiskRatio());
        }

        @Test
        void storesRiskRewardExitStyle() {
            OpeningRangeContinuationStrategy strategy = new OpeningRangeContinuationStrategy(
                    2,
                    OpeningRangeContinuationStrategy.ExitStyle.RISK_REWARD,
                    3.0
            );

            assertEquals(2, strategy.getQuantity());
            assertEquals(OpeningRangeContinuationStrategy.ExitStyle.RISK_REWARD, strategy.getExitStyle());
            assertEquals(3.0, strategy.getRewardRiskRatio());
        }

        @Test
        void rejectsInvalidConfiguration() {
            assertThrows(IllegalArgumentException.class, () -> new OpeningRangeContinuationStrategy(0));
            assertThrows(NullPointerException.class, () -> new OpeningRangeContinuationStrategy(
                    1,
                    null,
                    2.0
            ));
            assertThrows(IllegalArgumentException.class, () -> new OpeningRangeContinuationStrategy(
                    1,
                    OpeningRangeContinuationStrategy.ExitStyle.RISK_REWARD,
                    0
            ));
        }
    }

    @Nested
    class Evaluate {
        @Test
        void entersLongWhenFirstHourHighIsCrossedAfterInsideFirstHourRange() {
            OpeningRangeContinuationStrategy strategy = new OpeningRangeContinuationStrategy(2);

            buildOvernightRange(strategy, 90, 100);
            buildFirstHourRange(strategy, 92, 98);

            Optional<OrderRequest> decision = strategy.evaluate(context(LocalTime.of(9, 30), 98, false));

            assertTrue(decision.isPresent());
            assertEquals(OrderSide.BUY, decision.get().getSide());
            assertEquals(2, decision.get().getQuantity());
            OpeningRangeContinuationStrategy.TradePlan plan = strategy.getLastTradePlan().orElseThrow();
            assertEquals(OrderSide.BUY, plan.getSide());
            assertEquals(100, plan.getTargetPriceTicks());
            assertEquals(92, plan.getStopPriceTicks());
            assertEquals(LocalTime.of(10, 30), plan.getTimeStop());
        }

        @Test
        void entersShortWhenFirstHourLowIsCrossedAfterInsideFirstHourRange() {
            OpeningRangeContinuationStrategy strategy = new OpeningRangeContinuationStrategy();

            buildOvernightRange(strategy, 90, 100);
            buildFirstHourRange(strategy, 92, 98);

            Optional<OrderRequest> decision = strategy.evaluate(context(LocalTime.of(9, 30), 92, false));

            assertTrue(decision.isPresent());
            assertEquals(OrderSide.SELL, decision.get().getSide());
            OpeningRangeContinuationStrategy.TradePlan plan = strategy.getLastTradePlan().orElseThrow();
            assertEquals(OrderSide.SELL, plan.getSide());
            assertEquals(90, plan.getTargetPriceTicks());
            assertEquals(98, plan.getStopPriceTicks());
            assertEquals(LocalTime.of(10, 30), plan.getTimeStop());
        }

        @Test
        void canUseRiskRewardTargetForLongTrade() {
            OpeningRangeContinuationStrategy strategy = new OpeningRangeContinuationStrategy(
                    1,
                    OpeningRangeContinuationStrategy.ExitStyle.RISK_REWARD,
                    2.0
            );

            buildOvernightRange(strategy, 90, 110);
            buildFirstHourRange(strategy, 92, 98);

            Optional<OrderRequest> decision = strategy.evaluate(context(LocalTime.of(9, 30), 98, false));

            assertTrue(decision.isPresent());
            OpeningRangeContinuationStrategy.TradePlan plan = strategy.getLastTradePlan().orElseThrow();
            assertEquals(OrderSide.BUY, plan.getSide());
            assertEquals(110, plan.getTargetPriceTicks());
            assertEquals(92, plan.getStopPriceTicks());
        }

        @Test
        void canUseRiskRewardTargetForShortTrade() {
            OpeningRangeContinuationStrategy strategy = new OpeningRangeContinuationStrategy(
                    1,
                    OpeningRangeContinuationStrategy.ExitStyle.RISK_REWARD,
                    1.5
            );

            buildOvernightRange(strategy, 80, 110);
            buildFirstHourRange(strategy, 92, 100);

            Optional<OrderRequest> decision = strategy.evaluate(context(LocalTime.of(9, 30), 92, false));

            assertTrue(decision.isPresent());
            OpeningRangeContinuationStrategy.TradePlan plan = strategy.getLastTradePlan().orElseThrow();
            assertEquals(OrderSide.SELL, plan.getSide());
            assertEquals(80, plan.getTargetPriceTicks());
            assertEquals(100, plan.getStopPriceTicks());
        }

        @Test
        void doesNotTradeWhenFirstHourRangeExceedsOvernightRange() {
            OpeningRangeContinuationStrategy strategy = new OpeningRangeContinuationStrategy();

            buildOvernightRange(strategy, 90, 100);
            buildFirstHourRange(strategy, 92, 101);

            Optional<OrderRequest> decision = strategy.evaluate(context(LocalTime.of(9, 30), 101, false));

            assertFalse(decision.isPresent());
        }

        @Test
        void onlyAllowsOneTradePerSessionDate() {
            OpeningRangeContinuationStrategy strategy = new OpeningRangeContinuationStrategy();

            buildOvernightRange(strategy, 90, 100);
            buildFirstHourRange(strategy, 92, 98);

            assertTrue(strategy.evaluate(context(LocalTime.of(9, 30), 98, false)).isPresent());
            assertFalse(strategy.evaluate(context(LocalTime.of(9, 31), 92, false)).isPresent());
        }

        @Test
        void onlyTradesDuringAllowedWindow() {
            OpeningRangeContinuationStrategy strategy = new OpeningRangeContinuationStrategy();

            buildOvernightRange(strategy, 90, 100);
            buildFirstHourRange(strategy, 92, 98);

            assertFalse(strategy.evaluate(context(LocalTime.of(10, 30), 98, false)).isPresent());
        }

        @Test
        void doesNotTradeWhenPositionIsAlreadyOpen() {
            OpeningRangeContinuationStrategy strategy = new OpeningRangeContinuationStrategy();

            buildOvernightRange(strategy, 90, 100);
            buildFirstHourRange(strategy, 92, 98);

            assertFalse(strategy.evaluate(context(LocalTime.of(9, 30), 98, true)).isPresent());
        }

        @Test
        void clearsStateOnBacktestStart() {
            OpeningRangeContinuationStrategy strategy = new OpeningRangeContinuationStrategy();

            buildOvernightRange(strategy, 90, 100);
            buildFirstHourRange(strategy, 92, 98);
            assertTrue(strategy.evaluate(context(LocalTime.of(9, 30), 98, false)).isPresent());

            strategy.onBacktestStart();

            assertFalse(strategy.getLastTradePlan().isPresent());
            assertFalse(strategy.evaluate(context(LocalTime.of(9, 31), 98, false)).isPresent());
        }
    }

    private void buildOvernightRange(OpeningRangeContinuationStrategy strategy, long lowTicks, long highTicks) {
        strategy.evaluate(context(LocalDate.of(2025, 1, 5), LocalTime.of(17, 0), highTicks, false));
        strategy.evaluate(context(LocalTime.of(8, 0), lowTicks, false));
    }

    private void buildFirstHourRange(OpeningRangeContinuationStrategy strategy, long lowTicks, long highTicks) {
        strategy.evaluate(context(LocalTime.of(8, 30), lowTicks, false));
        strategy.evaluate(context(LocalTime.of(9, 29, 59), highTicks, false));
    }

    private MarketContext context(LocalTime centralTime, long priceTicks, boolean hasOpenPosition) {
        return context(LocalDate.of(2025, 1, 6), centralTime, priceTicks, hasOpenPosition);
    }

    private MarketContext context(LocalDate centralDate, LocalTime centralTime, long priceTicks, boolean hasOpenPosition) {
        LocalDateTime utcDateTime = LocalDateTime.of(centralDate, centralTime)
                .atZone(CENTRAL_TIME)
                .withZoneSameInstant(ZoneOffset.UTC)
                .toLocalDateTime();
        return new MarketContext("ES", utcDateTime, priceTicks, 0.25, 12.50, hasOpenPosition);
    }
}
