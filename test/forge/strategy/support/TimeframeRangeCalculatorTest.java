package forge.strategy.support;

import forge.data.market.TradeTick;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Execution(ExecutionMode.CONCURRENT)
class TimeframeRangeCalculatorTest {
    private final TimeframeRangeCalculator calculator = new TimeframeRangeCalculator();

    @Nested
    class CalculatePriceRange {
        @Test
        void returnsLowAndHighTicksInsideTimeframe() {
            List<TradeTick> ticks = List.of(
                    tick("2025-09-15T13:29:59Z", 20000),
                    tick("2025-09-15T13:30:00Z", 20010),
                    tick("2025-09-15T13:31:00Z", 20002),
                    tick("2025-09-15T13:32:00Z", 20015),
                    tick("2025-09-15T13:33:00Z", 19999)
            );

            Optional<PriceRange> range = calculator.calculatePriceRange(
                    ticks,
                    Instant.parse("2025-09-15T13:30:00Z"),
                    Instant.parse("2025-09-15T13:33:00Z")
            );

            assertTrue(range.isPresent());
            assertEquals(20002, range.get().getLowPriceTicks());
            assertEquals(20015, range.get().getHighPriceTicks());
            assertEquals(13, range.get().getRangeTicks());
        }

        @Test
        void treatsStartAsInclusiveAndEndAsExclusive() {
            List<TradeTick> ticks = List.of(
                    tick("2025-09-15T13:30:00Z", 20010),
                    tick("2025-09-15T13:31:00Z", 20020)
            );

            Optional<PriceRange> range = calculator.calculatePriceRange(
                    ticks,
                    Instant.parse("2025-09-15T13:30:00Z"),
                    Instant.parse("2025-09-15T13:31:00Z")
            );

            assertTrue(range.isPresent());
            assertEquals(20010, range.get().getLowPriceTicks());
            assertEquals(20010, range.get().getHighPriceTicks());
        }

        @Test
        void returnsEmptyWhenNoTicksFallInsideTimeframe() {
            Optional<PriceRange> range = calculator.calculatePriceRange(
                    List.of(tick("2025-09-15T13:29:59Z", 20010)),
                    Instant.parse("2025-09-15T13:30:00Z"),
                    Instant.parse("2025-09-15T13:31:00Z")
            );

            assertTrue(range.isEmpty());
        }

        @Test
        void rejectsInvalidTimeframe() {
            Instant start = Instant.parse("2025-09-15T13:30:00Z");
            Instant end = Instant.parse("2025-09-15T13:30:00Z");

            assertThrows(IllegalArgumentException.class, () -> calculator.calculatePriceRange(List.of(), start, end));
        }
    }

    @Nested
    class PriceConversion {
        @Test
        void convertsTickRangeToDisplayPrices() {
            PriceRange range = new PriceRange(20000, 20010);

            assertEquals(5000.0, range.getLowPrice(0.25));
            assertEquals(5002.5, range.getHighPrice(0.25));
        }
    }

    private TradeTick tick(String timestamp, long priceTicks) {
        return new TradeTick(
                "ESU25",
                Instant.parse(timestamp),
                priceTicks,
                priceTicks - 1,
                priceTicks + 1,
                1,
                1,
                priceTicks
        );
    }
}
