package forge.trade;

import forge.backtest.TradeResult;
import forge.data.market.TradeTick;
import forge.execution.Fill;
import forge.execution.OrderSide;
import forge.execution.OrderType;
import forge.model.FuturesInstrumentSpec;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TradeLifecycleEngineTest {
    private static final FuturesInstrumentSpec ES_SPEC = new FuturesInstrumentSpec("ES", "E-mini S&P 500", 0.25, 12.50);
    private static final ZoneId UTC = ZoneId.of("UTC");

    @Nested
    class PositionLifecycle {
        @Test
        void opensAndClosesLongTradeAtTarget() {
            TradeLifecycleEngine engine = new TradeLifecycleEngine();
            engine.openPosition(
                    fill(OrderSide.BUY, 2, 100, 1),
                    new TradePlan(OrderSide.BUY, 105, 95, LocalTime.of(16, 0), UTC),
                    ES_SPEC
            );

            assertFalse(engine.onTick(tick(104, 2)).isPresent());
            Optional<TradeResult> result = engine.onTick(tick(105, 3));

            assertTrue(result.isPresent());
            TradeResult trade = result.get();
            assertEquals("ES", trade.getInstrumentSymbol());
            assertEquals("ESU25", trade.getContractSymbol());
            assertEquals(OrderSide.BUY, trade.getSide());
            assertEquals(100, trade.getEntryPriceTicks());
            assertEquals(105, trade.getExitPriceTicks());
            assertEquals(10, trade.getGrossTicks());
            assertEquals(125.0, trade.getGrossDollars());
            assertEquals(125.0, trade.getMaxFavorableExcursionDollars());
            assertEquals(0.0, trade.getMaxAdverseExcursionDollars());
            assertEquals(TradeLifecycleEngine.EXIT_REASON_TARGET, trade.getExitReason());
            assertFalse(engine.hasOpenPosition());
        }

        @Test
        void tracksAdverseExcursionAndClosesShortAtStop() {
            TradeLifecycleEngine engine = new TradeLifecycleEngine();
            engine.openPosition(
                    fill(OrderSide.SELL, 1, 100, 1),
                    new TradePlan(OrderSide.SELL, 90, 105, LocalTime.of(16, 0), UTC),
                    ES_SPEC
            );

            assertFalse(engine.onTick(tick(97, 2)).isPresent());
            assertFalse(engine.onTick(tick(103, 3)).isPresent());
            TradeResult trade = engine.onTick(tick(105, 4)).orElseThrow();

            assertEquals(-5, trade.getGrossTicks());
            assertEquals(-62.5, trade.getGrossDollars());
            assertEquals(37.5, trade.getMaxFavorableExcursionDollars());
            assertEquals(-62.5, trade.getMaxAdverseExcursionDollars());
            assertEquals(TradeLifecycleEngine.EXIT_REASON_STOP, trade.getExitReason());
        }

        @Test
        void closesAtTimeStop() {
            TradeLifecycleEngine engine = new TradeLifecycleEngine();
            engine.openPosition(
                    fill(OrderSide.BUY, 1, 100, 1, "2025-08-01T10:29:00Z"),
                    new TradePlan(OrderSide.BUY, 110, 95, LocalTime.of(10, 30), UTC),
                    ES_SPEC
            );

            TradeResult trade = engine.onTick(tick(101, 2, "2025-08-01T10:30:00Z")).orElseThrow();

            assertEquals(TradeLifecycleEngine.EXIT_REASON_TIME_STOP, trade.getExitReason());
            assertEquals(12.5, trade.getGrossDollars());
        }

        @Test
        void rejectsOpeningAnotherPositionWhileOpen() {
            TradeLifecycleEngine engine = new TradeLifecycleEngine();
            TradePlan plan = new TradePlan(OrderSide.BUY, 105, 95, LocalTime.of(16, 0), UTC);
            engine.openPosition(fill(OrderSide.BUY, 1, 100, 1), plan, ES_SPEC);

            assertThrows(IllegalStateException.class, () ->
                    engine.openPosition(fill(OrderSide.BUY, 1, 101, 2), plan, ES_SPEC)
            );
        }
    }

    private Fill fill(OrderSide side, int quantity, long priceTicks, long recordIndex) {
        return fill(side, quantity, priceTicks, recordIndex, "2025-08-01T14:30:00Z");
    }

    private Fill fill(OrderSide side, int quantity, long priceTicks, long recordIndex, String timestamp) {
        return new Fill(
                "ES",
                "ESU25",
                side,
                OrderType.MARKET,
                quantity,
                Instant.parse(timestamp),
                priceTicks,
                recordIndex
        );
    }

    private TradeTick tick(long priceTicks, long recordIndex) {
        return tick(priceTicks, recordIndex, "2025-08-01T14:30:00Z");
    }

    private TradeTick tick(long priceTicks, long recordIndex, String timestamp) {
        return new TradeTick(
                "ESU25",
                Instant.parse(timestamp),
                priceTicks,
                priceTicks - 1,
                priceTicks + 1,
                1,
                1,
                recordIndex
        );
    }
}
