# Workflow Sequences

## User Workflows

```mermaid
flowchart TB
    subgraph GUI["JavaFX GUI"]
        G1["Import Data<br/>choose SCID file"]
        G2["Optional post-import<br/>derived data builds"]
        G3["Event Statistics<br/>select study, instruments, contracts"]
        G4["Backtest<br/>select instruments, contracts, strategy, risk"]
        G5["Result Views<br/>summary, details, simulated trades"]
        G6["Settings<br/>confirmed database wipe"]
    end

    subgraph CLI["Admin CLI"]
        C1["Configure Database"]
        C2["Import Data"]
        C3["Build/Refresh Derived Data"]
        C4["Run Benchmark Workflow"]
        C5["Wipe Database"]
    end

    DATA["PostgreSQL<br/>contract tables, metadata, derived rows"]
    ENGINE["Engine Facades<br/>statistics and backtest runs"]

    C1 --> DATA
    C2 --> DATA
    C3 --> DATA
    C4 --> DATA
    C5 --> DATA

    G1 --> DATA
    G2 --> DATA
    G3 --> ENGINE
    G4 --> ENGINE
    G6 --> DATA
    DATA --> ENGINE
    ENGINE --> G5
```

## GUI End-User Workflow

```mermaid
sequenceDiagram
    actor Trader
    participant GUI as JavaFX GUI
    participant App as FacadeForgeApplication
    participant Data as FacadeForgeData
    participant Importer as ScidDataImportService
    participant Builder as DerivedDataBuildService
    participant StatsEngine as EventStatisticsEngine
    participant BacktestEngine as BacktestEngine
    participant Jobs as EngineJobRunner
    participant Repo as PostgreSQL Tick Provider
    participant Strategy as TradingStrategy
    participant Risk as RiskManager
    participant Trade as TradeLifecycleEngine
    participant Report as Reporting Models

    Trader->>GUI: Select Import Data
    Trader->>GUI: Choose SCID file with system file browser
    Trader->>GUI: Choose Fill Missing or Overwrite Overlap import mode
    Trader->>GUI: Select derived data to build after import
    GUI->>App: planDataImport(DataImportRequest)
    App->>Data: planScidImport(path)
    Data->>Importer: Inspect contract name, metadata, file range, and overlap
    Importer-->>Data: DataImportPlan
    Data-->>App: DataImportPlan
    App-->>GUI: DataImportPlan
    GUI->>App: importData(DataImportRequest)
    App->>Data: importScidFile(path, importMode, progressListener)
    opt Overwrite Overlap mode
        Data->>Importer: Delete only stored rows overlapping selected file range
    end
    Data->>Importer: Import rollover-filtered SCID rows
    Importer->>Repo: Persist tick rows and import metadata
    Importer-->>GUI: ImportProgress
    Data-->>App: DataImportResult
    App-->>GUI: DataImportResult

    opt Build selected derived data after import
        GUI->>Data: runDatabaseBuild(DatabaseBuildRequest, progressListener)
        Data->>Builder: Build selected session/event derived data
        Builder->>Repo: Persist derived rows
        Builder-->>GUI: DataBuildProgress
        Data-->>GUI: DatabaseBuildResult
    end

    Trader->>GUI: Select Event Statistics
    Trader->>GUI: Select study, instruments, and rollover-clipped contract windows
    GUI->>App: runEventStatistics(EventStatisticsQueryRequest, progressListener)
    App->>StatsEngine: run(request, progressListener)
    StatsEngine->>Data: Build missing required derived data
    Data->>Builder: Build missing session/event rows
    Builder->>Repo: Persist missing derived rows
    StatsEngine->>Jobs: Run independent contract statistic jobs
    Jobs->>Repo: Read stored derived event data by contract window
    Repo-->>Jobs: Event/statistics rows
    Jobs-->>StatsEngine: Contract-level statistics results
    StatsEngine-->>GUI: Aggregate and per-contract progress
    StatsEngine->>Report: Build event-statistics report
    Report-->>StatsEngine: EventStatisticsReport
    StatsEngine-->>App: EventStatisticsReport
    App-->>GUI: EventStatisticsReport
    GUI-->>Trader: Display summary cards and details table

    Trader->>GUI: Select futures market, instruments, and contract windows
    Trader->>GUI: Select strategy and risk settings
    GUI->>GUI: Validate required selections and numeric risk values
    GUI->>GUI: Create JavaFX background backtest task
    GUI->>App: runBacktest(BacktestRequest, progressListener)
    Note over GUI,App: GUI remains responsive while backtest runs

    App->>BacktestEngine: run(request, progressListener)
    BacktestEngine->>Data: Validate selected rollover-filtered contract windows
    BacktestEngine->>Data: Build missing required derived data
    Data->>Builder: Persist any missing required derived rows
    BacktestEngine->>Jobs: Submit independent contract-window jobs
    Note over BacktestEngine,Jobs: Overlapping same-instrument windows stay grouped for strategy state and daily risk correctness

    par Independent contract window jobs
        Jobs->>Repo: Open batch reader for contract window A
        Repo-->>Jobs: TradeTick batches
        Jobs->>Strategy: evaluate(MarketContext)
        Strategy-->>Jobs: StrategyDecision / TradePlan
        Jobs->>Risk: Check per-trade and daily risk limits
        Risk-->>Jobs: RiskDecision
        Jobs->>Trade: Open, update, or close simulated position
        Trade-->>Jobs: Trade activity and completed TradeResult
        Jobs-->>GUI: Per-contract BacktestProgress
    and Independent contract window jobs
        Jobs->>Repo: Open batch reader for contract window B
        Repo-->>Jobs: TradeTick batches
        Jobs->>Strategy: evaluate(MarketContext)
        Strategy-->>Jobs: StrategyDecision / TradePlan
        Jobs->>Risk: Check per-trade and daily risk limits
        Risk-->>Jobs: RiskDecision
        Jobs->>Trade: Open, update, or close simulated position
        Trade-->>Jobs: Trade activity and completed TradeResult
        Jobs-->>GUI: Per-contract BacktestProgress
    end

    Jobs-->>BacktestEngine: Contract/instrument backtest results
    BacktestEngine-->>GUI: Aggregate BacktestProgress
    BacktestEngine->>Report: Aggregate trades into performance metrics
    Report-->>BacktestEngine: BacktestResult
    BacktestEngine-->>App: BacktestResult
    App-->>GUI: BacktestResult
    GUI-->>Trader: Display summary cards and simulated trades table

    alt Backtest failure
        Jobs-->>BacktestEngine: Job failure
        BacktestEngine-->>App: Error
        App-->>GUI: Error
        GUI-->>Trader: Display failure message without freezing application
    end

    opt Save report
        Trader->>GUI: Save Report
        GUI->>GUI: Serialize BacktestResult to project-local .dat file
    end

    opt Load report
        Trader->>GUI: Load Report
        GUI->>GUI: Deserialize saved BacktestResult .dat file
        GUI-->>Trader: Display loaded summary cards and simulated trades table
    end

    opt Settings database wipe
        Trader->>GUI: Select Settings
        GUI->>GUI: Require explicit confirmation
        GUI->>App: wipeDatabase()
        App->>Data: wipeDatabase()
        Data->>Repo: Drop FORGE-owned contract and forge_* tables
        Repo-->>GUI: Dropped table count
    end
```

