package forge.query;

import forge.event.EventSide;
import forge.event.EventBuildService;
import forge.event.FirstHourBreachEvent;
import forge.event.MarketEvent;
import forge.data.market.ContractTradeWindow;
import forge.data.market.InMemoryTickDataProvider;
import forge.data.market.TradeBatchReader;
import forge.data.market.TradeTick;
import forge.feature.FeatureBuildService;
import forge.feature.SessionRangeFeature;
import forge.feature.TradingDayClassifier;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
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

    @Nested
    class EventStatisticsQueryRunnerTests {
        @Test
        void runsFullEventStatisticsQueryPipeline() {
            List<TradeTick> ticks = List.of(
                    tick("ESU25", LocalDate.of(2025, 8, 3), LocalTime.of(17, 0), 100, 1),
                    tick("ESU25", LocalDate.of(2025, 8, 4), LocalTime.of(8, 30), 105, 2),
                    tick("ESU25", LocalDate.of(2025, 8, 4), LocalTime.of(9, 29), 115, 3),
                    tick("ESU25", LocalDate.of(2025, 8, 4), LocalTime.of(9, 45), 116, 4)
            );
            InMemoryTickDataProvider tickDataProvider = new InMemoryTickDataProvider(ticks);
            InMemoryQueryDerivedDataStore derivedDataStore = new InMemoryQueryDerivedDataStore();
            EventStatisticsQueryRunner runner = new EventStatisticsQueryRunner(
                    new InMemoryQueryTradeTickSource(tickDataProvider),
                    derivedDataStore,
                    new FeatureBuildService(),
                    new EventBuildService(),
                    new QueryService()
            );

            EventStatisticsReport report = runner.run(new EventStatisticsQueryRequest(
                    List.of(new ContractTradeWindow("ESU25", LocalDate.of(2025, 8, 4), LocalDate.of(2025, 8, 4))),
                    FirstHourBreachEvent.EVENT_NAME,
                    2
            ));

            assertEquals(1, report.getInstrumentResults().size());
            EventStatisticsResult result = report.getInstrumentResults().get(0);
            assertEquals("ES", result.getScopeName());
            assertEquals(1, result.getSessionsAnalyzed());
            assertEquals(1, result.getLongEventCount());
            assertEquals(0, result.getShortEventCount());
        }

        @Test
        void facadeRunsEventStatisticsQuery() {
            List<TradeTick> ticks = List.of(
                    tick("ESU25", LocalDate.of(2025, 8, 3), LocalTime.of(17, 0), 100, 1),
                    tick("ESU25", LocalDate.of(2025, 8, 4), LocalTime.of(8, 30), 105, 2),
                    tick("ESU25", LocalDate.of(2025, 8, 4), LocalTime.of(9, 29), 115, 3),
                    tick("ESU25", LocalDate.of(2025, 8, 4), LocalTime.of(9, 45), 104, 4)
            );
            InMemoryTickDataProvider tickDataProvider = new InMemoryTickDataProvider(ticks);
            QueryService queryService = new QueryService();
            QueryTradeTickSource tickSource = new InMemoryQueryTradeTickSource(tickDataProvider);
            InMemoryQueryDerivedDataStore derivedDataStore = new InMemoryQueryDerivedDataStore();
            FacadeForgeQuery facade = new FacadeForgeQuery(
                    queryService,
                    new EventStatisticsQueryRunner(
                            tickSource,
                            derivedDataStore,
                            new FeatureBuildService(),
                            new EventBuildService(),
                            queryService
                    )
            );

            EventStatisticsReport report = facade.forgeQueryAccess().runEventStatistics(new EventStatisticsQueryRequest(
                    List.of(new ContractTradeWindow("ESU25", LocalDate.of(2025, 8, 4), LocalDate.of(2025, 8, 4))),
                    FirstHourBreachEvent.EVENT_NAME
            ));

            assertEquals(1, report.getInstrumentResults().size());
            assertEquals(1, report.getInstrumentResults().get(0).getShortEventCount());
        }

        @Test
        void reportsProgressWithoutDuplicatingFinalUpdate() {
            List<TradeTick> ticks = List.of(
                    tick("ESU25", LocalDate.of(2025, 8, 3), LocalTime.of(17, 0), 100, 1),
                    tick("ESU25", LocalDate.of(2025, 8, 4), LocalTime.of(8, 30), 105, 2),
                    tick("ESU25", LocalDate.of(2025, 8, 4), LocalTime.of(9, 29), 115, 3),
                    tick("ESU25", LocalDate.of(2025, 8, 4), LocalTime.of(9, 45), 116, 4)
            );
            List<String> progressUpdates = new ArrayList<>();
            EventStatisticsQueryRunner runner = new EventStatisticsQueryRunner(
                    new InMemoryQueryTradeTickSource(new InMemoryTickDataProvider(ticks)),
                    new InMemoryQueryDerivedDataStore(),
                    new FeatureBuildService(),
                    new EventBuildService(),
                    new QueryService()
            );

            runner.run(new EventStatisticsQueryRequest(
                    List.of(new ContractTradeWindow("ESU25", LocalDate.of(2025, 8, 4), LocalDate.of(2025, 8, 4))),
                    FirstHourBreachEvent.EVENT_NAME,
                    2,
                    progress -> progressUpdates.add(progress.getProcessedTicks() + "/" + progress.getTotalTicks())
            ));

            assertEquals(List.of("0/4", "2/4", "4/4"), progressUpdates);
        }

        @Test
        void reusesStoredFeaturesAndEventsOnSubsequentRuns() {
            List<TradeTick> ticks = List.of(
                    tick("ESU25", LocalDate.of(2025, 8, 3), LocalTime.of(17, 0), 100, 1),
                    tick("ESU25", LocalDate.of(2025, 8, 4), LocalTime.of(8, 30), 105, 2),
                    tick("ESU25", LocalDate.of(2025, 8, 4), LocalTime.of(9, 29), 115, 3),
                    tick("ESU25", LocalDate.of(2025, 8, 4), LocalTime.of(9, 45), 116, 4)
            );
            InMemoryQueryTradeTickSource tickSource = new InMemoryQueryTradeTickSource(new InMemoryTickDataProvider(ticks));
            EventStatisticsQueryRunner runner = new EventStatisticsQueryRunner(
                    tickSource,
                    new InMemoryQueryDerivedDataStore(),
                    new FeatureBuildService(),
                    new EventBuildService(),
                    new QueryService()
            );
            EventStatisticsQueryRequest request = new EventStatisticsQueryRequest(
                    List.of(new ContractTradeWindow("ESU25", LocalDate.of(2025, 8, 4), LocalDate.of(2025, 8, 4))),
                    FirstHourBreachEvent.EVENT_NAME,
                    2
            );

            runner.run(request);
            runner.run(request);

            assertEquals(1, tickSource.getOpenReaderCount());
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

        private class InMemoryQueryTradeTickSource implements QueryTradeTickSource {
            private final InMemoryTickDataProvider tickDataProvider;
            private int openReaderCount;

            private InMemoryQueryTradeTickSource(InMemoryTickDataProvider tickDataProvider) {
                this.tickDataProvider = tickDataProvider;
            }

            @Override
            public TradeBatchReader openTradeBatchReader(List<ContractTradeWindow> windows, int batchSize) {
                openReaderCount++;
                return tickDataProvider.openReader(windows, batchSize);
            }

            @Override
            public long countTradeTicks(List<ContractTradeWindow> windows) {
                return tickDataProvider.countTicks(windows);
            }

            public int getOpenReaderCount() {
                return openReaderCount;
            }
        }

        private class InMemoryQueryDerivedDataStore implements QueryDerivedDataStore {
            private boolean sessionRangesBuilt;
            private boolean marketEventsBuilt;
            private final List<SessionRangeFeature> sessionRangeFeatures = new ArrayList<>();
            private final List<MarketEvent> marketEvents = new ArrayList<>();

            @Override
            public boolean areSessionRangesBuilt(List<ContractTradeWindow> windows) {
                return sessionRangesBuilt;
            }

            @Override
            public List<SessionRangeFeature> loadSessionRanges(List<ContractTradeWindow> windows) {
                return List.copyOf(sessionRangeFeatures);
            }

            @Override
            public void saveSessionRanges(java.util.Collection<SessionRangeFeature> sessionRangeFeatures) {
                this.sessionRangeFeatures.clear();
                this.sessionRangeFeatures.addAll(sessionRangeFeatures);
            }

            @Override
            public void markSessionRangesBuilt(List<ContractTradeWindow> windows) {
                sessionRangesBuilt = true;
            }

            @Override
            public boolean areMarketEventsBuilt(List<ContractTradeWindow> windows, String eventName) {
                return marketEventsBuilt;
            }

            @Override
            public List<MarketEvent> loadMarketEvents(List<ContractTradeWindow> windows, String eventName) {
                return List.copyOf(marketEvents);
            }

            @Override
            public void saveMarketEvents(java.util.Collection<MarketEvent> marketEvents) {
                this.marketEvents.clear();
                this.marketEvents.addAll(marketEvents);
            }

            @Override
            public void markMarketEventsBuilt(List<ContractTradeWindow> windows, String eventName) {
                marketEventsBuilt = true;
            }
        }
    }
}
