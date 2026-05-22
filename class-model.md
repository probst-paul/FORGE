# FORGE Class Model

## Facade Collaboration Overview

```mermaid
classDiagram
    direction LR

    class Main
    class FacadeForgeCli
    class ForgeCliAccess
    class CliApplicationController
    class FacadeForgeApplication
    class FacadeForgeConfig
    class FacadeForgeData
    class InstrumentDataCatalog
    class ContractNameResolver
    class ScidDataImportService
    class DerivedDataBuildService
    class PostgresTradeRepository
    class ContractRolloverCalendar
    class FacadeForgeStrategy
    class FacadeForgeTrigger
    class FacadeForgeEngine
    class FacadeForgeFeature
    class FacadeForgeEvent
    class FacadeForgeQuery
    class FacadeForgeTrade
    class FacadeForgeReporting
    class FacadeForgeExecution
    class FacadeForgeAnalytics
    class FacadeForgeBacktest
    class ForgeApplicationAccess
    class InstrumentSelectionService
    class StrategySelectionService
    class TriggerSelectionService
    class TargetSettingsSelectionService
    class RiskSettingsSelectionService

    Main --> FacadeForgeCli
    FacadeForgeCli --> ForgeCliAccess
    ForgeCliAccess --> CliApplicationController
    CliApplicationController --> FacadeForgeApplication : request objects
    FacadeForgeApplication --> ForgeApplicationAccess
    CliApplicationController --> InstrumentSelectionService : instruments + dates
    CliApplicationController --> StrategySelectionService : strategy
    CliApplicationController --> RiskSettingsSelectionService : risk settings
    CliApplicationController --> TriggerSelectionService : trigger
    CliApplicationController --> TargetSettingsSelectionService : target settings
    CliApplicationController --> FacadeForgeConfig : build request
    CliApplicationController --> FacadeForgeData : derived data build

    InstrumentSelectionService --> FacadeForgeData
    FacadeForgeData --> InstrumentDataCatalog : catalog access
    FacadeForgeData --> ScidDataImportService : import access
    FacadeForgeData --> DerivedDataBuildService : build access
    InstrumentDataCatalog --> PostgresTradeRepository : imported tables
    InstrumentDataCatalog --> ContractRolloverCalendar : active windows
    ScidDataImportService --> ContractNameResolver : contract root
    ScidDataImportService --> PostgresTradeRepository : persist rows
    StrategySelectionService --> FacadeForgeStrategy
    TriggerSelectionService --> FacadeForgeTrigger

    ForgeApplicationAccess ..> FacadeForgeEngine : run request
    FacadeForgeEngine ..> FacadeForgeFeature : build features
    FacadeForgeEngine ..> FacadeForgeEvent : build events
    ForgeApplicationAccess ..> FacadeForgeQuery : later run statistics
    ForgeApplicationAccess ..> FacadeForgeTrade : lifecycle support
    ForgeApplicationAccess ..> FacadeForgeExecution : later execute orders
    ForgeApplicationAccess ..> FacadeForgeReporting : later summarize result
    ForgeApplicationAccess ..> FacadeForgeAnalytics : later derive features
    ForgeApplicationAccess ..> FacadeForgeBacktest : result models
    FacadeForgeFeature ..> FacadeForgeEvent : features feed events
    FacadeForgeEvent ..> FacadeForgeQuery : events feed statistics
```

## CLI Interaction Overview

```mermaid
sequenceDiagram
    participant Main
    participant CliFacade as FacadeForgeCli
    participant Cli as CliApplicationController
    participant App as FacadeForgeApplication
    participant Input as UserInput
    participant Output as UserOutput
    participant Instruments as InstrumentSelectionService
    participant Data as FacadeForgeData
    participant Strategies as StrategySelectionService
    participant Strategy as FacadeForgeStrategy
    participant Config as FacadeForgeConfig

    Main->>CliFacade: forgeCliAccess().run()
    CliFacade->>Cli: run(input, output)
    Cli->>Output: print title
    Cli->>Output: print Run Backtest / Event Statistics / Import Data / Build Derived Data / Configure Database choices
    Cli->>Input: readInt(action)

    alt Run Backtest
        Cli->>Instruments: selectContracts(input, output)
        Instruments->>Data: forgeDataAccess().getAvailableInstruments()
        Instruments->>Data: forgeDataAccess().getAvailableContracts()
        Instruments->>Output: print All Available and custom contract choices
        Instruments->>Input: read selection
        Instruments-->>Cli: selected contract windows

        Cli->>Strategies: selectStrategy(input, output)
        Strategies->>Strategy: forgeStrategyAccess().findAvailableStrategies()
        Strategies->>Strategy: forgeStrategyAccess().getDisplayName(strategy)
        Strategies->>Input: readInt(selection)
        Strategies-->>Cli: selected strategy class
        Cli->>Strategies: getConfigurationProfile(selected strategy)
        Strategies->>Strategy: forgeStrategyAccess().getConfigurationProfile(strategy)
        Strategy-->>Strategies: StrategyConfigurationProfile
        Strategies-->>Cli: strategy trigger/target profile
        Cli->>Input: read risk, trigger, and target settings
        Cli->>Config: forgeConfigAccess().createBacktestRequest(...)
        Config-->>Cli: BacktestRequest
        Cli->>App: forgeApplicationAccess().runBacktest(request, progress listener)
        App-->>Cli: BacktestResult
        Cli->>Output: print backtest progress/result
    else Build/Refresh Derived Data
        Cli->>Instruments: selectContracts(input, output)
        Instruments->>Data: forgeDataAccess().getAvailableInstruments()
        Instruments->>Data: forgeDataAccess().getAvailableContracts()
        Instruments-->>Cli: selected contract windows
        Cli->>Input: readInt(derived data option)
        Cli->>Input: readString(rebuild existing)
        Cli->>Data: forgeDataAccess().planDatabaseBuild(request)
        Data-->>Cli: DatabaseBuildPlan
        Cli->>Data: forgeDataAccess().runDatabaseBuild(request, listener)
        Data-->>Cli: DatabaseBuildResult
        Cli->>Output: print build progress/result
    else Run Event Statistics
        Cli->>Instruments: selectContracts(input, output)
        Instruments-->>Cli: selected contract windows
        Cli->>Input: readInt(event statistic)
        Cli->>App: forgeApplicationAccess().runEventStatistics(request)
        App-->>Cli: EventStatisticsReport
        Cli->>Output: print statistic progress/result
    else Import Data
        Cli->>Input: readString(SCID data file path)
        Cli->>App: forgeApplicationAccess().planDataImport(request)
        App->>Data: forgeDataAccess().planScidImport(path)
        Data-->>App: DataImportPlan
        App-->>Cli: DataImportPlan
        opt Existing contract table
            Cli->>Input: confirm wipe/rebuild
        end
        Cli->>App: forgeApplicationAccess().importData(request)
        App->>Data: forgeDataAccess().importScidFile(path, rebuild, listener)
        Data-->>App: DataImportResult
        App-->>Cli: DataImportResult
        Cli->>Output: print import progress/result
    else Configure Database
        Cli->>Input: read database settings
        Cli->>App: forgeApplicationAccess().configureDatabase(...)
        App->>Data: forgeDataAccess().configurePostgresDatabase(settings)
        Data-->>App: configured
        App-->>Cli: configured
    end
```

