package forge.config;

import forge.execution.OrderType;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Execution(ExecutionMode.CONCURRENT)
class BacktestRequestTest {
    @Nested
    class Constructor {
        @Test
        void normalizesInstrumentSymbols() {
            BacktestRequest request = validRequest(List.of(" es ", "nq"));

            assertEquals(List.of("ES", "NQ"), request.getInstruments());
        }

        @Test
        void rejectsEmptyInstrumentList() {
            assertThrows(IllegalArgumentException.class, () -> validRequest(List.of()));
        }

        @Test
        void rejectsEndDateBeforeStartDate() {
            assertThrows(IllegalArgumentException.class, () -> new BacktestRequest(
                    new StrategyOptions("RangeBreakout"),
                    List.of("ES"),
                    LocalDate.of(2024, 1, 31),
                    LocalDate.of(2024, 1, 1),
                    new TradeTriggerOptions("OrderFlowExhaustion"),
                    new RiskSettings(500, 1500),
                    TargetSettings.fixedRiskReward("Fixed Risk/Reward", 2),
                    new OrderSettings(OrderType.MARKET, 1, 0, 0)
            ));
        }
    }

    private BacktestRequest validRequest(List<String> instruments) {
        return new BacktestRequest(
                new StrategyOptions("RangeBreakout"),
                instruments,
                LocalDate.of(2024, 1, 2),
                LocalDate.of(2024, 1, 31),
                new TradeTriggerOptions("OrderFlowExhaustion"),
                new RiskSettings(500, 1500),
                TargetSettings.fixedRiskReward("Fixed Risk/Reward", 2),
                new OrderSettings(OrderType.MARKET, 1, 0, 0)
        );
    }
}
