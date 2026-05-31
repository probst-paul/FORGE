package forge.risk;

import forge.config.RiskSettings;
import forge.data.market.TradeTick;
import forge.model.FuturesInstrumentSpec;
import forge.trade.Fill;
import forge.trade.OrderSide;
import forge.trade.OrderType;
import forge.trade.TradeLifecycleEngine;
import forge.trade.TradePlan;
import forge.trade.TradeResult;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RiskManagerTest {
    private static final FuturesInstrumentSpec ES_SPEC = new FuturesInstrumentSpec("ES", "E-mini S&P 500", 0.25, 12.50);
    private static final LocalDate DAY = LocalDate.of(2025, 8, 1);
    private static final ZoneId UTC = ZoneId.of("UTC");

    @Nested
    class EvaluateOpenTrade {
        @Test
        void closesWhenPerTradeLossIsReached() {
            RiskManager riskManager = new RiskManager(new RiskSettings(true, 100, false, 0));
            TradeLifecycleEngine lifecycleEngine = openLongAt(100);

            RiskDecision decision = riskManager.evaluateOpenTrade("ES", DAY, lifecycleEngine, tick(92));

            assertTrue(decision.shouldCloseTrade());
            assertEquals(RiskManager.EXIT_REASON_PER_TRADE_RISK, decision.getExitReason());
        }

        @Test
        void bypassesDisabledPerTradeRisk() {
            RiskManager riskManager = new RiskManager(new RiskSettings(false, 0, false, 0));
            TradeLifecycleEngine lifecycleEngine = openLongAt(100);

            RiskDecision decision = riskManager.evaluateOpenTrade("ES", DAY, lifecycleEngine, tick(1));

            assertFalse(decision.shouldCloseTrade());
        }
    }

    @Nested
    class DailyLockout {
        @Test
        void blocksNewTradesAfterRealizedDailyLossLimit() {
            RiskManager riskManager = new RiskManager(new RiskSettings(false, 0, true, 100));

            riskManager.recordClosedTrade("ES", DAY, trade(-100));

            assertFalse(riskManager.canOpenTrade("ES", DAY));
            assertTrue(riskManager.canOpenTrade("ES", DAY.plusDays(1)));
        }

        @Test
        void closesOpenTradeWhenRealizedPlusUnrealizedDailyLossIsReached() {
            RiskManager riskManager = new RiskManager(new RiskSettings(false, 0, true, 150));
            riskManager.recordClosedTrade("ES", DAY, trade(-50));
            TradeLifecycleEngine lifecycleEngine = openLongAt(100);

            RiskDecision decision = riskManager.evaluateOpenTrade("ES", DAY, lifecycleEngine, tick(92));

            assertTrue(decision.shouldCloseTrade());
            assertEquals(RiskManager.EXIT_REASON_DAILY_RISK, decision.getExitReason());
            assertFalse(riskManager.canOpenTrade("ES", DAY));
        }
    }

    private TradeLifecycleEngine openLongAt(long priceTicks) {
        TradeLifecycleEngine engine = new TradeLifecycleEngine();
        engine.openPosition(
                new Fill("ES", "ESU25", OrderSide.BUY, OrderType.MARKET, 1, Instant.parse("2025-08-01T14:30:00Z"), priceTicks, 1),
                new TradePlan(OrderSide.BUY, priceTicks + 1_000, 1, LocalTime.of(16, 0), UTC),
                ES_SPEC
        );
        return engine;
    }

    private TradeTick tick(long priceTicks) {
        return new TradeTick(
                "ESU25",
                Instant.parse("2025-08-01T14:31:00Z"),
                priceTicks,
                priceTicks - 1,
                priceTicks + 1,
                1,
                1,
                2
        );
    }

    private TradeResult trade(double grossDollars) {
        return new TradeResult(
                "ES",
                "ESU25",
                OrderSide.BUY,
                Instant.parse("2025-08-01T14:30:00Z"),
                100,
                Instant.parse("2025-08-01T14:31:00Z"),
                92,
                1,
                Math.round(grossDollars / ES_SPEC.getTickDollarAmount()),
                grossDollars,
                0,
                grossDollars,
                "TEST"
        );
    }
}
