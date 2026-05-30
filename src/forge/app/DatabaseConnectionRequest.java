package forge.app;

public class DatabaseConnectionRequest {
    private final String host;
    private final int port;
    private final String databaseName;
    private final String maintenanceDatabaseName;
    private final String username;
    private final String password;

    public DatabaseConnectionRequest(
            String host,
            int port,
            String databaseName,
            String maintenanceDatabaseName,
            String username,
            String password
    ) {
        /*
         * Intent: Package PostgreSQL connection settings from CLI or GUI input.
         * Precondition: Host and username must be nonblank; port must be a valid TCP port; database names must be PostgreSQL identifiers.
         * Returns: A constructed DatabaseConnectionRequest instance.
         * Postcondition: Required text is trimmed and a null password is normalized to an empty string.
         */
        this.host = requireText(host, "host is required");
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("port must be between 1 and 65535");
        }
        this.port = port;
        this.databaseName = requireDatabaseName(databaseName, "databaseName is required");
        this.maintenanceDatabaseName = requireDatabaseName(maintenanceDatabaseName, "maintenanceDatabaseName is required");
        this.username = requireText(username, "username is required");
        this.password = password == null ? "" : password;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public String getMaintenanceDatabaseName() {
        return maintenanceDatabaseName;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    /*
     * Intent: Validate required connection text.
     * Precondition: Value must not be null, empty, or whitespace-only.
     * Returns: Trimmed text.
     * Postcondition: Callers receive usable text or an exception before invalid state is stored.
     */
    private static String requireText(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    /*
     * Intent: Validate database names before they are passed into PostgreSQL setup logic.
     * Precondition: Value must be required text and match a conservative PostgreSQL identifier pattern.
     * Returns: Trimmed database identifier.
     * Postcondition: Invalid database names are rejected before reaching SQL/database configuration code.
     */
    private static String requireDatabaseName(String value, String message) {
        String databaseName = requireText(value, message);
        if (!databaseName.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            throw new IllegalArgumentException("database name must be a valid PostgreSQL identifier");
        }
        return databaseName;
    }
}
