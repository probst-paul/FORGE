package forge.gui.viewmodel;

public class BenchmarkViewModel extends GuiWorkflowViewModel {
    private String scidFilePath = "";
    private boolean rebuildExistingContract;
    private boolean rebuildDerivedData = true;
    private long rowsImported;
    private long derivedDataTicksRead;
    private long backtestTicksProcessed;
    private final BenchmarkPhaseProgress importProgress = new BenchmarkPhaseProgress("Import");
    private final BenchmarkPhaseProgress derivedDataProgress = new BenchmarkPhaseProgress("Derived Data");
    private final BenchmarkPhaseProgress eventStatisticsProgress = new BenchmarkPhaseProgress("Event Statistics");
    private final BenchmarkPhaseProgress backtestProgress = new BenchmarkPhaseProgress("Backtest");

    public String getScidFilePath() {
        return scidFilePath;
    }

    public void setScidFilePath(String scidFilePath) {
        this.scidFilePath = scidFilePath == null ? "" : scidFilePath;
    }

    public boolean isRebuildExistingContract() {
        return rebuildExistingContract;
    }

    public void setRebuildExistingContract(boolean rebuildExistingContract) {
        this.rebuildExistingContract = rebuildExistingContract;
    }

    public boolean isRebuildDerivedData() {
        return rebuildDerivedData;
    }

    public void setRebuildDerivedData(boolean rebuildDerivedData) {
        this.rebuildDerivedData = rebuildDerivedData;
    }

    public long getRowsImported() {
        return rowsImported;
    }

    public void setRowsImported(long rowsImported) {
        this.rowsImported = requireNonNegative(rowsImported, "rowsImported");
    }

    public long getDerivedDataTicksRead() {
        return derivedDataTicksRead;
    }

    public void setDerivedDataTicksRead(long derivedDataTicksRead) {
        this.derivedDataTicksRead = requireNonNegative(derivedDataTicksRead, "derivedDataTicksRead");
    }

    public long getBacktestTicksProcessed() {
        return backtestTicksProcessed;
    }

    public void setBacktestTicksProcessed(long backtestTicksProcessed) {
        this.backtestTicksProcessed = requireNonNegative(backtestTicksProcessed, "backtestTicksProcessed");
    }

    public BenchmarkPhaseProgress getImportProgress() {
        return importProgress;
    }

    public BenchmarkPhaseProgress getDerivedDataProgress() {
        return derivedDataProgress;
    }

    public BenchmarkPhaseProgress getEventStatisticsProgress() {
        return eventStatisticsProgress;
    }

    public BenchmarkPhaseProgress getBacktestProgress() {
        return backtestProgress;
    }

    public void resetPhaseProgress() {
        importProgress.reset();
        derivedDataProgress.reset();
        eventStatisticsProgress.reset();
        backtestProgress.reset();
    }

    public void refreshPhaseElapsedTimes() {
        importProgress.refreshElapsedTime();
        derivedDataProgress.refreshElapsedTime();
        eventStatisticsProgress.refreshElapsedTime();
        backtestProgress.refreshElapsedTime();
    }
}
