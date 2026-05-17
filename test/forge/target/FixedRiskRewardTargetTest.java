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

            TargetResult result = target.calculateTarget(OrderSide.BUY, 20000, 19992);

            assertEquals(20016, result.getTargetPriceTicks());
            assertEquals(19992, result.getStopPriceTicks());
            assertEquals(5004.0, result.getTargetPrice(0.25));
            assertEquals(4998.0, result.getStopPrice(0.25));
        }

        @Test
        void calculatesSellTargetFromRiskDistance() {
            FixedRiskRewardTarget target = new FixedRiskRewardTarget(2.0);

            TargetResult result = target.calculateTarget(OrderSide.SELL, 20000, 20008);

            assertEquals(19984, result.getTargetPriceTicks());
            assertEquals(20008, result.getStopPriceTicks());
            assertEquals(4996.0, result.getTargetPrice(0.25));
            assertEquals(5002.0, result.getStopPrice(0.25));
        }

        @Test
        void roundsFractionalRiskRewardTargetsToNearestTick() {
            FixedRiskRewardTarget target = new FixedRiskRewardTarget(1.5);

            TargetResult result = target.calculateTarget(OrderSide.BUY, 20000, 19997);

            assertEquals(20005, result.getTargetPriceTicks());
        }

        @Test
        void rejectsStopsOnWrongSideOfEntry() {
            FixedRiskRewardTarget target = new FixedRiskRewardTarget(2.0);

            assertThrows(IllegalArgumentException.class, () -> target.calculateTarget(OrderSide.BUY, 20000, 20004));
            assertThrows(IllegalArgumentException.class, () -> target.calculateTarget(OrderSide.SELL, 20000, 19996));
        }
    }
}
