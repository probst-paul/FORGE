package forge.config;

import forge.data.market.ContractTradeWindow;
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
        void storesSelectedContractWindows() {
            BacktestRequest request = new BacktestRequest(
                    new StrategyOptions("RangeBreakout"),
                    List.of(
                            new ContractTradeWindow("ESH25", LocalDate.of(2024, 12, 16), LocalDate.of(2025, 3, 16)),
                            new ContractTradeWindow("ESZ25", LocalDate.of(2025, 9, 15), LocalDate.of(2025, 12, 14))
                    ),
                    new TradeTriggerOptions("OrderFlowExhaustion"),
                    new RiskSettings(500, 1500),
                    TargetSettings.fixedRiskReward("Fixed Risk/Reward", 2),
                    new OrderSettings(OrderType.MARKET, 1, 0, 0)
            );

            assertEquals(List.of("ESH25", "ESZ25"), request.getInstruments());
            assertEquals(2, request.getContractWindows().size());
            assertEquals(LocalDate.of(2024, 12, 16), request.getStartDate());
            assertEquals(LocalDate.of(2025, 12, 14), request.getEndDate());
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
