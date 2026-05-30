package forge.model;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
        void reportsSupportedContractMonths() {
            assertTrue(provider.getBySymbol("ES").supportsMonthCode("H"));
            assertTrue(provider.getBySymbol("ES").supportsMonthCode("M"));
            assertTrue(provider.getBySymbol("ES").supportsMonthCode("U"));
            assertTrue(provider.getBySymbol("ES").supportsMonthCode("Z"));
            assertFalse(provider.getBySymbol("ES").supportsMonthCode("K"));
            assertTrue(provider.getBySymbol("CL").supportsMonthCode("K"));
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

    @Nested
    class PriceConversion {
        @Test
        void displaysNormalizedTickPrices() {
            FuturesInstrumentSpec es = provider.getBySymbol("ES");

            assertEquals(6476.75, es.displayPrice(25907));
            assertEquals(8, es.contractTicksBetween(25907, 25915));
        }

        @Test
        void treatsStoredValuesAsNormalizedTickCounts() {
            FuturesInstrumentSpec es = provider.getBySymbol("ES");

            assertEquals(161918.75, es.displayPrice(647675));
            assertEquals(200, es.contractTicksBetween(647675, 647875));
        }

        @Test
        void displaysCentScaledTickCounts() {
            FuturesInstrumentSpec es = provider.getBySymbol("ES");

            assertEquals(6233.25, es.displayPrice(2_493_300));
            assertEquals(14, es.contractTicksBetween(2_493_300, 2_494_700));
        }

        @Test
        void displaysWholePointInstrumentPricesWithoutScaling() {
            FuturesInstrumentSpec ym = provider.getBySymbol("YM");

            assertEquals(46934.0, ym.displayPrice(46934));
            assertEquals(-27, ym.contractTicksBetween(46934, 46907));
        }
    }
}
