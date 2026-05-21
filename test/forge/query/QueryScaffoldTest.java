package forge.query;

import forge.event.FirstHourBreachEvent;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QueryScaffoldTest {
    @Nested
    class Facade {
        @Test
        void exposesSingletonAccessToSupportedQueryEvents() {
            FacadeForgeQuery facade = FacadeForgeQuery.getTheInstance();

            assertSame(facade, FacadeForgeQuery.getTheInstance());
            assertEquals(List.of(FirstHourBreachEvent.EVENT_NAME), facade.forgeQueryAccess().getSupportedQueryEventNames());
        }
    }

    @Nested
    class EventStatisticsQueries {
        @Test
        void storesEventName() {
            EventStatisticsQuery query = new EventStatisticsQuery(FirstHourBreachEvent.EVENT_NAME);

            assertEquals(FirstHourBreachEvent.EVENT_NAME, query.getEventName());
        }
    }

    @Nested
    class EventStatisticsResults {
        @Test
        void calculatesSummaryValues() {
            EventStatisticsResult result = new EventStatisticsResult(
                    FirstHourBreachEvent.EVENT_NAME,
                    10,
                    3,
                    2
            );

            assertEquals(10, result.getSessionsAnalyzed());
            assertEquals(3, result.getLongEventCount());
            assertEquals(2, result.getShortEventCount());
            assertEquals(5, result.getTotalEventCount());
            assertEquals(5, result.getNoEventCount());
            assertEquals(0.5, result.getEventRate());
        }

        @Test
        void rejectsCountsThatExceedSessions() {
            assertThrows(IllegalArgumentException.class, () -> new EventStatisticsResult(
                    FirstHourBreachEvent.EVENT_NAME,
                    3,
                    2,
                    2
            ));
        }
    }
}
