# Workflows

FORGE uses the JavaFX GUI as its user-facing surface for research, setup, maintenance, and benchmarking workflows.

## JavaFX GUI Workflows

```text
FORGE JavaFX GUI
├─ Import Data
│  ├─ Select a SCID file with the system file chooser
│  ├─ Choose Fill Missing Trades or Overwrite Overlapping Stored Data
│  ├─ Optionally build session ranges after import
│  ├─ Optionally build first-hour breach events after import
│  └─ Import with progress and summary output
├─ Event Statistics
│  ├─ Select an event-statistics study
│  ├─ Select instruments, then imported rollover-clipped contract windows
│  ├─ Build missing derived data when needed and persist it to PostgreSQL
│  ├─ Display summary cards and event detail rows with supporting session data
│  └─ Save/load the latest statistics report as a project-local .dat file
├─ Backtest
│  ├─ Select instruments, then imported rollover-clipped contract windows
│  ├─ Select strategy and risk settings
│  ├─ Build missing derived data when needed and persist it to PostgreSQL
│  ├─ Display summary, instrument/contract tables, and simulated trades
│  └─ Save/load the latest backtest report as a project-local .dat file
└─ Settings
   ├─ Repair/create the configured database and support tables
   ├─ Open a benchmark window for SCID-based workflow timing
   └─ Drop FORGE-owned database tables after explicit confirmation
```

The GUI is the primary user-facing surface for market research. On startup, the GUI automatically prepares the configured PostgreSQL database in the background. Import, event-statistics, backtest, and benchmark runs execute as background tasks so the window remains responsive. Backtest and event-statistics results persist while the GUI window remains open, and report snapshots can be saved and loaded from project-local `.dat` files.

### GUI Import Data

The GUI import screen lets the user select a SCID file with the system file chooser. FORGE normalizes supported file naming conventions such as `ESU25_FUT_CME.scid` and `ESU5.CME.scid` to the same contract identity, then plans the import against the matching contract table.

If the target contract table already contains data, the user can choose `Fill missing trades` or `Overwrite overlapping stored data`. Fill-missing mode keeps existing rows and inserts only rows not already present. Overwrite-overlap mode deletes only stored rows inside the selected file's importable timestamp range, then imports the selected file. After import, optional checkboxes can build derived session ranges and first-hour breach event occurrences in the same workflow.

### GUI Event Statistics

The GUI event-statistics screen lets the user select a study, then select instruments and imported rollover-clipped contract windows beneath those instruments. Event statistics are the research base layer: first study how often a market setup occurs, then optionally simulate trades from that setup in a backtest.

`First Hour Breach Frequency` reports high-side breaches, low-side breaches, no-breach sessions, and breach rate by instrument and contract. Missing derived data is built and persisted to PostgreSQL before the statistic is displayed. The Details tab shows each event occurrence with event date/time, side, range size, volume, and other supporting session data joined from the derived-data tables.

When multiple contract windows are selected, independent event-statistics jobs can run concurrently. The screen reports aggregate progress for the full run and per-contract progress rows while contract-specific work is active.

Event-statistics reports can be saved to and loaded from `.dat` files in `runtime/reports`. The saved file contains the report snapshot, not database connection settings.

### GUI Backtest

The GUI backtest screen lets the user select instruments, rollover-clipped contract windows, a strategy, and risk settings. Backtest results include summary metrics, instrument and contract tables, and simulated trade rows.

When multiple non-overlapping contract windows are selected, the backtest engine can run those windows concurrently. Overlapping same-instrument windows remain grouped to preserve daily risk and strategy state. The screen reports aggregate progress for the full run and temporary per-contract progress rows while each contract is active.

Backtest reports can be saved to and loaded from `.dat` files in `runtime/reports`. The most recent run also remains available in memory while the GUI window is open.

### GUI Settings

The GUI settings screen contains maintenance actions that are useful while developing and testing locally. `Repair/Create Database` reruns the non-destructive startup preparation workflow, creating the configured database if missing and ensuring FORGE support tables are available. `Benchmark` opens a separate window where a SCID file can be selected and run through import, derived-data build, event statistics, and backtest timing. The database wipe action drops FORGE-owned contract tables and `forge_*` tables after explicit confirmation.

## Shared Concept: Contract Selection

GUI research workflows use valid front-month contract windows. The GUI shows instruments first, then contract windows beneath each instrument. For example, selecting `ES` exposes imported ES contract windows after rollover clipping, while individual contract rows use compact labels such as `H25: 2024-12-16 to 2025-03-16` and `Z25: 2025-09-15 to 2025-12-14`.

`BacktestRequest` carries selected contract windows so the backtest engine can read each contract table using its own valid rollover-clipped date range.
