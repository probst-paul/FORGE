# FORGE Class Model

## Facade Collaboration Overview

```mermaid
classDiagram
    direction LR

    class Main
    class FacadeForgeApplication
    class FacadeForgeConfig
    class FacadeForgeData
    class FacadeForgeStrategy
    class FacadeForgeTrigger
    class FacadeForgeTarget
    class FacadeForgeEngine
    class FacadeForgeReporting
    class InstrumentSelectionService
    class StrategySelectionService
    class TriggerSelectionService
    class TargetModelSelectionService
    class RiskSettingsSelectionService

    Main --> FacadeForgeApplication
    FacadeForgeApplication --> InstrumentSelectionService : instruments + dates
    FacadeForgeApplication --> StrategySelectionService : strategy
    FacadeForgeApplication --> RiskSettingsSelectionService : risk settings
    FacadeForgeApplication --> TriggerSelectionService : trigger
    FacadeForgeApplication --> TargetModelSelectionService : target settings
    FacadeForgeApplication --> FacadeForgeConfig : final request

    InstrumentSelectionService --> FacadeForgeData
    StrategySelectionService --> FacadeForgeStrategy
    TriggerSelectionService --> FacadeForgeTrigger
    TargetModelSelectionService --> FacadeForgeTarget

    FacadeForgeApplication ..> FacadeForgeEngine : later run request
    FacadeForgeApplication ..> FacadeForgeReporting : later summarize result
```

## Backtest Setup Interaction

```mermaid
sequenceDiagram
    participant Main
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

    Main->>App: runBacktestSetup(input, output)
    App->>Output: print title

    App->>Instruments: selectInstruments(input, output)
    Instruments->>Data: getAvailableInstruments()
    Instruments->>Output: print instrument choices
    Instruments->>Input: readString(selection)
    Instruments-->>App: selected symbols

    App->>Instruments: selectDateRange(input, output, symbols)
    Instruments->>Data: getSharedDateRange(symbols)
    Instruments->>Input: readDateOrDefault(start)
    Instruments->>Input: readDateOrDefault(end)
    Instruments->>Data: validateDateRange(symbols, start, end)
    Instruments-->>App: selected date range

    App->>Strategies: selectStrategy(input, output)
    Strategies->>Strategy: findAvailableStrategies()
    Strategies->>Strategy: getDisplayName(strategy)
    Strategies->>Input: readInt(selection)
    Strategies-->>App: selected strategy class

    App->>Risk: readRiskSettings(input)
    Risk->>Input: readDouble(risk per trade)
    Risk->>Input: readDouble(max daily loss)
    Risk-->>App: RiskSettings

    App->>Triggers: selectTrigger(input, output)
    Triggers->>Trigger: findAvailableTriggers()
    Triggers->>Trigger: getDisplayName(trigger)
    Triggers->>Input: readInt(selection)
    Triggers-->>App: selected trigger class

    App->>Targets: selectTargetModel(input, output)
    Targets->>Target: findAvailableTargetModels()
    Targets->>Target: getDisplayName(target)
    Targets->>Input: readInt(selection)
    Targets-->>App: selected target class

    App->>Targets: readTargetModelSettings(input, selected target)
    Targets->>Input: read target-specific value
    Targets->>Target: create target settings
    Targets-->>App: TargetSettings

    App->>Config: createBacktestRequest(...)
    Config-->>App: BacktestRequest
    App->>Output: print accepted request
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
        +void runBacktestSetup(UserInput input, UserOutput output)
        +BacktestRequest configureBacktest(UserInput input, UserOutput output)
    }

    class InstrumentSelectionService {
        +List~String~ selectInstruments(UserInput input, UserOutput output)
        +LocalDate[] selectDateRange(UserInput input, UserOutput output, List~String~ instruments)
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
    }

    class ConsoleUserOutput {
        +void printLine(String text)
    }

    Main --> FacadeForgeApplication
    Main --> ConsoleUserInput
    Main --> ConsoleUserOutput
    ConsoleUserInput ..|> UserInput
    ConsoleUserOutput ..|> UserOutput
    FacadeForgeApplication --> UserInput
    FacadeForgeApplication --> UserOutput
    FacadeForgeApplication --> InstrumentSelectionService
    FacadeForgeApplication --> StrategySelectionService
    FacadeForgeApplication --> RiskSettingsSelectionService
    FacadeForgeApplication --> TriggerSelectionService
    FacadeForgeApplication --> TargetModelSelectionService
```

## config Package

