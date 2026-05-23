package forge.strategy;

import forge.config.TargetSettings;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import forge.condition.OrderFlowExhaustionCondition;
import forge.condition.PriceCrossoverCondition;

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
    class GetDescription {
        @Test
        void returnsShortUserFacingStrategyDescriptions() {
            assertTrue(catalog.getDescription(RangeBreakoutStrategy.class).contains("break"));
            assertTrue(catalog.getDescription(OpeningRangeContinuationStrategy.class).contains("first-hour"));
        }
    }

    @Nested
    class GetConfigurationProfile {
        @Test
        void rangeBreakoutDefinesCompatibleConditionAndTargets() {
            StrategyConfigurationProfile profile = catalog.getConfigurationProfile(RangeBreakoutStrategy.class);

            assertEquals(RangeBreakoutStrategy.class, profile.getStrategyClass());
            assertEquals(List.of(OrderFlowExhaustionCondition.class, PriceCrossoverCondition.class), profile.getAllowedConditions());
            assertEquals(OrderFlowExhaustionCondition.class, profile.getDefaultCondition());
            assertTrue(profile.isConditionSelectionAllowed());
            assertEquals(List.of(TargetSettings.FIXED_RISK_REWARD, TargetSettings.FIXED_TARGET), profile.getAllowedTargets());
            assertEquals(TargetSettings.FIXED_RISK_REWARD, profile.getDefaultTarget());
            assertTrue(profile.isTargetSelectionAllowed());
            assertEquals(2.0, profile.getDefaultTargetSettings(TargetSettings.FIXED_RISK_REWARD).getRewardRiskRatio());
            assertEquals(8, profile.getDefaultTargetSettings(TargetSettings.FIXED_TARGET).getProfitTargetTicks());
        }

        @Test
        void openingRangeContinuationDefinesInternalConditionAndTargetProfile() {
            StrategyConfigurationProfile profile = catalog.getConfigurationProfile(OpeningRangeContinuationStrategy.class);

            assertEquals(OpeningRangeContinuationStrategy.class, profile.getStrategyClass());
            assertEquals(List.of(PriceCrossoverCondition.class), profile.getAllowedConditions());
            assertEquals(PriceCrossoverCondition.class, profile.getDefaultCondition());
            assertFalse(profile.isConditionSelectionAllowed());
            assertEquals(List.of(TargetSettings.FIXED_TARGET), profile.getAllowedTargets());
            assertEquals(TargetSettings.FIXED_TARGET, profile.getDefaultTarget());
            assertFalse(profile.isTargetSelectionAllowed());
        }
    }
}
