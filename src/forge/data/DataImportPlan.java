package forge.data;

public class DataImportPlan {
    private final String contractSymbol;
    private final String tableName;
    private final boolean existingContractTable;
    private final long existingRows;
    private final String currentSourceFileName;
    private final String currentImportStatus;

    public DataImportPlan(
            String contractSymbol,
            String tableName,
            boolean existingContractTable,
            long existingRows,
            String currentSourceFileName,
            String currentImportStatus
    ) {
        this.contractSymbol = requireText(contractSymbol, "contractSymbol is required");
        this.tableName = requireText(tableName, "tableName is required");
        if (existingRows < 0) {
            throw new IllegalArgumentException("existingRows cannot be negative");
        }
        this.existingContractTable = existingContractTable;
        this.existingRows = existingRows;
        this.currentSourceFileName = currentSourceFileName;
        this.currentImportStatus = currentImportStatus;
    }

    public String getContractSymbol() {
        return contractSymbol;
    }

    public String getTableName() {
        return tableName;
    }

    public boolean hasExistingContractTable() {
        return existingContractTable;
    }

    public boolean hasExistingRows() {
        return existingRows > 0;
    }

    public long getExistingRows() {
        return existingRows;
    }

    public String getCurrentSourceFileName() {
        return currentSourceFileName;
    }

    public String getCurrentImportStatus() {
        return currentImportStatus;
    }

    private static String requireText(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
