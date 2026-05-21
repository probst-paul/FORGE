package forge.feature;

import forge.data.market.TradeTick;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SessionRangeFeatureCalculatorTest {
    private final SessionRangeFeatureCalculator calculator = new SessionRangeFeatureCalculator();

    @Test
    void calculatesOvernightFirstHourAndRthRangesForTradingDay() {
        List<TradeTick> ticks = List.of(
                tick("ESU25", LocalDate.of(2025, 1, 5), LocalTime.of(17, 0), 100, 1),
                tick("ESU25", LocalDate.of(2025, 1, 6), LocalTime.of(8, 29, 59), 90, 2),
                tick("ESU25", LocalDate.of(2025, 1, 6), LocalTime.of(8, 30), 95, 3),
                tick("ESU25", LocalDate.of(2025, 1, 6), LocalTime.of(9, 29, 59), 110, 4),
                tick("ESU25", LocalDate.of(2025, 1, 6), LocalTime.of(10, 0), 80, 5),
                tick("ESU25", LocalDate.of(2025, 1, 6), LocalTime.of(16, 59, 59), 120, 6)
        );

        List<SessionRangeFeature> features = calculator.calculate(ticks);

        assertEquals(1, features.size());
        SessionRangeFeature feature = features.get(0);
        assertEquals("ESU25", feature.getContractSymbol());
        assertEquals(LocalDate.of(2025, 1, 6), feature.getSessionDate());
        assertEquals(90, feature.getOvernightLowTicks());
        assertEquals(100, feature.getOvernightHighTicks());
        assertEquals(95, feature.getFirstHourLowTicks());
        assertEquals(110, feature.getFirstHourHighTicks());
        assertEquals(80, feature.getRthLowTicks());
        assertEquals(120, feature.getRthHighTicks());
    }

    @Test
    void eveningTicksAfterRthBelongToNextTradingDay() {
        List<TradeTick> ticks = List.of(
                tick("ESU25", LocalDate.of(2025, 1, 6), LocalTime.of(17, 0), 200, 1),
                tick("ESU25", LocalDate.of(2025, 1, 7), LocalTime.of(8, 30), 210, 2),
                tick("ESU25", LocalDate.of(2025, 1, 7), LocalTime.of(9, 30), 205, 3)
        );

        List<SessionRangeFeature> features = calculator.calculate(ticks);

        assertEquals(1, features.size());
        assertEquals(LocalDate.of(2025, 1, 7), features.get(0).getSessionDate());
        assertEquals(200, features.get(0).getOvernightLowTicks());
        assertEquals(200, features.get(0).getOvernightHighTicks());
    }

    @Test
    void skipsTradingDaysMissingAnyRequiredRange() {
        List<TradeTick> ticks = List.of(
                tick("ESU25", LocalDate.of(2025, 1, 6), LocalTime.of(8, 30), 210, 1),
                tick("ESU25", LocalDate.of(2025, 1, 6), LocalTime.of(10, 0), 205, 2)
        );

        assertEquals(List.of(), calculator.calculate(ticks));
    }

    @Test
    void facadeCalculatesSessionRanges() {
        List<TradeTick> ticks = List.of(
                tick("ESU25", LocalDate.of(2025, 1, 5), LocalTime.of(17, 0), 100, 1),
                tick("ESU25", LocalDate.of(2025, 1, 6), LocalTime.of(8, 30), 95, 2),
                tick("ESU25", LocalDate.of(2025, 1, 6), LocalTime.of(9, 30), 105, 3)
        );

        List<SessionRangeFeature> features = FacadeForgeFeature.getTheInstance()
                .forgeFeatureAccess()
                .calculateSessionRanges(ticks);

        assertEquals(1, features.size());
    }

    private TradeTick tick(
            String contractSymbol,
            LocalDate centralDate,
            LocalTime centralTime,
            long priceTicks,
            long scidRecordIndex
    ) {
        Instant instant = ZonedDateTime.of(
                centralDate,
                centralTime,
                TradingDayClassifier.CENTRAL_TIME
        ).toInstant();
        return new TradeTick(
                contractSymbol,
                instant,
                priceTicks,
                priceTicks - 1,
                priceTicks + 1,
                1,
                1,
                scidRecordIndex
        );
    }
}
