package forge.event;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

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
    class MarketEvents {
        @Test
        void storesEventIdentityAndTiming() {
            MarketEvent event = new MarketEvent(
                    "esu25",
                    LocalDate.of(2025, 8, 1),
                    FirstHourBreachEvent.EVENT_NAME,
                    FirstHourBreachEvent.EVENT_VERSION,
                    EventSide.LONG,
                    Instant.parse("2025-08-01T14:30:00Z"),
                    24000
            );

            assertEquals("ESU25", event.getContractSymbol());
            assertEquals(LocalDate.of(2025, 8, 1), event.getSessionDate());
            assertEquals(FirstHourBreachEvent.EVENT_NAME, event.getEventName());
            assertEquals(EventSide.LONG, event.getSide());
            assertEquals(24000, event.getEventPriceTicks());
        }

        @Test
        void rejectsInvalidEventPrice() {
            assertThrows(IllegalArgumentException.class, () -> new MarketEvent(
                    "ESU25",
                    LocalDate.of(2025, 8, 1),
                    FirstHourBreachEvent.EVENT_NAME,
                    FirstHourBreachEvent.EVENT_VERSION,
                    EventSide.LONG,
                    Instant.parse("2025-08-01T14:30:00Z"),
                    0
            ));
        }
    }
}
