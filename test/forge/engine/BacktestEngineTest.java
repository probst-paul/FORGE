package forge.engine;

import forge.config.BacktestRequest;
import forge.config.OrderSettings;
import forge.config.RiskSettings;
import forge.config.StrategyOptions;
import forge.config.TargetSettings;
import forge.config.TradeTriggerOptions;
import forge.data.market.ContractTradeWindow;
import forge.data.market.InMemoryTickDataProvider;
import forge.data.market.TradeTick;
import forge.execution.OrderType;
import forge.reporting.BacktestResult;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Execution(ExecutionMode.CONCURRENT)
class BacktestEngineTest {
    @Nested
    class Run {
        @Test
        void processesSelectedContractWindowTicksInBatches() {
            BacktestEngine engine = new BacktestEngine(new InMemoryTickDataProvider(List.of(
                    tick(1),
                    tick(2)
            )));

            BacktestResult result = engine.run(request());

            assertEquals("RangeBreakout", result.getStrategyName());
            assertEquals(List.of("ESU25"), result.getContractSymbols());
            assertEquals(2, result.getTicksProcessed());
            assertEquals(0, result.getOrderSignalsGenerated());
        }
    }

    private BacktestRequest request() {
        return new BacktestRequest(
                new StrategyOptions("RangeBreakout"),
                List.of(new ContractTradeWindow("ESU25", LocalDate.of(2025, 8, 1), LocalDate.of(2025, 9, 14))),
                new TradeTriggerOptions("OrderFlowExhaustion"),
                new RiskSettings(500, 1500),
                TargetSettings.fixedRiskReward("Fixed Risk/Reward", 2),
                new OrderSettings(OrderType.MARKET, 1, 0, 0)
        );
    }

    private TradeTick tick(long scidRecordIndex) {
        return new TradeTick(
                "ESU25",
                Instant.parse("2025-08-01T14:30:00Z").plusSeconds(scidRecordIndex),
                24000 + scidRecordIndex,
                23999 + scidRecordIndex,
                24001 + scidRecordIndex,
                1,
                1,
                scidRecordIndex
        );
    }
}
