# FORGE

**Futures Order Replay and Generalized Execution Engine**

FORGE is an early-stage Java futures research and backtesting project. It provides a JavaFX GUI for user-facing import, event statistics, and backtest workflows; a CLI for administration tasks such as import, derived-data builds, database configuration, benchmark runs, and database wipes; and a SCID-to-PostgreSQL import flow with contract validation, checkpointing, progress reporting, and front-month rollover filtering.

The project is organized around reusable market research layers: raw imported ticks, derived features, market events, studies, statistics, strategies, simulation engines, trade lifecycle handling, risk checks, and reporting models. The goal is to support futures market structure research first, then layer simulated trade execution on top of those reusable study/event results.

FORGE is an early-stage system rather than a complete historical market replay or backtesting engine.

## Highlights

- JavaFX GUI for user-facing import, event statistics, and backtest workflows
- Admin CLI for database configuration, import, derived-data refresh, benchmarks, and database wipes
- PostgreSQL-backed SCID ingestion with contract validation, checkpointing, rollover filtering, and progress reporting
- Database-derived instrument catalog based on imported, rollover-clipped contract windows
- Tick-native price storage and strategy math using `BIGINT` tick counts instead of floating-point price fields
- Derived session features for overnight, first-hour RTH, and full RTH ranges
- Event/statistics workflow for studying market setup frequency before simulating trades
- Basic backtest simulation with trade plans, risk checks, target/stop/time-stop exits, P/L, MFE, and MAE
- Java serialization support for GUI preference save/load files using `.dat`
- Unit-tested behavior across implemented application, data, engine, GUI, risk, statistics, strategy, and trade layers

## User-Facing GUI

```text
FORGE JavaFX GUI
├─ Import Data
│  ├─ Select a SCID file with the system file chooser
│  ├─ Optionally build session ranges after import
│  ├─ Optionally build first-hour breach events after import
│  └─ Import with progress and summary output
├─ Event Statistics
│  ├─ Select an event-statistics study
│  ├─ Select imported rollover-clipped contract windows
│  ├─ Build missing derived data when needed and persist it to PostgreSQL
│  └─ Display instrument and contract result cards/tabs
└─ Backtest
   ├─ Select imported rollover-clipped contract windows
   ├─ Select strategy and risk settings
   ├─ Build missing derived data when needed and persist it to PostgreSQL
   └─ Display summary, instrument/contract tables, and simulated trades
```

The GUI is the primary user-facing surface for research workflows. Backtest and event-statistics results persist while the GUI window remains open.

## Admin CLI

```text
Select Action
├─ Import Data
├─ Build/Refresh Derived Data
├─ Configure Database
├─ Run Benchmark Workflow
└─ Wipe Database
```

The CLI is reserved for admin and maintenance operations. The benchmark workflow runs import, derived-data build, event statistics, and backtest through the normal application facades with compact progress/timing output.

## Object-Oriented Design

- **Facade pattern:** Package-level facades such as `FacadeForgeApplication`, `FacadeForgeData`, `FacadeForgeGui`, `FacadeForgeRisk`, `FacadeForgeTrade`, and `FacadeForgeEngine` provide narrow entry points into larger subsystems.
- **Abstract class:** `Instrument` stores common instrument identity and requires subclasses to provide `getInstrumentType()`.
- **Inheritance:** `FuturesInstrument` and `FuturesContract` extend `Instrument`.
- **Interfaces:** `TradingStrategy`, `MarketEvent`, `ExecutionEngine`, and data source/store interfaces define interchangeable behavior.
- **Polymorphism:** Strategy, event, execution, and data-access implementations can be selected and evaluated through shared interfaces without depending on concrete classes.
- **Upcasting:** `InstrumentDataCatalog` creates `FuturesInstrument` entries from imported contract tables and stores them as `Instrument`.
- **Downcasting:** `InstrumentDataCatalog.AvailableInstrumentData` safely downcasts `Instrument` to `FuturesInstrument` when futures-specific tick details are needed.
- **Generics:** Utility/catalog classes such as `ImmutableLists` and `ClasspathCatalog` use generic type parameters to preserve compile-time type safety across reusable collection operations.
- **Serialization:** GUI preferences are saved and loaded through `ObjectOutputStream`, `ObjectInputStream`, and `Serializable` preference models.

## Project Structure

```text
src/forge/app        Application facade, workflow requests, progress listeners, console input/output abstractions
src/forge/benchmark  Optional benchmark workflow over import, derived data, statistics, and backtest
src/forge/cli        Admin CLI controller and selection services
src/forge/config     Backtest configuration objects
src/forge/data       Data facade, importing, PostgreSQL, market data, catalog, rollover, and derived build packages
src/forge/engine     Engine facade, market context, backtest engine, and event-statistics engine packages
src/forge/feature    Derived feature architecture, session ranges, and reusable range helpers
src/forge/event      Market event interface, occurrence detection, catalog, facade, and result models
src/forge/gui        JavaFX application, facade, controllers, views, view models, and presets
src/forge/model      Instrument and futures contract models
src/forge/risk       Risk manager, risk decisions, and risk facade
src/forge/statistics Research/statistics aggregation services and facade
src/forge/study      Market study definitions and study facade
src/forge/strategy   Strategy interface, catalog, context/decision models, and strategies
src/forge/trade      Order/fill models, MVP execution, position lifecycle, trade plans, trade results, and lifecycle facade
src/forge/reporting  Reporting facade, performance metrics, backtest reports, and event-statistics reports
src/forge/util       Shared utilities for classpath catalogs and immutable list copies
test/forge           JUnit 5 tests
```

## Run

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

## Sample Data

The repository includes a small sample SCID file for local testing:

```text
sample/YMM6_CME_Sample.scid
```

## More Documentation

- [Setup](docs/setup.md): PostgreSQL setup, environment variables, build commands, and sample import path
- [Workflows](docs/workflows.md): GUI and CLI workflow details
- [Data Import](docs/data-import.md): SCID mapping, PostgreSQL schema, contract validation, rollover filtering, and progress behavior
- [Architecture](docs/architecture.md): Package responsibilities, study/statistics/backtest layering, and object-oriented design notes
- [Diagrams](docs/diagrams/index.md): Architecture, workflow, data import, research/backtest, OO design, and full class model diagrams
- [Roadmap](docs/roadmap.md): Known gaps and planned design areas
