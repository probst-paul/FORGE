# FORGE

**Futures Order Replay and Generalized Execution Engine**

FORGE is an early-stage Java futures backtesting project. The current code focuses on the setup/configuration model, futures contract modeling, strategy and target-model abstractions, and unit-tested behavior for the implemented classes.

It is not yet a complete historical market replay or backtesting engine.

## Currently Implemented

- Command-line backtest setup flow in `forge.app.Main`
- Command-line import flow for preparing PostgreSQL contract tables from SCID file names
- Maven build with JUnit 5 and PostgreSQL JDBC dependencies
- Database-derived instrument/date catalog based on imported contract tables
- Date-based front-month rollover windows for imported equity index and CL futures
- Futures contract model with symbol code, tick size, tick dollar amount, and expiration date
- Static futures instrument definitions for ES, NQ, YM, RTY, and CL
- Abstract `Instrument` base class with concrete futures instrument and futures contract models
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
│  │  ├─ Choose an instrument's All Available front-month contracts
│  │  └─ Or Select Custom Contracts from rollover-clipped contract windows
│  ├─ Select Trading Strategy
│  ├─ Risk Settings
│  ├─ Select Trade Trigger
│  ├─ Select Target Model
│  ├─ Target Model Options
│  ├─ Build BacktestRequest
│  └─ Press Enter or type anything to return to Select Action
├─ Import Data
│  └─ Prepare PostgreSQL database/table for the selected SCID file
└─ Configure Database
   └─ Set PostgreSQL host, port, database, maintenance database, username, and password
```

At the `Select action` prompt, enter `quit` to exit the program. After the backtest setup summary is displayed, press Enter or type anything to return to `Select Action`, or enter `quit` to exit.

Backtest setup no longer asks for a free-form date range. The CLI selects valid front-month contract windows instead. For example, `ES - All Available` includes all imported ES contract windows after rollover clipping, while `Select Custom Contracts` lets the user choose specific contracts such as `ESH25: 2024-12-16 to 2025-03-16` and `ESZ25: 2025-09-15 to 2025-12-14`. This avoids implying continuous data coverage when imported contract months have gaps.

Order settings are currently defaulted internally and are not exposed in the CLI.

## Not Yet Implemented

- Real market data retrieval from PostgreSQL
- Market data reads from PostgreSQL with active-contract rollover filters
- Derived market analytics
- Analytics feature calculation beyond placeholder models
- Full backtest package behavior beyond placeholder position/trade-result models
- Execution package behavior beyond basic order request modeling
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
src/forge/data       Data facade
src/forge/data/catalog    Database-derived instrument/date catalog
src/forge/data/contract   Futures contract parsing helpers
src/forge/data/importing  SCID import services, import DTOs, and trade rows
src/forge/data/market     Market data provider abstractions
src/forge/data/postgres   PostgreSQL settings and repository
src/forge/data/rollover   Contract rollover calendars and rules
src/forge/analytics  Placeholder analytics feature models
src/forge/backtest   Placeholder backtest position/trade-result models
src/forge/engine     Market context and placeholder engine facade
src/forge/execution  Basic order request/enums; execution simulation is not implemented yet
src/forge/model      Instrument and futures contract models
src/forge/strategy   Strategy interface, catalog, and range breakout strategy
src/forge/target     Target model interface, implementations, and results
src/forge/trigger    Trigger interface, catalog, and trigger result model
src/forge/reporting  Placeholder reporting/metrics models
test/forge           JUnit 5 tests
```

## Object-Oriented Design

- **Abstract class:** `Instrument` stores common instrument identity and requires subclasses to provide `getInstrumentType()`.
- **Inheritance:** `FuturesInstrument` and `FuturesContract` extend `Instrument`.
- **Interfaces:** `TradingStrategy`, `TradeTrigger`, and `TargetModel` define interchangeable behavior.
- **Polymorphism:** `FixedRiskRewardTarget` and `FixedTarget` both implement `TargetModel.calculateTarget(...)` with different behavior.
- **Upcasting:** `InstrumentDataCatalog` creates `FuturesInstrument` entries from imported contract tables and stores them as `Instrument`.
- **Downcasting:** `InstrumentDataCatalog.AvailableInstrumentData` safely downcasts `Instrument` to `FuturesInstrument` when futures-specific tick details are needed.

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

