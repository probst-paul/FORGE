package forge.gui.viewmodel;

public class ImportDataViewModel extends GuiWorkflowViewModel {
    private String scidFilePath = "";
    private String contractSymbol = "";
    private String tableName = "";
    private boolean rebuildExistingContract;
    private long rowsImported;
    private long nullSideRowsImported;
    private long skippedOutsideFrontMonthRows;

    public String getScidFilePath() {
        return scidFilePath;
    }

    public void setScidFilePath(String scidFilePath) {
        this.scidFilePath = scidFilePath == null ? "" : scidFilePath;
    }

    public String getContractSymbol() {
        return contractSymbol;
    }

    public void setContractSymbol(String contractSymbol) {
        this.contractSymbol = contractSymbol == null ? "" : contractSymbol;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName == null ? "" : tableName;
    }

    public boolean isRebuildExistingContract() {
        return rebuildExistingContract;
    }

    public void setRebuildExistingContract(boolean rebuildExistingContract) {
        this.rebuildExistingContract = rebuildExistingContract;
    }

    public long getRowsImported() {
        return rowsImported;
    }

    public void setRowsImported(long rowsImported) {
        this.rowsImported = requireNonNegative(rowsImported, "rowsImported");
    }

    public long getNullSideRowsImported() {
        return nullSideRowsImported;
    }

    public void setNullSideRowsImported(long nullSideRowsImported) {
        this.nullSideRowsImported = requireNonNegative(nullSideRowsImported, "nullSideRowsImported");
    }

    public long getSkippedOutsideFrontMonthRows() {
        return skippedOutsideFrontMonthRows;
    }

    public void setSkippedOutsideFrontMonthRows(long skippedOutsideFrontMonthRows) {
        this.skippedOutsideFrontMonthRows = requireNonNegative(skippedOutsideFrontMonthRows, "skippedOutsideFrontMonthRows");
    }
}
