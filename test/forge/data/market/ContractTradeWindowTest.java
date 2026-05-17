package forge.data.market;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Execution(ExecutionMode.CONCURRENT)
class ContractTradeWindowTest {
    @Nested
    class Constructor {
        @Test
        void normalizesContractSymbolAndConvertsInclusiveDatesToUtcBounds() {
            ContractTradeWindow window = new ContractTradeWindow(
                    " esu25 ",
                    LocalDate.of(2025, 8, 1),
                    LocalDate.of(2025, 9, 14)
            );

            assertEquals("ESU25", window.getContractSymbol());
            assertEquals(Instant.parse("2025-08-01T00:00:00Z"), window.getStartInclusiveInstant());
            assertEquals(Instant.parse("2025-09-15T00:00:00Z"), window.getEndExclusiveInstant());
        }

        @Test
        void rejectsInvalidInputs() {
            assertThrows(IllegalArgumentException.class, () -> new ContractTradeWindow(
                    "",
                    LocalDate.of(2025, 8, 1),
                    LocalDate.of(2025, 9, 14)
            ));
            assertThrows(IllegalArgumentException.class, () -> new ContractTradeWindow(
                    "ESU25",
                    LocalDate.of(2025, 9, 15),
                    LocalDate.of(2025, 9, 14)
            ));
        }
    }
}