## app Package

```mermaid
classDiagram
    direction LR

    class Main {
        +main(String[] args)
    }

    class FacadeForgeApplication {
        +FacadeForgeApplication getTheInstance()
        +ForgeApplicationAccess forgeApplicationAccess()
    }

    class ForgeApplicationAccess {
        +BacktestResult runBacktest(BacktestRequest request)
        +BacktestResult runBacktest(BacktestRequest request, BacktestProgressListener listener)
        +DataImportPlan planDataImport(DataImportRequest request)
        +DataImportResult importData(DataImportRequest request)
        +void configureDatabase(DatabaseConfigurationRequest request)
    }

    class DataImportRequest {
        -String scidFilePath
        -boolean rebuildExistingContract
        -ImportProgressListener progressListener
        +String getScidFilePath()
    }

    class DatabaseConfigurationRequest {
        -String host
        -int port
        -String databaseName
        -String maintenanceDatabaseName
        -String username
        -String password
    }

    class ImportProgress {
        -String contractSymbol
        -long processedRecords
        -long totalRecords
        +int getCompletionPercent()
    }

    class ImportProgressListener {
        <<interface>>
        +void onProgress(ImportProgress progress)
    }

    class BacktestProgress {
        -long processedTicks
        -long totalTicks
        +int getCompletionPercent()
    }

    class BacktestProgressListener {
        <<interface>>
        +void onProgress(BacktestProgress progress)
    }

    class UserInput {
        <<interface>>
        +String readString(String label)
        +int readInt(String label)
        +double readDouble(String label)
        +LocalDate readDateOrDefault(String label, LocalDate defaultDate)
    }

    class ConsoleUserInput {
        -Scanner scanner
        +String readString(String label)
    }

    class UserOutput {
        <<interface>>
        +void printLine(String text)
        +void printBlankLine()
        +void printStatusLine(String text)
        +void finishStatusLine()
    }

    class ConsoleUserOutput {
        +void printLine(String text)
    }

    Main --> ConsoleUserInput
    Main --> ConsoleUserOutput
    Main --> FacadeForgeCli
    ConsoleUserInput ..|> UserInput
    ConsoleUserOutput ..|> UserOutput
    FacadeForgeApplication --> ForgeApplicationAccess
    ForgeApplicationAccess --> BacktestRequest
    ForgeApplicationAccess --> BacktestProgressListener
    ForgeApplicationAccess --> DataImportRequest
    ForgeApplicationAccess --> DatabaseConfigurationRequest
    DataImportRequest --> ImportProgressListener
    ImportProgressListener --> ImportProgress
    BacktestProgressListener --> BacktestProgress
```

## cli Package

```mermaid
classDiagram
    direction LR

    class FacadeForgeCli {
        +FacadeForgeCli getTheInstance()
        +ForgeCliAccess forgeCliAccess()
    }

    class ForgeCliAccess {
        +void run()
        +void run(UserInput input, UserOutput output)
    }

    class CliApplicationController {
        +void run()
        +void run(UserInput input, UserOutput output)
    }

    class InstrumentSelectionService {
        +SelectedBacktestContracts selectContracts(UserInput input, UserOutput output)
        +List~String~ selectInstruments(UserInput input, UserOutput output)
    }

    class SelectedBacktestContracts {
        +List~String~ getContractSymbols()
        +LocalDate getStartDate()
        +LocalDate getEndDate()
    }

    class StrategySelectionService {
        +Class selectStrategy(UserInput input, UserOutput output)
        +String getDisplayName(Class strategy)
    }

    class RiskSettingsSelectionService {
        +RiskSettings readRiskSettings(UserInput input)
    }

    class TriggerSelectionService {
        +Class selectTrigger(UserInput input, UserOutput output)
        +TradeTriggerOptions readTriggerOptions(UserInput input, UserOutput output, Class trigger)
        +String getDisplayName(Class trigger)
    }

    class TargetSettingsSelectionService {
        +String selectTargetMode(UserInput input, UserOutput output)
        +TargetSettings readTargetSettings(UserInput input, String targetMode)
        +String getDisplayName(String targetMode)
    }

    FacadeForgeCli --> ForgeCliAccess
    ForgeCliAccess --> CliApplicationController
    CliApplicationController --> FacadeForgeApplication
    CliApplicationController --> FacadeForgeConfig
    CliApplicationController --> InstrumentSelectionService
    CliApplicationController --> StrategySelectionService
    CliApplicationController --> RiskSettingsSelectionService
    CliApplicationController --> TriggerSelectionService
    CliApplicationController --> TargetSettingsSelectionService
```

## config Package

```mermaid
classDiagram
    direction LR

    class FacadeForgeConfig {
        +FacadeForgeConfig getTheInstance()
        +ForgeConfigAccess forgeConfigAccess()
    }

    class ForgeConfigAccess {
        +BacktestRequest createBacktestRequest(...)
        +OrderSettings defaultOrderSettings()
    }

    class BacktestRequest {
        -StrategyOptions strategyOptions
        -List~String~ instruments
        -List~ContractTradeWindow~ contractWindows
        -LocalDate startDate
        -LocalDate endDate
        -TradeTriggerOptions tradeTriggerOptions
        -RiskSettings riskSettings
        -TargetSettings targetSettings
        -OrderSettings orderSettings
    }

    class StrategyOptions {
        -String strategyName
        -Map~String,String~ parameters
    }

    class TradeTriggerOptions {
        -String triggerName
        -Map~String,String~ parameters
    }

    class RiskSettings {
        -double riskPerTrade
        -double maxDailyLoss
    }

    class TargetSettings {
        -String targetMode
        -Double rewardRiskRatio
        -Integer profitTargetTicks
    }

    class OrderSettings {
        -OrderType entryOrderType
        -int quantity
        -double limitOffsetTicks
        -double stopOffsetTicks
    }

    FacadeForgeConfig --> ForgeConfigAccess
    ForgeConfigAccess --> BacktestRequest : creates
    ForgeConfigAccess --> StrategyOptions : creates
    ForgeConfigAccess --> TradeTriggerOptions : creates
    ForgeConfigAccess --> OrderSettings : creates default
    BacktestRequest --> StrategyOptions
    BacktestRequest --> TradeTriggerOptions
    BacktestRequest --> RiskSettings
    BacktestRequest --> TargetSettings
    BacktestRequest --> OrderSettings
    BacktestRequest --> ContractTradeWindow
```

