package forge.strategy;

import forge.engine.MarketContext;
import forge.trade.OrderRequest;
import forge.trade.OrderSide;
import forge.data.market.TradeTick;
import forge.feature.TpoPeriodClassifier;
import forge.feature.TradingDayClassifier;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Execution(ExecutionMode.CONCURRENT)
class RangeBreakoutStrategyTest {
    @Nested
    class Constructor {
        @Test
        void storesConfiguredRangeAndQuantity() {
            RangeBreakoutStrategy strategy = new RangeBreakoutStrategy(5000, 4900, 2);

            assertEquals("RangeBreakout", strategy.getName());
            assertEquals(5000, strategy.getRangeHigh());
            assertEquals(4900, strategy.getRangeLow());
            assertEquals(2, strategy.getQuantity());
        }

        @Test
        void rejectsInvalidRangeAndQuantity() {
            assertThrows(IllegalArgumentException.class, () -> new RangeBreakoutStrategy(-1, 4900, 1));
            assertThrows(IllegalArgumentException.class, () -> new RangeBreakoutStrategy(4900, 5000, 1));
            assertThrows(IllegalArgumentException.class, () -> new RangeBreakoutStrategy(5000, 4900, 0));
        }
    }

    @Nested
    class Evaluate {
        @Test
        void returnsBuyOrderWhenPriceBreaksAboveRangeHigh() {
            RangeBreakoutStrategy strategy = new RangeBreakoutStrategy(5000, 4900, 2);

            StrategyDecision decision = strategy.evaluate(context(5000.25, false));

            assertTrue(decision.hasOrderRequest());
            OrderRequest orderRequest = decision.getOrderRequest().orElseThrow();
            assertEquals(OrderSide.BUY, orderRequest.getSide());
            assertEquals(2, orderRequest.getQuantity());
        }

        @Test
        void returnsSellOrderWhenPriceBreaksBelowRangeLow() {
            RangeBreakoutStrategy strategy = new RangeBreakoutStrategy(5000, 4900, 2);

            StrategyDecision decision = strategy.evaluate(context(4899.75, false));

            assertTrue(decision.hasOrderRequest());
            assertEquals(OrderSide.SELL, decision.getOrderRequest().orElseThrow().getSide());
        }

        @Test
        void holdsInsideRangeWhenPositionIsOpenOrRangeIsUnset() {
            RangeBreakoutStrategy configured = new RangeBreakoutStrategy(5000, 4900, 1);
            RangeBreakoutStrategy unset = new RangeBreakoutStrategy();

            assertFalse(configured.evaluate(context(4950, false)).hasOrderRequest());
            assertFalse(configured.evaluate(context(5001, true)).hasOrderRequest());
            assertFalse(unset.evaluate(context(5001, false)).hasOrderRequest());
        }
    }

    private StrategyContext context(double price, boolean hasOpenPosition) {
        LocalDateTime timestamp = LocalDateTime.of(2024, 1, 2, 9, 30);
        MarketContext marketContext = new MarketContext("ES", timestamp, price, hasOpenPosition);
        Instant instant = timestamp.toInstant(ZoneOffset.UTC);
        TradeTick tick = new TradeTick("ESU25", instant, Math.round(price), null, null, 1, 1, 1);
        return new StrategyContext(
                marketContext,
                tick,
                new TradingDayClassifier().classify(instant),
                new TpoPeriodClassifier().classify(instant),
                null,
                List.of()
        );
    }
}
