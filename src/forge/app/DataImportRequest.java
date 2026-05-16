package forge.app;

public class DataImportRequest {
    private final String scidFilePath;

    public DataImportRequest(String scidFilePath) {
        if (scidFilePath == null || scidFilePath.trim().isEmpty()) {
            throw new IllegalArgumentException("SCID data file path is required");
        }
        if (!scidFilePath.trim().toLowerCase().endsWith(".scid")) {
            throw new IllegalArgumentException("SCID data file path must end with .scid");
        }
        this.scidFilePath = scidFilePath.trim();
    }

    public String getScidFilePath() {
        return scidFilePath;
    }

    @Override
    public String toString() {
        return "DataImportRequest{" +
                "scidFilePath='" + scidFilePath + '\'' +
                '}';
    }
}