## data Package Facade

```mermaid
classDiagram
    direction LR

    class FacadeForgeData {
        +FacadeForgeData getTheInstance()
        +ForgeDataAccess forgeDataAccess()
    }

    class ForgeDataAccess {
        +List~AvailableInstrumentData~ getAvailableInstruments()
        +List~AvailableContractData~ getAvailableContracts()
        +AvailableDateRange getSharedDateRange(List~String~ symbols)
        +void validateDateRange(List~String~ symbols, LocalDate startDate, LocalDate endDate)
        +DataImportPlan planScidImport(String scidFilePath)
        +DataImportResult importScidFile(String scidFilePath, boolean rebuildExistingContract, ImportProgressListener listener)
        +TradeBatchReader openTradeBatchReader(List~ContractTradeWindow~ windows, int batchSize)
        +long countTradeTicks(List~ContractTradeWindow~ windows)
        +DatabaseBuildPlan planDatabaseBuild(DatabaseBuildRequest request)
        +DatabaseBuildResult runDatabaseBuild(DatabaseBuildRequest request, DataBuildProgressListener listener)
        +void configurePostgresDatabase(PostgresDatabaseSettings settings)
    }

    class InstrumentDataCatalog
    class ScidDataImportService
    class DerivedDataBuildService
    class PostgresTradeRepository
    class PostgresTickDataProvider
    class PostgresDatabaseSettings
    class TradeBatchReader
    class DataImportPlan
    class DataImportResult

    FacadeForgeData --> ForgeDataAccess
    ForgeDataAccess --> InstrumentDataCatalog : catalog
    ForgeDataAccess --> ScidDataImportService : import
    ForgeDataAccess --> DerivedDataBuildService : derived build
    ForgeDataAccess --> PostgresTickDataProvider : tick batches/counts
    ForgeDataAccess --> PostgresDatabaseSettings : configure
    ScidDataImportService --> PostgresTradeRepository
    InstrumentDataCatalog --> PostgresTradeRepository
    PostgresTickDataProvider --> TradeBatchReader : creates
    ForgeDataAccess --> DataImportPlan
    ForgeDataAccess --> DataImportResult
```

## data.build Package

```mermaid
classDiagram
    direction LR

    class DatabaseBuildRequest {
        -List~ContractTradeWindow~ contractWindows
        -Set~DerivedDataBuildOption~ options
        -boolean rebuildExisting
        -int batchSize
    }

    class DatabaseBuildPlan {
        -long totalTicks
        -boolean sessionRangesAlreadyBuilt
        -boolean firstHourBreachEventsAlreadyBuilt
        -boolean willBuildSessionRanges
        -boolean willBuildFirstHourBreachEvents
        +boolean hasWorkToRun()
    }

    class DatabaseBuildResult {
        -long ticksRead
        -long sessionRangesBuilt
        -long marketEventsBuilt
        -Duration elapsedTime
    }

    class DerivedDataBuildService {
        +DatabaseBuildPlan planBuild(DatabaseBuildRequest request)
        +DatabaseBuildResult runBuild(DatabaseBuildRequest request, DataBuildProgressListener listener)
    }

    class DerivedDataBuildOption {
        <<enumeration>>
        SESSION_RANGES
        FIRST_HOUR_BREACH_EVENTS
    }

    class DerivedDataBuildTradeSource {
        <<interface>>
        +TradeBatchReader openTradeBatchReader(List~ContractTradeWindow~ windows, int batchSize)
        +long countTradeTicks(List~ContractTradeWindow~ windows)
    }

    class DerivedDataBuildStore {
        <<interface>>
        +boolean areSessionRangesBuilt(List~ContractTradeWindow~ windows)
        +void saveSessionRanges(Collection~SessionRangeFeature~ features)
        +void clearSessionRanges(List~ContractTradeWindow~ windows)
        +boolean areMarketEventsBuilt(List~ContractTradeWindow~ windows, String eventName)
        +void saveMarketEvents(Collection~MarketEvent~ events)
        +void clearMarketEvents(List~ContractTradeWindow~ windows, String eventName)
    }

    class DataBuildProgress
    class DataBuildProgressListener

    DerivedDataBuildService --> DatabaseBuildRequest
    DerivedDataBuildService --> DatabaseBuildPlan
    DerivedDataBuildService --> DatabaseBuildResult
    DerivedDataBuildService --> DerivedDataBuildTradeSource
    DerivedDataBuildService --> DerivedDataBuildStore
    DerivedDataBuildService --> FeatureBuildService
    DerivedDataBuildService --> EventBuildService
    DatabaseBuildRequest --> DerivedDataBuildOption
    DatabaseBuildPlan --> DerivedDataBuildOption
    DatabaseBuildResult --> DatabaseBuildPlan
    DerivedDataBuildService --> DataBuildProgressListener : reports
    DataBuildProgressListener --> DataBuildProgress
```

## data.catalog and model Packages

```mermaid
classDiagram
    direction LR

    class InstrumentDataCatalog {
        +List~AvailableInstrumentData~ getAvailableInstruments()
        +List~AvailableContractData~ getAvailableContracts()
        +AvailableDateRange getSharedDateRange(List~String~ symbols)
        +void validateDateRange(List~String~ symbols, LocalDate startDate, LocalDate endDate)
    }

    class ContractDataSummary {
        -String contractSymbol
        -LocalDate startDate
        -LocalDate endDate
    }

    class AvailableInstrumentData {
        -Instrument instrument
        -LocalDate startDate
        -LocalDate endDate
        +String getSymbol()
        +double getFuturesTickSize()
        +double getFuturesTickDollarAmount()
    }

    class AvailableContractData {
        -String contractSymbol
        -String instrumentSymbol
        -LocalDate startDate
        -LocalDate endDate
    }

    class AvailableDateRange {
        -LocalDate startDate
        -LocalDate endDate
    }

    class ContractNameResolver
    class ContractRolloverCalendar
    class PostgresTradeRepository

    class Instrument {
        <<abstract>>
        -String symbolCode
        -String displayName
        +String getSymbolCode()
        +String getDisplayName()
        +String getInstrumentType()*
    }

    class FuturesContract {
        -double tickSize
        -double tickDollarAmount
        -LocalDate expirationDate
        +double calculateDollarValueForTicks(double ticks)
    }

    class FuturesInstrument {
        -double tickSize
        -double tickDollarAmount
    }

    class FuturesInstrumentSpec {
        -String symbolCode
        -String displayName
        -double tickSize
        -double tickDollarAmount
    }

    class FuturesInstrumentSpecProvider {
        <<interface>>
        +FuturesInstrumentSpec getBySymbol(String symbol)
        +boolean supports(String symbol)
    }

    class StaticFuturesInstrumentSpecProvider

    InstrumentDataCatalog --> PostgresTradeRepository : listImportedContractData()
    InstrumentDataCatalog --> ContractNameResolver : root symbol
    InstrumentDataCatalog --> ContractRolloverCalendar : clip active windows
    InstrumentDataCatalog --> AvailableInstrumentData : creates
    InstrumentDataCatalog --> AvailableContractData : creates
    InstrumentDataCatalog --> AvailableDateRange : creates
    InstrumentDataCatalog --> ContractDataSummary : groups
    InstrumentDataCatalog --> FuturesInstrumentSpecProvider
    FuturesInstrumentSpecProvider <|.. StaticFuturesInstrumentSpecProvider
    StaticFuturesInstrumentSpecProvider --> FuturesInstrumentSpec
    AvailableInstrumentData --> Instrument
    Instrument <|-- FuturesInstrument
    Instrument <|-- FuturesContract
```

