package forge.data.build;

import forge.data.market.ContractTradeWindow;
import forge.data.market.InMemoryTickDataProvider;
import forge.data.market.TradeBatchReader;
import forge.data.market.TradeTick;
import forge.event.EventBuildService;
import forge.event.MarketEventOccurrence;
import forge.feature.FeatureBuildService;
import forge.feature.SessionRangeFeature;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DerivedDataBuildServiceTest {
    private static final List<ContractTradeWindow> WINDOWS = List.of(
            new ContractTradeWindow("ESU25", LocalDate.of(2025, 8, 4), LocalDate.of(2025, 8, 4))
    );

    @Nested
    class PlanBuild {
        @Test
        void plansMissingSelectedDerivedData() {
            InMemoryBuildStore store = new InMemoryBuildStore();
            DerivedDataBuildService service = service(store);

            DatabaseBuildPlan plan = service.planBuild(new DatabaseBuildRequest(
                    WINDOWS,
                    EnumSet.of(DerivedDataBuildOption.SESSION_RANGES, DerivedDataBuildOption.FIRST_HOUR_BREACH_EVENTS),
                    false
            ));

            assertEquals(4, plan.getTotalTicks());
            assertFalse(plan.isSessionRangesAlreadyBuilt());
            assertFalse(plan.isFirstHourBreachEventsAlreadyBuilt());
            assertTrue(plan.willBuildSessionRanges());
            assertTrue(plan.willBuildFirstHourBreachEvents());
        }

        @Test
        void plansNoWorkWhenSelectedDerivedDataAlreadyExists() {
            InMemoryBuildStore store = new InMemoryBuildStore();
            store.sessionRangesBuilt = true;
            store.marketEventsBuilt = true;
            DerivedDataBuildService service = service(store);

            DatabaseBuildPlan plan = service.planBuild(new DatabaseBuildRequest(
                    WINDOWS,
                    EnumSet.of(DerivedDataBuildOption.SESSION_RANGES, DerivedDataBuildOption.FIRST_HOUR_BREACH_EVENTS),
                    false
            ));

            assertFalse(plan.hasWorkToRun());
        }
    }

    @Nested
    class RunBuild {
        @Test
        void buildsSessionRangesAndFirstHourBreachEvents() {
            InMemoryBuildStore store = new InMemoryBuildStore();
            DerivedDataBuildService service = service(store);
            List<String> progressUpdates = new ArrayList<>();

            DatabaseBuildResult result = service.runBuild(new DatabaseBuildRequest(
                    WINDOWS,
                    EnumSet.of(DerivedDataBuildOption.SESSION_RANGES, DerivedDataBuildOption.FIRST_HOUR_BREACH_EVENTS),
                    false,
                    2
            ), progress -> progressUpdates.add(progress.getProcessedTicks() + "/" + progress.getTotalTicks()));

            assertEquals(4, result.getTicksRead());
            assertEquals(1, result.getSessionRangesBuilt());
            assertEquals(1, result.getMarketEventOccurrencesBuilt());
            assertEquals(List.of("0/4", "2/4", "4/4"), progressUpdates);
            assertTrue(store.sessionRangesBuilt);
            assertTrue(store.marketEventsBuilt);
            assertEquals(1, store.sessionRanges.size());
            assertEquals(1, store.marketEvents.size());
        }

        @Test
        void rebuildClearsExistingDerivedRowsBeforeBuilding() {
            InMemoryBuildStore store = new InMemoryBuildStore();
            store.sessionRangesBuilt = true;
            store.marketEventsBuilt = true;
            store.sessionRanges.add(new SessionRangeFeature("ESU25", LocalDate.of(2025, 8, 3), 1, 2, 1, 2, 1, 2));
            DerivedDataBuildService service = service(store);

            service.runBuild(new DatabaseBuildRequest(
                    WINDOWS,
                    EnumSet.of(DerivedDataBuildOption.SESSION_RANGES),
                    true,
                    2
            ), DataBuildProgressListener.NO_OP);

            assertEquals(1, store.clearSessionRangeCalls);
            assertEquals(1, store.clearMarketEventOccurrenceCalls);
            assertEquals(1, store.sessionRanges.size());
        }

        @Test
        void reportsProgressWhileReadingTicks() {
            InMemoryBuildStore store = new InMemoryBuildStore();
            DerivedDataBuildService service = service(store);
            List<String> progressUpdates = new ArrayList<>();

            service.runBuild(new DatabaseBuildRequest(
                    WINDOWS,
                    EnumSet.of(DerivedDataBuildOption.SESSION_RANGES),
                    false,
                    2
            ), progress -> progressUpdates.add(progress.getProcessedTicks() + "/" + progress.getTotalTicks()));

            assertEquals(List.of("0/4", "2/4", "4/4"), progressUpdates);
        }
    }

    private DerivedDataBuildService service(InMemoryBuildStore store) {
        InMemoryTickDataProvider tickDataProvider = new InMemoryTickDataProvider(List.of(
                tick(LocalDate.of(2025, 8, 3), LocalTime.of(17, 0), 100, 1),
                tick(LocalDate.of(2025, 8, 4), LocalTime.of(8, 30), 105, 2),
                tick(LocalDate.of(2025, 8, 4), LocalTime.of(9, 29), 115, 3),
                tick(LocalDate.of(2025, 8, 4), LocalTime.of(9, 45), 116, 4)
        ));
        return new DerivedDataBuildService(
                new DerivedDataBuildTradeSource() {
                    @Override
                    public TradeBatchReader openTradeBatchReader(List<ContractTradeWindow> windows, int batchSize) {
                        return tickDataProvider.openReader(windows, batchSize);
                    }

                    @Override
                    public long countTradeTicks(List<ContractTradeWindow> windows) {
                        return tickDataProvider.countTicks(windows);
                    }
                },
                store,
                new FeatureBuildService(),
                new EventBuildService()
        );
    }

    private TradeTick tick(LocalDate centralDate, LocalTime centralTime, long priceTicks, long scidRecordIndex) {
        Instant instant = ZonedDateTime.of(
                centralDate,
                centralTime,
                forge.feature.TradingDayClassifier.CENTRAL_TIME
        ).toInstant();
        return new TradeTick("ESU25", instant, priceTicks, priceTicks - 1, priceTicks + 1, 1, 1, scidRecordIndex);
    }

    private static class InMemoryBuildStore implements DerivedDataBuildStore {
        private boolean sessionRangesBuilt;
        private boolean marketEventsBuilt;
        private int clearSessionRangeCalls;
        private int clearMarketEventOccurrenceCalls;
        private final List<SessionRangeFeature> sessionRanges = new ArrayList<>();
        private final List<MarketEventOccurrence> marketEvents = new ArrayList<>();

        @Override
        public boolean areSessionRangesBuilt(List<ContractTradeWindow> windows) {
            return sessionRangesBuilt;
        }

        @Override
        public List<SessionRangeFeature> loadSessionRanges(List<ContractTradeWindow> windows) {
            return List.copyOf(sessionRanges);
        }

        @Override
        public void saveSessionRanges(Collection<SessionRangeFeature> sessionRangeFeatures) {
            sessionRanges.clear();
            sessionRanges.addAll(sessionRangeFeatures);
        }

        @Override
        public void markSessionRangesBuilt(List<ContractTradeWindow> windows) {
            sessionRangesBuilt = true;
        }

        @Override
        public void clearSessionRanges(List<ContractTradeWindow> windows) {
            clearSessionRangeCalls++;
            sessionRangesBuilt = false;
            sessionRanges.clear();
        }

        @Override
        public boolean areMarketEventOccurrencesBuilt(List<ContractTradeWindow> windows, String eventName) {
            return marketEventsBuilt;
        }

        @Override
        public List<MarketEventOccurrence> loadMarketEventOccurrences(List<ContractTradeWindow> windows, String eventName) {
            return List.copyOf(marketEvents);
        }

        @Override
        public void saveMarketEventOccurrences(Collection<MarketEventOccurrence> marketEvents) {
            this.marketEvents.clear();
            this.marketEvents.addAll(marketEvents);
        }

        @Override
        public void markMarketEventOccurrencesBuilt(List<ContractTradeWindow> windows, String eventName) {
            marketEventsBuilt = true;
        }

        @Override
        public void clearMarketEventOccurrences(List<ContractTradeWindow> windows, String eventName) {
            clearMarketEventOccurrenceCalls++;
            marketEventsBuilt = false;
            marketEvents.clear();
        }
    }
}
