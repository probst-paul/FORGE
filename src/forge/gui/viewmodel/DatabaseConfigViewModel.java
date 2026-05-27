package forge.gui.viewmodel;

public class DatabaseConfigViewModel extends GuiWorkflowViewModel {
    private String host = "";
    private int port = 5432;
    private String databaseName = "";
    private String maintenanceDatabaseName = "postgres";
    private String username = "";
    private boolean configured;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host == null ? "" : host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("port must be between 1 and 65535");
        }
        this.port = port;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName == null ? "" : databaseName;
    }

    public String getMaintenanceDatabaseName() {
        return maintenanceDatabaseName;
    }

    public void setMaintenanceDatabaseName(String maintenanceDatabaseName) {
        this.maintenanceDatabaseName = maintenanceDatabaseName == null ? "" : maintenanceDatabaseName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username == null ? "" : username;
    }

    public boolean isConfigured() {
        return configured;
    }

    public void setConfigured(boolean configured) {
        this.configured = configured;
    }
}
