package forge.data;

import forge.data.InstrumentDataCatalog.AvailableDateRange;
import forge.data.InstrumentDataCatalog.AvailableInstrumentData;
import forge.model.FuturesContract;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Execution(ExecutionMode.CONCURRENT)
class InstrumentDataCatalogTest {
    private final InstrumentDataCatalog catalog = new InstrumentDataCatalog();

    @Nested
    class GetAvailableInstruments {
        @Test
        void returnsConfiguredFuturesContracts() {
            List<AvailableInstrumentData> instruments = catalog.getAvailableInstruments();

            assertEquals(3, instruments.size());
            assertEquals("ES", instruments.get(0).getSymbol());
            assertInstanceOf(FuturesContract.class, instruments.get(0).getInstrument());
            assertEquals(0.25, instruments.get(0).getFuturesTickSize());
            assertEquals(12.50, instruments.get(0).getFuturesTickDollarAmount());
        }
    }

    @Nested
    class GetSharedDateRange {
        @Test
        void returnsOverlapForSelectedSymbols() {
            AvailableDateRange range = catalog.getSharedDateRange(List.of("ES", "CL"));

            assertEquals(LocalDate.of(2024, 2, 1), range.getStartDate());
            assertEquals(LocalDate.of(2024, 3, 15), range.getEndDate());
        }

        @Test
        void rejectsUnknownSymbols() {
            assertThrows(IllegalArgumentException.class, () -> catalog.getSharedDateRange(List.of("GC")));
        }
    }

    @Nested
    class ValidateDateRange {
        @Test
        void acceptsDatesInsideSharedRange() {
            catalog.validateDateRange(
                    List.of("ES", "NQ"),
                    LocalDate.of(2024, 1, 2),
                    LocalDate.of(2024, 3, 29)
            );
        }

        @Test
        void rejectsDatesOutsideSharedRange() {
            assertThrows(IllegalArgumentException.class, () -> catalog.validateDateRange(
                    List.of("ES", "CL"),
                    LocalDate.of(2024, 1, 2),
                    LocalDate.of(2024, 3, 15)
            ));
        }
    }
}