## data.contract Package

```mermaid
classDiagram
    direction LR

    class ContractNameResolver {
        +String resolveFromScidPath(String scidFilePath)
        +String resolveInstrumentSymbol(String contractSymbol)
        +String resolveContractMonthCode(String contractSymbol)
        +String resolveContractYear(String contractSymbol)
        +FuturesContractCode resolveContractCode(String contractSymbol)
    }

    class FuturesContractCode {
        -String instrumentSymbol
        -String monthCode
        -int year
        +Month getMonth()
        +String toContractSymbol()
    }

    ContractNameResolver --> FuturesContractCode : creates
```

## data.rollover Package

```mermaid
classDiagram
    direction LR

    class ContractRolloverCalendar {
        +Optional~ContractRolloverWindow~ findActiveWindow(String contractSymbol)
    }

    class RolloverRule {
        <<interface>>
        +Optional~ContractRolloverWindow~ resolveActiveWindow(FuturesContractCode contractCode)
    }

    class EquityIndexRolloverRule
    class CrudeOilRolloverRule

    class ContractRolloverWindow {
        -String contractSymbol
        -LocalDate activeStartDate
        -LocalDate activeEndDate
    }

    class ContractNameResolver
    class FuturesContractCode

    ContractRolloverCalendar --> ContractNameResolver
    ContractRolloverCalendar --> RolloverRule
    RolloverRule <|.. EquityIndexRolloverRule
    RolloverRule <|.. CrudeOilRolloverRule
    RolloverRule --> FuturesContractCode
    RolloverRule --> ContractRolloverWindow : creates
```

## data.importing and data.postgres Packages

```mermaid
classDiagram
    direction LR

    class ScidDataImportService {
        +DataImportPlan planImport(String scidFilePath)
        +DataImportResult importScidFile(String scidFilePath, boolean rebuildExistingContract, ImportProgressListener listener)
    }

    class ScidTradeReader {
        +List~TradeRow~ readTrades(Path scidFilePath, double tickSize)
        +void readTrades(Path scidFilePath, long startRecordIndex, int batchSize, double tickSize, Consumer consumer)
    }

    class TradeRow {
        -Instant tradeDateTime
        -long priceTicks
        -Long bidPriceTicks
        -Long askPriceTicks
        -long quantity
        -Integer side
        -long numTrades
        -long scidRecordIndex
    }

    class DataImportPlan {
        -String contractSymbol
        -String tableName
        -boolean existingContractTable
        -long existingRows
    }

    class DataImportResult {
        -String databaseName
        -String tableName
        -String contractSymbol
        -int importedRows
        -long nullSideRowsImported
        -long skippedOutsideFrontMonthRows
        -Duration elapsedTime
    }

    class ImportCheckpoint {
        -String tableName
        -String sourceFileName
        -long nextRecordIndex
    }

    class PostgresTradeRepository {
        +void ensureDatabaseExists()
        +void ensureContractTradesTableExists(String tableName)
        +DataImportPlan planImport(String contractSymbol, String tableName)
        +List~ContractDataSummary~ listImportedContractData()
        +ImportCheckpoint prepareImportCheckpoint(...)
        +int insertTradesAndAdvanceCheckpoint(...)
    }

    class PostgresTickDataProvider {
        +TradeBatchReader openReader(List~ContractTradeWindow~ windows, int batchSize)
        +long countTicks(List~ContractTradeWindow~ windows)
    }

    class PostgresDatabaseSettings {
        +PostgresDatabaseSettings fromEnvironment()
        +String primaryJdbcUrl()
        +String maintenanceJdbcUrl()
    }

    class ContractNameResolver
    class ContractRolloverCalendar
    class ContractRolloverWindow
    class FuturesInstrumentSpecProvider
    class ContractDataSummary

    ScidDataImportService --> ContractNameResolver : derive contract
    ScidDataImportService --> ContractRolloverCalendar : active window
    ScidDataImportService --> ContractRolloverWindow : filter rows
    ScidDataImportService --> FuturesInstrumentSpecProvider : tick size
    ScidDataImportService --> ScidTradeReader : reads batches
    ScidDataImportService --> PostgresTradeRepository : persists
    ScidTradeReader --> TradeRow : creates
    PostgresTradeRepository --> PostgresDatabaseSettings
    PostgresTradeRepository --> DataImportPlan : creates
    PostgresTradeRepository --> ImportCheckpoint : creates/updates
    PostgresTradeRepository --> TradeRow : inserts
    PostgresTradeRepository --> ContractDataSummary : creates
    PostgresTickDataProvider --> TradeTick : reads
    PostgresTickDataProvider --> ContractTradeWindow : filters
    ScidDataImportService --> DataImportResult : creates
```

## data.market Package

```mermaid
classDiagram
    direction LR

    class MarketDataProvider {
        <<interface>>
    }

    class TickDataProvider {
        <<interface>>
        +TradeBatchReader openReader(List~ContractTradeWindow~ windows, int batchSize)
        +long countTicks(List~ContractTradeWindow~ windows)
    }

    class TradeBatchReader {
        <<interface>>
        +List~TradeTick~ readNextBatch()
    }

    class ContractTradeWindow {
        -String contractSymbol
        -LocalDate startDate
        -LocalDate endDate
    }

    class TradeTick {
        -String contractSymbol
        -Instant tradeDateTime
        -long priceTicks
        -Long bidPriceTicks
        -Long askPriceTicks
        -long quantity
        -int side
        -long scidRecordIndex
    }

    class InMemoryTickDataProvider

    class DerivedMarketDataService {
        <<interface>>
    }

    TickDataProvider <|.. InMemoryTickDataProvider
    TickDataProvider --> TradeBatchReader : opens
    TradeBatchReader --> TradeTick : returns
    TickDataProvider --> ContractTradeWindow : reads
```

