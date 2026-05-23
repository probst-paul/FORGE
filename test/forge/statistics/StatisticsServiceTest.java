package forge.statistics;

import forge.condition.ConditionSide;
import forge.condition.FirstHourBreachCondition;
import forge.condition.MarketConditionOccurrence;
import forge.feature.SessionRangeFeature;
import forge.engine.EventStatisticsReport;
import forge.engine.EventStatisticsResult;
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
                        event("ESU25", LocalDate.of(2025, 8, 1), ConditionSide.LONG),
                        event("NQZ25", LocalDate.of(2025, 12, 1), ConditionSide.SHORT)
                )
        );

        assertEquals(FirstHourBreachCondition.EVENT_NAME, report.getEventName());
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

    private MarketConditionOccurrence event(String contractSymbol, LocalDate sessionDate, ConditionSide side) {
        return new MarketConditionOccurrence(
                contractSymbol,
                sessionDate,
                FirstHourBreachCondition.EVENT_NAME,
                FirstHourBreachCondition.EVENT_VERSION,
                side,
                Instant.parse("2025-08-01T15:00:00Z"),
                116
        );
    }
}
