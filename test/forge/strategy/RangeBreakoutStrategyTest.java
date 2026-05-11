package forge.strategy;

import forge.engine.MarketContext;
import forge.execution.OrderRequest;
import forge.execution.OrderSide;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.time.LocalDateTime;
import java.util.Optional;

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

            Optional<OrderRequest> decision = strategy.evaluate(context(5000.25, false));

            assertTrue(decision.isPresent());
            assertEquals(OrderSide.BUY, decision.get().getSide());
            assertEquals(2, decision.get().getQuantity());
        }

        @Test
        void returnsSellOrderWhenPriceBreaksBelowRangeLow() {
            RangeBreakoutStrategy strategy = new RangeBreakoutStrategy(5000, 4900, 2);

            Optional<OrderRequest> decision = strategy.evaluate(context(4899.75, false));

            assertTrue(decision.isPresent());
            assertEquals(OrderSide.SELL, decision.get().getSide());
        }

        @Test
        void holdsInsideRangeWhenPositionIsOpenOrRangeIsUnset() {
            RangeBreakoutStrategy configured = new RangeBreakoutStrategy(5000, 4900, 1);
            RangeBreakoutStrategy unset = new RangeBreakoutStrategy();

            assertFalse(configured.evaluate(context(4950, false)).isPresent());
            assertFalse(configured.evaluate(context(5001, true)).isPresent());
            assertFalse(unset.evaluate(context(5001, false)).isPresent());
        }
    }

    private MarketContext context(double price, boolean hasOpenPosition) {
        return new MarketContext("ES", LocalDateTime.of(2024, 1, 2, 9, 30), price, hasOpenPosition);
    }
}
