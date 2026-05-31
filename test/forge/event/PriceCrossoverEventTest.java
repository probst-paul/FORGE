package forge.event;

import forge.engine.MarketContext;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Execution(ExecutionMode.CONCURRENT)
class PriceCrossoverEventTest {
    @Nested
    class Constructor {
        @Test
        void storesDirectionAndThresholdTicks() {
            PriceCrossoverEvent condition = new PriceCrossoverEvent(EventDirection.LONG, 20000);

            assertEquals("PriceCrossover", condition.getName());
            assertEquals(EventDirection.LONG, condition.getDirection());
            assertEquals(20000, condition.getPriceThresholdTicks());
        }

        @Test
        void rejectsInvalidInputs() {
            assertThrows(IllegalArgumentException.class, () -> new PriceCrossoverEvent(EventDirection.NONE, 20000));
            assertThrows(IllegalArgumentException.class, () -> new PriceCrossoverEvent(EventDirection.LONG, 0));
        }
    }

    @Nested
    class Evaluate {
        @Test
        void longConditionFiresAtOrAboveThreshold() {
            PriceCrossoverEvent condition = new PriceCrossoverEvent(EventDirection.LONG, 20000);

            assertFalse(condition.evaluate(context(19999)).isConditioned());

            EventResult atThreshold = condition.evaluate(context(20000));
            assertTrue(atThreshold.isConditioned());
            assertEquals(EventDirection.LONG, atThreshold.getDirection());

            EventResult aboveThreshold = condition.evaluate(context(20001));
            assertTrue(aboveThreshold.isConditioned());
            assertEquals(EventDirection.LONG, aboveThreshold.getDirection());
        }

        @Test
        void shortConditionFiresAtOrBelowThreshold() {
            PriceCrossoverEvent condition = new PriceCrossoverEvent(EventDirection.SHORT, 20000);

            assertFalse(condition.evaluate(context(20001)).isConditioned());

            EventResult atThreshold = condition.evaluate(context(20000));
            assertTrue(atThreshold.isConditioned());
            assertEquals(EventDirection.SHORT, atThreshold.getDirection());

            EventResult belowThreshold = condition.evaluate(context(19999));
            assertTrue(belowThreshold.isConditioned());
            assertEquals(EventDirection.SHORT, belowThreshold.getDirection());
        }
    }

    private MarketContext context(long lastPriceTicks) {
        return new MarketContext(
                "ES",
                LocalDateTime.of(2025, 9, 15, 9, 30),
                lastPriceTicks,
                0.25,
                12.50,
                false
        );
    }
}
