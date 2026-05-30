package forge.benchmark;

import forge.app.BacktestProgressListener;
import forge.app.EventStatisticsProgressListener;
import forge.app.ImportProgressListener;
import forge.data.build.DataBuildProgressListener;

public class BenchmarkRunRequest {
    private final String scidFilePath;
    private final boolean rebuildExistingContract;
    private final boolean rebuildDerivedData;
    private final ImportProgressListener importProgressListener;
    private final DataBuildProgressListener dataBuildProgressListener;
    private final EventStatisticsProgressListener eventStatisticsProgressListener;
    private final BacktestProgressListener backtestProgressListener;

    public BenchmarkRunRequest(
            String scidFilePath,
            boolean rebuildExistingContract,
            boolean rebuildDerivedData,
            ImportProgressListener importProgressListener,
            DataBuildProgressListener dataBuildProgressListener,
            EventStatisticsProgressListener eventStatisticsProgressListener,
            BacktestProgressListener backtestProgressListener
    ) {
        /*
         * Intent: Package all inputs and progress callbacks needed for one benchmark workflow run.
         * Precondition: SCID file path must be nonblank and end with .scid; listener arguments may be null.
         * Returns: A constructed BenchmarkRunRequest instance.
         * Postcondition: File path is trimmed, rebuild flags are stored, and null listeners are replaced with no-op listeners.
         */
        if (scidFilePath == null || scidFilePath.trim().isEmpty()) {
            throw new IllegalArgumentException("SCID data file path is required");
        }
        if (!scidFilePath.trim().toLowerCase().endsWith(".scid")) {
            throw new IllegalArgumentException("SCID data file path must end with .scid");
        }
        this.scidFilePath = scidFilePath.trim();
        this.rebuildExistingContract = rebuildExistingContract;
        this.rebuildDerivedData = rebuildDerivedData;
        this.importProgressListener = importProgressListener == null ? ImportProgressListener.NO_OP : importProgressListener;
        this.dataBuildProgressListener = dataBuildProgressListener == null ? DataBuildProgressListener.NO_OP : dataBuildProgressListener;
        this.eventStatisticsProgressListener = eventStatisticsProgressListener == null
                ? EventStatisticsProgressListener.NO_OP
                : eventStatisticsProgressListener;
        this.backtestProgressListener = backtestProgressListener == null ? BacktestProgressListener.NO_OP : backtestProgressListener;
    }

    public String getScidFilePath() {
        return scidFilePath;
    }

    public boolean shouldRebuildExistingContract() {
        return rebuildExistingContract;
    }

    public boolean shouldRebuildDerivedData() {
        return rebuildDerivedData;
    }

    public ImportProgressListener getImportProgressListener() {
        return importProgressListener;
    }

    public DataBuildProgressListener getDataBuildProgressListener() {
        return dataBuildProgressListener;
    }

    public EventStatisticsProgressListener getEventStatisticsProgressListener() {
        return eventStatisticsProgressListener;
    }

    public BacktestProgressListener getBacktestProgressListener() {
        return backtestProgressListener;
    }
}
