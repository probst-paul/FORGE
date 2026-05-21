package forge.app;

import forge.config.BacktestRequest;
import forge.data.FacadeForgeData;
import forge.data.importing.DataImportPlan;
import forge.data.importing.DataImportResult;
import forge.data.market.TradeBatchReader;
import forge.data.market.TradeTick;
import forge.data.postgres.PostgresDatabaseSettings;
import forge.engine.FacadeForgeEngine;
import forge.event.FacadeForgeEvent;
import forge.event.FirstHourBreachEvent;
import forge.event.MarketEvent;
import forge.feature.FacadeForgeFeature;
import forge.feature.SessionRangeFeature;
import forge.query.EventStatisticsQuery;
import forge.query.EventStatisticsReport;
import forge.query.FacadeForgeQuery;
import forge.reporting.BacktestResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class FacadeForgeApplication {
    private static final int EVENT_STATISTICS_BATCH_SIZE = 10_000;
    private static final FacadeForgeApplication THE_INSTANCE = new FacadeForgeApplication();

    private final FacadeForgeData forgeData;
    private final FacadeForgeEngine forgeEngine;
    private final FacadeForgeFeature forgeFeature;
    private final FacadeForgeEvent forgeEvent;
    private final FacadeForgeQuery forgeQuery;
    private final ForgeApplicationAccess access = new ForgeApplicationAccess();

    public FacadeForgeApplication() {
        this(
                FacadeForgeData.getTheInstance(),
                FacadeForgeEngine.getTheInstance(),
                FacadeForgeFeature.getTheInstance(),
                FacadeForgeEvent.getTheInstance(),
                FacadeForgeQuery.getTheInstance()
        );
    }

    public FacadeForgeApplication(FacadeForgeData forgeData) {
        this(
                forgeData,
                FacadeForgeEngine.getTheInstance(),
                FacadeForgeFeature.getTheInstance(),
                FacadeForgeEvent.getTheInstance(),
                FacadeForgeQuery.getTheInstance()
        );
    }

    public FacadeForgeApplication(FacadeForgeData forgeData, FacadeForgeEngine forgeEngine) {
        this(
                forgeData,
                forgeEngine,
                FacadeForgeFeature.getTheInstance(),
                FacadeForgeEvent.getTheInstance(),
                FacadeForgeQuery.getTheInstance()
        );
    }

    public FacadeForgeApplication(
            FacadeForgeData forgeData,
            FacadeForgeEngine forgeEngine,
            FacadeForgeFeature forgeFeature,
            FacadeForgeEvent forgeEvent,
            FacadeForgeQuery forgeQuery
    ) {
        this.forgeData = Objects.requireNonNull(forgeData, "forgeData is required");
        this.forgeEngine = Objects.requireNonNull(forgeEngine, "forgeEngine is required");
        this.forgeFeature = Objects.requireNonNull(forgeFeature, "forgeFeature is required");
        this.forgeEvent = Objects.requireNonNull(forgeEvent, "forgeEvent is required");
        this.forgeQuery = Objects.requireNonNull(forgeQuery, "forgeQuery is required");
    }

    public static FacadeForgeApplication getTheInstance() {
        return THE_INSTANCE;
    }

    public ForgeApplicationAccess forgeApplicationAccess() {
        return access;
    }

    public class ForgeApplicationAccess {
        public BacktestResult runBacktest(BacktestRequest request) {
            Objects.requireNonNull(request, "request is required");
            return forgeEngine.forgeEngineAccess().run(request);
        }

        public BacktestResult runBacktest(BacktestRequest request, BacktestProgressListener progressListener) {
            Objects.requireNonNull(request, "request is required");
            return forgeEngine.forgeEngineAccess().run(request, progressListener);
        }

        public EventStatisticsReport runEventStatistics(EventStatisticsRequest request) {
            Objects.requireNonNull(request, "request is required");
            if (!FirstHourBreachEvent.EVENT_NAME.equals(request.getEventName())) {
                throw new IllegalArgumentException("Unsupported event statistics request: " + request.getEventName());
            }

            List<TradeTick> ticks = readTicks(request);
            List<SessionRangeFeature> sessionRangeFeatures = forgeFeature.forgeFeatureAccess().calculateSessionRanges(ticks);
            List<MarketEvent> events = forgeEvent.forgeEventAccess().detectFirstHourBreachEvents(sessionRangeFeatures, ticks);
            return forgeQuery.forgeQueryAccess().summarizeEventStatistics(
                    new EventStatisticsQuery(request.getEventName()),
                    sessionRangeFeatures,
                    events
            );
        }

        public DataImportResult importData(DataImportRequest request) {
            Objects.requireNonNull(request, "request is required");
            return forgeData.forgeDataAccess().importScidFile(
                    request.getScidFilePath(),
                    request.shouldRebuildExistingContract(),
                    request.getProgressListener()
            );
        }

        public DataImportPlan planDataImport(DataImportRequest request) {
            Objects.requireNonNull(request, "request is required");
            return forgeData.forgeDataAccess().planScidImport(request.getScidFilePath());
        }

        public DatabaseConnectionRequest configureDatabase(DatabaseConnectionRequest request) {
            Objects.requireNonNull(request, "request is required");
            forgeData.forgeDataAccess().configurePostgresDatabase(new PostgresDatabaseSettings(
                    request.getHost(),
                    request.getPort(),
                    request.getDatabaseName(),
                    request.getMaintenanceDatabaseName(),
                    request.getUsername(),
                    request.getPassword()
            ));
            return request;
        }

        private List<TradeTick> readTicks(EventStatisticsRequest request) {
            TradeBatchReader reader = forgeData.forgeDataAccess().openTradeBatchReader(
                    request.getContractWindows(),
                    EVENT_STATISTICS_BATCH_SIZE
            );
            List<TradeTick> ticks = new ArrayList<>();
            while (true) {
                List<TradeTick> batch = reader.readNextBatch();
                if (batch.isEmpty()) {
                    return ticks;
                }
                ticks.addAll(batch);
            }
        }
    }
}
