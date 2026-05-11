package forge.target;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Execution(ExecutionMode.CONCURRENT)
class TargetModelCatalogTest {
    private final TargetModelCatalog catalog = new TargetModelCatalog();

    @Nested
    class FindAvailableTargetModels {
        @Test
        void includesConcreteTargetModels() {
            List<Class<? extends TargetModel>> targetModels = catalog.findAvailableTargetModels();

            assertTrue(targetModels.contains(FixedRiskRewardTarget.class));
            assertTrue(targetModels.contains(FixedTarget.class));
        }
    }

    @Nested
    class GetDisplayName {
        @Test
        void returnsUserFacingNames() {
            assertEquals("Fixed Risk/Reward", catalog.getDisplayName(FixedRiskRewardTarget.class));
            assertEquals("Target", catalog.getDisplayName(FixedTarget.class));
        }
    }
}
