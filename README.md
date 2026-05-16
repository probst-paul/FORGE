# FORGE

**Futures Order Replay and Generalized Execution Engine**

FORGE is an early-stage Java futures backtesting project. The current code focuses on the setup/configuration model, futures contract modeling, strategy and target-model abstractions, and unit-tested behavior for the implemented classes.

It is not yet a complete historical market replay or backtesting engine.

## Currently Implemented

- Command-line backtest setup flow in `forge.app.Main`
- Command-line import flow for preparing PostgreSQL contract tables from SCID file names
- Maven build with JUnit 5 and PostgreSQL JDBC dependencies
- In-memory instrument/date catalog with sample futures instruments
- Futures contract model with symbol code, tick size, tick dollar amount, and expiration date
- Static futures instrument definitions for ES, NQ, YM, RTY, and CL
- Abstract `Instrument` base class and concrete `FuturesContract`
- Strategy interface with an implemented `RangeBreakoutStrategy`
- Trade trigger interface with an implemented no-op `OrderFlowExhaustionTrigger`
- Target model interface with:
  - `FixedRiskRewardTarget`
  - `FixedTarget`
- Basic `OrderRequest` modeling
- JUnit 5 tests for implemented behavior
- Mermaid class and sequence diagrams:
  - `class-model.md`

## Current CLI Flow

```text
Select Action
├─ Run Backtest
│  ├─ Select Instrument(s)
│  ├─ Select Date Range
│  ├─ Select Trading Strategy
│  ├─ Risk Settings
│  ├─ Select Trade Trigger
│  ├─ Select Target Model
│  ├─ Target Model Options
│  └─ Build BacktestRequest
├─ Import Data
│  └─ Prepare PostgreSQL database/table for the selected SCID file
├─ Configure Database
│  └─ Set PostgreSQL host, port, database, maintenance database, username, and password
└─ Exit
```

Order settings are currently defaulted internally and are not exposed in the CLI.

## Not Yet Implemented

- Real market data retrieval from PostgreSQL
- Contract rollover handling
- Derived market analytics
- Full trade trigger evaluation against market data
- Backtest engine orchestration
- Order execution simulation
- Completed trade result calculation
- Performance reporting

## Project Structure

```text
src/forge/app        Application facade, requests, console input/output abstractions
src/forge/cli        CLI controller and selection services
src/forge/config     Backtest configuration objects
src/forge/data       Instrument catalog, PostgreSQL setup, and SCID import preparation
src/forge/engine     Market context
src/forge/execution  Order request and order enums
src/forge/model      Instrument and futures contract models
src/forge/strategy   Strategy interface, catalog, and range breakout strategy
src/forge/target     Target model interface, implementations, and results
src/forge/trigger    Trigger interface, catalog, and trigger result model
test/forge           JUnit 5 tests
```

## Object-Oriented Design

- **Abstract class:** `Instrument` stores common instrument identity and requires subclasses to provide `getInstrumentType()`.
- **Inheritance:** `FuturesContract` extends `Instrument`.
- **Interfaces:** `TradingStrategy`, `TradeTrigger`, and `TargetModel` define interchangeable behavior.
- **Polymorphism:** `FixedRiskRewardTarget` and `FixedTarget` both implement `TargetModel.calculateTarget(...)` with different behavior.
- **Upcasting:** `InstrumentDataCatalog` creates `FuturesContract` instances and stores them as `Instrument`.
- **Downcasting:** `InstrumentDataCatalog.AvailableInstrumentData` safely downcasts `Instrument` to `FuturesContract` when futures-specific details are needed.

## Run the Application

From the project root:

```bash
mvn exec:java
```

To build a runnable jar with dependencies included:

```bash
mvn package
java -jar target/forge-1.0-SNAPSHOT.jar
```

## PostgreSQL Setup

