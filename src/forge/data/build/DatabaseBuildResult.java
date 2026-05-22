package forge.data.build;

import java.time.Duration;

public class DatabaseBuildResult {
    private final DatabaseBuildPlan plan;
    private final long ticksRead;
    private final long sessionRangesBuilt;
    private final long marketEventsBuilt;
    private final Duration elapsedTime;

    public DatabaseBuildResult(
            DatabaseBuildPlan plan,
            long ticksRead,
            long sessionRangesBuilt,
            long marketEventsBuilt,
            Duration elapsedTime
    ) {
        if (plan == null) {
            throw new IllegalArgumentException("plan is required");
        }
        if (ticksRead < 0 || sessionRangesBuilt < 0 || marketEventsBuilt < 0) {
            throw new IllegalArgumentException("result counts cannot be negative");
        }
        if (elapsedTime == null) {
            throw new IllegalArgumentException("elapsedTime is required");
        }
        this.plan = plan;
        this.ticksRead = ticksRead;
        this.sessionRangesBuilt = sessionRangesBuilt;
        this.marketEventsBuilt = marketEventsBuilt;
        this.elapsedTime = elapsedTime;
    }

    public DatabaseBuildPlan getPlan() {
        return plan;
    }

    public long getTicksRead() {
        return ticksRead;
    }

    public long getSessionRangesBuilt() {
        return sessionRangesBuilt;
    }

    public long getMarketEventsBuilt() {
        return marketEventsBuilt;
    }

    public Duration getElapsedTime() {
        return elapsedTime;
    }
}
