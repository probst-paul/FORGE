package forge.execution;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Execution(ExecutionMode.CONCURRENT)
class OrderRequestTest {
    @Nested
    class Constructor {
        @Test
        void normalizesInstrumentSymbolAndStoresOrderDetails() {
            OrderRequest request = new OrderRequest(" es ", OrderSide.BUY, OrderType.LIMIT, 2, 5000.25, null);

            assertEquals("ES", request.getInstrumentSymbol());
            assertEquals(OrderSide.BUY, request.getSide());
            assertEquals(OrderType.LIMIT, request.getOrderType());
            assertEquals(2, request.getQuantity());
            assertEquals(5000.25, request.getLimitPrice());
            assertNull(request.getStopPrice());
        }

        @Test
        void rejectsInvalidInputs() {
            assertThrows(IllegalArgumentException.class, () -> new OrderRequest("", OrderSide.BUY, OrderType.MARKET, 1, null, null));
            assertThrows(IllegalArgumentException.class, () -> new OrderRequest("ES", OrderSide.BUY, OrderType.MARKET, 0, null, null));
            assertThrows(NullPointerException.class, () -> new OrderRequest("ES", null, OrderType.MARKET, 1, null, null));
            assertThrows(NullPointerException.class, () -> new OrderRequest("ES", OrderSide.BUY, null, 1, null, null));
        }
    }

    @Nested
    class Market {
        @Test
        void createsMarketOrderWithoutLimitOrStopPrices() {
            OrderRequest request = OrderRequest.market("nq", OrderSide.SELL, 3);

            assertEquals("NQ", request.getInstrumentSymbol());
            assertEquals(OrderSide.SELL, request.getSide());
            assertEquals(OrderType.MARKET, request.getOrderType());
            assertEquals(3, request.getQuantity());
            assertNull(request.getLimitPrice());
            assertNull(request.getStopPrice());
        }
    }
}
