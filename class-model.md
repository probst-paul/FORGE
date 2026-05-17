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
    class PostgresTradeRepository
    class ContractRolloverCalendar
    class FacadeForgeStrategy
    class FacadeForgeTrigger
    class FacadeForgeTarget
    class FacadeForgeEngine
    class FacadeForgeReporting
    class FacadeForgeExecution
    class FacadeForgeAnalytics
    class FacadeForgeBacktest
    class ForgeApplicationAccess
    class InstrumentSelectionService
    class StrategySelectionService
    class TriggerSelectionService
    class TargetModelSelectionService
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
    CliApplicationController --> TargetModelSelectionService : target settings
    CliApplicationController --> FacadeForgeConfig : build request

    InstrumentSelectionService --> FacadeForgeData
    FacadeForgeData --> InstrumentDataCatalog : catalog access
    FacadeForgeData --> ScidDataImportService : import access
    InstrumentDataCatalog --> PostgresTradeRepository : imported tables
    InstrumentDataCatalog --> ContractRolloverCalendar : active windows
    ScidDataImportService --> ContractNameResolver : contract root
    ScidDataImportService --> PostgresTradeRepository : persist rows
    StrategySelectionService --> FacadeForgeStrategy
    TriggerSelectionService --> FacadeForgeTrigger
    TargetModelSelectionService --> FacadeForgeTarget

    ForgeApplicationAccess ..> FacadeForgeEngine : later run request
    ForgeApplicationAccess ..> FacadeForgeExecution : later execute orders
    ForgeApplicationAccess ..> FacadeForgeReporting : later summarize result
    ForgeApplicationAccess ..> FacadeForgeAnalytics : later derive features
    ForgeApplicationAccess ..> FacadeForgeBacktest : later track positions
