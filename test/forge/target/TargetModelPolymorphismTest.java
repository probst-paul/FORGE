package forge.target;

import forge.execution.OrderSide;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Execution(ExecutionMode.CONCURRENT)
class TargetModelPolymorphismTest {
    @Test
    void calculatesDifferentTargetsThroughSameInterface() {
        List<TargetModel> targetModels = List.of(
                new FixedRiskRewardTarget(2.0),
                new FixedTarget(8)
        );

        TargetResult riskReward = targetModels.get(0).calculateTarget(OrderSide.BUY, 5000, 4998, 0.25);
        TargetResult fixedTarget = targetModels.get(1).calculateTarget(OrderSide.BUY, 5000, 4998, 0.25);

        assertEquals(5004.0, riskReward.getTargetPrice());
        assertEquals(5002.0, fixedTarget.getTargetPrice());
    }
}