FORGE uses PostgreSQL for imported market data storage. The import flow currently creates or reuses a database, creates a table for the contract derived from the SCID file name, reads Sierra Chart intraday records, and inserts each record as a trade row.

Supported futures roots for import are currently `ES`, `NQ`, `YM`, `RTY`, and `CL`. Importing an unsupported root fails before the database table is created or modified.

Install and start PostgreSQL on macOS with Homebrew:

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

Run FORGE through Maven so the PostgreSQL JDBC driver is on the runtime classpath:

```bash
mvn exec:java
```

In the CLI, choose `3. Configure Database` and use:

```text
Host: localhost
Port: 5432
Database name: forge
Maintenance database: postgres
Username: postgres
Password: postgres
```

Then choose `2. Import Data` and enter a SCID path, for example:

```text
/Users/paulprobst/path/to/ESU25_FUT_CME.scid
```

FORGE derives the contract from the file name and prepares a matching table:

```text
Importing ESU25 [########################] 100% 123456/123456
Data storage prepared:
Database: forge
Table: ESU25
Contract: ESU25
Rows imported: <number of SCID records imported>
```

The contract table currently uses this trade-level schema:

```sql
"tradeDateTime" TIMESTAMPTZ NOT NULL,
"priceTicks" BIGINT NOT NULL,
"bidPriceTicks" BIGINT,
"askPriceTicks" BIGINT,
quantity BIGINT NOT NULL,
side INT,
"numTrades" BIGINT NOT NULL,
"sourceFileName" TEXT NOT NULL,
"scidRecordIndex" BIGINT NOT NULL
```

This maps to Sierra Chart SCID single-trade records as:

```text
"tradeDateTime" <- DateTime converted from Sierra Chart UTC microseconds
priceTicks      <- Close converted to instrument ticks
"bidPriceTicks" <- Low converted to instrument ticks
"askPriceTicks" <- High converted to instrument ticks
quantity        <- TotalVolume
side            <- AskVolume > 0 means buy aggressor, BidVolume > 0 means sell aggressor
"numTrades"     <- NumTrades
```

FORGE converts SCID float prices to integer tick counts during import using the hardcoded instrument tick size. The database stores those tick counts as `BIGINT` rather than storing floating-point prices, so strategy calculations can work in exact tick space and convert back to display prices only at the UI/reporting edge. Sierra Chart's count and volume fields are unsigned 4-byte integers, so FORGE stores imported count/volume values as `BIGINT` to preserve their full range in PostgreSQL. `side` is FORGE-specific rather than a Sierra Chart field, with `1` for buy aggressor, `-1` for sell aggressor, and `NULL` when the aggressor side cannot be identified. Backtest data reads should filter to strategy-usable trades with `side IS NOT NULL`.

Each contract table is treated as the authoritative dataset for that contract. If a contract table already exists, the CLI prompts before wiping and rebuilding it from the selected SCID file. FORGE stores the source file name and SCID record index on each row, creates a unique index over the SCID record index inside the contract table, and inserts with `ON CONFLICT DO NOTHING`. It also maintains a `forge_contract_imports` table with the source file metadata and next record index to process. The checkpoint advances only after a batch insert succeeds.

The CLI renders import progress as a single updating terminal line. The underlying progress calculation is exposed through `ImportProgress`, so a future JavaFX or Swing UI can render the same import state with a graphical progress bar.

Database settings can also be provided with environment variables:

```bash
export FORGE_DB_HOST=localhost
export FORGE_DB_PORT=5432
export FORGE_DB_NAME=forge
export FORGE_DB_MAINTENANCE_NAME=postgres
export FORGE_DB_USER=postgres
export FORGE_DB_PASSWORD=postgres
```

## Run Tests

The project uses Maven and JUnit 5:

```bash
mvn test
```

## Status

FORGE is in early architectural development. The current implementation is intentionally small and focuses on clean object modeling, selection/configuration flow, and tested domain behavior before adding market data storage, replay, execution, and reporting.
