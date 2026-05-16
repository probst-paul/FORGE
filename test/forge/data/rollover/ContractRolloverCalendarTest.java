package forge.data.rollover;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Execution(ExecutionMode.CONCURRENT)
class ContractRolloverCalendarTest {
    private final ContractRolloverCalendar calendar = new ContractRolloverCalendar();

    @Nested
    class FindActiveWindow {
        @Test
        void calculatesEquityIndexQuarterlyWindowsFromMondayBeforeThirdFriday() {
            Optional<ContractRolloverWindow> window = calendar.findActiveWindow("ESM25");

            assertTrue(window.isPresent());
            assertEquals(LocalDate.of(2025, 3, 17), window.get().getActiveStartDate());
            assertEquals(LocalDate.of(2025, 6, 15), window.get().getActiveEndDate());
        }

        @Test
        void supportsPriorYearPreviousContractForMarchContracts() {
            Optional<ContractRolloverWindow> window = calendar.findActiveWindow("ESH25");

            assertTrue(window.isPresent());
            assertEquals(LocalDate.of(2024, 12, 16), window.get().getActiveStartDate());
            assertEquals(LocalDate.of(2025, 3, 16), window.get().getActiveEndDate());
        }

        @Test
        void returnsEmptyWhenNoRolloverRuleApplies() {
            Optional<ContractRolloverWindow> window = calendar.findActiveWindow("GCZ25");

            assertTrue(window.isEmpty());
        }

        @Test
        void calculatesCrudeOilMonthlyWindowsFromFridayBeforeExpiration() {
            Optional<ContractRolloverWindow> window = calendar.findActiveWindow("CLX25");

            assertTrue(window.isPresent());
            assertEquals(LocalDate.of(2025, 9, 19), window.get().getActiveStartDate());
            assertEquals(LocalDate.of(2025, 10, 16), window.get().getActiveEndDate());
        }

        @Test
        void calculatesCrudeOilJanuaryWindowFromPriorDecemberContract() {
            Optional<ContractRolloverWindow> window = calendar.findActiveWindow("CLF26");

            assertTrue(window.isPresent());
            assertEquals(LocalDate.of(2025, 11, 14), window.get().getActiveStartDate());
            assertEquals(LocalDate.of(2025, 12, 18), window.get().getActiveEndDate());
        }
    }
}
