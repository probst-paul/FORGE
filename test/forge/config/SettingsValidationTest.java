package forge.config;

import forge.execution.OrderType;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Execution(ExecutionMode.CONCURRENT)
class SettingsValidationTest {
    @Nested
    class StrategyOptionsValidation {
        @Test
        void preservesParametersAsReadOnlyMap() {
            Map<String, String> parameters = new LinkedHashMap<>();
            parameters.put("lookback", "20");

            StrategyOptions options = new StrategyOptions("RangeBreakout", parameters);

            assertEquals("20", options.getParameters().get("lookback"));
            assertThrows(UnsupportedOperationException.class, () -> options.getParameters().put("x", "y"));
        }
    }

    @Nested
    class TradeTriggerOptionsValidation {
        @Test
        void preservesParametersAsReadOnlyMap() {
            Map<String, String> parameters = new LinkedHashMap<>();
            parameters.put("imbalanceRatio", "2.5");

            TradeTriggerOptions options = new TradeTriggerOptions("OrderFlowExhaustion", parameters);

            assertEquals("2.5", options.getParameters().get("imbalanceRatio"));
            assertThrows(UnsupportedOperationException.class, () -> options.getParameters().put("x", "y"));
        }
    }

    @Nested
    class RiskSettingsValidation {
        @Test
        void rejectsInvalidRiskValues() {
            assertThrows(IllegalArgumentException.class, () -> new RiskSettings(0, 100));
            assertThrows(IllegalArgumentException.class, () -> new RiskSettings(100, -1));
        }
    }

    @Nested
    class TargetSettingsValidation {
        @Test
        void createsFixedRiskRewardSettings() {
            TargetSettings settings = TargetSettings.fixedRiskReward("Fixed Risk/Reward", 2.0);

            assertEquals("Fixed Risk/Reward", settings.getTargetModel());
            assertEquals(2.0, settings.getRewardRiskRatio());
        }

        @Test
        void rejectsMissingTargetOptions() {
            assertThrows(IllegalArgumentException.class, () -> new TargetSettings("Target", null, null));
        }
    }

    @Nested
    class OrderSettingsValidation {
        @Test
        void rejectsInvalidQuantityAndOffsets() {
            assertThrows(IllegalArgumentException.class, () -> new OrderSettings(OrderType.MARKET, 0, 0, 0));
            assertThrows(IllegalArgumentException.class, () -> new OrderSettings(OrderType.MARKET, 1, -1, 0));
            assertThrows(IllegalArgumentException.class, () -> new OrderSettings(OrderType.MARKET, 1, 0, -1));
        }
    }
}