Supported futures roots for import are currently `ES`, `NQ`, `YM`, `RTY`, and `CL`. Importing an unsupported root fails before the database table is created or modified. FORGE also validates contract month codes before database work begins: equity index imports currently allow only quarterly contracts (`H`, `M`, `U`, and `Z`), while `CL` allows the standard monthly futures cycle. If a file name points to a contract month that should not exist, the import stops and reports that the SCID file may be corrupted or incorrectly named.

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
Import time: <elapsed time>
Null-side rows imported: <records with no identifiable aggressor side>
Rows skipped outside front-month window: <records outside the active contract window>
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

FORGE converts SCID float prices to integer tick counts during import using the hardcoded instrument tick size. The database stores those tick counts as `BIGINT` rather than storing floating-point prices, so strategy calculations can work in exact tick space and convert back to display prices only at the UI/reporting edge. Sierra Chart's count and volume fields are unsigned 4-byte integers, so FORGE stores imported count/volume values as `BIGINT` to preserve their full range in PostgreSQL. `side` is FORGE-specific rather than a Sierra Chart field, with `1` for buy aggressor, `-1` for sell aggressor, and `NULL` when the aggressor side cannot be identified. The import summary reports how many null-side rows were stored. Backtest data reads should filter to strategy-usable trades with `side IS NOT NULL`.

Each contract table is treated as the authoritative dataset for that contract. If a contract table already exists, the CLI prompts before wiping and rebuilding it from the selected SCID file. FORGE stores the source file name and SCID record index on each row, creates a unique index over the SCID record index inside the contract table, and inserts with `ON CONFLICT DO NOTHING`. It also maintains a `forge_contract_imports` table with the source file metadata, next record index to process, row count, and first/last imported trade timestamps. The checkpoint advances only after a batch insert succeeds.

The `Select Instrument(s)` screen is driven by the `forge_contract_imports` metadata table instead of scanning large contract trade tables for `MIN`/`MAX` timestamps. That keeps the instrument list responsive even when imported contracts contain tens of millions of rows. Existing contract imports created before this metadata existed may need to be rebuilt before they appear in the instrument selection list.

The import flow skips records outside the contract's active front-month window before storing rows. For CME equity index roots (`ES`, `NQ`, `YM`, and `RTY`), FORGE clips each contract table to its active window using the common convention of rolling on the Monday before the third Friday of the contract month. For `CL`, FORGE estimates expiration as three business days before the 25th calendar day of the month before delivery, then rolls on the Friday before that expiration date. The backtest instrument list is derived from imported contract tables after that same active-window logic. Instruments without a rollover rule currently use the imported table date range as-is.

The CLI renders import progress as a single updating terminal line. The underlying progress calculation is exposed through `ImportProgress`, so a future JavaFX or Swing UI can render the same import state with a graphical progress bar.

### Future Import Performance Ideas

Current SCID imports use PostgreSQL text `COPY` in batches of `100,000` records and have tested at just under two minutes for a roughly 53 million record file on the current local setup. That is good enough for now, so the import path is intentionally being left as-is.

Possible future performance enhancements:

- Drop/recreate nonessential indexes during confirmed full rebuilds, then recreate them after import.
- Stream parsed SCID rows directly into `COPY` instead of building batch strings in memory.
- Use PostgreSQL binary `COPY` if text `COPY` becomes a bottleneck.
- Add database tuning notes for large imports, such as `maintenance_work_mem`, `synchronous_commit`, and local disk considerations.
- Add import timing/history records so performance changes can be compared across runs.

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

FORGE is in early architectural development. The current implementation now includes SCID-to-PostgreSQL ingestion, rollover-aware catalog availability, and exact tick-based price storage, but it is still not a complete backtesting system.

The `analytics`, `backtest`, `engine`, `execution`, and `reporting` packages are largely placeholders or partial foundations. They exist to preserve the package/facade architecture while the real market replay, execution simulation, analytics, and reporting behavior are still being designed and implemented.
