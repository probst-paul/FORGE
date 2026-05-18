package forge.strategy;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import forge.target.FixedRiskRewardTarget;
import forge.target.FixedTarget;
import forge.trigger.OrderFlowExhaustionTrigger;
import forge.trigger.PriceCrossoverTrigger;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Execution(ExecutionMode.CONCURRENT)
class StrategyCatalogTest {
    private final StrategyCatalog catalog = new StrategyCatalog();

    @Nested
    class FindAvailableStrategies {
        @Test
        void includesConcreteTradingStrategies() {
            List<Class<? extends TradingStrategy>> strategies = catalog.findAvailableStrategies();

            assertTrue(strategies.contains(RangeBreakoutStrategy.class));
        }
    }

    @Nested
    class GetDisplayName {
        @Test
        void removesStrategySuffix() {
            assertEquals("RangeBreakout", catalog.getDisplayName(RangeBreakoutStrategy.class));
        }
    }

    @Nested
    class GetConfigurationProfile {
        @Test
        void rangeBreakoutDefinesCompatibleTriggerAndTargets() {
            StrategyConfigurationProfile profile = catalog.getConfigurationProfile(RangeBreakoutStrategy.class);

            assertEquals(RangeBreakoutStrategy.class, profile.getStrategyClass());
            assertEquals(List.of(OrderFlowExhaustionTrigger.class, PriceCrossoverTrigger.class), profile.getAllowedTriggers());
            assertEquals(OrderFlowExhaustionTrigger.class, profile.getDefaultTrigger());
            assertTrue(profile.isTriggerSelectionAllowed());
            assertEquals(List.of(FixedRiskRewardTarget.class, FixedTarget.class), profile.getAllowedTargets());
            assertEquals(FixedRiskRewardTarget.class, profile.getDefaultTarget());
            assertTrue(profile.isTargetSelectionAllowed());
            assertEquals(2.0, profile.getDefaultTargetSettings(FixedRiskRewardTarget.class).getRewardRiskRatio());
            assertEquals(8, profile.getDefaultTargetSettings(FixedTarget.class).getProfitTargetTicks());
        }
    }
}
