package forge.data.catalog;

import forge.data.catalog.InstrumentDataCatalog.AvailableDateRange;
import forge.data.catalog.InstrumentDataCatalog.AvailableInstrumentData;
import forge.data.contract.ContractNameResolver;
import forge.data.rollover.ContractRolloverCalendar;
import forge.model.FuturesInstrument;
import forge.model.StaticFuturesInstrumentSpecProvider;
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
    private final InstrumentDataCatalog catalog = new InstrumentDataCatalog(
            () -> List.of(
                    new ContractDataSummary("ESU25", LocalDate.of(2025, 8, 1), LocalDate.of(2025, 9, 15)),
                    new ContractDataSummary("ESZ25", LocalDate.of(2025, 10, 1), LocalDate.of(2025, 12, 15)),
                    new ContractDataSummary("NQZ25", LocalDate.of(2025, 11, 1), LocalDate.of(2025, 12, 12)),
                    new ContractDataSummary("CLX25", LocalDate.of(2025, 9, 1), LocalDate.of(2025, 10, 20))
            ),
            new ContractNameResolver(),
            new StaticFuturesInstrumentSpecProvider(),
            new ContractRolloverCalendar()
    );

    @Nested
    class GetAvailableInstruments {
        @Test
        void returnsInstrumentRootsFromImportedContracts() {
            List<AvailableInstrumentData> instruments = catalog.getAvailableInstruments();

            assertEquals(3, instruments.size());
            assertEquals("ES", instruments.get(0).getSymbol());
            assertInstanceOf(FuturesInstrument.class, instruments.get(0).getInstrument());
            assertEquals(0.25, instruments.get(0).getFuturesTickSize());
            assertEquals(12.50, instruments.get(0).getFuturesTickDollarAmount());
            assertEquals(LocalDate.of(2025, 8, 1), instruments.get(0).getStartDate());
            assertEquals(LocalDate.of(2025, 12, 14), instruments.get(0).getEndDate());
        }

        @Test
        void omitsContractTablesOutsideTheirActiveRolloverWindow() {
            InstrumentDataCatalog clippedCatalog = new InstrumentDataCatalog(
                    () -> List.of(
                            new ContractDataSummary("ESH25", LocalDate.of(2025, 3, 17), LocalDate.of(2025, 3, 18)),
                            new ContractDataSummary("ESM25", LocalDate.of(2025, 3, 17), LocalDate.of(2025, 6, 15))
                    ),
                    new ContractNameResolver(),
                    new StaticFuturesInstrumentSpecProvider(),
                    new ContractRolloverCalendar()
            );

            List<AvailableInstrumentData> instruments = clippedCatalog.getAvailableInstruments();

            assertEquals(1, instruments.size());
            assertEquals("ES", instruments.get(0).getSymbol());
            assertEquals(LocalDate.of(2025, 3, 17), instruments.get(0).getStartDate());
            assertEquals(LocalDate.of(2025, 6, 15), instruments.get(0).getEndDate());
        }
    }

    @Nested
    class GetSharedDateRange {
        @Test
        void returnsOverlapForSelectedSymbols() {
            AvailableDateRange range = catalog.getSharedDateRange(List.of("ES", "CL"));

            assertEquals(LocalDate.of(2025, 9, 19), range.getStartDate());
            assertEquals(LocalDate.of(2025, 10, 16), range.getEndDate());
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
                    LocalDate.of(2025, 11, 1),
                    LocalDate.of(2025, 12, 12)
            );
        }

        @Test
        void rejectsDatesOutsideSharedRange() {
            assertThrows(IllegalArgumentException.class, () -> catalog.validateDateRange(
                    List.of("ES", "CL"),
                    LocalDate.of(2025, 8, 1),
                    LocalDate.of(2025, 10, 20)
            ));
        }
    }
}
