package forge.data.build;

import forge.data.market.TradeBatchReader;
import forge.data.market.TradeTick;
import forge.condition.ConditionBuildService;
import forge.condition.FirstHourBreachCondition;
import forge.condition.MarketConditionOccurrence;
import forge.feature.FeatureBuildService;
import forge.feature.SessionRangeFeature;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
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

        List<TradeTick> ticks = readTicks(request, plan.getTotalTicks(), listener);
        List<SessionRangeFeature> sessionRangeFeatures = plan.willBuildSessionRanges()
                ? featureBuildService.calculateSessionRanges(ticks)
                : buildStore.loadSessionRanges(request.getContractWindows());
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
            List<MarketConditionOccurrence> events = eventBuildService.detectFirstHourBreachConditions(sessionRangeFeatures, ticks);
            if (request.isRebuildExisting()) {
                buildStore.clearMarketConditionOccurrences(request.getContractWindows(), FirstHourBreachCondition.EVENT_NAME);
            }
            buildStore.saveMarketConditionOccurrences(events);
            buildStore.markMarketConditionOccurrencesBuilt(request.getContractWindows(), FirstHourBreachCondition.EVENT_NAME);
            marketEventsBuilt = events.size();
        }

        return new DatabaseBuildResult(
                plan,
                ticks.size(),
                sessionRangesBuilt,
                marketEventsBuilt,
                Duration.between(startedAt, Instant.now())
        );
    }

    private List<TradeTick> readTicks(
            DatabaseBuildRequest request,
            long totalTicks,
            DataBuildProgressListener listener
    ) {
        listener.onProgress(new DataBuildProgress(0, totalTicks));
        TradeBatchReader reader = tradeSource.openTradeBatchReader(
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
            listener.onProgress(new DataBuildProgress(Math.min(processedTicks, totalTicks), totalTicks));
        }
    }
}
