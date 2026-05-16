package forge.model;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Execution(ExecutionMode.CONCURRENT)
class StaticFuturesInstrumentSpecProviderTest {
    private final StaticFuturesInstrumentSpecProvider provider = new StaticFuturesInstrumentSpecProvider();

    @Nested
    class GetBySymbol {
        @Test
        void returnsSupportedInstrumentSpecs() {
            assertEquals(0.25, provider.getBySymbol("ES").getTickSize());
            assertEquals(12.50, provider.getBySymbol("ES").getTickDollarAmount());
            assertEquals(0.25, provider.getBySymbol("NQ").getTickSize());
            assertEquals(5.00, provider.getBySymbol("YM").getTickDollarAmount());
            assertEquals(0.10, provider.getBySymbol("RTY").getTickSize());
            assertEquals(0.01, provider.getBySymbol("CL").getTickSize());
        }

        @Test
        void normalizesSymbols() {
            assertEquals("ES", provider.getBySymbol(" es ").getSymbolCode());
        }

        @Test
        void rejectsUnsupportedSymbols() {
            assertThrows(IllegalArgumentException.class, () -> provider.getBySymbol("GC"));
        }
    }

    @Nested
    class Supports {
        @Test
        void reportsSupportedSymbols() {
            assertTrue(provider.supports("ES"));
        }
    }
}