## strategy Package

```mermaid
classDiagram
    direction LR

    class FacadeForgeStrategy {
        +FacadeForgeStrategy getTheInstance()
        +ForgeStrategyAccess forgeStrategyAccess()
    }

    class ForgeStrategyAccess {
        +List~Class~ findAvailableStrategies()
        +String getDisplayName(Class strategy)
        +String getDescription(Class strategy)
        +StrategyOptions createStrategyOptions(Class strategy)
        +StrategyConfigurationProfile getConfigurationProfile(Class strategy)
        +TradingStrategy createStrategy(Class strategy)
    }

    class StrategyCatalog {
        +List~Class~ findAvailableStrategies()
        +String getDisplayName(Class strategyClass)
        +String getDescription(Class strategyClass)
        +StrategyConfigurationProfile getConfigurationProfile(Class strategyClass)
    }

    class StrategyConfigurationProfile {
        -List~Class~ allowedTriggers
        -Class defaultTrigger
        -boolean triggerSelectionAllowed
        -List~String~ allowedTargets
        -String defaultTarget
        -boolean targetSelectionAllowed
        +TargetSettings getDefaultTargetSettings(String targetMode)
    }

    class TradingStrategy {
        <<interface>>
        +String getName()
        +StrategyDecision evaluate(StrategyContext context)
        +StrategyRequirements getRequirements()
        +void onBacktestStart()
    }

    class StrategyRequirements {
        -Set~String~ requiredFeatureNames
        -Set~String~ requiredEventNames
        -Set~TradingSession~ evaluationSessions
        -Set~TpoPeriod~ evaluationTpoPeriods
        +boolean shouldEvaluate(StrategyContext context)
        +boolean requiresFeature(String featureName)
        +boolean requiresEvent(String eventName)
    }

    class StrategyContext {
        -MarketContext marketContext
        -TradeTick currentTick
        -TradingDayContext tradingDayContext
        -TpoPeriod tpoPeriod
        -SessionRangeFeature sessionRangeFeature
        -List~MarketEvent~ currentEvents
    }

    class StrategyDecision {
        -OrderRequest orderRequest
        -TradePlan tradePlan
        +StrategyDecision noAction()
        +StrategyDecision signal(OrderRequest orderRequest)
        +StrategyDecision trade(OrderRequest orderRequest, TradePlan tradePlan)
        +Optional~OrderRequest~ getOrderRequest()
        +Optional~TradePlan~ getTradePlan()
    }

    class RangeBreakoutStrategy {
        -double rangeHigh
        -double rangeLow
        -int quantity
    }

    class OpeningRangeContinuationStrategy {
        -int quantity
        -ExitStyle exitStyle
        -double rewardRiskRatio
        -Map tradeTakenBySession
        +StrategyDecision evaluate(StrategyContext context)
    }

    class ExitStyle

    class TradePlan {
        -OrderSide side
        -long targetPriceTicks
        -long stopPriceTicks
        -LocalTime timeStop
    }

    class TimeframeRangeCalculator {
        +Optional~PriceRange~ calculatePriceRange(Collection~TradeTick~ ticks, Instant startInclusive, Instant endExclusive)
    }

    class PriceRange {
        -long lowPriceTicks
        -long highPriceTicks
        +long getRangeTicks()
        +double getLowPrice(double tickSize)
        +double getHighPrice(double tickSize)
    }

    FacadeForgeStrategy --> ForgeStrategyAccess
    ForgeStrategyAccess --> StrategyCatalog
    ForgeStrategyAccess --> StrategyConfigurationProfile : exposes
    StrategyCatalog --> StrategyConfigurationProfile : creates
    ForgeStrategyAccess --> TradingStrategy : creates
    StrategyConfigurationProfile --> TradeTrigger : allowed/default triggers
    StrategyConfigurationProfile --> TargetSettings : allowed/default targets
    StrategyConfigurationProfile --> TargetSettings : defaults
    TradingStrategy <|.. RangeBreakoutStrategy
    TradingStrategy <|.. OpeningRangeContinuationStrategy
    TradingStrategy --> StrategyContext : evaluates
    TradingStrategy --> StrategyDecision : returns
    TradingStrategy --> StrategyRequirements : declares
    StrategyRequirements --> TradingSession : filters
    StrategyRequirements --> TpoPeriod : filters
    StrategyContext --> MarketContext
    StrategyContext --> TradeTick
    StrategyContext --> TradingDayContext
    StrategyContext --> TpoPeriod
    StrategyContext --> SessionRangeFeature
    StrategyContext --> MarketEvent
    StrategyDecision --> OrderRequest
    StrategyDecision --> TradePlan
    OpeningRangeContinuationStrategy --> MarketEvent : consumes breach events
    OpeningRangeContinuationStrategy --> SessionRangeFeature : consumes ranges
    OpeningRangeContinuationStrategy --> ExitStyle
    TimeframeRangeCalculator --> TradeTick : scans
    TimeframeRangeCalculator --> PriceRange : returns
```

## trigger Package

```mermaid
classDiagram
    direction LR

    class FacadeForgeTrigger {
        +FacadeForgeTrigger getTheInstance()
        +ForgeTriggerAccess forgeTriggerAccess()
    }

    class ForgeTriggerAccess {
        +List~Class~ findAvailableTriggers()
        +String getDisplayName(Class trigger)
        +TradeTriggerOptions createTriggerOptions(Class trigger)
        +TradeTriggerOptions createTriggerOptions(Class trigger, Map parameters)
        +TradeTrigger createTrigger(Class trigger)
    }

    class TriggerCatalog {
        +List~Class~ findAvailableTriggers()
        +String getDisplayName(Class triggerClass)
    }

    class TradeTrigger {
        <<interface>>
        +String getName()
        +TriggerResult evaluate(MarketContext context)
    }

    class OrderFlowExhaustionTrigger
    class PriceCrossoverTrigger {
        -TriggerDirection direction
        -long priceThresholdTicks
        +TriggerResult evaluate(MarketContext context)
    }
    class TriggerResult {
        -boolean triggered
        -TriggerDirection direction
        +TriggerResult triggered(TriggerDirection direction)
        +TriggerResult notTriggered()
        +boolean isTriggered()
        +TriggerDirection getDirection()
    }
    class TriggerDirection

    FacadeForgeTrigger --> ForgeTriggerAccess
    ForgeTriggerAccess --> TriggerCatalog
    ForgeTriggerAccess --> TradeTrigger : creates
    TradeTrigger <|.. OrderFlowExhaustionTrigger
    TradeTrigger <|.. PriceCrossoverTrigger
    TradeTrigger --> TriggerResult
    PriceCrossoverTrigger --> TriggerDirection
    TriggerResult --> TriggerDirection
```

