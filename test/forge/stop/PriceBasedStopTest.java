package forge.stop;

import forge.engine.MarketContext;
import forge.execution.OrderSide;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PriceBasedStopTest {
    @Nested
    class Constructor {
        @Test
        void rejectsNonPositiveStopPriceTicks() {
            assertThrows(IllegalArgumentException.class, () -> new PriceBasedStop(0));
            assertThrows(IllegalArgumentException.class, () -> new PriceBasedStop(-1));
        }
    }

    @Nested
    class EvaluateStop {
        @Test
        void stopsLongWhenPriceTradesAtOrBelowStop() {
            PriceBasedStop stop = new PriceBasedStop(100);

            StopResult atStop = stop.evaluateStop(OrderSide.BUY, contextAt(100));
            StopResult beyondStop = stop.evaluateStop(OrderSide.BUY, contextAt(99));

            assertTrue(atStop.isStopped());
            assertEquals(StopReason.PRICE, atStop.getReason());
            assertEquals(100, atStop.getExitPriceTicks());
            assertTrue(beyondStop.isStopped());
            assertEquals(99, beyondStop.getExitPriceTicks());
        }

        @Test
        void doesNotStopLongWhenPriceIsAboveStop() {
            StopResult result = new PriceBasedStop(100).evaluateStop(OrderSide.BUY, contextAt(101));

            assertFalse(result.isStopped());
            assertEquals(StopReason.NONE, result.getReason());
        }

        @Test
        void stopsShortWhenPriceTradesAtOrAboveStop() {
            PriceBasedStop stop = new PriceBasedStop(100);

            StopResult atStop = stop.evaluateStop(OrderSide.SELL, contextAt(100));
            StopResult beyondStop = stop.evaluateStop(OrderSide.SELL, contextAt(101));

            assertTrue(atStop.isStopped());
            assertEquals(StopReason.PRICE, atStop.getReason());
            assertEquals(100, atStop.getExitPriceTicks());
            assertTrue(beyondStop.isStopped());
            assertEquals(101, beyondStop.getExitPriceTicks());
        }

        @Test
        void doesNotStopShortWhenPriceIsBelowStop() {
            StopResult result = new PriceBasedStop(100).evaluateStop(OrderSide.SELL, contextAt(99));

            assertFalse(result.isStopped());
            assertEquals(StopReason.NONE, result.getReason());
        }
    }

    private MarketContext contextAt(long priceTicks) {
        return new MarketContext("ES", LocalDateTime.of(2025, 1, 2, 9, 30), priceTicks, 0.25, 12.50, true);
    }
}
