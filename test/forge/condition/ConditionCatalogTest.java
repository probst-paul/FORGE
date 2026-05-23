package forge.condition;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Execution(ExecutionMode.CONCURRENT)
class ConditionCatalogTest {
    private final ConditionCatalog catalog = new ConditionCatalog();

    @Nested
    class FindAvailableConditions {
        @Test
        void includesConcreteMarketConditions() {
            List<Class<? extends MarketCondition>> conditions = catalog.findAvailableConditions();

            assertTrue(conditions.contains(OrderFlowExhaustionCondition.class));
            assertTrue(conditions.contains(PriceCrossoverCondition.class));
        }
    }

    @Nested
    class GetDisplayName {
        @Test
        void removesConditionSuffix() {
            assertEquals("OrderFlowExhaustion", catalog.getDisplayName(OrderFlowExhaustionCondition.class));
            assertEquals("PriceCrossover", catalog.getDisplayName(PriceCrossoverCondition.class));
        }
    }
}
