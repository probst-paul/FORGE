package forge.config;

import forge.trade.OrderType;
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
    class MarketEventOptionsValidation {
        @Test
        void preservesParametersAsReadOnlyMap() {
            Map<String, String> parameters = new LinkedHashMap<>();
            parameters.put("imbalanceRatio", "2.5");

            MarketEventOptions options = new MarketEventOptions("OrderFlowExhaustion", parameters);

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
            assertThrows(IllegalArgumentException.class, () -> new RiskSettings(true, 0, false, 0));
            assertThrows(IllegalArgumentException.class, () -> new RiskSettings(false, 0, true, 0));
            assertThrows(IllegalArgumentException.class, () -> new RiskSettings(true, 500, true, 400));
        }

        @Test
        void allowsDisabledRiskLimitsToUseZeroValues() {
            RiskSettings settings = new RiskSettings(false, 0, false, 0);

            assertEquals(false, settings.isPerTradeRiskEnabled());
            assertEquals(0, settings.getRiskPerTrade());
            assertEquals(false, settings.isDailyRiskEnabled());
            assertEquals(0, settings.getMaxDailyLoss());
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
