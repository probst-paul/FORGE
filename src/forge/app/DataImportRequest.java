package forge.app;

import forge.data.importing.DataImportMode;

public class DataImportRequest {
    private final String scidFilePath;
    private final DataImportMode importMode;
    private final ImportProgressListener progressListener;

    public DataImportRequest(String scidFilePath) {
        this(scidFilePath, DataImportMode.FILL_MISSING);
    }

    public DataImportRequest(String scidFilePath, DataImportMode importMode) {
        this(scidFilePath, importMode, ImportProgressListener.NO_OP);
    }

    public DataImportRequest(String scidFilePath, boolean rebuildExistingContract) {
        this(scidFilePath, rebuildExistingContract ? DataImportMode.OVERWRITE_OVERLAP : DataImportMode.FILL_MISSING);
    }

    public DataImportRequest(
            String scidFilePath,
            boolean rebuildExistingContract,
            ImportProgressListener progressListener
    ) {
        this(
                scidFilePath,
                rebuildExistingContract ? DataImportMode.OVERWRITE_OVERLAP : DataImportMode.FILL_MISSING,
                progressListener
        );
    }

    public DataImportRequest(
            String scidFilePath,
            DataImportMode importMode,
            ImportProgressListener progressListener
    ) {
        /*
         * Intent: Describe a SCID import request and optional import/progress behavior.
         * Precondition: File path must be nonblank and must point to a .scid file by extension.
         * Returns: A constructed DataImportRequest instance.
         * Postcondition: Path is trimmed and a null progress listener is replaced with a no-op listener.
         */
        if (scidFilePath == null || scidFilePath.trim().isEmpty()) {
            throw new IllegalArgumentException("SCID data file path is required");
        }
        if (!scidFilePath.trim().toLowerCase().endsWith(".scid")) {
            throw new IllegalArgumentException("SCID data file path must end with .scid");
        }
        this.scidFilePath = scidFilePath.trim();
        this.importMode = importMode == null ? DataImportMode.FILL_MISSING : importMode;
        this.progressListener = progressListener == null ? ImportProgressListener.NO_OP : progressListener;
    }

    public String getScidFilePath() {
        return scidFilePath;
    }

    public boolean shouldRebuildExistingContract() {
        return importMode == DataImportMode.OVERWRITE_OVERLAP;
    }

    public DataImportMode getImportMode() {
        return importMode;
    }

    public ImportProgressListener getProgressListener() {
        return progressListener;
    }

    /*
     * Intent: Provide a readable import request representation for logs and debugging.
     * Precondition: DataImportRequest has been constructed successfully.
     * Returns: Text containing the import path and rebuild flag.
     * Postcondition: Object state is unchanged.
     */
    @Override
    public String toString() {
        return "DataImportRequest{" +
                "scidFilePath='" + scidFilePath + '\'' +
                ", importMode=" + importMode +
                '}';
    }
}
