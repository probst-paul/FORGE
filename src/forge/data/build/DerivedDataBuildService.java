package forge.data.build;

import forge.data.market.TradeBatchReader;
import forge.data.market.TradeTick;
import forge.data.market.TradeTickStreamProcessor;
import forge.condition.ConditionBuildService;
import forge.condition.FirstHourBreachCondition;
import forge.condition.MarketConditionOccurrence;
import forge.feature.FeatureBuildService;
import forge.feature.SessionRangeFeature;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public class DerivedDataBuildService {
    private final DerivedDataBuildTradeSource tradeSource;
    private final DerivedDataBuildStore buildStore;
    private final FeatureBuildService featureBuildService;
    private final ConditionBuildService eventBuildService;

    public DerivedDataBuildService(
            DerivedDataBuildTradeSource tradeSource,
            DerivedDataBuildStore buildStore,
            FeatureBuildService featureBuildService,
            ConditionBuildService eventBuildService
    ) {
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
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
        boolean sessionRangesBuilt = buildStore.areSessionRangesBuilt(request.getContractWindows());
        boolean firstHourBreachEventsBuilt = buildStore.areMarketConditionOccurrencesBuilt(
                request.getContractWindows(),
                FirstHourBreachCondition.EVENT_NAME
        );
        boolean needsSessionRanges = request.shouldBuild(DerivedDataBuildOption.SESSION_RANGES)
                || request.shouldBuild(DerivedDataBuildOption.FIRST_HOUR_BREACH_EVENTS);
        boolean willBuildSessionRanges = needsSessionRanges
                && (request.isRebuildExisting() || !sessionRangesBuilt);
        boolean willBuildFirstHourBreachConditions = request.shouldBuild(DerivedDataBuildOption.FIRST_HOUR_BREACH_EVENTS)
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
                willBuildFirstHourBreachConditions
        );
    }

    public DatabaseBuildResult runBuild(DatabaseBuildRequest request, DataBuildProgressListener progressListener) {
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
        List<MarketConditionOccurrence> marketConditionOccurrences = null;
        if (plan.willBuildSessionRanges() && plan.willBuildFirstHourBreachConditions()) {
            forge.feature.SessionRangeFeatureCalculator.Accumulator sessionRangeAccumulator =
                    featureBuildService.newSessionRangeAccumulator();
            forge.condition.FirstHourBreachConditionDetector.LiveAccumulator eventAccumulator =
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
                buildStore.clearMarketConditionOccurrences(request.getContractWindows(), FirstHourBreachCondition.EVENT_NAME);
            }
            buildStore.saveSessionRanges(sessionRangeFeatures);
            buildStore.markSessionRangesBuilt(request.getContractWindows());
            sessionRangesBuilt = sessionRangeFeatures.size();
        }

        long marketEventsBuilt = 0;
        if (plan.willBuildFirstHourBreachConditions()) {
            List<MarketConditionOccurrence> events = marketConditionOccurrences;
            if (events == null) {
                forge.condition.FirstHourBreachConditionDetector.Accumulator eventAccumulator =
                        eventBuildService.newFirstHourBreachAccumulator(sessionRangeFeatures);
                streamTicks(request, listener, processedProgressTicks, totalProgressTicks, eventAccumulator);
                events = eventAccumulator.getEvents();
            }
            if (request.isRebuildExisting()) {
                buildStore.clearMarketConditionOccurrences(request.getContractWindows(), FirstHourBreachCondition.EVENT_NAME);
            }
            buildStore.saveMarketConditionOccurrences(events);
            buildStore.markMarketConditionOccurrencesBuilt(request.getContractWindows(), FirstHourBreachCondition.EVENT_NAME);
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
        return plan.hasWorkToRun() ? 1 : 0;
    }

    private long streamTicks(
            DatabaseBuildRequest request,
            DataBuildProgressListener listener,
            long processedBeforePass,
            long totalProgressTicks,
            TradeTickStreamProcessor processor
    ) {
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
            this.firstProcessor = firstProcessor;
            this.secondProcessor = secondProcessor;
        }

        @Override
        public void onTick(TradeTick tick) {
            firstProcessor.onTick(tick);
            secondProcessor.onTick(tick);
        }

        @Override
        public void onComplete() {
            firstProcessor.onComplete();
            secondProcessor.onComplete();
        }
    }
}
