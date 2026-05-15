# FORGE Class Model

## Facade Collaboration Overview

```mermaid
classDiagram
    direction LR

    class Main
    class FacadeForgeApplication
    class FacadeBacktestConfiguration
    class FacadeData
    class FacadeStrategy
    class FacadeTrigger
    class FacadeTarget
    class FacadeEngine
    class FacadeReporting
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
    FacadeForgeApplication --> FacadeBacktestConfiguration : final request

    InstrumentSelectionService --> FacadeData
    StrategySelectionService --> FacadeStrategy
    TriggerSelectionService --> FacadeTrigger
    TargetModelSelectionService --> FacadeTarget

    FacadeForgeApplication ..> FacadeEngine : later run request
    FacadeForgeApplication ..> FacadeReporting : later summarize result
```

## Backtest Setup Interaction

```mermaid
sequenceDiagram
    participant Main
    participant App as FacadeForgeApplication
    participant Input as UserInput
    participant Output as UserOutput
    participant Instruments as InstrumentSelectionService
    participant Data as FacadeData
    participant Strategies as StrategySelectionService
    participant Strategy as FacadeStrategy
    participant Risk as RiskSettingsSelectionService
    participant Triggers as TriggerSelectionService
    participant Trigger as FacadeTrigger
    participant Targets as TargetModelSelectionService
    participant Target as FacadeTarget
    participant Config as FacadeBacktestConfiguration

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

    class FacadeBacktestConfiguration {
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

    FacadeBacktestConfiguration --> BacktestRequest : creates
    FacadeBacktestConfiguration --> StrategyOptions : creates
    FacadeBacktestConfiguration --> TradeTriggerOptions : creates
    FacadeBacktestConfiguration --> OrderSettings : creates default
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

    class FacadeData {
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

    FacadeData --> InstrumentDataCatalog
    InstrumentDataCatalog --> Instrument : stores
    InstrumentDataCatalog ..> FuturesContract : futures details
    Instrument <|-- FuturesContract
```

## strategy Package

```mermaid
classDiagram
    direction LR

    class FacadeStrategy {
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

    FacadeStrategy --> StrategyCatalog
    FacadeStrategy --> TradingStrategy : creates
    TradingStrategy <|.. RangeBreakoutStrategy
```

## trigger Package

```mermaid
classDiagram
    direction LR

    class FacadeTrigger {
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

    FacadeTrigger --> TriggerCatalog
    FacadeTrigger --> TradeTrigger : creates
    TradeTrigger <|.. OrderFlowExhaustionTrigger
    TradeTrigger --> TriggerResult
    TriggerResult --> TriggerDirection
```

## target Package

```mermaid
classDiagram
    direction LR

    class FacadeTarget {
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

    FacadeTarget --> TargetModelCatalog
    FacadeTarget --> TargetModel : creates
    FacadeTarget --> TargetSettings : creates
    TargetModel <|.. FixedRiskRewardTarget
    TargetModel <|.. FixedTarget
    TargetModel --> TargetResult
```

## engine Package

```mermaid
classDiagram
    direction LR

    class FacadeEngine {
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

    FacadeEngine --> BacktestEngine
    FacadeEngine --> MarketContext : creates
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

    class FacadeReporting {
        +BacktestResult createBacktestResult()
        +PerformanceMetrics createPerformanceMetrics()
        +InstrumentPerformanceReport createInstrumentPerformanceReport()
        +String summarize(BacktestResult result)
    }

    class BacktestResult
    class PerformanceMetrics
    class InstrumentPerformanceReport

    FacadeReporting --> BacktestResult : creates/summarizes
    FacadeReporting --> PerformanceMetrics : creates
    FacadeReporting --> InstrumentPerformanceReport : creates
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
- **Facade pattern:** `FacadeForgeApplication` coordinates setup, while package facades such as `FacadeBacktestConfiguration`, `FacadeData`, `FacadeStrategy`, `FacadeTrigger`, `FacadeTarget`, `FacadeEngine`, and `FacadeReporting` hide package internals behind simpler entry points.
- **Input/output abstraction:** `UserInput` and `UserOutput` keep console input/output separate from the application workflow.
- **Service decomposition:** The app selection services own individual setup steps so the app facade can focus on coordinating the overall backtest setup.