## CLI Admin Workflow

```mermaid
sequenceDiagram
    actor Admin
    participant CLI as Admin CLI
    participant App as FacadeForgeApplication
    participant Data as FacadeForgeData
    participant Importer as ScidDataImportService
    participant Builder as DerivedDataBuildService
    participant Benchmark as FacadeForgeBenchmark
    participant Repo as PostgreSQL Repository

    Admin->>CLI: Start CLI
    CLI-->>Admin: Show Import / Derived Data / Configure DB / Benchmark / Wipe

    alt Configure Database
        Admin->>CLI: Enter database host, port, database, user, password
        CLI->>App: configureDatabase(DatabaseConnectionRequest)
        App->>Data: configurePostgresDatabase(settings)
        Data->>Repo: Create/reuse database and schema support tables
        Repo-->>Data: Database ready
        Data-->>App: Configured
        App-->>CLI: Accepted database settings
        CLI-->>Admin: Show configuration result
    else Import Data
        Admin->>CLI: Enter SCID file path
        CLI->>App: planDataImport(DataImportRequest)
        App->>Data: planScidImport(path)
        Data->>Importer: Inspect SCID file and contract metadata
        Importer->>Repo: Check existing contract table/import metadata
        Repo-->>Importer: Import plan
        Importer-->>Data: DataImportPlan
        Data-->>App: DataImportPlan
        App-->>CLI: DataImportPlan
        CLI->>CLI: Confirm overlap overwrite or keep existing rows
        CLI->>App: importData(DataImportRequest)
        App->>Data: importScidFile(path, importMode, progressListener)
        Data->>Importer: Import rollover-filtered rows
        Importer->>Repo: COPY batches and advance checkpoint
        Importer-->>CLI: ImportProgress
        Data-->>App: DataImportResult
        App-->>CLI: DataImportResult
        CLI-->>Admin: Show import summary
    else Build/Refresh Derived Data
        Admin->>CLI: Select contract windows and derived data options
        CLI->>Data: planDatabaseBuild(DatabaseBuildRequest)
        Data->>Builder: Build plan
        Builder-->>Data: DatabaseBuildPlan
        Data-->>CLI: DatabaseBuildPlan
        CLI->>Data: runDatabaseBuild(request, progressListener)
        Data->>Builder: Build selected derived rows
        Builder->>Repo: Persist derived session/event data
        Builder-->>CLI: DataBuildProgress
        Data-->>CLI: DatabaseBuildResult
        CLI-->>Admin: Show derived-data result
    else Run Benchmark Workflow
        Admin->>CLI: Enter SCID file path and import/build choices
        CLI->>Benchmark: runBenchmark(BenchmarkRunRequest)
        Benchmark->>App: importData(...)
        Benchmark->>Data: runDatabaseBuild(...)
        Benchmark->>App: runEventStatistics(...)
        Benchmark->>App: runBacktest(...)
        Benchmark-->>CLI: BenchmarkRunResult with timings
        CLI-->>Admin: Show compact benchmark summary
    else Wipe Database
        Admin->>CLI: Confirm y/n
        Admin->>CLI: Type WIPE
        CLI->>App: wipeDatabase()
        App->>Data: wipeDatabase()
        Data->>Repo: Drop FORGE-owned contract and forge_* tables
        Repo-->>Data: Dropped table count
        Data-->>App: Dropped table count
        App-->>CLI: Dropped table count
        CLI-->>Admin: Show wipe result
    end
```
