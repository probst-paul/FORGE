package forge.event;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;

import forge.data.market.TradeTick;
import forge.feature.SessionRangeFeature;
import forge.feature.TradingDayClassifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EventScaffoldTest {
    @Nested
    class Facade {
        @Test
        void exposesSingletonAccessToSupportedEvents() {
            FacadeForgeEvent facade = FacadeForgeEvent.getTheInstance();

            assertSame(facade, FacadeForgeEvent.getTheInstance());
            assertEquals(List.of(FirstHourBreachEvent.EVENT_NAME), facade.forgeEventAccess().getSupportedEventNames());
        }
    }

    @Nested
    class Definition {
        @Test
        void definesFirstHourBreachEvent() {
            FirstHourBreachEvent definition = new FirstHourBreachEvent();

            assertEquals("FIRST_HOUR_BREACH", definition.getName());
            assertEquals(1, definition.getVersion());
        }
    }

    @Nested
    class MarketEventOccurrences {
        @Test
        void storesEventIdentityAndTiming() {
            MarketEventOccurrence event = new MarketEventOccurrence(
                    "esu25",
                    LocalDate.of(2025, 8, 1),
                    FirstHourBreachEvent.EVENT_NAME,
                    FirstHourBreachEvent.EVENT_VERSION,
                    EventSide.HIGH,
                    Instant.parse("2025-08-01T14:30:00Z"),
                    24000
            );

            assertEquals("ESU25", event.getContractSymbol());
            assertEquals(LocalDate.of(2025, 8, 1), event.getSessionDate());
            assertEquals(FirstHourBreachEvent.EVENT_NAME, event.getEventName());
            assertEquals(EventSide.HIGH, event.getSide());
            assertEquals(24000, event.getEventPriceTicks());
        }

        @Test
        void rejectsInvalidEventPrice() {
            assertThrows(IllegalArgumentException.class, () -> new MarketEventOccurrence(
                    "ESU25",
                    LocalDate.of(2025, 8, 1),
                    FirstHourBreachEvent.EVENT_NAME,
                    FirstHourBreachEvent.EVENT_VERSION,
                    EventSide.HIGH,
                    Instant.parse("2025-08-01T14:30:00Z"),
                    0
            ));
        }
    }

    @Nested
    class FirstHourBreachDetection {
        @Test
        void detectsFirstBreachAfterFirstHour() {
            SessionRangeFeature feature = new SessionRangeFeature(
                    "ESU25",
                    LocalDate.of(2025, 8, 1),
                    100,
                    120,
                    105,
                    115,
                    95,
                    125
            );
            FirstHourBreachEventDetector detector = new FirstHourBreachEventDetector();

            List<MarketEventOccurrence> events = detector.detect(List.of(feature), List.of(
                    tick(LocalDate.of(2025, 8, 1), LocalTime.of(8, 45), 115, 1),
                    tick(LocalDate.of(2025, 8, 1), LocalTime.of(9, 31), 114, 2),
                    tick(LocalDate.of(2025, 8, 1), LocalTime.of(9, 45), 116, 3),
                    tick(LocalDate.of(2025, 8, 1), LocalTime.of(10, 0), 104, 4)
            ));

            assertEquals(1, events.size());
            assertEquals(EventSide.HIGH, events.get(0).getSide());
            assertEquals(116, events.get(0).getEventPriceTicks());
        }

        @Test
        void facadeDetectsFirstHourBreachEvents() {
            SessionRangeFeature feature = new SessionRangeFeature(
                    "ESU25",
                    LocalDate.of(2025, 8, 1),
                    100,
                    120,
                    105,
                    115,
                    95,
                    125
            );

            List<MarketEventOccurrence> events = FacadeForgeEvent.getTheInstance()
                    .forgeEventAccess()
                    .detectFirstHourBreachEvents(List.of(feature), List.of(
                            tick(LocalDate.of(2025, 8, 1), LocalTime.of(9, 45), 104, 1)
                    ));

            assertEquals(1, events.size());
            assertEquals(EventSide.LOW, events.get(0).getSide());
        }

        private TradeTick tick(LocalDate centralDate, LocalTime centralTime, long priceTicks, long scidRecordIndex) {
            Instant instant = ZonedDateTime.of(
                    centralDate,
                    centralTime,
                    TradingDayClassifier.CENTRAL_TIME
            ).toInstant();
            return new TradeTick(
                    "ESU25",
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
}
