package forge.gui.preset;

import java.io.Serial;
import java.io.Serializable;

public class GuiUserPreferences implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String databaseHost = "localhost";
    private int databasePort = 5432;
    private String databaseName = "forge";
    private String maintenanceDatabaseName = "postgres";
    private String databaseUsername = "postgres";
    private String lastScidDirectory = "";
    private String importScidFilePath = "";
    private String benchmarkScidFilePath = "";
    private boolean benchmarkRebuildExistingContract = true;
    private boolean benchmarkRebuildDerivedData = true;

    /*
     * Intent: Normalize optional text preference values before persistence.
     * Precondition: value may be null or blank; fallback should be a valid default.
     * Returns: Trimmed value or fallback.
     * Postcondition: Stored preferences do not contain null or accidental blank values.
     */
    private String clean(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }

    public String getDatabaseHost() {
        return databaseHost;
    }

    public void setDatabaseHost(String databaseHost) {
        this.databaseHost = clean(databaseHost, "localhost");
    }

    public int getDatabasePort() {
        return databasePort;
    }

    public void setDatabasePort(int databasePort) {
        if (databasePort < 1 || databasePort > 65535) {
            throw new IllegalArgumentException("databasePort must be between 1 and 65535");
        }
        this.databasePort = databasePort;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = clean(databaseName, "forge");
    }

    public String getMaintenanceDatabaseName() {
        return maintenanceDatabaseName;
    }

    public void setMaintenanceDatabaseName(String maintenanceDatabaseName) {
        this.maintenanceDatabaseName = clean(maintenanceDatabaseName, "postgres");
    }

    public String getDatabaseUsername() {
        return databaseUsername;
    }

    public void setDatabaseUsername(String databaseUsername) {
        this.databaseUsername = clean(databaseUsername, "postgres");
    }

    public String getLastScidDirectory() {
        return lastScidDirectory;
    }

    public void setLastScidDirectory(String lastScidDirectory) {
        this.lastScidDirectory = clean(lastScidDirectory, "");
    }

    public String getImportScidFilePath() {
        return importScidFilePath;
    }

    public void setImportScidFilePath(String importScidFilePath) {
        this.importScidFilePath = clean(importScidFilePath, "");
    }

    public String getBenchmarkScidFilePath() {
        return benchmarkScidFilePath;
    }

    public void setBenchmarkScidFilePath(String benchmarkScidFilePath) {
        this.benchmarkScidFilePath = clean(benchmarkScidFilePath, "");
    }

    public boolean shouldBenchmarkRebuildExistingContract() {
        return benchmarkRebuildExistingContract;
    }

    public void setBenchmarkRebuildExistingContract(boolean benchmarkRebuildExistingContract) {
        this.benchmarkRebuildExistingContract = benchmarkRebuildExistingContract;
    }

    public boolean shouldBenchmarkRebuildDerivedData() {
        return benchmarkRebuildDerivedData;
    }

    public void setBenchmarkRebuildDerivedData(boolean benchmarkRebuildDerivedData) {
        this.benchmarkRebuildDerivedData = benchmarkRebuildDerivedData;
    }

}
