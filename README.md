# FORGE

**Futures Order Replay and Generalized Execution Engine**

FORGE is an early-stage Java futures backtesting project. The current code focuses on the setup/configuration model, futures contract modeling, feature/event-driven strategy evaluation, and unit-tested behavior for the implemented classes.

It is not yet a complete historical market replay or backtesting engine.

## Currently Implemented

- Command-line backtest setup flow in `forge.app.Main`
- Command-line import flow for preparing PostgreSQL contract tables from SCID file names
- Maven build with JUnit 5 and PostgreSQL JDBC dependencies
- Database-derived instrument/date catalog based on imported contract tables
- Presentation-neutral database build/refresh workflow for derived session ranges and first-hour breach events
- Date-based front-month rollover windows for imported equity index and CL futures
- Batch-driven backtest replay over selected contract windows, with CLI progress reporting
- Position-based trade lifecycle for strategies that emit a trade plan, including target, stop, time-stop, P/L, MFE, and MAE tracking
- MVP execution engine that fills generated orders at the current tick price
- Futures contract model with symbol code, tick size, tick dollar amount, and expiration date
- Static futures instrument definitions for ES, NQ, YM, RTY, and CL
- Abstract `Instrument` base class with concrete futures instrument and futures contract models
- Strategy interface with:
  - `RangeBreakoutStrategy`
  - `OpeningRangeContinuationStrategy`
- Feature/event-driven strategy context and strategy decisions
- Strategy requirements for declaring required features/events plus session and TPO-period evaluation filters
- CLI strategy and event-statistic descriptions plus empty-result guidance
- Trade trigger interface with:
  - `OrderFlowExhaustionTrigger`
  - `PriceCrossoverTrigger`
- Target/stop exit settings represented by strategy trade plans and the trade lifecycle layer
- Basic `OrderRequest` and `Fill` modeling
- Completed-trade reporting metrics by instrument and contract
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
│  ├─ Use or select strategy-compatible trade trigger
│  ├─ Trigger options when the selected trigger requires parameters
│  ├─ Use or select strategy-compatible target mode
│  ├─ Target model options with strategy defaults
│  ├─ Build BacktestRequest
│  ├─ Run backtest with progress
│  └─ Press Enter or type anything to return to Select Action
├─ Run Event Statistics
│  ├─ Select Instrument(s)
│  │  ├─ Choose an instrument's All Available front-month contracts
│  │  └─ Or Select Custom Contracts from rollover-clipped contract windows
│  ├─ Select Event Statistic
│  ├─ Calculate session ranges and first-hour breach events
│  └─ Display event counts by instrument and contract
├─ Import Data
│  └─ Prepare PostgreSQL database/table for the selected SCID file
├─ Build/Refresh Derived Data
│  ├─ Select Instrument(s)
│  │  ├─ Choose an instrument's All Available front-month contracts
│  │  └─ Or Select Custom Contracts from rollover-clipped contract windows
│  ├─ Select derived data to build
│  ├─ Choose whether to rebuild existing derived rows
│  ├─ Review the build plan
│  └─ Build selected derived data with a single-line status bar
└─ Configure Database
   └─ Set PostgreSQL host, port, database, maintenance database, username, and password
