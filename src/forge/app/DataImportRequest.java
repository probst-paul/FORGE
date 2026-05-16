package forge.app;

public class DataImportRequest {
    private final String scidFilePath;
    private final boolean rebuildExistingContract;

    public DataImportRequest(String scidFilePath) {
        this(scidFilePath, false);
    }

    public DataImportRequest(String scidFilePath, boolean rebuildExistingContract) {
        if (scidFilePath == null || scidFilePath.trim().isEmpty()) {
            throw new IllegalArgumentException("SCID data file path is required");
        }
        if (!scidFilePath.trim().toLowerCase().endsWith(".scid")) {
            throw new IllegalArgumentException("SCID data file path must end with .scid");
        }
        this.scidFilePath = scidFilePath.trim();
        this.rebuildExistingContract = rebuildExistingContract;
    }

    public String getScidFilePath() {
        return scidFilePath;
    }

    public boolean shouldRebuildExistingContract() {
        return rebuildExistingContract;
    }

    @Override
    public String toString() {
        return "DataImportRequest{" +
                "scidFilePath='" + scidFilePath + '\'' +
                ", rebuildExistingContract=" + rebuildExistingContract +
                '}';
    }
}