## engine Package

```mermaid
classDiagram
    direction LR

    class FacadeForgeEngine {
        +FacadeForgeEngine getTheInstance()
        +ForgeEngineAccess forgeEngineAccess()
    }

    class ForgeEngineAccess {
        +BacktestEngine getBacktestEngine()
        +MarketContext createMarketContext(String instrumentSymbol, LocalDateTime timestamp, double lastPrice, boolean hasOpenPosition)
        +MarketContext createMarketContext(String instrumentSymbol, LocalDateTime timestamp, long lastPriceTicks, double tickSize, double tickDollarValue, boolean hasOpenPosition)
        +BacktestResult run(BacktestRequest request)
        +BacktestResult run(BacktestRequest request, BacktestProgressListener listener)
    }

    class BacktestEngine {
        +BacktestResult run(BacktestRequest request)
        +BacktestResult run(BacktestRequest request, BacktestProgressListener listener)
    }

    class MarketContext {
        -String instrumentSymbol
        -LocalDateTime timestamp
        -long lastPriceTicks
        -double lastPrice
        -double tickSize
        -double tickDollarValue
        -boolean hasOpenPosition
    }

    FacadeForgeEngine --> ForgeEngineAccess
    ForgeEngineAccess --> BacktestEngine
    ForgeEngineAccess --> MarketContext : creates
    BacktestEngine --> BacktestRequest
    BacktestEngine --> BacktestProgressListener : reports progress
    BacktestEngine --> TradeBatchReader : reads batches
    BacktestEngine --> FeatureBuildService : derives features
    BacktestEngine --> EventBuildService : derives events
    BacktestEngine --> TpoPeriodClassifier : classifies periods
    BacktestEngine --> StrategyRequirements : plans evaluation
    BacktestEngine --> StrategyContext : builds
    BacktestEngine --> TradingStrategy : evaluates
    BacktestEngine --> StrategyDecision : consumes
    BacktestEngine --> ExecutionEngine : creates fills
    BacktestEngine --> FacadeForgeTrade : creates lifecycle engines
    BacktestEngine --> BacktestResult : creates
```

## trade Package

```mermaid
classDiagram
    direction LR

    class FacadeForgeTrade {
        +FacadeForgeTrade getTheInstance()
        +ForgeTradeAccess forgeTradeAccess()
    }

    class ForgeTradeAccess {
        +TradeLifecycleEngine createTradeLifecycleEngine()
        +TradePlan createTradePlan(OrderSide side, long targetPriceTicks, long stopPriceTicks, LocalTime timeStop, ZoneId timeZone)
    }

    class TradeLifecycleEngine {
        +boolean hasOpenPosition()
        +void openPosition(Fill entryFill, TradePlan plan, FuturesInstrumentSpec instrumentSpec)
        +Optional~TradeResult~ onTick(TradeTick tick)
        +Optional~TradeResult~ closeOpenPositionAtEnd()
    }

    class Position {
        -String instrumentSymbol
        -String contractSymbol
        -OrderSide side
        -Instant entryTime
        -long entryPriceTicks
        -int quantity
        -long maxFavorableExcursionTicks
        -long maxAdverseExcursionTicks
        +void updateExcursion(long priceTicks)
        +TradeResult close(Instant exitTime, long exitPriceTicks, String exitReason)
    }

    class TradePlan {
        -OrderSide side
        -long targetPriceTicks
        -long stopPriceTicks
        -LocalTime timeStop
        -ZoneId timeZone
    }

    FacadeForgeTrade --> ForgeTradeAccess
    ForgeTradeAccess --> TradeLifecycleEngine : creates
    ForgeTradeAccess --> TradePlan : creates
    TradeLifecycleEngine --> Position
    TradeLifecycleEngine --> Fill
    TradeLifecycleEngine --> TradePlan
    TradeLifecycleEngine --> TradeTick
    TradeLifecycleEngine --> TradeResult
    Position --> TradeResult
```

## execution Package

```mermaid
classDiagram
    direction LR

    class FacadeForgeExecution {
        +FacadeForgeExecution getTheInstance()
        +ForgeExecutionAccess forgeExecutionAccess()
    }

    class ForgeExecutionAccess {
        +ExecutionEngine createSimpleExecutionEngine()
        +OrderRequest createMarketOrderRequest(String instrumentSymbol, OrderSide side, int quantity)
        +Order createOrder()
        +Fill createFill(String instrumentSymbol, String contractSymbol, OrderSide side, OrderType orderType, int quantity, Instant fillTime, long fillPriceTicks, long scidRecordIndex)
    }

    class ExecutionEngine {
        <<interface>>
        +Optional~Fill~ execute(OrderRequest orderRequest, TradeTick currentTick)
    }

    class SimpleExecutionEngine {
        +Optional~Fill~ execute(OrderRequest orderRequest, TradeTick currentTick)
    }
    class OrderRequest
    class Order
    class Fill {
        -String instrumentSymbol
        -String contractSymbol
        -OrderSide side
        -OrderType orderType
        -int quantity
        -Instant fillTime
        -long fillPriceTicks
        -long scidRecordIndex
    }
    class OrderSide
    class OrderType

    FacadeForgeExecution --> ForgeExecutionAccess
    ForgeExecutionAccess --> ExecutionEngine : creates
    ForgeExecutionAccess --> OrderRequest : creates
    ForgeExecutionAccess --> Order : creates
    ForgeExecutionAccess --> Fill : creates
    ExecutionEngine <|.. SimpleExecutionEngine
    SimpleExecutionEngine --> Fill : current tick price
    SimpleExecutionEngine --> TradeTick : reads current tick
    OrderRequest --> OrderSide
    OrderRequest --> OrderType
    Order --> OrderType
    Order --> OrderSide
    Fill --> OrderSide
    Fill --> OrderType
```

## reporting Package

