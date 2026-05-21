package forge.feature;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TradingDayClassifierTest {
    private final TradingDayClassifier classifier = new TradingDayClassifier();

    @Test
    void eveningOvernightBelongsToNextTradingDay() {
        TradingDayContext context = classifier.classify(centralInstant(
                LocalDate.of(2025, 1, 5),
                LocalTime.of(17, 0)
        ));

        assertEquals(LocalDate.of(2025, 1, 6), context.getTradingDay());
        assertEquals(TradingSession.OVERNIGHT, context.getSession());
        assertTrue(context.isOvernight());
        assertFalse(context.isRth());
    }

    @Test
    void morningBeforeRthBelongsToSameTradingDayOvernight() {
        TradingDayContext context = classifier.classify(centralInstant(
                LocalDate.of(2025, 1, 6),
                LocalTime.of(8, 29, 59, 999_999_999)
        ));

        assertEquals(LocalDate.of(2025, 1, 6), context.getTradingDay());
        assertEquals(TradingSession.OVERNIGHT, context.getSession());
    }

    @Test
    void rthOpenStartsFirstHourAndRth() {
        TradingDayContext context = classifier.classify(centralInstant(
                LocalDate.of(2025, 1, 6),
                LocalTime.of(8, 30)
        ));

        assertEquals(LocalDate.of(2025, 1, 6), context.getTradingDay());
        assertEquals(TradingSession.FIRST_HOUR, context.getSession());
        assertTrue(context.isFirstHour());
        assertTrue(context.isRth());
    }

    @Test
    void firstHourEndSwitchesToRthOnly() {
        TradingDayContext context = classifier.classify(centralInstant(
                LocalDate.of(2025, 1, 6),
                LocalTime.of(9, 30)
        ));

        assertEquals(LocalDate.of(2025, 1, 6), context.getTradingDay());
        assertEquals(TradingSession.RTH, context.getSession());
        assertFalse(context.isFirstHour());
        assertTrue(context.isRth());
    }

    @Test
    void rthLastMomentIsSameTradingDay() {
        TradingDayContext context = classifier.classify(centralInstant(
                LocalDate.of(2025, 1, 6),
                LocalTime.of(16, 59, 59, 999_999_999)
        ));

        assertEquals(LocalDate.of(2025, 1, 6), context.getTradingDay());
        assertEquals(TradingSession.RTH, context.getSession());
    }

    private Instant centralInstant(LocalDate date, LocalTime time) {
        return ZonedDateTime.of(date, time, TradingDayClassifier.CENTRAL_TIME).toInstant();
    }
}