```

## Backtest Setup Interaction

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
    participant Risk as RiskSettingsSelectionService
    participant Triggers as TriggerSelectionService
    participant Trigger as FacadeForgeTrigger
    participant Targets as TargetModelSelectionService
    participant Target as FacadeForgeTarget
    participant Config as FacadeForgeConfig

    Main->>CliFacade: forgeCliAccess().run()
    CliFacade->>Cli: run(input, output)
    Cli->>Output: print title
    Cli->>Output: print Run Backtest / Import Data choices
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

        Cli->>Risk: readRiskSettings(input)
        Risk->>Input: readDouble(risk per trade)
        Risk->>Input: readDouble(max daily loss)
        Risk-->>Cli: RiskSettings

        Cli->>Triggers: selectTrigger(input, output)
        Triggers->>Trigger: forgeTriggerAccess().findAvailableTriggers()
        Triggers->>Trigger: forgeTriggerAccess().getDisplayName(trigger)
        Triggers->>Input: readInt(selection)
        Triggers-->>Cli: selected trigger class

        Cli->>Targets: selectTargetModel(input, output)
        Targets->>Target: forgeTargetAccess().findAvailableTargetModels()
        Targets->>Target: forgeTargetAccess().getDisplayName(target)
        Targets->>Input: readInt(selection)
        Targets-->>Cli: selected target class

        Cli->>Targets: readTargetModelSettings(input, selected target)
        Targets->>Input: read target-specific value
        Targets->>Target: forgeTargetAccess().create target settings
        Targets-->>Cli: TargetSettings

        Cli->>Config: forgeConfigAccess().createBacktestRequest(...)
        Config-->>Cli: BacktestRequest
        Cli->>App: forgeApplicationAccess().runBacktest(request)
        App-->>Cli: accepted request
        Cli->>Output: print accepted request
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
        +BacktestRequest runBacktest(BacktestRequest request)
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
    ForgeApplicationAccess --> DataImportRequest
    ForgeApplicationAccess --> DatabaseConfigurationRequest
    DataImportRequest --> ImportProgressListener
    ImportProgressListener --> ImportProgress
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
        +String getDisplayName(Class trigger)
    }

    class TargetModelSelectionService {
        +Class selectTargetModel(UserInput input, UserOutput output)
        +TargetSettings readTargetModelSettings(UserInput input, Class targetModel)
        +String getDisplayName(Class targetModel)
    }

    FacadeForgeCli --> ForgeCliAccess
    ForgeCliAccess --> CliApplicationController
    CliApplicationController --> FacadeForgeApplication
    CliApplicationController --> FacadeForgeConfig
    CliApplicationController --> InstrumentSelectionService
    CliApplicationController --> StrategySelectionService
    CliApplicationController --> RiskSettingsSelectionService
    CliApplicationController --> TriggerSelectionService
    CliApplicationController --> TargetModelSelectionService
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
        -String targetModel
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
        +void configurePostgresDatabase(PostgresDatabaseSettings settings)
    }

    class InstrumentDataCatalog
    class ScidDataImportService
    class PostgresTradeRepository
    class PostgresTickDataProvider
    class PostgresDatabaseSettings
    class TradeBatchReader
    class DataImportPlan
    class DataImportResult

    FacadeForgeData --> ForgeDataAccess
    ForgeDataAccess --> InstrumentDataCatalog : catalog
    ForgeDataAccess --> ScidDataImportService : import
    ForgeDataAccess --> PostgresTickDataProvider : tick batches
    ForgeDataAccess --> PostgresDatabaseSettings : configure
    ScidDataImportService --> PostgresTradeRepository
    InstrumentDataCatalog --> PostgresTradeRepository
    PostgresTickDataProvider --> TradeBatchReader : creates
    ForgeDataAccess --> DataImportPlan
    ForgeDataAccess --> DataImportResult
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
        +StrategyOptions createStrategyOptions(Class strategy)
        +TradingStrategy createStrategy(Class strategy)
    }

    class StrategyCatalog {
        +List~Class~ findAvailableStrategies()
        +String getDisplayName(Class strategyClass)
    }

    class TradingStrategy {
        <<interface>>
        +String getName()
        +Optional~OrderRequest~ evaluate(MarketContext context)
        +void onBacktestStart()
    }

    class RangeBreakoutStrategy {
        -double rangeHigh
        -double rangeLow
        -int quantity
    }

    FacadeForgeStrategy --> ForgeStrategyAccess
    ForgeStrategyAccess --> StrategyCatalog
    ForgeStrategyAccess --> TradingStrategy : creates
    TradingStrategy <|.. RangeBreakoutStrategy
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
    class TriggerResult
    class TriggerDirection

    FacadeForgeTrigger --> ForgeTriggerAccess
    ForgeTriggerAccess --> TriggerCatalog
    ForgeTriggerAccess --> TradeTrigger : creates
    TradeTrigger <|.. OrderFlowExhaustionTrigger
    TradeTrigger --> TriggerResult
    TriggerResult --> TriggerDirection
```

## target Package

