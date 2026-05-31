package forge.statistics;

import forge.event.EventSide;
import forge.event.FirstHourBreachEvent;
import forge.event.MarketEventOccurrence;
import forge.feature.SessionRangeFeature;
import forge.reporting.eventstatistics.EventStatisticsReport;
import forge.engine.eventstatistics.EventStatisticsResult;
import forge.study.FirstHourBreachStudy;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class StatisticsServiceTest {
    @Test
    void summarizesStudyOccurrencesByInstrumentAndContract() {
        StatisticsService statisticsService = new StatisticsService();

        EventStatisticsReport report = statisticsService.summarizeStudyOccurrences(
                new FirstHourBreachStudy(),
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

        assertEquals(FirstHourBreachEvent.EVENT_NAME, report.getEventName());
        assertEquals(2, report.getInstrumentResults().size());
        EventStatisticsResult es = report.getInstrumentResults().get(0);
        EventStatisticsResult nq = report.getInstrumentResults().get(1);
        assertEquals("ES", es.getScopeName());
        assertEquals(2, es.getSessionsAnalyzed());
        assertEquals(1, es.getLongEventCount());
        assertEquals(0, es.getShortEventCount());
        assertEquals("NQ", nq.getScopeName());
        assertEquals(1, nq.getSessionsAnalyzed());
        assertEquals(0, nq.getLongEventCount());
        assertEquals(1, nq.getShortEventCount());
        assertEquals(3, report.getContractResults().size());
    }

    @Test
    void facadeExposesSingletonStatisticsAccess() {
        FacadeForgeStatistics facade = FacadeForgeStatistics.getTheInstance();

        assertSame(facade, FacadeForgeStatistics.getTheInstance());
    }

    private SessionRangeFeature feature(String contractSymbol, LocalDate sessionDate) {
        return new SessionRangeFeature(contractSymbol, sessionDate, 100, 120, 105, 115, 95, 125);
    }

    private MarketEventOccurrence event(String contractSymbol, LocalDate sessionDate, EventSide side) {
        return new MarketEventOccurrence(
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
