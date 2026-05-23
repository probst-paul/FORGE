package forge.condition;

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
class PriceCrossoverConditionTest {
    @Nested
    class Constructor {
        @Test
        void storesDirectionAndThresholdTicks() {
            PriceCrossoverCondition condition = new PriceCrossoverCondition(ConditionDirection.LONG, 20000);

            assertEquals("PriceCrossover", condition.getName());
            assertEquals(ConditionDirection.LONG, condition.getDirection());
            assertEquals(20000, condition.getPriceThresholdTicks());
        }

        @Test
        void rejectsInvalidInputs() {
            assertThrows(IllegalArgumentException.class, () -> new PriceCrossoverCondition(ConditionDirection.NONE, 20000));
            assertThrows(IllegalArgumentException.class, () -> new PriceCrossoverCondition(ConditionDirection.LONG, 0));
        }
    }

    @Nested
    class Evaluate {
        @Test
        void longConditionFiresAtOrAboveThreshold() {
            PriceCrossoverCondition condition = new PriceCrossoverCondition(ConditionDirection.LONG, 20000);

            assertFalse(condition.evaluate(context(19999)).isConditioned());

            ConditionResult atThreshold = condition.evaluate(context(20000));
            assertTrue(atThreshold.isConditioned());
            assertEquals(ConditionDirection.LONG, atThreshold.getDirection());

            ConditionResult aboveThreshold = condition.evaluate(context(20001));
            assertTrue(aboveThreshold.isConditioned());
            assertEquals(ConditionDirection.LONG, aboveThreshold.getDirection());
        }

        @Test
        void shortConditionFiresAtOrBelowThreshold() {
            PriceCrossoverCondition condition = new PriceCrossoverCondition(ConditionDirection.SHORT, 20000);

            assertFalse(condition.evaluate(context(20001)).isConditioned());

            ConditionResult atThreshold = condition.evaluate(context(20000));
            assertTrue(atThreshold.isConditioned());
            assertEquals(ConditionDirection.SHORT, atThreshold.getDirection());

            ConditionResult belowThreshold = condition.evaluate(context(19999));
            assertTrue(belowThreshold.isConditioned());
            assertEquals(ConditionDirection.SHORT, belowThreshold.getDirection());
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