```mermaid
classDiagram
    direction LR

    class FacadeForgeTarget {
        +FacadeForgeTarget getTheInstance()
        +ForgeTargetAccess forgeTargetAccess()
    }

    class ForgeTargetAccess {
        +List~Class~ findAvailableTargetModels()
        +String getDisplayName(Class targetModel)
        +TargetSettings createFixedRiskRewardSettings(Class targetModel, double rewardRiskRatio)
        +TargetSettings createFixedTargetSettings(Class targetModel, int profitTargetTicks)
        +TargetModel createTargetModel(Class targetModel)
    }

    class TargetModelCatalog {
        +List~Class~ findAvailableTargetModels()
        +String getDisplayName(Class targetModelClass)
    }

    class TargetModel {
        <<interface>>
        +String getName()
        +TargetResult calculateTarget(OrderSide side, double entryPrice, double stopPrice, double tickSize)
    }

    class FixedRiskRewardTarget {
        -double rewardRiskRatio
    }

    class FixedTarget {
        -int targetTicks
    }

    class TargetResult {
        -double targetPrice
        -double stopPrice
    }

    FacadeForgeTarget --> ForgeTargetAccess
    ForgeTargetAccess --> TargetModelCatalog
    ForgeTargetAccess --> TargetModel : creates
    ForgeTargetAccess --> TargetSettings : creates
    TargetModel <|.. FixedRiskRewardTarget
    TargetModel <|.. FixedTarget
    TargetModel --> TargetResult
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
    }

    class BacktestEngine

    class MarketContext {
        -String instrumentSymbol
        -LocalDateTime timestamp
        -double lastPrice
        -boolean hasOpenPosition
    }

    FacadeForgeEngine --> ForgeEngineAccess
    ForgeEngineAccess --> BacktestEngine
    ForgeEngineAccess --> MarketContext : creates
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
        +Fill createFill()
    }

    class ExecutionEngine {
        <<interface>>
    }

    class SimpleExecutionEngine
    class OrderRequest
    class Order
    class Fill
    class OrderSide
    class OrderType

    FacadeForgeExecution --> ForgeExecutionAccess
    ForgeExecutionAccess --> ExecutionEngine : creates
    ForgeExecutionAccess --> OrderRequest : creates
    ForgeExecutionAccess --> Order : creates
    ForgeExecutionAccess --> Fill : creates
    ExecutionEngine <|.. SimpleExecutionEngine
    OrderRequest --> OrderSide
    Order --> OrderType
    Order --> OrderSide
    Fill --> Order
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

    class BacktestResult
    class PerformanceMetrics
    class InstrumentPerformanceReport

    FacadeForgeReporting --> ForgeReportingAccess
    ForgeReportingAccess --> BacktestResult : creates/summarizes
    ForgeReportingAccess --> PerformanceMetrics : creates
    ForgeReportingAccess --> InstrumentPerformanceReport : creates
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
    class TradeResult

    FacadeForgeBacktest --> ForgeBacktestAccess
    ForgeBacktestAccess --> Position : creates
    ForgeBacktestAccess --> TradeResult : creates
    Position --> TradeResult
```

## Assignment 1 Technique Mapping

- **Abstract class:** `Instrument` defines shared instrument behavior while requiring subclasses to provide the instrument type.
- **Inheritance:** `FuturesInstrument` and `FuturesContract` extend `Instrument` because futures instruments/contracts are specialized tradable instruments.
- **Interfaces:** `TradingStrategy`, `TradeTrigger`, `TargetModel`, and `ExecutionEngine` define interchangeable behavior.
- **Polymorphism:** Backtest workflow code can work with interfaces such as `TradingStrategy`, `TradeTrigger`, and `TargetModel` without depending on specific implementations.
- **Upcasting:** `FuturesInstrument` and `FuturesContract` objects can be stored or passed as `Instrument` references.
- **Downcasting:** `InstrumentDataCatalog` can downcast an `Instrument` to `FuturesInstrument` when futures-specific details such as tick size or tick dollar amount are needed.
- **Facade pattern:** `FacadeForgeApplication` coordinates setup, while package facades such as `FacadeForgeConfig`, `FacadeForgeData`, `FacadeForgeStrategy`, `FacadeForgeTrigger`, `FacadeForgeTarget`, `FacadeForgeEngine`, `FacadeForgeExecution`, `FacadeForgeReporting`, `FacadeForgeAnalytics`, and `FacadeForgeBacktest` are singleton entry points obtained with `getTheInstance()`. Package functionality is reached through package access methods such as `forgeDataAccess()` and `forgeStrategyAccess()`.
- **Input/output abstraction:** `UserInput` and `UserOutput` keep console input/output separate from the application workflow.
- **Service decomposition:** The app selection services own individual setup steps so the app facade can focus on coordinating the overall backtest setup.
