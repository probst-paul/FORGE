package forge.engine.eventstatistics;

import forge.app.EventStatisticsProgress;
import forge.app.EventStatisticsProgressListener;
import forge.engine.QueryDerivedDataStore;
import forge.engine.QueryService;
import forge.engine.QueryTradeTickSource;
import forge.data.market.TradeBatchReader;
import forge.data.market.TradeTick;
import forge.data.market.ContractTradeWindow;
import forge.engine.concurrency.EngineJob;
import forge.engine.concurrency.EngineJobRunner;
import forge.event.EventBuildService;
import forge.event.MarketEventOccurrence;
import forge.feature.FeatureBuildService;
import forge.feature.SessionRangeFeature;
import forge.util.ImmutableLists;
import forge.reporting.eventstatistics.EventStatisticsReport;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class EventStatisticsQueryRunner {
    private final QueryTradeTickSource tradeTickSource;
    private final QueryDerivedDataStore derivedDataStore;
    private final FeatureBuildService featureBuildService;
    private final EventBuildService eventBuildService;
    private final QueryService queryService;
    private final EngineJobRunner jobRunner;

    public EventStatisticsQueryRunner(
            QueryTradeTickSource tradeTickSource,
            QueryDerivedDataStore derivedDataStore,
            FeatureBuildService featureBuildService,
            EventBuildService eventBuildService,
            QueryService queryService
    ) {
        this(tradeTickSource, derivedDataStore, featureBuildService, eventBuildService, queryService,
                new EngineJobRunner("forge-event-statistics"));
    }

    public EventStatisticsQueryRunner(
            QueryTradeTickSource tradeTickSource,
            QueryDerivedDataStore derivedDataStore,
            FeatureBuildService featureBuildService,
            EventBuildService eventBuildService,
            QueryService queryService,
            EngineJobRunner jobRunner
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
        if (jobRunner == null) {
            throw new IllegalArgumentException("jobRunner is required");
        }
        this.tradeTickSource = tradeTickSource;
        this.derivedDataStore = derivedDataStore;
        this.featureBuildService = featureBuildService;
        this.eventBuildService = eventBuildService;
        this.queryService = queryService;
        this.jobRunner = jobRunner;
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
        if (request.getContractWindows().size() > 1) {
            return runConcurrent(request);
        }

        return runSequential(request);
    }

    private EventStatisticsReport runConcurrent(EventStatisticsQueryRequest request) {
        /*
         * Intent: Run event statistics independently per contract window and aggregate the completed reports.
         * Precondition: Request contains more than one contract window.
         * Returns: EventStatisticsReport with instrument and contract rows merged across jobs.
         * Postcondition: Each window may build and persist missing derived data independently.
         */
        long totalTicks = tradeTickSource.countTradeTicks(request.getContractWindows());
        AtomicLong processedTicks = new AtomicLong(0);
        request.getProgressListener().onProgress(new EventStatisticsProgress(0, totalTicks));

        List<EngineJob<EventStatisticsReport>> jobs = new ArrayList<>();
        for (ContractTradeWindow window : request.getContractWindows()) {
            EventStatisticsProgressListener aggregateListener = aggregateProgressListener(
                    window.getContractSymbol(),
                    request.getProgressListener(),
                    processedTicks,
                    totalTicks
            );
            EventStatisticsQueryRequest windowRequest = new EventStatisticsQueryRequest(
                    List.of(window),
                    request.getEventName(),
                    request.getBatchSize(),
                    aggregateListener
            );
            jobs.add(() -> runSequential(windowRequest));
        }

        List<EventStatisticsReport> reports = jobRunner.runAll(jobs);
        if (processedTicks.get() < totalTicks) {
            request.getProgressListener().onProgress(new EventStatisticsProgress(totalTicks, totalTicks));
        }
        return mergeReports(request.getEventName(), reports);
    }

    private EventStatisticsReport runSequential(EventStatisticsQueryRequest request) {
        /*
         * Intent: Run event statistics for the supplied request using the existing single-stream behavior.
         * Precondition: Request must be validated.
         * Returns: EventStatisticsReport for the supplied windows.
         * Postcondition: Missing derived data may be persisted by this request.
         */

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

        List<MarketEventOccurrence> events;
        if (derivedDataStore.areMarketEventOccurrencesBuilt(request.getContractWindows(), request.getEventName())) {
            events = derivedDataStore.loadMarketEventOccurrences(request.getContractWindows(), request.getEventName());
            if (ticks == null) {
                request.getProgressListener().onProgress(new EventStatisticsProgress(1, 1));
            }
        } else {
            if (ticks == null) {
                ticks = readTicks(request);
            }
            events = eventBuildService.detectFirstHourBreachEvents(sessionRangeFeatures, ticks);
            derivedDataStore.saveMarketEventOccurrences(events);
            derivedDataStore.markMarketEventOccurrencesBuilt(request.getContractWindows(), request.getEventName());
        }
        return enrichedReport(request, sessionRangeFeatures, events);
    }

    private EventStatisticsReport enrichedReport(
            EventStatisticsQueryRequest request,
            List<SessionRangeFeature> sessionRangeFeatures,
            List<MarketEventOccurrence> events
    ) {
        EventStatisticsReport report = queryService.summarizeEventStatistics(
                new EventStatisticsQuery(request.getEventName()),
                sessionRangeFeatures,
                events
        );
        List<EventStatisticsDetail> details = derivedDataStore.loadEventStatisticsDetails(
                request.getContractWindows(),
                request.getEventName()
        );
        if (details.isEmpty()) {
            details = report.getEventDetails();
        }
        List<EventStatisticsResult> contractResults = derivedDataStore.loadEventStatisticsContractResults(
                request.getContractWindows(),
                request.getEventName()
        );
        if (contractResults.isEmpty()) {
            contractResults = report.getContractResults();
        }
        return new EventStatisticsReport(
                request.getEventName(),
                report.getInstrumentResults(),
                contractResults,
                details
        );
    }

    private EventStatisticsProgressListener aggregateProgressListener(
            String contractSymbol,
            EventStatisticsProgressListener listener,
            AtomicLong aggregateProcessedTicks,
            long totalTicks
    ) {
        /*
         * Intent: Convert one contract-window progress stream into request-level aggregate progress.
         * Precondition: Listener and aggregate counter are shared by all event-statistics jobs.
         * Returns: Progress listener safe for one job to call.
         * Postcondition: Aggregate progress increases by each job's local delta.
         */
        AtomicLong localProcessedTicks = new AtomicLong(0);
        return progress -> {
            long previousLocal = localProcessedTicks.getAndSet(progress.getProcessedTicks());
            long delta = Math.max(0, progress.getProcessedTicks() - previousLocal);
            long aggregate = aggregateProcessedTicks.addAndGet(delta);
            listener.onProgress(new EventStatisticsProgress(Math.min(aggregate, totalTicks), totalTicks));
            listener.onContractProgress(contractSymbol, progress);
        };
    }

    private EventStatisticsReport mergeReports(String eventName, List<EventStatisticsReport> reports) {
        /*
         * Intent: Merge per-window event-statistics reports into one final report.
         * Precondition: Reports must all belong to the same event name.
         * Returns: Aggregated EventStatisticsReport.
         * Postcondition: Source reports are not modified.
         */
        Map<String, EventStatisticsBucket> instrumentBuckets = new LinkedHashMap<>();
        Map<String, EventStatisticsBucket> contractBuckets = new LinkedHashMap<>();
        List<EventStatisticsDetail> details = new ArrayList<>();
        for (EventStatisticsReport report : ImmutableLists.copyOfRequired(reports, "reports")) {
            details.addAll(report.getEventDetails());
            for (EventStatisticsResult result : report.getInstrumentResults()) {
                instrumentBuckets
                        .computeIfAbsent(result.getScopeName(), EventStatisticsBucket::new)
                        .add(result);
            }
            for (EventStatisticsResult result : report.getContractResults()) {
                contractBuckets
                        .computeIfAbsent(result.getScopeName(), EventStatisticsBucket::new)
                        .add(result);
            }
        }
        return new EventStatisticsReport(
                eventName,
                toMergedResults(eventName, instrumentBuckets),
                toMergedResults(eventName, contractBuckets),
                details
        );
    }

    private List<EventStatisticsResult> toMergedResults(
            String eventName,
            Map<String, EventStatisticsBucket> buckets
    ) {
        List<EventStatisticsResult> results = new ArrayList<>();
        for (EventStatisticsBucket bucket : buckets.values()) {
            results.add(bucket.toResult(eventName));
        }
        results.sort(Comparator.comparing(EventStatisticsResult::getScopeName));
        return results;
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

    private static class EventStatisticsBucket {
        private final String scopeName;
        private long sessionsAnalyzed;
        private long highEventCount;
        private long lowEventCount;
        private double overnightRangeTotal;
        private double firstHourRangeTotal;
        private double rthRangeTotal;
        private double overnightVolumeTotal;
        private double firstHourVolumeTotal;
        private double rthVolumeTotal;

        private EventStatisticsBucket(String scopeName) {
            this.scopeName = scopeName;
        }

        private void add(EventStatisticsResult result) {
            /*
             * Intent: Add one result row into a mutable aggregation bucket.
             * Precondition: Result must match this bucket's scope.
             * Returns: Nothing.
             * Postcondition: Counts include the supplied result.
             */
            sessionsAnalyzed += result.getSessionsAnalyzed();
            highEventCount += result.getHighEventCount();
            lowEventCount += result.getLowEventCount();
            overnightRangeTotal += result.getAverageOvernightRangeTicks() * result.getSessionsAnalyzed();
            firstHourRangeTotal += result.getAverageFirstHourRangeTicks() * result.getSessionsAnalyzed();
            rthRangeTotal += result.getAverageRthRangeTicks() * result.getSessionsAnalyzed();
            overnightVolumeTotal += result.getAverageOvernightVolume() * result.getSessionsAnalyzed();
            firstHourVolumeTotal += result.getAverageFirstHourVolume() * result.getSessionsAnalyzed();
            rthVolumeTotal += result.getAverageRthVolume() * result.getSessionsAnalyzed();
        }

        private EventStatisticsResult toResult(String eventName) {
            return new EventStatisticsResult(
                    scopeName,
                    eventName,
                    sessionsAnalyzed,
                    highEventCount,
                    lowEventCount,
                    average(overnightRangeTotal),
                    average(firstHourRangeTotal),
                    average(rthRangeTotal),
                    average(overnightVolumeTotal),
                    average(firstHourVolumeTotal),
                    average(rthVolumeTotal)
            );
        }

        private double average(double total) {
            if (sessionsAnalyzed == 0) {
                return 0;
            }
            return total / sessionsAnalyzed;
        }
    }
}
