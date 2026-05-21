package forge.execution;

import forge.data.market.TradeTick;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimpleExecutionEngineTest {
    @Nested
    class Execute {
        @Test
        void fillsOrderAtCurrentTickPrice() {
            SimpleExecutionEngine engine = new SimpleExecutionEngine();
            TradeTick tick = tick(24_000, 42);

            Fill fill = engine.execute(OrderRequest.market("es", OrderSide.BUY, 2), tick).orElseThrow();

            assertEquals("ES", fill.getInstrumentSymbol());
            assertEquals("ESU25", fill.getContractSymbol());
            assertEquals(OrderSide.BUY, fill.getSide());
            assertEquals(OrderType.MARKET, fill.getOrderType());
            assertEquals(2, fill.getQuantity());
            assertEquals(Instant.parse("2025-08-01T14:30:00Z"), fill.getFillTime());
            assertEquals(24_000, fill.getFillPriceTicks());
            assertEquals(42, fill.getScidRecordIndex());
        }

        @Test
        void rejectsMissingInputs() {
            SimpleExecutionEngine engine = new SimpleExecutionEngine();

            assertThrows(NullPointerException.class, () -> engine.execute(null, tick(24_000, 1)));
            assertThrows(NullPointerException.class, () -> engine.execute(OrderRequest.market("ES", OrderSide.BUY, 1), null));
        }

        @Test
        void currentlyFillsLimitOrdersAtCurrentTickPriceUntilLimitSimulationIsAdded() {
            SimpleExecutionEngine engine = new SimpleExecutionEngine();
            OrderRequest request = new OrderRequest("ES", OrderSide.SELL, OrderType.LIMIT, 1, 6000.0, null);

            assertTrue(engine.execute(request, tick(24_010, 3)).isPresent());
            assertEquals(OrderType.LIMIT, engine.execute(request, tick(24_010, 3)).orElseThrow().getOrderType());
        }
    }

    private TradeTick tick(long priceTicks, long recordIndex) {
        return new TradeTick(
                "ESU25",
                Instant.parse("2025-08-01T14:30:00Z"),
                priceTicks,
                priceTicks - 1,
                priceTicks + 1,
                1,
                1,
                recordIndex
        );
    }
}
