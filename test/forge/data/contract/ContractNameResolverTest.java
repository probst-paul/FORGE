package forge.data.contract;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Execution(ExecutionMode.CONCURRENT)
class ContractNameResolverTest {
    private final ContractNameResolver resolver = new ContractNameResolver();

    @Nested
    class ResolveFromScidPath {
        @Test
        void resolvesFiveCharacterContractBeforeUnderscore() {
            assertEquals("ESU25", resolver.resolveFromScidPath("/data/ESU25_FUT_CME.scid"));
        }

        @Test
        void resolvesFourCharacterContractBeforeDot() {
            assertEquals("ESZ5", resolver.resolveFromScidPath("/data/ESZ5.CME.scid"));
        }

        @Test
        void resolvesWindowsStylePaths() {
            assertEquals("NQM26", resolver.resolveFromScidPath("C:\\data\\NQM26_FUT_CME.scid"));
        }

        @Test
        void resolvesContractParts() {
            assertEquals("RTY", resolver.resolveInstrumentSymbol("RTYM26"));
            assertEquals("M", resolver.resolveContractMonthCode("RTYM26"));
            assertEquals("26", resolver.resolveContractYear("RTYM26"));
        }

        @Test
        void resolvesContractCodeWithFullYear() {
            FuturesContractCode contractCode = resolver.resolveContractCode("ESZ5");

            assertEquals("ES", contractCode.getInstrumentSymbol());
            assertEquals("Z", contractCode.getMonthCode());
            assertEquals(2025, contractCode.getYear());
        }

        @Test
        void rejectsFilesThatDoNotLookLikeFuturesContracts() {
            assertThrows(IllegalArgumentException.class, () -> resolver.resolveFromScidPath("/data/BAD_FILE.scid"));
        }
    }
}
