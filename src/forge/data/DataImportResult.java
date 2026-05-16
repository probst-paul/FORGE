package forge.data;

public class DataImportResult {
    private final String databaseName;
    private final String tableName;
    private final String contractSymbol;
    private final int importedRows;

    public DataImportResult(String databaseName, String tableName, String contractSymbol, int importedRows) {
        this.databaseName = requireText(databaseName, "databaseName is required");
        this.tableName = requireText(tableName, "tableName is required");
        this.contractSymbol = requireText(contractSymbol, "contractSymbol is required");
        if (importedRows < 0) {
            throw new IllegalArgumentException("importedRows cannot be negative");
        }
        this.importedRows = importedRows;
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

    private static String requireText(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