```mermaid
classDiagram
    direction LR

    class FacadeForgeReporting {
        +FacadeForgeReporting getTheInstance()
        +ForgeReportingAccess forgeReportingAccess()
    }

    class ForgeReportingAccess {
        +BacktestResult createBacktestResult()
        +PerformanceMetrics createPerformanceMetrics()
        +InstrumentPerformanceReport createInstrumentPerformanceReport()
        +String summarize(BacktestResult result)
    }

    class BacktestResult {
        -String strategyName
        -List~String~ contractSymbols
        -long ticksProcessed
        -long orderSignalsGenerated
        -List~InstrumentBacktestResult~ instrumentResults
    }
    class InstrumentBacktestResult {
        -String instrumentSymbol
        -long ticksProcessed
        -long orderSignalsGenerated
        -List~ContractBacktestResult~ contractResults
        -PerformanceMetrics performanceMetrics
    }
    class ContractBacktestResult {
        -String contractSymbol
        -long ticksProcessed
        -long orderSignalsGenerated
        -List~TradeResult~ trades
        -PerformanceMetrics performanceMetrics
    }
    class PerformanceMetrics {
        -int totalTrades
        -int winningTrades
        -int losingTrades
        -double netProfitLoss
        -double profitFactor
        -double maximumDrawdown
    }
    class InstrumentPerformanceReport

    FacadeForgeReporting --> ForgeReportingAccess
    ForgeReportingAccess --> BacktestResult : creates/summarizes
    ForgeReportingAccess --> PerformanceMetrics : creates
    ForgeReportingAccess --> InstrumentPerformanceReport : creates
    BacktestResult --> InstrumentBacktestResult
    InstrumentBacktestResult --> ContractBacktestResult
    InstrumentBacktestResult --> PerformanceMetrics
    ContractBacktestResult --> PerformanceMetrics
    ContractBacktestResult --> TradeResult
```

## analytics Package

```mermaid
classDiagram
    direction LR

    class FacadeForgeAnalytics {
        +FacadeForgeAnalytics getTheInstance()
        +ForgeAnalyticsAccess forgeAnalyticsAccess()
    }

    class ForgeAnalyticsAccess {
        +FeatureSet createFeatureSet()
        +MarketFeature createMarketFeature()
    }

    class FeatureCalculator
    class FeatureSet
    class MarketFeature

    FacadeForgeAnalytics --> ForgeAnalyticsAccess
    ForgeAnalyticsAccess --> FeatureSet : creates
    ForgeAnalyticsAccess --> MarketFeature : creates
    FeatureCalculator --> FeatureSet
    FeatureSet --> MarketFeature
```

## feature Package

```mermaid
classDiagram
    direction LR

    class FacadeForgeFeature {
        +FacadeForgeFeature getTheInstance()
        +ForgeFeatureAccess forgeFeatureAccess()
    }

    class ForgeFeatureAccess {
        +List~String~ getSupportedFeatureNames()
        +List~SessionRangeFeature~ calculateSessionRanges(Collection~TradeTick~ ticks)
    }

    class FeatureDefinition {
        <<interface>>
        +String getName()
        +int getVersion()
    }

    class FeatureResult {
        -String featureName
        -int featureVersion
    }

    class FeatureCalculator {
        <<interface>>
        +FeatureDefinition getDefinition()
    }

    class FeatureBuildService {
        +List~String~ getSupportedFeatureNames()
        +List~SessionRangeFeature~ calculateSessionRanges(Collection~TradeTick~ ticks)
    }

    class TradingDayClassifier {
        +TradingDayContext classify(Instant tradeDateTime)
    }

    class TradingDayContext {
        -LocalDate tradingDay
        -TradingSession session
        +boolean isOvernight()
        +boolean isFirstHour()
        +boolean isRth()
    }

    class TpoPeriodClassifier {
        +TpoPeriod classify(Instant tradeDateTime)
    }

    class TpoPeriod {
        <<enumeration>>
        A
        B
        C
        D
        E
        F
        G
        H
        I
        J
        K
        L
        M
        N
        O
        P
        Q
        OUTSIDE_RTH
    }

    class TradingSession {
        <<enumeration>>
        OVERNIGHT
        FIRST_HOUR
        RTH
    }

    class SessionRangeFeatureCalculator {
        +List~SessionRangeFeature~ calculate(Collection~TradeTick~ ticks)
    }

    class SessionRangeFeature {
        -String contractSymbol
        -LocalDate sessionDate
        -long overnightLowTicks
        -long overnightHighTicks
        -long firstHourLowTicks
        -long firstHourHighTicks
        -long rthLowTicks
        -long rthHighTicks
    }

    FacadeForgeFeature --> ForgeFeatureAccess
    ForgeFeatureAccess --> FeatureBuildService
    FeatureCalculator --> FeatureDefinition
    FeatureBuildService --> SessionRangeFeatureCalculator
    SessionRangeFeatureCalculator --> TradingDayClassifier
    TradingDayClassifier --> TradingDayContext
    TradingDayContext --> TradingSession
    TpoPeriodClassifier --> TpoPeriod
    SessionRangeFeatureCalculator --> SessionRangeFeature
    SessionRangeFeature --|> FeatureResult
```

## event Package

```mermaid
classDiagram
    direction LR

    class FacadeForgeEvent {
        +FacadeForgeEvent getTheInstance()
        +ForgeEventAccess forgeEventAccess()
    }

    class ForgeEventAccess {
        +List~String~ getSupportedEventNames()
        +List~MarketEvent~ detectFirstHourBreachEvents(Collection~SessionRangeFeature~ features, Collection~TradeTick~ ticks)
    }

    class EventDefinition {
        <<interface>>
        +String getName()
        +int getVersion()
    }

    class EventDetector {
        <<interface>>
        +EventDefinition getDefinition()
    }

    class EventBuildService {
        +List~String~ getSupportedEventNames()
        +List~MarketEvent~ detectFirstHourBreachEvents(Collection~SessionRangeFeature~ features, Collection~TradeTick~ ticks)
    }

    class FirstHourBreachEventDetector {
        +EventDefinition getDefinition()
        +List~MarketEvent~ detect(Collection~SessionRangeFeature~ features, Collection~TradeTick~ ticks)
    }

    class FirstHourBreachEvent {
        +String getName()
        +int getVersion()
    }

    class MarketEvent {
        -String contractSymbol
        -LocalDate sessionDate
        -String eventName
        -EventSide side
        -Instant eventTime
        -long eventPriceTicks
    }

    class EventSide

    FacadeForgeEvent --> ForgeEventAccess
    ForgeEventAccess --> EventBuildService
    EventBuildService --> FirstHourBreachEventDetector
    EventDetector --> EventDefinition
    FirstHourBreachEventDetector ..|> EventDetector
    FirstHourBreachEventDetector --> SessionRangeFeature
    FirstHourBreachEventDetector --> TradeTick
    FirstHourBreachEventDetector --> MarketEvent
    FirstHourBreachEvent ..|> EventDefinition
    MarketEvent --> EventSide
```

## query Package