```mermaid
classDiagram
    direction LR

    class FacadeForgeConfig {
        +FacadeForgeConfig getTheInstance()
        +BacktestRequest createBacktestRequest(...)
        +OrderSettings defaultOrderSettings()
    }

    class BacktestRequest {
        -StrategyOptions strategyOptions
        -List~String~ instruments
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

    FacadeForgeConfig --> BacktestRequest : creates
    FacadeForgeConfig --> StrategyOptions : creates
    FacadeForgeConfig --> TradeTriggerOptions : creates
    FacadeForgeConfig --> OrderSettings : creates default
    BacktestRequest --> StrategyOptions
    BacktestRequest --> TradeTriggerOptions
    BacktestRequest --> RiskSettings
    BacktestRequest --> TargetSettings
    BacktestRequest --> OrderSettings
```

## data and model Packages

```mermaid
classDiagram
    direction LR

    class FacadeForgeData {
        +FacadeForgeData getTheInstance()
        +List~AvailableInstrumentData~ getAvailableInstruments()
        +AvailableDateRange getSharedDateRange(List~String~ symbols)
        +void validateDateRange(List~String~ symbols, LocalDate startDate, LocalDate endDate)
    }

    class InstrumentDataCatalog {
        +List~AvailableInstrumentData~ getAvailableInstruments()
        +AvailableDateRange getSharedDateRange(List~String~ symbols)
        +void validateDateRange(List~String~ symbols, LocalDate startDate, LocalDate endDate)
    }

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

    class MarketDataSet
    class PriceBar
    class TickRecord
    class PriceLevelVolume
    class DateRange

    FacadeForgeData --> InstrumentDataCatalog
    InstrumentDataCatalog --> Instrument : stores
    InstrumentDataCatalog ..> FuturesContract : futures details
    Instrument <|-- FuturesContract
```

## strategy Package

```mermaid
classDiagram
    direction LR

    class FacadeForgeStrategy {
        +FacadeForgeStrategy getTheInstance()
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

    FacadeForgeStrategy --> StrategyCatalog
    FacadeForgeStrategy --> TradingStrategy : creates
    TradingStrategy <|.. RangeBreakoutStrategy
```

## trigger Package

```mermaid
classDiagram
    direction LR

    class FacadeForgeTrigger {
        +FacadeForgeTrigger getTheInstance()
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

    FacadeForgeTrigger --> TriggerCatalog
    FacadeForgeTrigger --> TradeTrigger : creates
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

    FacadeForgeTarget --> TargetModelCatalog
    FacadeForgeTarget --> TargetModel : creates
    FacadeForgeTarget --> TargetSettings : creates
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

    FacadeForgeEngine --> BacktestEngine
    FacadeForgeEngine --> MarketContext : creates
```

## execution Package

```mermaid
classDiagram
    direction LR

    class ExecutionEngine {
        <<interface>>
    }

    class SimpleExecutionEngine
    class OrderRequest
    class Order
    class Fill
    class OrderSide
    class OrderType

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
        +BacktestResult createBacktestResult()
        +PerformanceMetrics createPerformanceMetrics()
        +InstrumentPerformanceReport createInstrumentPerformanceReport()
        +String summarize(BacktestResult result)
    }

    class BacktestResult
    class PerformanceMetrics
    class InstrumentPerformanceReport

    FacadeForgeReporting --> BacktestResult : creates/summarizes
    FacadeForgeReporting --> PerformanceMetrics : creates
    FacadeForgeReporting --> InstrumentPerformanceReport : creates
```

## analytics and backtest Packages

```mermaid
classDiagram
    direction LR

    class FeatureCalculator
    class FeatureSet
    class MarketFeature
    class Position
    class TradeResult

    FeatureCalculator --> FeatureSet
    FeatureSet --> MarketFeature
    Position --> TradeResult
```

## Assignment 1 Technique Mapping

- **Abstract class:** `Instrument` defines shared instrument behavior while requiring subclasses to provide the instrument type.
- **Inheritance:** `FuturesContract` extends `Instrument` because futures contracts are a specialized type of tradable instrument.
- **Interfaces:** `TradingStrategy`, `TradeTrigger`, `TargetModel`, and `ExecutionEngine` define interchangeable behavior.
- **Polymorphism:** Backtest workflow code can work with interfaces such as `TradingStrategy`, `TradeTrigger`, and `TargetModel` without depending on specific implementations.
- **Upcasting:** `FuturesContract` objects can be stored or passed as `Instrument` references.
- **Downcasting:** `InstrumentDataCatalog` can downcast an `Instrument` to `FuturesContract` when futures-specific details such as tick size or tick dollar amount are needed.
- **Facade pattern:** `FacadeForgeApplication` coordinates setup, while package facades such as `FacadeForgeConfig`, `FacadeForgeData`, `FacadeForgeStrategy`, `FacadeForgeTrigger`, `FacadeForgeTarget`, `FacadeForgeEngine`, and `FacadeForgeReporting` hide package internals behind simpler singleton entry points obtained with `getTheInstance()`.
- **Input/output abstraction:** `UserInput` and `UserOutput` keep console input/output separate from the application workflow.
- **Service decomposition:** The app selection services own individual setup steps so the app facade can focus on coordinating the overall backtest setup.
