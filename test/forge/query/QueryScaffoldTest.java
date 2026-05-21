package forge.query;

import forge.event.EventSide;
import forge.event.FirstHourBreachEvent;
import forge.event.MarketEvent;
import forge.feature.SessionRangeFeature;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
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
                    "ES",
                    FirstHourBreachEvent.EVENT_NAME,
                    10,
                    3,
                    2
            );

            assertEquals("ES", result.getScopeName());
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
                    "ES",
                    FirstHourBreachEvent.EVENT_NAME,
                    3,
                    2,
                    2
            ));
        }

        @Test
        void summarizesEventCountsByInstrumentAndContract() {
            QueryService queryService = new QueryService();
            EventStatisticsReport report = queryService.summarizeEventStatistics(
                    new EventStatisticsQuery(FirstHourBreachEvent.EVENT_NAME),
                    List.of(
                            feature("ESU25", LocalDate.of(2025, 8, 1)),
                            feature("ESZ25", LocalDate.of(2025, 12, 1)),
                            feature("NQZ25", LocalDate.of(2025, 12, 1))
                    ),
                    List.of(
                            event("ESU25", LocalDate.of(2025, 8, 1), EventSide.LONG),
                            event("NQZ25", LocalDate.of(2025, 12, 1), EventSide.SHORT)
                    )
            );

            assertEquals(2, report.getInstrumentResults().size());
            EventStatisticsResult es = report.getInstrumentResults().get(0);
            EventStatisticsResult nq = report.getInstrumentResults().get(1);
            assertEquals("ES", es.getScopeName());
            assertEquals(2, es.getSessionsAnalyzed());
            assertEquals(1, es.getLongEventCount());
            assertEquals(0, es.getShortEventCount());
            assertEquals(1, es.getNoEventCount());
            assertEquals("NQ", nq.getScopeName());
            assertEquals(1, nq.getSessionsAnalyzed());
            assertEquals(0, nq.getLongEventCount());
            assertEquals(1, nq.getShortEventCount());

            assertEquals(3, report.getContractResults().size());
        }

        private SessionRangeFeature feature(String contractSymbol, LocalDate sessionDate) {
            return new SessionRangeFeature(contractSymbol, sessionDate, 100, 120, 105, 115, 95, 125);
        }

        private MarketEvent event(String contractSymbol, LocalDate sessionDate, EventSide side) {
            return new MarketEvent(
                    contractSymbol,
                    sessionDate,
                    FirstHourBreachEvent.EVENT_NAME,
                    FirstHourBreachEvent.EVENT_VERSION,
                    side,
                    Instant.parse("2025-08-01T15:00:00Z"),
                    116
            );
        }
    }
}
