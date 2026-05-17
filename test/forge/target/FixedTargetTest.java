package forge.target;

import forge.execution.OrderSide;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Execution(ExecutionMode.CONCURRENT)
class FixedTargetTest {
    @Nested
    class Constructor {
        @Test
        void storesTargetTicks() {
            FixedTarget target = new FixedTarget(12);

            assertEquals("Target", target.getName());
            assertEquals(12, target.getTargetTicks());
        }

        @Test
        void rejectsInvalidTargetTicks() {
            assertThrows(IllegalArgumentException.class, () -> new FixedTarget(0));
        }
    }

    @Nested
    class CalculateTarget {
        @Test
        void calculatesBuyTargetFromFixedTicks() {
            FixedTarget target = new FixedTarget(8);

            TargetResult result = target.calculateTarget(OrderSide.BUY, 20000, 19992);

            assertEquals(20008, result.getTargetPriceTicks());
            assertEquals(19992, result.getStopPriceTicks());
            assertEquals(5002.0, result.getTargetPrice(0.25));
            assertEquals(4998.0, result.getStopPrice(0.25));
        }

        @Test
        void calculatesSellTargetFromFixedTicks() {
            FixedTarget target = new FixedTarget(8);

            TargetResult result = target.calculateTarget(OrderSide.SELL, 20000, 20008);

            assertEquals(19992, result.getTargetPriceTicks());
            assertEquals(20008, result.getStopPriceTicks());
            assertEquals(4998.0, result.getTargetPrice(0.25));
            assertEquals(5002.0, result.getStopPrice(0.25));
        }
    }
}
