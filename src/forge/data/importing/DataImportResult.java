package forge.data.importing;

import java.time.Duration;

public class DataImportResult {
    private final String databaseName;
    private final String tableName;
    private final String contractSymbol;
    private final DataImportMode importMode;
    private final int importedRows;
    private final long duplicateRowsSkipped;
    private final long overlappingRowsRemoved;
    private final long nullSideRowsImported;
    private final long skippedOutsideFrontMonthRows;
    private final Duration elapsedTime;

    public DataImportResult(String databaseName, String tableName, String contractSymbol, int importedRows) {
        this(databaseName, tableName, contractSymbol, importedRows, 0, 0);
    }

    public DataImportResult(
            String databaseName,
            String tableName,
            String contractSymbol,
            int importedRows,
            long nullSideRowsImported,
            long skippedOutsideFrontMonthRows
    ) {
        this(
                databaseName,
                tableName,
                contractSymbol,
                importedRows,
                nullSideRowsImported,
                skippedOutsideFrontMonthRows,
                Duration.ZERO
        );
    }

    public DataImportResult(
            String databaseName,
            String tableName,
            String contractSymbol,
            int importedRows,
            long nullSideRowsImported,
            long skippedOutsideFrontMonthRows,
            Duration elapsedTime
    ) {
        this(
                databaseName,
                tableName,
                contractSymbol,
                DataImportMode.FILL_MISSING,
                importedRows,
                0,
                0,
                nullSideRowsImported,
                skippedOutsideFrontMonthRows,
                elapsedTime
        );
    }

    public DataImportResult(
            String databaseName,
            String tableName,
            String contractSymbol,
            DataImportMode importMode,
            int importedRows,
            long duplicateRowsSkipped,
            long overlappingRowsRemoved,
            long nullSideRowsImported,
            long skippedOutsideFrontMonthRows,
            Duration elapsedTime
    ) {
        this.databaseName = requireText(databaseName, "databaseName is required");
        this.tableName = requireText(tableName, "tableName is required");
        this.contractSymbol = requireText(contractSymbol, "contractSymbol is required");
        this.importMode = importMode == null ? DataImportMode.FILL_MISSING : importMode;
        if (importedRows < 0) {
            throw new IllegalArgumentException("importedRows cannot be negative");
        }
        if (duplicateRowsSkipped < 0) {
            throw new IllegalArgumentException("duplicateRowsSkipped cannot be negative");
        }
        if (overlappingRowsRemoved < 0) {
            throw new IllegalArgumentException("overlappingRowsRemoved cannot be negative");
        }
        if (nullSideRowsImported < 0) {
            throw new IllegalArgumentException("nullSideRowsImported cannot be negative");
        }
        if (skippedOutsideFrontMonthRows < 0) {
            throw new IllegalArgumentException("skippedOutsideFrontMonthRows cannot be negative");
        }
        if (elapsedTime == null) {
            throw new IllegalArgumentException("elapsedTime is required");
        }
        if (elapsedTime.isNegative()) {
            throw new IllegalArgumentException("elapsedTime cannot be negative");
        }
        this.importedRows = importedRows;
        this.duplicateRowsSkipped = duplicateRowsSkipped;
        this.overlappingRowsRemoved = overlappingRowsRemoved;
        this.nullSideRowsImported = nullSideRowsImported;
        this.skippedOutsideFrontMonthRows = skippedOutsideFrontMonthRows;
        this.elapsedTime = elapsedTime;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public String getTableName() {
        return tableName;
    }

    public String getContractSymbol() {
        return contractSymbol;
    }

    public int getImportedRows() {
        return importedRows;
    }

    public DataImportMode getImportMode() {
        return importMode;
    }

    public long getDuplicateRowsSkipped() {
        return duplicateRowsSkipped;
    }

    public long getOverlappingRowsRemoved() {
        return overlappingRowsRemoved;
    }

    public long getNullSideRowsImported() {
        return nullSideRowsImported;
    }

    public long getSkippedOutsideFrontMonthRows() {
        return skippedOutsideFrontMonthRows;
    }

    public Duration getElapsedTime() {
        return elapsedTime;
    }

    private static String requireText(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
