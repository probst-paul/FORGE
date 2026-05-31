package forge.reporting.backtest;

import forge.engine.backtest.BacktestResult;
import forge.engine.backtest.ContractBacktestResult;
import forge.engine.backtest.InstrumentBacktestResult;
import forge.reporting.FacadeForgeReporting;
import forge.trade.OrderSide;
import forge.trade.TradeResult;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Execution(ExecutionMode.CONCURRENT)
class FacadeForgeReportingTest {
    @Nested
    class BuildBacktestReport {
        @Test
        void createsReportSectionsFromRealBacktestResult() {
            BacktestResult result = new BacktestResult(
                    "OpeningRangeContinuation",
                    List.of(new InstrumentBacktestResult(
                            "ES",
                            List.of(new ContractBacktestResult(
                                    "ESU25",
                                    100,
                                    2,
                                    List.of(trade())
                            ))
                    ))
            );

            BacktestReport report = FacadeForgeReporting.getTheInstance()
                    .forgeReportingAccess()
                    .buildBacktestReport(result);

            assertEquals("OpeningRangeContinuation", report.getStrategyName());
            assertEquals(List.of("ESU25"), report.getContractSymbols());
            assertEquals(100, report.getTicksProcessed());
            assertEquals(2, report.getOrderSignalsGenerated());
            assertEquals(1, report.getInstrumentReports().size());
            assertEquals(1, report.getContractReports().size());
            assertEquals(1, report.getTrades().size());
            assertEquals("ES", report.getInstrumentReports().get(0).getInstrumentSymbol());
            assertEquals("ESU25", report.getContractReports().get(0).getContractSymbol());
            assertEquals(50, report.getTrades().get(0).getGrossDollars(), 0.0001);
        }
    }

    @Nested
    class Summarize {
        @Test
        void rendersReportTextFromBuiltReport() {
            BacktestResult result = new BacktestResult(
                    "RangeBreakout",
                    List.of(new InstrumentBacktestResult(
                            "NQ",
                            List.of(new ContractBacktestResult(
                                    "NQU25",
                                    20,
                                    1,
                                    List.of(trade())
                            ))
                    ))
            );

            String summary = FacadeForgeReporting.getTheInstance()
                    .forgeReportingAccess()
                    .summarize(result);

            assertTrue(summary.contains("Strategy: RangeBreakout"));
            assertTrue(summary.contains("NQ Summary"));
            assertTrue(summary.contains("NQU25"));
            assertTrue(summary.contains("Total Trades: 1"));
        }
    }

    private TradeResult trade() {
        return new TradeResult(
                "ES",
                "ESU25",
                OrderSide.BUY,
                Instant.parse("2025-08-01T14:30:00Z"),
                25000,
                Instant.parse("2025-08-01T14:31:00Z"),
                25004,
                1,
                4,
                50,
                75,
                -25,
                "TARGET"
        );
    }
}
