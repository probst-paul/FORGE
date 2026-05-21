package forge.query;

import forge.app.EventStatisticsProgress;
import forge.data.market.TradeBatchReader;
import forge.data.market.TradeTick;
import forge.event.EventBuildService;
import forge.event.MarketEvent;
import forge.feature.FeatureBuildService;
import forge.feature.SessionRangeFeature;

import java.util.ArrayList;
import java.util.List;

public class EventStatisticsQueryRunner {
    private final QueryTradeTickSource tradeTickSource;
    private final QueryDerivedDataStore derivedDataStore;
    private final FeatureBuildService featureBuildService;
    private final EventBuildService eventBuildService;
    private final QueryService queryService;

    public EventStatisticsQueryRunner(
            QueryTradeTickSource tradeTickSource,
            QueryDerivedDataStore derivedDataStore,
            FeatureBuildService featureBuildService,
            EventBuildService eventBuildService,
            QueryService queryService
    ) {
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

        List<MarketEvent> events;
        if (derivedDataStore.areMarketEventsBuilt(request.getContractWindows(), request.getEventName())) {
            events = derivedDataStore.loadMarketEvents(request.getContractWindows(), request.getEventName());
            if (ticks == null) {
                request.getProgressListener().onProgress(new EventStatisticsProgress(0, 0));
            }
        } else {
            if (ticks == null) {
                ticks = readTicks(request);
            }
            events = eventBuildService.detectFirstHourBreachEvents(sessionRangeFeatures, ticks);
            derivedDataStore.saveMarketEvents(events);
            derivedDataStore.markMarketEventsBuilt(request.getContractWindows(), request.getEventName());
        }
        return queryService.summarizeEventStatistics(
                new EventStatisticsQuery(request.getEventName()),
                sessionRangeFeatures,
                events
        );
    }

    private List<TradeTick> readTicks(EventStatisticsQueryRequest request) {
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
