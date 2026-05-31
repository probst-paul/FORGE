package forge.data.build;

import forge.data.market.TradeBatchReader;
import forge.data.market.TradeTick;
import forge.data.market.TradeTickStreamProcessor;
import forge.event.EventBuildService;
import forge.event.FirstHourBreachEvent;
import forge.event.MarketEventOccurrence;
import forge.feature.FeatureBuildService;
import forge.feature.SessionRangeFeature;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public class DerivedDataBuildService {
    private final DerivedDataBuildTradeSource tradeSource;
    private final DerivedDataBuildStore buildStore;
    private final FeatureBuildService featureBuildService;
    private final EventBuildService eventBuildService;

    public DerivedDataBuildService(
            DerivedDataBuildTradeSource tradeSource,
            DerivedDataBuildStore buildStore,
            FeatureBuildService featureBuildService,
            EventBuildService eventBuildService
    ) {
        /*
         * Intent: Create the derived-data build service from streaming trade source, persistence store, and builders.
         * Precondition: All dependencies must be non-null.
         * Returns: A constructed DerivedDataBuildService instance.
         * Postcondition: Service can plan and run derived-data builds without owning database-specific details.
         */
        if (tradeSource == null) {
            throw new IllegalArgumentException("tradeSource is required");
        }
        if (buildStore == null) {
            throw new IllegalArgumentException("buildStore is required");
        }
        if (featureBuildService == null) {
            throw new IllegalArgumentException("featureBuildService is required");
        }
        if (eventBuildService == null) {
            throw new IllegalArgumentException("eventBuildService is required");
        }
        this.tradeSource = tradeSource;
        this.buildStore = buildStore;
        this.featureBuildService = featureBuildService;
        this.eventBuildService = eventBuildService;
    }

    public DatabaseBuildPlan planBuild(DatabaseBuildRequest request) {
        /*
         * Intent: Decide which derived-data artifacts need to be built for selected contract windows.
         * Precondition: Request must contain contract windows and selected build options.
         * Returns: DatabaseBuildPlan with current build state, work flags, and total tick count.
         * Postcondition: No derived data is created or deleted during planning.
         */
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
        boolean sessionRangesBuilt = buildStore.areSessionRangesBuilt(request.getContractWindows());
        boolean firstHourBreachEventsBuilt = buildStore.areMarketEventOccurrencesBuilt(
                request.getContractWindows(),
                FirstHourBreachEvent.EVENT_NAME
        );
        boolean needsSessionRanges = request.shouldBuild(DerivedDataBuildOption.SESSION_RANGES)
                || request.shouldBuild(DerivedDataBuildOption.FIRST_HOUR_BREACH_EVENTS);
        boolean willBuildSessionRanges = needsSessionRanges
                && (request.isRebuildExisting() || !sessionRangesBuilt);
        boolean willBuildFirstHourBreachEvents = request.shouldBuild(DerivedDataBuildOption.FIRST_HOUR_BREACH_EVENTS)
                && (request.isRebuildExisting() || !firstHourBreachEventsBuilt);
        long totalTicks = tradeSource.countTradeTicks(request.getContractWindows());

        return new DatabaseBuildPlan(
                request.getContractWindows(),
                request.getOptions(),
                request.isRebuildExisting(),
                totalTicks,
                sessionRangesBuilt,
                firstHourBreachEventsBuilt,
                willBuildSessionRanges,
                willBuildFirstHourBreachEvents
        );
    }

    public DatabaseBuildResult runBuild(DatabaseBuildRequest request, DataBuildProgressListener progressListener) {
        /*
         * Intent: Build selected derived data by streaming trade ticks through feature/condition accumulators.
         * Precondition: Request must be valid and underlying trade source/store must be available.
         * Returns: DatabaseBuildResult with counts and elapsed time.
         * Postcondition: Requested derived-data rows and build markers are persisted.
         */
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
        DataBuildProgressListener listener = progressListener == null
                ? DataBuildProgressListener.NO_OP
                : progressListener;
        Instant startedAt = Instant.now();
        DatabaseBuildPlan plan = planBuild(request);
        if (!plan.hasWorkToRun()) {
            listener.onProgress(new DataBuildProgress(0, 0));
            return new DatabaseBuildResult(plan, 0, 0, 0, Duration.between(startedAt, Instant.now()));
        }

        int streamPasses = streamPasses(plan);
        long totalProgressTicks = plan.getTotalTicks() * streamPasses;
        long processedProgressTicks = 0;
        listener.onProgress(new DataBuildProgress(0, totalProgressTicks));

        List<SessionRangeFeature> sessionRangeFeatures;
        List<MarketEventOccurrence> marketConditionOccurrences = null;
        if (plan.willBuildSessionRanges() && plan.willBuildFirstHourBreachEvents()) {
            forge.feature.SessionRangeFeatureCalculator.Accumulator sessionRangeAccumulator =
                    featureBuildService.newSessionRangeAccumulator();
            forge.event.FirstHourBreachEventDetector.LiveAccumulator eventAccumulator =
                    eventBuildService.newLiveFirstHourBreachAccumulator(sessionRangeAccumulator);
            long ticksReadForPass = streamTicks(
                    request,
                    listener,
                    processedProgressTicks,
                    totalProgressTicks,
                    new CompositeTradeTickStreamProcessor(sessionRangeAccumulator, eventAccumulator)
            );
            processedProgressTicks += ticksReadForPass;
            sessionRangeFeatures = sessionRangeAccumulator.getFeatures();
            marketConditionOccurrences = eventAccumulator.getEvents();
        } else if (plan.willBuildSessionRanges()) {
            forge.feature.SessionRangeFeatureCalculator.Accumulator sessionRangeAccumulator =
                    featureBuildService.newSessionRangeAccumulator();
            long ticksReadForPass = streamTicks(request, listener, processedProgressTicks, totalProgressTicks, sessionRangeAccumulator);
            processedProgressTicks += ticksReadForPass;
            sessionRangeFeatures = sessionRangeAccumulator.getFeatures();
        } else {
            sessionRangeFeatures = buildStore.loadSessionRanges(request.getContractWindows());
        }

        long sessionRangesBuilt = 0;
        if (plan.willBuildSessionRanges()) {
            if (request.isRebuildExisting()) {
                buildStore.clearSessionRanges(request.getContractWindows());
                buildStore.clearMarketEventOccurrences(request.getContractWindows(), FirstHourBreachEvent.EVENT_NAME);
            }
            buildStore.saveSessionRanges(sessionRangeFeatures);
            buildStore.markSessionRangesBuilt(request.getContractWindows());
            sessionRangesBuilt = sessionRangeFeatures.size();
        }

        long marketEventsBuilt = 0;
        if (plan.willBuildFirstHourBreachEvents()) {
            List<MarketEventOccurrence> events = marketConditionOccurrences;
            if (events == null) {
                forge.event.FirstHourBreachEventDetector.Accumulator eventAccumulator =
                        eventBuildService.newFirstHourBreachAccumulator(sessionRangeFeatures);
                streamTicks(request, listener, processedProgressTicks, totalProgressTicks, eventAccumulator);
                events = eventAccumulator.getEvents();
            }
            if (request.isRebuildExisting()) {
                buildStore.clearMarketEventOccurrences(request.getContractWindows(), FirstHourBreachEvent.EVENT_NAME);
            }
            buildStore.saveMarketEventOccurrences(events);
            buildStore.markMarketEventOccurrencesBuilt(request.getContractWindows(), FirstHourBreachEvent.EVENT_NAME);
            marketEventsBuilt = events.size();
        }

        return new DatabaseBuildResult(
                plan,
                plan.getTotalTicks(),
                sessionRangesBuilt,
                marketEventsBuilt,
                Duration.between(startedAt, Instant.now())
        );
    }

    private int streamPasses(DatabaseBuildPlan plan) {
        /*
         * Intent: Count how many full tick-stream passes are needed for the current build plan.
         * Precondition: Build plan must be non-null.
         * Returns: Number of stream passes used for progress totals.
         * Postcondition: Plan is unchanged.
         */
        return plan.hasWorkToRun() ? 1 : 0;
    }

    private long streamTicks(
            DatabaseBuildRequest request,
            DataBuildProgressListener listener,
            long processedBeforePass,
            long totalProgressTicks,
            TradeTickStreamProcessor processor
    ) {
        /*
         * Intent: Stream selected contract ticks in batches through one processor while reporting progress.
         * Precondition: Request, listener, and processor must be valid; total progress count should match planned passes.
         * Returns: Number of ticks processed during this pass.
         * Postcondition: Processor receives onComplete after the final batch.
         */
        TradeBatchReader reader = tradeSource.openTradeBatchReader(
                request.getContractWindows(),
                request.getBatchSize()
        );
        long processedTicks = 0;
        while (true) {
            List<TradeTick> batch = reader.readNextBatch();
            if (batch.isEmpty()) {
                processor.onComplete();
                return processedTicks;
            }
            for (TradeTick tick : batch) {
                processor.onTick(tick);
            }
            processedTicks += batch.size();
            listener.onProgress(new DataBuildProgress(
                    Math.min(processedBeforePass + processedTicks, totalProgressTicks),
                    totalProgressTicks
            ));
        }
    }

    private static class CompositeTradeTickStreamProcessor implements TradeTickStreamProcessor {
        private final TradeTickStreamProcessor firstProcessor;
        private final TradeTickStreamProcessor secondProcessor;

        private CompositeTradeTickStreamProcessor(
                TradeTickStreamProcessor firstProcessor,
                TradeTickStreamProcessor secondProcessor
        ) {
            /*
             * Intent: Combine two stream processors so one database read can feed multiple derived-data builders.
             * Precondition: Both processors must be ready to consume the same ordered tick stream.
             * Returns: A constructed CompositeTradeTickStreamProcessor instance.
             * Postcondition: Future tick and completion events are forwarded to both processors.
             */
            this.firstProcessor = firstProcessor;
            this.secondProcessor = secondProcessor;
        }

        @Override
        public void onTick(TradeTick tick) {
            /*
             * Intent: Forward one tick to both child processors.
             * Precondition: Tick may be null only if child processors tolerate null ticks.
             * Returns: Nothing.
             * Postcondition: Both processors have seen the same tick in the same order.
             */
            firstProcessor.onTick(tick);
            secondProcessor.onTick(tick);
        }

        @Override
        public void onComplete() {
            /*
             * Intent: Signal both child processors that the ordered tick stream has ended.
             * Precondition: Child processors must be non-null.
             * Returns: Nothing.
             * Postcondition: Both processors can finalize accumulated state.
             */
            firstProcessor.onComplete();
            secondProcessor.onComplete();
        }
    }
}
