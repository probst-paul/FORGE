package forge.engine;

import forge.app.EventStatisticsProgress;
import forge.data.market.TradeBatchReader;
import forge.data.market.TradeTick;
import forge.condition.ConditionBuildService;
import forge.condition.MarketConditionOccurrence;
import forge.feature.FeatureBuildService;
import forge.feature.SessionRangeFeature;

import java.util.ArrayList;
import java.util.List;

public class EventStatisticsQueryRunner {
    private final QueryTradeTickSource tradeTickSource;
    private final QueryDerivedDataStore derivedDataStore;
    private final FeatureBuildService featureBuildService;
    private final ConditionBuildService eventBuildService;
    private final QueryService queryService;

    public EventStatisticsQueryRunner(
            QueryTradeTickSource tradeTickSource,
            QueryDerivedDataStore derivedDataStore,
            FeatureBuildService featureBuildService,
            ConditionBuildService eventBuildService,
            QueryService queryService
    ) {
        /*
         * Intent: Create the event-statistics runner from tick source, derived-data cache, builders, and query service.
         * Precondition: All dependencies must be non-null.
         * Returns: A constructed EventStatisticsQueryRunner instance.
         * Postcondition: Runner can load/build derived data before summarizing statistics.
         */
        if (tradeTickSource == null) {
            throw new IllegalArgumentException("tradeTickSource is required");
        }
        if (derivedDataStore == null) {
            throw new IllegalArgumentException("derivedDataStore is required");
        }
        if (featureBuildService == null) {
            throw new IllegalArgumentException("featureBuildService is required");
        }
        if (eventBuildService == null) {
            throw new IllegalArgumentException("eventBuildService is required");
        }
        if (queryService == null) {
            throw new IllegalArgumentException("queryService is required");
        }
        this.tradeTickSource = tradeTickSource;
        this.derivedDataStore = derivedDataStore;
        this.featureBuildService = featureBuildService;
        this.eventBuildService = eventBuildService;
        this.queryService = queryService;
    }

    public EventStatisticsReport run(EventStatisticsQueryRequest request) {
        /*
         * Intent: Run event statistics using cached derived data when available and building missing data when needed.
         * Precondition: Request must identify supported event name and valid contract windows.
         * Returns: EventStatisticsReport.
         * Postcondition: Missing session ranges or condition occurrences may be saved and marked built.
         */
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }

        List<TradeTick> ticks = null;
        List<SessionRangeFeature> sessionRangeFeatures;
        if (derivedDataStore.areSessionRangesBuilt(request.getContractWindows())) {
            sessionRangeFeatures = derivedDataStore.loadSessionRanges(request.getContractWindows());
        } else {
            ticks = readTicks(request);
            sessionRangeFeatures = featureBuildService.calculateSessionRanges(ticks);
            derivedDataStore.saveSessionRanges(sessionRangeFeatures);
            derivedDataStore.markSessionRangesBuilt(request.getContractWindows());
        }

        List<MarketConditionOccurrence> events;
        if (derivedDataStore.areMarketConditionOccurrencesBuilt(request.getContractWindows(), request.getEventName())) {
            events = derivedDataStore.loadMarketConditionOccurrences(request.getContractWindows(), request.getEventName());
            if (ticks == null) {
                request.getProgressListener().onProgress(new EventStatisticsProgress(0, 0));
            }
        } else {
            if (ticks == null) {
                ticks = readTicks(request);
            }
            events = eventBuildService.detectFirstHourBreachConditions(sessionRangeFeatures, ticks);
            derivedDataStore.saveMarketConditionOccurrences(events);
            derivedDataStore.markMarketConditionOccurrencesBuilt(request.getContractWindows(), request.getEventName());
        }
        return queryService.summarizeEventStatistics(
                new EventStatisticsQuery(request.getEventName()),
                sessionRangeFeatures,
                events
        );
    }

    private List<TradeTick> readTicks(EventStatisticsQueryRequest request) {
        /*
         * Intent: Read all selected ticks for statistics derivation while reporting progress.
         * Precondition: Request must contain valid windows and batch size.
         * Returns: In-memory list of ticks used by current statistics workflow.
         * Postcondition: Progress listener reaches total tick count when all batches are read.
         */
        long totalTicks = tradeTickSource.countTradeTicks(request.getContractWindows());
        request.getProgressListener().onProgress(new EventStatisticsProgress(0, totalTicks));

        TradeBatchReader reader = tradeTickSource.openTradeBatchReader(
                request.getContractWindows(),
                request.getBatchSize()
        );
        List<TradeTick> ticks = new ArrayList<>();
        long processedTicks = 0;
        while (true) {
            List<TradeTick> batch = reader.readNextBatch();
            if (batch.isEmpty()) {
                return ticks;
            }
            ticks.addAll(batch);
            processedTicks += batch.size();
            request.getProgressListener().onProgress(new EventStatisticsProgress(
                    Math.min(processedTicks, totalTicks),
                    totalTicks
            ));
        }
    }
}
