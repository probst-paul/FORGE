package forge.trigger;

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
class PriceCrossoverTriggerTest {
    @Nested
    class Constructor {
        @Test
        void storesDirectionAndThresholdTicks() {
            PriceCrossoverTrigger trigger = new PriceCrossoverTrigger(TriggerDirection.LONG, 20000);

            assertEquals("PriceCrossover", trigger.getName());
            assertEquals(TriggerDirection.LONG, trigger.getDirection());
            assertEquals(20000, trigger.getPriceThresholdTicks());
        }

        @Test
        void rejectsInvalidInputs() {
            assertThrows(IllegalArgumentException.class, () -> new PriceCrossoverTrigger(TriggerDirection.NONE, 20000));
            assertThrows(IllegalArgumentException.class, () -> new PriceCrossoverTrigger(TriggerDirection.LONG, 0));
        }
    }

    @Nested
    class Evaluate {
        @Test
        void longTriggerFiresAtOrAboveThreshold() {
            PriceCrossoverTrigger trigger = new PriceCrossoverTrigger(TriggerDirection.LONG, 20000);

            assertFalse(trigger.evaluate(context(19999)).isTriggered());

            TriggerResult atThreshold = trigger.evaluate(context(20000));
            assertTrue(atThreshold.isTriggered());
            assertEquals(TriggerDirection.LONG, atThreshold.getDirection());

            TriggerResult aboveThreshold = trigger.evaluate(context(20001));
            assertTrue(aboveThreshold.isTriggered());
            assertEquals(TriggerDirection.LONG, aboveThreshold.getDirection());
        }

        @Test
        void shortTriggerFiresAtOrBelowThreshold() {
            PriceCrossoverTrigger trigger = new PriceCrossoverTrigger(TriggerDirection.SHORT, 20000);

            assertFalse(trigger.evaluate(context(20001)).isTriggered());

            TriggerResult atThreshold = trigger.evaluate(context(20000));
            assertTrue(atThreshold.isTriggered());
            assertEquals(TriggerDirection.SHORT, atThreshold.getDirection());

            TriggerResult belowThreshold = trigger.evaluate(context(19999));
            assertTrue(belowThreshold.isTriggered());
            assertEquals(TriggerDirection.SHORT, belowThreshold.getDirection());
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
