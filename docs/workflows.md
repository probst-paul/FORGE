# Workflows

FORGE has two user surfaces: the JavaFX GUI for research workflows and the admin CLI for setup, maintenance, and benchmarking.

## JavaFX GUI Workflows

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
│  ├─ Display instrument and contract result cards/tabs
│  └─ Save/load the latest statistics report as a project-local .dat file
└─ Backtest
   ├─ Select imported rollover-clipped contract windows
   ├─ Select strategy and risk settings
   ├─ Build missing derived data when needed and persist it to PostgreSQL
   ├─ Display summary, instrument/contract tables, and simulated trades
   └─ Save/load the latest backtest report as a project-local .dat file
```

The GUI is the primary user-facing surface for market research. Import, event-statistics, and backtest runs execute as background tasks so the window remains responsive. Backtest and event-statistics results persist while the GUI window remains open, and report snapshots can be saved and loaded from project-local `.dat` files.

### GUI Import Data

The GUI import screen lets the user select a SCID file with the system file chooser. After import, optional checkboxes can build derived session ranges and first-hour breach event occurrences in the same workflow.

### GUI Event Statistics

The GUI event-statistics screen lets the user select a study and imported rollover-clipped contract windows. Event statistics are the research base layer: first study how often a market setup occurs, then optionally simulate trades from that setup in a backtest.

`First Hour Breach Frequency` reports long breaches, short breaches, no-breach sessions, and breach rate by instrument and contract. Missing derived data is built and persisted to PostgreSQL before the statistic is displayed.

When multiple contract windows are selected, independent event-statistics jobs can run concurrently. The screen reports aggregate progress for the full run and per-contract progress rows while contract-specific work is active.

Event-statistics reports can be saved to and loaded from `.dat` files in `runtime/reports`. The saved file contains the report snapshot, not database connection settings.

### GUI Backtest

The GUI backtest screen lets the user select rollover-clipped contract windows, a strategy, and risk settings. Backtest results include summary metrics, instrument and contract tables, and simulated trade rows.

When multiple non-overlapping contract windows are selected, the backtest engine can run those windows concurrently. Overlapping same-instrument windows remain grouped to preserve daily risk and strategy state. The screen reports aggregate progress for the full run and temporary per-contract progress rows while each contract is active.

Backtest reports can be saved to and loaded from `.dat` files in `runtime/reports`. The most recent run also remains available in memory while the GUI window is open.

## Admin CLI Workflows

```text
Select Action
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
├─ Configure Database
│  └─ Set PostgreSQL host, port, database, maintenance database, username, and password
├─ Run Benchmark Workflow
│  └─ Run import, derived data, event statistics, and backtest with compact progress/timing output
└─ Wipe Database
   └─ Drop FORGE-owned contract and forge_* tables after two confirmations
```

At the CLI `Select action` prompt, enter `quit` to exit the program. The CLI is reserved for admin and maintenance operations.

### CLI Import Data

The CLI import workflow prepares the PostgreSQL database/table for a selected SCID file and imports contract rows with a single-line status bar.

### CLI Build/Refresh Derived Data

The CLI derived-data workflow runs the reusable database build process without requiring a new SCID file. It uses selected contract windows, derived-data choices such as session ranges and first-hour breach event occurrences, and a rebuild flag to create a `DatabaseBuildRequest`.

FORGE previews the work with a `DatabaseBuildPlan`, runs selected build work with a single-line status bar, and returns a `DatabaseBuildResult`.

### CLI Configure Database

The CLI database configuration workflow sets PostgreSQL host, port, database, maintenance database, username, and password. In this build, database configuration is handled through the admin CLI before the GUI is used.

### CLI Benchmark Workflow

The benchmark workflow takes a SCID file, runs import, derived-data build, event statistics, and backtest through the normal application facades, and displays only progress bars/timers plus a compact summary. It is intended for refactoring benchmarks and can be removed without affecting core workflows.

### CLI Wipe Database

The wipe workflow drops FORGE-owned contract and `forge_*` tables after two confirmation prompts.

## Shared Concept: Contract Selection

Both GUI research workflows and CLI derived-data builds use valid front-month contract windows. For example, `ES - All Available` includes all imported ES contract windows after rollover clipping, while custom contract selection lets the user choose specific contracts such as `ESH25: 2024-12-16 to 2025-03-16` and `ESZ25: 2025-09-15 to 2025-12-14`.

`BacktestRequest` carries selected contract windows so the backtest engine can read each contract table using its own valid rollover-clipped date range.
