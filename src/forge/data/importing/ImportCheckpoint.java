package forge.data.importing;

public class ImportCheckpoint {
    private final String tableName;
    private final String sourceFileName;
    private final long nextRecordIndex;

    public ImportCheckpoint(String tableName, String sourceFileName, long nextRecordIndex) {
        this.tableName = requireText(tableName, "tableName is required");
        this.sourceFileName = requireText(sourceFileName, "sourceFileName is required");
        if (nextRecordIndex < 1) {
            throw new IllegalArgumentException("nextRecordIndex must be greater than zero");
        }
        this.nextRecordIndex = nextRecordIndex;
    }

    public String getTableName() {
        return tableName;
    }

    public String getSourceFileName() {
        return sourceFileName;
    }

    public long getNextRecordIndex() {
        return nextRecordIndex;
    }

    private static String requireText(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
