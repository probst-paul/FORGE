package forge.data.postgres;

public class PostgresDatabaseSettings {
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 5432;
    private static final String DEFAULT_DATABASE_NAME = "forge";
    private static final String DEFAULT_MAINTENANCE_DATABASE_NAME = "postgres";
    private static final String DEFAULT_USERNAME = "postgres";

    private final String host;
    private final int port;
    private final String databaseName;
    private final String maintenanceDatabaseName;
    private final String username;
    private final String password;

    public PostgresDatabaseSettings(
            String host,
            int port,
            String databaseName,
            String maintenanceDatabaseName,
            String username,
            String password
    ) {
        /*
         * Intent: Store PostgreSQL connection settings for repository and provider classes.
         * Precondition: Host, database names, and username must be valid; port must be in TCP range.
         * Returns: A constructed PostgresDatabaseSettings instance.
         * Postcondition: Blank/null password is normalized to an empty password.
         */
        this.host = requireText(host, "host is required");
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("port must be between 1 and 65535");
        }
        this.port = port;
        this.databaseName = requireDatabaseName(databaseName);
        this.maintenanceDatabaseName = requireDatabaseName(maintenanceDatabaseName);
        this.username = requireText(username, "username is required");
        this.password = password == null ? "" : password;
    }

    public static PostgresDatabaseSettings fromEnvironment() {
        /*
         * Intent: Build database settings from FORGE_DB_* environment variables with local defaults.
         * Precondition: Environment variables may be absent; port value must parse as an integer when present.
         * Returns: PostgresDatabaseSettings instance.
         * Postcondition: Process environment is not modified.
         */
        return new PostgresDatabaseSettings(
                environmentValue("FORGE_DB_HOST", DEFAULT_HOST),
                environmentInt("FORGE_DB_PORT", DEFAULT_PORT),
                environmentValue("FORGE_DB_NAME", DEFAULT_DATABASE_NAME),
                environmentValue("FORGE_DB_MAINTENANCE_NAME", DEFAULT_MAINTENANCE_DATABASE_NAME),
                environmentValue("FORGE_DB_USER", DEFAULT_USERNAME),
                environmentValue("FORGE_DB_PASSWORD", "")
        );
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
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

    public String primaryJdbcUrl() {
        /*
         * Intent: Build the JDBC URL for the primary FORGE database.
         * Precondition: Database settings must be valid.
         * Returns: PostgreSQL JDBC URL string.
         * Postcondition: Settings are unchanged.
         */
        return jdbcUrl(databaseName);
    }

    public String maintenanceJdbcUrl() {
        /*
         * Intent: Build the JDBC URL for the maintenance database used to create the primary database.
         * Precondition: Database settings must be valid.
         * Returns: PostgreSQL JDBC URL string.
         * Postcondition: Settings are unchanged.
         */
        return jdbcUrl(maintenanceDatabaseName);
    }

    private String jdbcUrl(String database) {
        return "jdbc:postgresql://" + host + ":" + port + "/" + database;
    }

    private static String environmentValue(String name, String defaultValue) {
        String value = System.getenv(name);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        return value.trim();
    }

    private static int environmentInt(String name, int defaultValue) {
        String value = System.getenv(name);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        return Integer.parseInt(value.trim());
    }

    private static String requireText(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private static String requireDatabaseName(String value) {
        /*
         * Intent: Validate database names before they are used in JDBC/admin operations.
         * Precondition: Value must be nonblank.
         * Returns: Trimmed database name when it is a valid PostgreSQL identifier.
         * Postcondition: Invalid names fail before SQL identifiers are built.
         */
        String databaseName = requireText(value, "databaseName is required");
        if (!databaseName.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            throw new IllegalArgumentException("databaseName must be a valid PostgreSQL identifier");
        }
        return databaseName;
    }
}
