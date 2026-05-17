package forge.engine;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Execution(ExecutionMode.CONCURRENT)
class MarketContextTest {
    @Nested
    class Constructor {
        @Test
        void normalizesInstrumentSymbolAndStoresMarketState() {
            LocalDateTime timestamp = LocalDateTime.of(2024, 1, 2, 9, 30);

            MarketContext context = new MarketContext(" es ", timestamp, 20001, 0.25, 12.50, true);

            assertEquals("ES", context.getInstrumentSymbol());
            assertEquals(timestamp, context.getTimestamp());
            assertEquals(5000.25, context.getLastPrice());
            assertEquals(20001, context.getLastPriceTicks());
            assertEquals(0.25, context.getTickSize());
            assertEquals(12.50, context.getTickDollarValue());
            assertTrue(context.hasOpenPosition());
        }

        @Test
        void rejectsInvalidInputs() {
            LocalDateTime timestamp = LocalDateTime.of(2024, 1, 2, 9, 30);

            assertThrows(IllegalArgumentException.class, () -> new MarketContext("", timestamp, 5000, false));
            assertThrows(IllegalArgumentException.class, () -> new MarketContext("ES", timestamp, 0, false));
            assertThrows(NullPointerException.class, () -> new MarketContext("ES", null, 5000, false));
            assertThrows(IllegalArgumentException.class, () -> new MarketContext("ES", timestamp, 0, 0.25, 12.50, false));
            assertThrows(IllegalArgumentException.class, () -> new MarketContext("ES", timestamp, 20000, 0, 12.50, false));
            assertThrows(IllegalArgumentException.class, () -> new MarketContext("ES", timestamp, 20000, 0.25, 0, false));
        }
    }
}
