# FORGE Class Model

```mermaid
---
config:
  layout: elk
---
classDiagram
    direction LR

    class Instrument {
        <<abstract>>
        -String symbolCode
        -String displayName
        #Instrument(String symbolCode, String displayName)
        +String getSymbolCode()
        +String getDisplayName()
        +String getInstrumentType()*
    }

    class FuturesContract {
        -double tickSize
        -double tickDollarAmount
        -LocalDate expirationDate
        +String getInstrumentType()
        +double getTickSize()
        +double getTickDollarAmount()
        +LocalDate getExpirationDate()
        +double calculateDollarValueForTicks(double ticks)
    }

    Instrument <|-- FuturesContract : inheritance

    class TradingStrategy {
        <<interface>>
        +String getName()
        +Optional~OrderRequest~ evaluate(MarketContext context)
    }

    class RangeBreakoutStrategy {
        -double rangeHigh
        -double rangeLow
        -int quantity
        +String getName()
        +Optional~OrderRequest~ evaluate(MarketContext context)
    }

    TradingStrategy <|.. RangeBreakoutStrategy : polymorphism

    class TradeTrigger {
        <<interface>>
        +String getName()
        +TriggerResult evaluate(MarketContext context)
    }

    class OrderFlowExhaustionTrigger {
        +String getName()
        +TriggerResult evaluate(MarketContext context)
    }

    TradeTrigger <|.. OrderFlowExhaustionTrigger : polymorphism

    class TargetModel {
        <<interface>>
        +String getName()
        +TargetResult calculateTarget(OrderSide side, double entryPrice, double stopPrice, double tickSize)
    }

    class FixedRiskRewardTarget {
        -double rewardRiskRatio
        +String getName()
        +TargetResult calculateTarget(OrderSide side, double entryPrice, double stopPrice, double tickSize)
    }

    class FixedTarget {
        -int targetTicks
        +String getName()
        +TargetResult calculateTarget(OrderSide side, double entryPrice, double stopPrice, double tickSize)
    }

    TargetModel <|.. FixedRiskRewardTarget : polymorphism
    TargetModel <|.. FixedTarget : polymorphism

    class InstrumentDataCatalog {
        +List~Instrument~ getAvailableInstruments()
        +void validateDateRange(List~String~ symbols, LocalDate startDate, LocalDate endDate)
        +double getFuturesTickSize(String symbol)
    }

    InstrumentDataCatalog --> Instrument : stores as abstract type / upcasting
    InstrumentDataCatalog ..> FuturesContract : downcasting when futures details are needed

    class BacktestRequest {
        -TradingStrategy strategy
        -TradeTrigger tradeTrigger
        -TargetModel targetModel
        -List~Instrument~ instruments
    }

    BacktestRequest --> TradingStrategy
    BacktestRequest --> TradeTrigger
    BacktestRequest --> TargetModel
    BacktestRequest --> Instrument
```

## Assignment 1 Technique Mapping

- **Abstract class:** `Instrument` defines shared instrument behavior while requiring subclasses to provide the instrument type.
- **Inheritance:** `FuturesContract` extends `Instrument` because futures contracts are a specialized type of tradable instrument.
- **Interfaces:** `TradingStrategy`, `TradeTrigger`, and `TargetModel` define interchangeable behavior for strategies, triggers, and target calculations.
- **Polymorphism:** `BacktestRequest` and the backtesting engine can work with interface references such as `TradingStrategy`, `TradeTrigger`, and `TargetModel` without depending on specific implementations.
- **Upcasting:** `FuturesContract` objects can be stored or passed as `Instrument` references.
- **Downcasting:** `InstrumentDataCatalog` can downcast an `Instrument` to `FuturesContract` when futures-specific details such as tick size or tick dollar amount are needed.
