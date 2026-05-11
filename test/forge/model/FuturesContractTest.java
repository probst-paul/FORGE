package forge.model;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Execution(ExecutionMode.CONCURRENT)
class FuturesContractTest {
    @Nested
    class Constructor {
        @Test
        void normalizesSymbolCode() {
            FuturesContract contract = new FuturesContract(
                    " es ",
                    "E-mini S&P 500",
                    0.25,
                    12.50,
                    LocalDate.of(2024, 3, 15)
            );

            assertEquals("ES", contract.getSymbolCode());
            assertEquals("E-mini S&P 500", contract.getDisplayName());
        }

        @Test
        void rejectsInvalidTickSpecifications() {
            LocalDate expirationDate = LocalDate.of(2024, 3, 15);

            assertThrows(IllegalArgumentException.class, () -> new FuturesContract("ES", 0, 12.50, expirationDate));
            assertThrows(IllegalArgumentException.class, () -> new FuturesContract("ES", 0.25, 0, expirationDate));
        }
    }

    @Nested
    class IsExpiredOn {
        @Test
        void returnsTrueOnAndAfterExpiration() {
            FuturesContract contract = new FuturesContract("ES", 0.25, 12.50, LocalDate.of(2024, 3, 15));

            assertFalse(contract.isExpiredOn(LocalDate.of(2024, 3, 14)));
            assertTrue(contract.isExpiredOn(LocalDate.of(2024, 3, 15)));
            assertTrue(contract.isExpiredOn(LocalDate.of(2024, 3, 16)));
        }
    }

    @Nested
    class PriceConversions {
        @Test
        void calculatesDollarValueForTicks() {
            FuturesContract contract = new FuturesContract("ES", 0.25, 12.50, LocalDate.of(2024, 3, 15));

            assertEquals(50.0, contract.calculateDollarValueForTicks(4));
        }

        @Test
        void calculatesTicksForPriceMove() {
            FuturesContract contract = new FuturesContract("NQ", 0.25, 5.00, LocalDate.of(2024, 3, 15));

            assertEquals(8.0, contract.calculateTicksForPriceMove(2.0));
        }
    }

    @Nested
    class Equality {
        @Test
        void usesSymbolAndExpirationDate() {
            FuturesContract march = new FuturesContract("ES", 0.25, 12.50, LocalDate.of(2024, 3, 15));
            FuturesContract sameMarch = new FuturesContract("es", "ES March", 0.25, 12.50, LocalDate.of(2024, 3, 15));
            FuturesContract june = new FuturesContract("ES", 0.25, 12.50, LocalDate.of(2024, 6, 21));

            assertEquals(march, sameMarch);
            assertEquals(march.hashCode(), sameMarch.hashCode());
            assertNotEquals(march, june);
        }
    }
}