```

At the `Select action` prompt, enter `quit` to exit the program. After the backtest setup summary is displayed, press Enter or type anything to return to `Select Action`, or enter `quit` to exit.

Backtest setup no longer asks for a free-form date range. The CLI selects valid front-month contract windows instead. For example, `ES - All Available` includes all imported ES contract windows after rollover clipping, while `Select Custom Contracts` lets the user choose specific contracts such as `ESH25: 2024-12-16 to 2025-03-16` and `ESZ25: 2025-09-15 to 2025-12-14`. This avoids implying continuous data coverage when imported contract months have gaps.

`BacktestRequest` carries those selected contract windows so the backtest engine can read each contract table using its own valid rollover-clipped date range.

Event statistics reuse the same selected contract windows as backtests. `First Hour Breach Frequency` is executed through the query layer with a single-line CLI progress bar. On the first run for a selected contract window, FORGE reads the selected trade ticks, stores derived session ranges and first-hour breach events in PostgreSQL, then reports long breaches, short breaches, no-breach sessions, and breach rate by instrument and by contract. Later runs for the same contract window reuse the stored derived rows instead of scanning the trade ticks again.

The CLI also exposes `Build/Refresh Derived Data`, which runs the same reusable database build workflow without requiring a new SCID file. It uses selected contract windows, derived-data choices such as session ranges and first-hour breach events, and a rebuild flag to create a `DatabaseBuildRequest`. FORGE previews the work with a `DatabaseBuildPlan`, runs selected build work with a single-line status bar, and returns a `DatabaseBuildResult`. This is the same backend shape a future GUI can drive with checkboxes.

Order settings are currently defaulted internally and are not exposed in the CLI.

Strategies own their compatible trigger and target choices. The CLI only asks the user to select a trigger or target when the selected strategy profile allows multiple choices. Strategy profiles also provide default trigger and target settings; for example, `RangeBreakoutStrategy` defaults to `OrderFlowExhaustionTrigger`, also allows `PriceCrossoverTrigger`, defaults to `Fixed Risk/Reward` at `2.0R`, and also allows `Fixed Target` with an `8` tick default.

The CLI displays a short description beside each trading strategy and event statistic so users can choose by intent rather than by internal feature/event requirements. When a backtest or statistic has no usable ticks, no generated signals, no completed trades, or no complete event sessions, the CLI prints a short explanation and returns to `Select Action`.

`PriceCrossoverTrigger` is configured in ticks. For a long trigger, the condition is true when the current trade price reaches or exceeds the threshold. For a short trigger, the condition is true when the current trade price reaches or falls below the threshold.

Strategies can declare `StrategyRequirements`, which tell the engine which derived features, market events, trading sessions, and TPO periods are relevant. The engine uses those requirements to build only the currently supported required facts and to skip strategy evaluation outside the allowed session/TPO filters. RTH TPO periods are 30-minute Central Time periods starting at `08:30`: `A` is `08:30-08:59`, `B` is `09:00-09:29`, `C` is `09:30-09:59`, and so on through the RTH session.

`OpeningRangeContinuationStrategy` uses derived Central Time session features and first-hour breach events. The feature layer calculates the overnight range from `17:00` through `08:29:59`, the RTH first-hour range from `08:30` through `09:29:59`, and the event layer detects the first breach after the first hour. The strategy declares that it requires session ranges and first-hour breach events, and that entries should only evaluate during RTH TPO periods `C` and `D` (`09:30-10:29`). It only arms if the first-hour range remains inside the overnight range, allows one trade per day, and defaults to targeting the overnight high/low with a first-hour opposite-side stop and `10:30` time stop. The strategy can also be configured to use a risk/reward exit style, which keeps the same first-hour stop and calculates the target from entry risk in ticks.

The MVP execution layer fills generated orders at the current tick price. This is intentionally simple so the trade lifecycle can create real completed trades now; future execution work will add realistic market, limit, stop, slippage, and partial-fill behavior.

## Not Yet Implemented

- Additional event detectors beyond first-hour breach
- Analytics feature calculation beyond placeholder models
- Full multi-position and scale-in/scale-out trade lifecycle behavior
- Realistic market, limit, stop, and slippage execution simulation
- Full trade trigger evaluation against market data
- CLI stop selection and stop evaluation inside the backtest engine
- Partial fills and advanced order execution simulation

## Project Structure

```text
src/forge/app        Application facade, requests, console input/output abstractions
src/forge/cli        CLI controller and selection services
src/forge/config     Backtest configuration objects
src/forge/data       Data facade
src/forge/data/build      Derived data build/refresh requests, plans, results, and service
src/forge/data/catalog    Database-derived instrument/date catalog
src/forge/data/contract   Futures contract parsing helpers
src/forge/data/importing  SCID import services, import DTOs, and trade rows
src/forge/data/market     Market data provider abstractions and trade batch models
src/forge/data/postgres   PostgreSQL settings, import repository, and tick data provider
src/forge/data/rollover   Contract rollover calendars and rules
src/forge/analytics  Placeholder analytics feature models
src/forge/backtest   Placeholder backtest position/trade-result models
src/forge/engine     Market context and simple batch-driven backtest engine
src/forge/event      Market event definitions, detection, and event facade
src/forge/execution  Order request/fill models and MVP current-tick execution engine
src/forge/feature    Derived feature architecture and session range calculation
src/forge/model      Instrument and futures contract models
src/forge/query      Event statistics query scaffolding
src/forge/strategy   Strategy interface, catalog, context/decision models, and strategies
src/forge/strategy/support  Reusable strategy helper services and value objects
src/forge/trade      Position-based trade lifecycle, trade plans, and lifecycle facade
src/forge/trigger    Trigger interface, catalog, and trigger result model
src/forge/reporting  Backtest result and performance metric models
test/forge           JUnit 5 tests
```

## Object-Oriented Design

- **Abstract class:** `Instrument` stores common instrument identity and requires subclasses to provide `getInstrumentType()`.
- **Inheritance:** `FuturesInstrument` and `FuturesContract` extend `Instrument`.
- **Interfaces:** `TradingStrategy`, `TradeTrigger`, and `ExecutionEngine` define interchangeable behavior.
- **Polymorphism:** Strategy and trigger implementations can be selected and evaluated through shared interfaces without depending on concrete classes.
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

`MarketContext` carries both tick-native values and display-price accessors. New strategy logic should prefer `getLastPriceTicks()`, `getTickSize()`, and `getTickDollarValue()` for exact and efficient calculations, while `getLastPrice()` remains available for display-oriented or legacy strategy code.

Exit calculations are tick-native as well. Strategies emit `TradePlan` values containing target and stop prices in ticks, and `TradeLifecycleEngine` evaluates target, stop, and time-stop exits directly in tick space.

Each contract table is treated as the authoritative dataset for that contract. If a contract table already exists, the CLI prompts before wiping and rebuilding it from the selected SCID file. FORGE stores the source file name and SCID record index on each row, creates a unique index over the SCID record index inside the contract table, and inserts with `ON CONFLICT DO NOTHING`. It also maintains a `forge_contract_imports` table with the source file metadata, next record index to process, row count, and first/last imported trade timestamps. The checkpoint advances only after a batch insert succeeds.

The `Select Instrument(s)` screen is driven by the `forge_contract_imports` metadata table instead of scanning large contract trade tables for `MIN`/`MAX` timestamps. That keeps the instrument list responsive even when imported contracts contain tens of millions of rows. Existing contract imports created before this metadata existed may need to be rebuilt before they appear in the instrument selection list.

The import flow skips records outside the contract's active front-month window before storing rows. For CME equity index roots (`ES`, `NQ`, `YM`, and `RTY`), FORGE clips each contract table to its active window using the common convention of rolling on the Monday before the third Friday of the contract month. For `CL`, FORGE estimates expiration as three business days before the 25th calendar day of the month before delivery, then rolls on the Friday before that expiration date. The backtest instrument list is derived from imported contract tables after that same active-window logic. Instruments without a rollover rule currently use the imported table date range as-is.

The CLI renders import progress as a single updating terminal line. The underlying progress calculation is exposed through `ImportProgress`, so a future JavaFX or Swing UI can render the same import state with a graphical progress bar.

Backtest runs use the same presentation idea. Before replay begins, FORGE counts strategy-usable ticks for the selected contract windows, then renders a single-line backtest progress bar while batches are processed:

```text
Running backtest [############------------] 50% 500000/1000000
```

The reusable backtest progress state is exposed through `BacktestProgress` and `BacktestProgressListener`; the CLI only owns the terminal-specific rendering.

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

The `analytics`, `backtest`, `engine`, and `execution` packages are still partial foundations. They exist to preserve the package/facade architecture while deeper analytics, richer execution simulation, and scale-in/scale-out trade lifecycle behavior are still being designed and implemented.
