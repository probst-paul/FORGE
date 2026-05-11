package forge.target;

import forge.execution.OrderSide;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Execution(ExecutionMode.CONCURRENT)
class FixedRiskRewardTargetTest {
    @Nested
    class Constructor {
        @Test
        void storesRewardRiskRatio() {
            FixedRiskRewardTarget target = new FixedRiskRewardTarget(2.5);

            assertEquals("Fixed Risk/Reward", target.getName());
            assertEquals(2.5, target.getRewardRiskRatio());
        }

        @Test
        void rejectsInvalidRewardRiskRatio() {
            assertThrows(IllegalArgumentException.class, () -> new FixedRiskRewardTarget(0));
        }
    }

    @Nested
    class CalculateTarget {
        @Test
        void calculatesBuyTargetFromRiskDistance() {
            FixedRiskRewardTarget target = new FixedRiskRewardTarget(2.0);

            TargetResult result = target.calculateTarget(OrderSide.BUY, 5000, 4998, 0.25);

            assertEquals(5004.0, result.getTargetPrice());
            assertEquals(4998.0, result.getStopPrice());
        }

        @Test
        void calculatesSellTargetFromRiskDistance() {
            FixedRiskRewardTarget target = new FixedRiskRewardTarget(2.0);

            TargetResult result = target.calculateTarget(OrderSide.SELL, 5000, 5002, 0.25);

            assertEquals(4996.0, result.getTargetPrice());
            assertEquals(5002.0, result.getStopPrice());
        }

        @Test
        void rejectsStopsOnWrongSideOfEntry() {
            FixedRiskRewardTarget target = new FixedRiskRewardTarget(2.0);

            assertThrows(IllegalArgumentException.class, () -> target.calculateTarget(OrderSide.BUY, 5000, 5001, 0.25));
            assertThrows(IllegalArgumentException.class, () -> target.calculateTarget(OrderSide.SELL, 5000, 4999, 0.25));
        }
    }
}
