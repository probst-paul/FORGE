package forge.gui.viewmodel;

public class BenchmarkViewModel extends GuiWorkflowViewModel {
    private String scidFilePath = "";
    private boolean rebuildExistingContract;
    private boolean rebuildDerivedData = true;
    private long rowsImported;
    private long derivedDataTicksRead;
    private long backtestTicksProcessed;

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
}
