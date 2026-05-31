package forge.engine.backtest;

import forge.config.BacktestRequest;
import forge.config.OrderSettings;
import forge.config.RiskSettings;
import forge.config.StrategyOptions;
import forge.config.MarketEventOptions;
import forge.data.market.ContractTradeWindow;
import forge.data.market.InMemoryTickDataProvider;
import forge.data.market.TradeTick;
import forge.trade.OrderType;
import forge.engine.backtest.BacktestResult;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Execution(ExecutionMode.CONCURRENT)
class BacktestEngineTest {
    private static final ZoneId CENTRAL_TIME = ZoneId.of("America/Chicago");

    @Nested
    class Run {
        @Test
        void processesSelectedContractWindowTicksInBatches() {
            BacktestEngine engine = new BacktestEngine(new InMemoryTickDataProvider(List.of(
                    tick(1),
                    tick(2)
            )));
            List<Long> progressTicks = new ArrayList<>();

            BacktestResult result = engine.run(request(), progress -> progressTicks.add(progress.getProcessedTicks()));

            assertEquals("RangeBreakout", result.getStrategyName());
            assertEquals(List.of("ESU25"), result.getContractSymbols());
            assertEquals(2, result.getTicksProcessed());
            assertEquals(0, result.getOrderSignalsGenerated());
            assertEquals(1, result.getInstrumentResults().size());
            assertEquals("ES", result.getInstrumentResults().get(0).getInstrumentSymbol());
            assertEquals(1, result.getInstrumentResults().get(0).getContractResults().size());
            assertEquals("ESU25", result.getInstrumentResults().get(0).getContractResults().get(0).getContractSymbol());
            assertEquals(List.of(0L, 2L), progressTicks);
        }

        @Test
        void createsTradeFromOpeningRangeSignalAndTradePlan() {
            BacktestEngine engine = new BacktestEngine(new InMemoryTickDataProvider(List.of(
                    tickAtCentral(LocalDate.of(2025, 1, 5), LocalTime.of(17, 0), 100, 1),
                    tickAtCentral(LocalDate.of(2025, 1, 6), LocalTime.of(8, 0), 90, 2),
                    tickAtCentral(LocalDate.of(2025, 1, 6), LocalTime.of(8, 30), 92, 3),
                    tickAtCentral(LocalDate.of(2025, 1, 6), LocalTime.of(9, 29, 59), 98, 4),
                    tickAtCentral(LocalDate.of(2025, 1, 6), LocalTime.of(9, 30), 98, 5),
                    tickAtCentral(LocalDate.of(2025, 1, 6), LocalTime.of(9, 31), 100, 6)
            )));

            BacktestResult result = engine.run(openingRangeRequest());

            assertEquals("OpeningRangeContinuation", result.getStrategyName());
            assertEquals(6, result.getTicksProcessed());
            assertEquals(1, result.getOrderSignalsGenerated());
            assertEquals(1, result.getInstrumentResults().size());
            assertEquals(1, result.getInstrumentResults().get(0).getOrderSignalsGenerated());
            assertEquals(1, result.getInstrumentResults().get(0).getContractResults().size());
            assertEquals(1, result.getInstrumentResults().get(0).getContractResults().get(0).getOrderSignalsGenerated());
            assertEquals(1, result.getInstrumentResults().get(0).getPerformanceMetrics().getTotalTrades());
            assertEquals(25.0, result.getInstrumentResults().get(0).getPerformanceMetrics().getNetProfitLoss());
        }
    }

    private BacktestRequest request() {
        return new BacktestRequest(
                new StrategyOptions("RangeBreakout"),
                List.of(new ContractTradeWindow("ESU25", LocalDate.of(2025, 8, 1), LocalDate.of(2025, 9, 14))),
                new MarketEventOptions("OrderFlowExhaustion"),
                new RiskSettings(500, 1500),
                new OrderSettings(OrderType.MARKET, 1, 0, 0)
        );
    }

    private BacktestRequest openingRangeRequest() {
        return new BacktestRequest(
                new StrategyOptions("OpeningRangeContinuation"),
                List.of(new ContractTradeWindow("ESU25", LocalDate.of(2025, 1, 5), LocalDate.of(2025, 1, 6))),
                new MarketEventOptions("PriceCrossover"),
                new RiskSettings(500, 1500),
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

    private TradeTick tickAtCentral(LocalDate date, LocalTime time, long priceTicks, long scidRecordIndex) {
        LocalDateTime utcDateTime = LocalDateTime.of(date, time)
                .atZone(CENTRAL_TIME)
                .withZoneSameInstant(ZoneOffset.UTC)
                .toLocalDateTime();
        return new TradeTick(
                "ESU25",
                utcDateTime.toInstant(ZoneOffset.UTC),
                priceTicks,
                priceTicks - 1,
                priceTicks + 1,
                1,
                1,
                scidRecordIndex
        );
    }
}
