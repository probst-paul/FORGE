package forge.condition;

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

class ConditionScaffoldTest {
    @Nested
    class Facade {
        @Test
        void exposesSingletonAccessToSupportedEvents() {
            FacadeForgeCondition facade = FacadeForgeCondition.getTheInstance();

            assertSame(facade, FacadeForgeCondition.getTheInstance());
            assertEquals(List.of(FirstHourBreachCondition.EVENT_NAME), facade.forgeConditionAccess().getSupportedConditionNames());
        }
    }

    @Nested
    class Definition {
        @Test
        void definesFirstHourBreachCondition() {
            FirstHourBreachCondition definition = new FirstHourBreachCondition();

            assertEquals("FIRST_HOUR_BREACH", definition.getName());
            assertEquals(1, definition.getVersion());
        }
    }

    @Nested
    class MarketConditionOccurrences {
        @Test
        void storesEventIdentityAndTiming() {
            MarketConditionOccurrence event = new MarketConditionOccurrence(
                    "esu25",
                    LocalDate.of(2025, 8, 1),
                    FirstHourBreachCondition.EVENT_NAME,
                    FirstHourBreachCondition.EVENT_VERSION,
                    ConditionSide.LONG,
                    Instant.parse("2025-08-01T14:30:00Z"),
                    24000
            );

            assertEquals("ESU25", event.getContractSymbol());
            assertEquals(LocalDate.of(2025, 8, 1), event.getSessionDate());
            assertEquals(FirstHourBreachCondition.EVENT_NAME, event.getEventName());
            assertEquals(ConditionSide.LONG, event.getSide());
            assertEquals(24000, event.getEventPriceTicks());
        }

        @Test
        void rejectsInvalidEventPrice() {
            assertThrows(IllegalArgumentException.class, () -> new MarketConditionOccurrence(
                    "ESU25",
                    LocalDate.of(2025, 8, 1),
                    FirstHourBreachCondition.EVENT_NAME,
                    FirstHourBreachCondition.EVENT_VERSION,
                    ConditionSide.LONG,
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
            FirstHourBreachConditionDetector detector = new FirstHourBreachConditionDetector();

            List<MarketConditionOccurrence> events = detector.detect(List.of(feature), List.of(
                    tick(LocalDate.of(2025, 8, 1), LocalTime.of(8, 45), 115, 1),
                    tick(LocalDate.of(2025, 8, 1), LocalTime.of(9, 31), 114, 2),
                    tick(LocalDate.of(2025, 8, 1), LocalTime.of(9, 45), 116, 3),
                    tick(LocalDate.of(2025, 8, 1), LocalTime.of(10, 0), 104, 4)
            ));

            assertEquals(1, events.size());
            assertEquals(ConditionSide.LONG, events.get(0).getSide());
            assertEquals(116, events.get(0).getEventPriceTicks());
        }

        @Test
        void facadeDetectsFirstHourBreachConditions() {
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

            List<MarketConditionOccurrence> events = FacadeForgeCondition.getTheInstance()
                    .forgeConditionAccess()
                    .detectFirstHourBreachConditions(List.of(feature), List.of(
                            tick(LocalDate.of(2025, 8, 1), LocalTime.of(9, 45), 104, 1)
                    ));

            assertEquals(1, events.size());
            assertEquals(ConditionSide.SHORT, events.get(0).getSide());
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
