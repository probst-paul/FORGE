package forge.feature;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TpoPeriodClassifierTest {
    private static final ZoneId CENTRAL_TIME = ZoneId.of("America/Chicago");

    private final TpoPeriodClassifier classifier = new TpoPeriodClassifier();

    @Test
    void classifiesThirtyMinuteRthPeriodsFromCentralTime() {
        assertEquals(TpoPeriod.A, classifier.classify(instantAtCentral(LocalTime.of(8, 30))));
        assertEquals(TpoPeriod.B, classifier.classify(instantAtCentral(LocalTime.of(9, 0))));
        assertEquals(TpoPeriod.C, classifier.classify(instantAtCentral(LocalTime.of(9, 30))));
        assertEquals(TpoPeriod.D, classifier.classify(instantAtCentral(LocalTime.of(10, 0))));
        assertEquals(TpoPeriod.Q, classifier.classify(instantAtCentral(LocalTime.of(16, 30))));
    }

    @Test
    void classifiesTimesOutsideRthAsOutsideRth() {
        assertEquals(TpoPeriod.OUTSIDE_RTH, classifier.classify(instantAtCentral(LocalTime.of(8, 29, 59))));
        assertEquals(TpoPeriod.OUTSIDE_RTH, classifier.classify(instantAtCentral(LocalTime.of(17, 0))));
    }

    private java.time.Instant instantAtCentral(LocalTime time) {
        return LocalDateTime.of(LocalDate.of(2025, 1, 6), time)
                .atZone(CENTRAL_TIME)
                .withZoneSameInstant(ZoneOffset.UTC)
                .toInstant();
    }
}
