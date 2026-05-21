package forge.strategy;

import forge.config.TargetSettings;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
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
            assertTrue(strategies.contains(OpeningRangeContinuationStrategy.class));
        }
    }

    @Nested
    class GetDisplayName {
        @Test
        void removesStrategySuffix() {
            assertEquals("RangeBreakout", catalog.getDisplayName(RangeBreakoutStrategy.class));
            assertEquals("OpeningRangeContinuation", catalog.getDisplayName(OpeningRangeContinuationStrategy.class));
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
            assertEquals(List.of(TargetSettings.FIXED_RISK_REWARD, TargetSettings.FIXED_TARGET), profile.getAllowedTargets());
            assertEquals(TargetSettings.FIXED_RISK_REWARD, profile.getDefaultTarget());
            assertTrue(profile.isTargetSelectionAllowed());
            assertEquals(2.0, profile.getDefaultTargetSettings(TargetSettings.FIXED_RISK_REWARD).getRewardRiskRatio());
            assertEquals(8, profile.getDefaultTargetSettings(TargetSettings.FIXED_TARGET).getProfitTargetTicks());
        }

        @Test
        void openingRangeContinuationDefinesInternalTriggerAndTargetProfile() {
            StrategyConfigurationProfile profile = catalog.getConfigurationProfile(OpeningRangeContinuationStrategy.class);

            assertEquals(OpeningRangeContinuationStrategy.class, profile.getStrategyClass());
            assertEquals(List.of(PriceCrossoverTrigger.class), profile.getAllowedTriggers());
            assertEquals(PriceCrossoverTrigger.class, profile.getDefaultTrigger());
            assertFalse(profile.isTriggerSelectionAllowed());
            assertEquals(List.of(TargetSettings.FIXED_TARGET), profile.getAllowedTargets());
            assertEquals(TargetSettings.FIXED_TARGET, profile.getDefaultTarget());
            assertFalse(profile.isTargetSelectionAllowed());
        }
    }
}
