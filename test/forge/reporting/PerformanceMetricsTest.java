package forge.reporting;

import forge.backtest.TradeResult;
import forge.execution.OrderSide;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Execution(ExecutionMode.CONCURRENT)
class PerformanceMetricsTest {
    @Nested
    class FromTrades {
        @Test
        void calculatesProfitLossAndExcursionMetrics() {
            PerformanceMetrics metrics = PerformanceMetrics.fromTrades(List.of(
                    trade(100, 150, -25),
                    trade(-50, 40, -75),
                    trade(25, 60, -10)
            ));

            assertEquals(3, metrics.getTotalTrades());
            assertEquals(2, metrics.getWinningTrades());
            assertEquals(1, metrics.getLosingTrades());
            assertEquals(66.6667, metrics.getWinRate(), 0.0001);
            assertEquals(75, metrics.getNetProfitLoss(), 0.0001);
            assertEquals(125, metrics.getGrossProfit(), 0.0001);
            assertEquals(-50, metrics.getGrossLoss(), 0.0001);
            assertEquals(25, metrics.getAverageTrade(), 0.0001);
            assertEquals(62.5, metrics.getAverageWinningTrade(), 0.0001);
            assertEquals(-50, metrics.getAverageLosingTrade(), 0.0001);
            assertEquals(2.5, metrics.getProfitFactor(), 0.0001);
            assertEquals(-50, metrics.getMaximumDrawdown(), 0.0001);
            assertEquals(100, metrics.getMaximumRunup(), 0.0001);
            assertEquals(150, metrics.getMaxFavorableExcursion(), 0.0001);
            assertEquals(83.3333, metrics.getAverageFavorableExcursion(), 0.0001);
            assertEquals(-75, metrics.getMaxAdverseExcursion(), 0.0001);
            assertEquals(-36.6667, metrics.getAverageAdverseExcursion(), 0.0001);
        }
    }

    private TradeResult trade(double grossDollars, double favorableExcursion, double adverseExcursion) {
        return new TradeResult(
                "ES",
                "ESU25",
                OrderSide.BUY,
                Instant.parse("2025-08-01T14:30:00Z"),
                24000,
                Instant.parse("2025-08-01T14:31:00Z"),
                24001,
                1,
                1,
                grossDollars,
                favorableExcursion,
                adverseExcursion,
                "TEST"
        );
    }
}
