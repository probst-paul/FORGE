# Setup

## PostgreSQL

FORGE uses PostgreSQL for imported market data storage. Install and start PostgreSQL on macOS with Homebrew:

```bash
brew install postgresql@16
brew services start postgresql@16
```

Open the maintenance database:

```bash
psql postgres
```

Create or update the default local user:

```sql
CREATE USER postgres WITH PASSWORD 'postgres';
ALTER USER postgres CREATEDB;
```

If the user already exists, update it instead:

```sql
ALTER USER postgres WITH PASSWORD 'postgres';
ALTER USER postgres CREATEDB;
```

Exit `psql`:

```sql
\q
```

## Database Configuration

The JavaFX GUI automatically prepares the configured PostgreSQL database on startup. If the database is missing, FORGE attempts to create it and ensure the required support tables exist. The Settings screen also provides a `Repair/Create Database` action for manually rerunning the same non-destructive preparation workflow.

The admin CLI can still be used to change database connection settings when needed:

```bash
mvn exec:java
```

Then choose `3. Configure Database` and use:

```text
Host: localhost
Port: 5432
Database name: forge
Maintenance database: postgres
Username: postgres
Password: postgres
```

Launch the JavaFX GUI:

```bash
mvn javafx:run
```

Database settings can also be provided with environment variables:

```bash
export FORGE_DB_HOST=localhost
export FORGE_DB_PORT=5432
export FORGE_DB_NAME=forge
export FORGE_DB_MAINTENANCE_NAME=postgres
export FORGE_DB_USER=postgres
export FORGE_DB_PASSWORD=postgres
```

## Runtime Commands

Launch the JavaFX GUI:

```bash
mvn javafx:run
```

Run the admin CLI:

```bash
mvn exec:java
```

Build a runnable jar with dependencies included:

```bash
mvn package
java -jar target/forge-1.0-SNAPSHOT.jar
```

Run tests:

```bash
mvn test
```

## Runtime Files

FORGE writes GUI preferences and saved report snapshots to project-local runtime folders. GUI import-path preferences are stored under `runtime/preferences`, and saved event-statistics/backtest report snapshots are stored as `.dat` files under:

```text
runtime/reports
```

These files are local runtime artifacts and are not intended to be committed.

## Sample SCID File

The repository includes one small importable SCID sample and two intentionally invalid/error-oriented SCID files for testing error handling:

```text
sample/YMM6_CME_Sample.scid
sample/ESU24_FUT_CME_InvalidHeader.scid
sample/ESU23_FUT_CME_RuntimeTickError.scid
```

For a local import, provide either the sample path or a Sierra Chart SCID path such as:

```text
/Users/paulprobst/path/to/ESU25_FUT_CME.scid
```
