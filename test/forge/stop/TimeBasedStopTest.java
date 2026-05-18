package forge.stop;

import forge.engine.MarketContext;
import forge.execution.OrderSide;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimeBasedStopTest {
    @Nested
    class Constructor {
        @Test
        void rejectsNullExitTime() {
            assertThrows(NullPointerException.class, () -> new TimeBasedStop(null));
        }
    }

    @Nested
    class EvaluateStop {
        @Test
        void doesNotStopBeforeExitTime() {
            TimeBasedStop stop = new TimeBasedStop(LocalTime.of(15, 55));

            StopResult result = stop.evaluateStop(OrderSide.BUY, contextAt(15, 54));

            assertFalse(result.isStopped());
            assertEquals(StopReason.NONE, result.getReason());
        }

        @Test
        void stopsAtExitTime() {
            TimeBasedStop stop = new TimeBasedStop(LocalTime.of(15, 55));

            StopResult result = stop.evaluateStop(OrderSide.BUY, contextAt(15, 55));

            assertTrue(result.isStopped());
            assertEquals(StopReason.TIME, result.getReason());
            assertEquals(20000, result.getExitPriceTicks());
        }

        @Test
        void stopsAfterExitTime() {
            TimeBasedStop stop = new TimeBasedStop(LocalTime.of(15, 55));

            StopResult result = stop.evaluateStop(OrderSide.SELL, contextAt(15, 56));

            assertTrue(result.isStopped());
            assertEquals(StopReason.TIME, result.getReason());
            assertEquals(20000, result.getExitPriceTicks());
        }
    }

    private MarketContext contextAt(int hour, int minute) {
        return new MarketContext("ES", LocalDateTime.of(2025, 1, 2, hour, minute), 20000, 0.25, 12.50, true);
    }
}
