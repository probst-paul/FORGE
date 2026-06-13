# Data Import

## Supported Contracts

Supported futures roots for import are `ES`, `NQ`, `YM`, `RTY`, and `CL`. Importing an unsupported root fails before the database table is created or modified.

FORGE validates contract month codes before database work begins: equity index imports allow only quarterly contracts (`H`, `M`, `U`, and `Z`), while `CL` allows the standard monthly futures cycle. If a file name points to a contract month that should not exist, the import stops and reports that the SCID file may be corrupted or incorrectly named.

FORGE derives the instrument and contract from the SCID file name and normalizes supported naming conventions to the same contract table. For example, `ESU25_FUT_CME.scid` and `ESU5.CME.scid` both resolve to instrument `ES` and contract `U25`, stored in the `ESU25` contract table.

## Import Output

FORGE derives the contract from the file name and prepares a matching table:

```text
Importing ESU25 [########################] 100% 123456/123456
Data storage prepared:
Database: forge
Table: ESU25
Contract: ESU25
Rows imported: <number of SCID records imported>
Duplicate rows skipped: <rows already present when filling missing data>
Overlapping rows removed: <rows deleted by overwrite-overlap mode>
Import time: <elapsed time>
Null-side rows imported: <records with no identifiable aggressor side>
Rows skipped outside front-month window: <records outside the active contract window>
```

## Contract Table Schema

The contract table uses this trade-level schema:

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

FORGE converts SCID float prices to integer tick counts during import using the hardcoded instrument tick size. The database stores those tick counts as `BIGINT` rather than storing floating-point prices, so strategy calculations can work in exact tick space and convert back to display prices only at the UI/reporting edge.

Sierra Chart's count and volume fields are unsigned 4-byte integers, so FORGE stores imported count/volume values as `BIGINT` to preserve their full range in PostgreSQL. `side` is FORGE-specific rather than a Sierra Chart field, with `1` for buy aggressor, `-1` for sell aggressor, and `NULL` when the aggressor side cannot be identified. Backtest data reads should filter to strategy-usable trades with `side IS NOT NULL`.

## Import Metadata

Each contract table is treated as the authoritative dataset for that standardized contract. If a contract table already exists, FORGE plans the import by comparing the selected file's importable timestamp range to the existing table coverage.

The import mode controls how existing rows are handled:

- `Fill missing trades` keeps existing rows and inserts only rows that are not already present.
- `Overwrite overlapping stored data` deletes only stored rows inside the selected file's importable timestamp range, then imports the selected file.

FORGE stores the source file name and SCID record index on each row, creates a unique trade-content index inside the contract table, and inserts with `ON CONFLICT DO NOTHING`. It also maintains a `forge_contract_imports` table with the source file metadata, next record index to process, row count, and first/last imported trade timestamps. The checkpoint advances only after a batch insert succeeds.

The `Select Instrument(s)` screen is driven by the `forge_contract_imports` metadata table. That keeps the instrument list responsive even when imported contracts contain tens of millions of rows.

## Rollover Filtering

The import flow skips records outside the contract's active front-month window before storing rows. For CME equity index roots (`ES`, `NQ`, `YM`, and `RTY`), FORGE clips each contract table to its active window using the common convention of rolling on the Monday before the third Friday of the contract month.

For `CL`, FORGE estimates expiration as three business days before the 25th calendar day of the month before delivery, then rolls on the Friday before that expiration date. The backtest instrument list is derived from imported contract tables after that same active-window logic.

## Progress Reporting

The CLI renders import progress as a single updating terminal line, while the GUI renders the same progress state with JavaFX progress bars. The underlying progress calculation is exposed through `ImportProgress`.

Backtest and event-statistics runs use the same presentation idea. Before replay begins, FORGE counts strategy-usable ticks for the selected contract windows, then reports progress while batches are processed:

```text
Running backtest [############------------] 50% 500000/1000000
```

The reusable backtest progress state is exposed through `BacktestProgress` and `BacktestProgressListener`; event statistics use the equivalent event-statistics progress objects. Each UI layer owns only its specific rendering.

For multi-contract research runs, the GUI displays one aggregate progress bar and, while work is active, per-contract progress rows underneath it. Independent contract windows can be processed concurrently by the engine, so per-contract rows may advance at the same time. When aggregate progress reaches 100%, the contract rows collapse back to the single aggregate view.

The GUI footer also shows background task state while import, event-statistics, or backtest work is running. The footer hides when no background workflow is active.

## Import Performance Notes

SCID imports use PostgreSQL text `COPY` in batches of `100,000` records. Possible performance enhancements:

- Drop/recreate nonessential indexes during confirmed full rebuilds, then recreate them after import.
- Stream parsed SCID rows directly into `COPY` instead of building batch strings in memory.
- Use PostgreSQL binary `COPY` if text `COPY` becomes a bottleneck.
- Add database tuning notes for large imports, such as `maintenance_work_mem`, `synchronous_commit`, and local disk considerations.
- Add import timing/history records so performance changes can be compared across runs.
