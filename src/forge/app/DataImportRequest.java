package forge.app;

public class DataImportRequest {
    private final String scidFilePath;
    private final boolean rebuildExistingContract;
    private final ImportProgressListener progressListener;

    public DataImportRequest(String scidFilePath) {
        this(scidFilePath, false);
    }

    public DataImportRequest(String scidFilePath, boolean rebuildExistingContract) {
        this(scidFilePath, rebuildExistingContract, ImportProgressListener.NO_OP);
    }

    public DataImportRequest(
            String scidFilePath,
            boolean rebuildExistingContract,
            ImportProgressListener progressListener
    ) {
        if (scidFilePath == null || scidFilePath.trim().isEmpty()) {
            throw new IllegalArgumentException("SCID data file path is required");
        }
        if (!scidFilePath.trim().toLowerCase().endsWith(".scid")) {
            throw new IllegalArgumentException("SCID data file path must end with .scid");
        }
        this.scidFilePath = scidFilePath.trim();
        this.rebuildExistingContract = rebuildExistingContract;
        this.progressListener = progressListener == null ? ImportProgressListener.NO_OP : progressListener;
    }

    public String getScidFilePath() {
        return scidFilePath;
    }

    public boolean shouldRebuildExistingContract() {
        return rebuildExistingContract;
    }

    public ImportProgressListener getProgressListener() {
        return progressListener;
    }

    @Override
    public String toString() {
        return "DataImportRequest{" +
                "scidFilePath='" + scidFilePath + '\'' +
                ", rebuildExistingContract=" + rebuildExistingContract +
                '}';
    }
}