```mermaid
classDiagram
    direction LR

    class FacadeForgeQuery {
        +FacadeForgeQuery getTheInstance()
        +ForgeQueryAccess forgeQueryAccess()
    }

    class ForgeQueryAccess {
        +List~String~ getSupportedQueryEventNames()
        +String getEventStatisticDisplayName(String eventName)
        +String getEventStatisticDescription(String eventName)
        +EventStatisticsReport summarizeEventStatistics(EventStatisticsQuery query, Collection~SessionRangeFeature~ features, Collection~MarketEvent~ events)
        +EventStatisticsReport runEventStatistics(EventStatisticsQueryRequest request)
    }

    class QueryService {
        +List~String~ getSupportedQueryEventNames()
        +String getEventStatisticDisplayName(String eventName)
        +String getEventStatisticDescription(String eventName)
        +EventStatisticsReport summarizeEventStatistics(EventStatisticsQuery query, Collection~SessionRangeFeature~ features, Collection~MarketEvent~ events)
    }

    class EventStatisticsQueryRunner {
        +EventStatisticsReport run(EventStatisticsQueryRequest request)
    }

    class QueryTradeTickSource {
        <<interface>>
        +TradeBatchReader openTradeBatchReader(List~ContractTradeWindow~ windows, int batchSize)
        +long countTradeTicks(List~ContractTradeWindow~ windows)
    }

    class QueryDerivedDataStore {
        <<interface>>
        +boolean areSessionRangesBuilt(List~ContractTradeWindow~ windows)
        +List~SessionRangeFeature~ loadSessionRanges(List~ContractTradeWindow~ windows)
        +void saveSessionRanges(Collection~SessionRangeFeature~ features)
        +boolean areMarketEventsBuilt(List~ContractTradeWindow~ windows, String eventName)
        +List~MarketEvent~ loadMarketEvents(List~ContractTradeWindow~ windows, String eventName)
        +void saveMarketEvents(Collection~MarketEvent~ events)
    }

    class EventStatisticsQuery {
        -String eventName
    }

    class EventStatisticsQueryRequest {
        -List~ContractTradeWindow~ contractWindows
        -String eventName
        -int batchSize
        -EventStatisticsProgressListener progressListener
    }

    class EventStatisticsResult {
        -String scopeName
        -String eventName
        -long sessionsAnalyzed
        -long longEventCount
        -long shortEventCount
        +long getTotalEventCount()
        +long getNoEventCount()
        +double getEventRate()
    }

    class EventStatisticsReport {
        -String eventName
        -List~EventStatisticsResult~ instrumentResults
        -List~EventStatisticsResult~ contractResults
    }

    FacadeForgeQuery --> ForgeQueryAccess
    ForgeQueryAccess --> QueryService
    ForgeQueryAccess --> EventStatisticsQueryRunner
    EventStatisticsQueryRunner --> QueryTradeTickSource
    EventStatisticsQueryRunner --> QueryDerivedDataStore
    EventStatisticsQueryRunner --> EventStatisticsQueryRequest
    EventStatisticsQueryRunner --> FeatureBuildService
    EventStatisticsQueryRunner --> EventBuildService
    EventStatisticsQueryRunner --> QueryService
    QueryService --> EventStatisticsQuery
    QueryService --> EventStatisticsReport
    QueryService --> EventStatisticsResult
    EventStatisticsReport --> EventStatisticsResult
```

## backtest Package

```mermaid
classDiagram
    direction LR

    class FacadeForgeBacktest {
        +FacadeForgeBacktest getTheInstance()
        +ForgeBacktestAccess forgeBacktestAccess()
    }

    class ForgeBacktestAccess {
        +Position createPosition()
        +TradeResult createTradeResult()
    }

    class Position
    class TradeResult {
        -String instrumentSymbol
        -String contractSymbol
        -OrderSide side
        -Instant entryTime
        -long entryPriceTicks
        -Instant exitTime
        -long exitPriceTicks
        -double grossDollars
    }

    FacadeForgeBacktest --> ForgeBacktestAccess
    ForgeBacktestAccess --> Position : creates
    ForgeBacktestAccess --> TradeResult : creates
    Position --> TradeResult
```

## Technique Mapping

- **Abstract class:** `Instrument` defines shared instrument behavior while requiring subclasses to provide the instrument type.
- **Inheritance:** `FuturesInstrument` and `FuturesContract` extend `Instrument` because futures instruments/contracts are specialized tradable instruments.
- **Interfaces:** `TradingStrategy`, `TradeTrigger`, and `ExecutionEngine` define interchangeable behavior.
- **Polymorphism:** Backtest workflow code can work with interfaces such as `TradingStrategy`, `TradeTrigger`, and `ExecutionEngine` without depending on specific implementations.
- **Upcasting:** `FuturesInstrument` and `FuturesContract` objects can be stored or passed as `Instrument` references.
- **Downcasting:** `InstrumentDataCatalog` can downcast an `Instrument` to `FuturesInstrument` when futures-specific details such as tick size or tick dollar amount are needed.
- **Facade design pattern:** `FacadeForgeApplication` is the main application facade. It exposes high-level operations such as `runBacktest(...)`, `planDataImport(...)`, `importData(...)`, and `configureDatabase(...)` through `forgeApplicationAccess()`, so the CLI does not directly coordinate the engine, data import service, PostgreSQL repository, or configuration builders. Other package facades such as `FacadeForgeConfig`, `FacadeForgeData`, `FacadeForgeStrategy`, `FacadeForgeTrigger`, `FacadeForgeEngine`, `FacadeForgeFeature`, `FacadeForgeEvent`, `FacadeForgeQuery`, `FacadeForgeTrade`, `FacadeForgeExecution`, `FacadeForgeReporting`, `FacadeForgeAnalytics`, and `FacadeForgeBacktest` follow the singleton `getTheInstance()` pattern and expose package behavior through package access methods such as `forgeDataAccess()` and `forgeStrategyAccess()`.
- **Integrated file I/O:** `ScidTradeReader.readTrades(...)` performs the core file I/O by opening a SCID file with `FileChannel.open(scidFilePath, StandardOpenOption.READ)`, reading binary records into a `ByteBuffer`, validating the SCID header, and converting complete records into `TradeRow` objects. `ScidDataImportService` integrates that file reader into the import workflow and also uses `Files.size(...)` and `Files.getLastModifiedTime(...)` to capture file metadata for checkpointing.
- **Exception handling:** `ConsoleUserInput` throws the user-defined `UserQuitException` when the user enters `quit` or console input ends, and `CliApplicationController` catches it to exit cleanly. Validation failures use `IllegalArgumentException` to reject invalid settings, unsupported contracts, and malformed SCID records before processing continues. File and database failures are caught as lower-level exceptions such as `IOException` or `SQLException` and wrapped in `IllegalStateException` with application-level messages.
- **Input/output abstraction:** `UserInput` and `UserOutput` keep console input/output separate from the application workflow, while `ConsoleUserInput` and `ConsoleUserOutput` provide the terminal implementation.
- **Service decomposition:** The app selection services own individual setup steps so the app facade can focus on coordinating the overall backtest setup.
