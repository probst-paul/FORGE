# Architecture

## Layering

FORGE separates market research from trade simulation:

```text
data/
  raw imported ticks and contract windows

feature/
  reusable derived measurements such as overnight range, first-hour range, and RTH range

event/
  reusable market events such as first-hour breach and price crossover

study/
  market setup definitions built from features and events

statistics/
  aggregation of setup outcomes, occurrence counts, side counts, and rates

strategy/
  trade interpretation for a setup, including entry decisions, exits, filters, and risk settings

engine/
  execution of event statistics and backtest simulation workflows

trade/
  order, fill, position lifecycle, trade plan, and trade result models

reporting/
  report models and performance metrics for UI display and export-oriented workflows
```

This structure lets FORGE ask both:

- “How often does this market event occur?”
- “When that event occurs, what happens if a strategy trades it?”

See the [diagram index](diagrams/index.md) for architecture, workflow, data import, research/backtest, object-oriented design, and full class model diagrams.

## Object-Oriented Design

- **Facade pattern:** Package-level facades such as `FacadeForgeApplication`, `FacadeForgeData`, `FacadeForgeGui`, `FacadeForgeRisk`, `FacadeForgeTrade`, and `FacadeForgeEngine` provide narrow entry points into larger subsystems.
- **Abstract class:** `Instrument` stores common instrument identity and requires subclasses to provide `getInstrumentType()`.
- **Inheritance:** `FuturesInstrument` and `FuturesContract` extend `Instrument`.
- **Interfaces:** `TradingStrategy`, `MarketEvent`, `ExecutionEngine`, and data source/store interfaces define interchangeable behavior.
- **Polymorphism:** Strategy, event, execution, and data-access implementations can be selected and evaluated through shared interfaces without depending on concrete classes.
- **Upcasting:** `InstrumentDataCatalog` creates `FuturesInstrument` entries from imported contract tables and stores them as `Instrument`.
- **Downcasting:** `InstrumentDataCatalog.AvailableInstrumentData` safely downcasts `Instrument` to `FuturesInstrument` when futures-specific tick details are needed.
- **Generics:** Utility/catalog classes such as `ImmutableLists` and `ClasspathCatalog` use generic type parameters to preserve compile-time type safety across reusable collection operations.
- **Serialization:** GUI import-path preferences are saved and loaded through `ObjectOutputStream`, `ObjectInputStream`, and `Serializable` preference models.

## Tick-Native Strategy Math

`MarketContext` carries both tick-native values and display-price accessors. Strategy logic should prefer `getLastPriceTicks()`, `getTickSize()`, and `getTickDollarValue()` for exact and efficient calculations, while `getLastPrice()` remains available for display-oriented code.

Exit calculations are tick-native as well. Strategies emit `TradePlan` values containing target and stop prices in ticks, and `TradeLifecycleEngine` evaluates target, stop, and time-stop exits directly in tick space.

## Strategy Requirements

Strategies can declare `StrategyRequirements`, which tell the engine which derived features, market event occurrences, trading sessions, and TPO periods are relevant. The engine uses those requirements to build required facts and to skip strategy evaluation outside the allowed session/TPO filters.

RTH TPO periods are 30-minute Central Time periods starting at `08:30`: `A` is `08:30-08:59`, `B` is `09:00-09:29`, `C` is `09:30-09:59`, and so on through the RTH session.

`OpeningRangeContinuationStrategy` uses derived Central Time session features and first-hour breach event occurrences. The feature layer calculates the overnight range from `17:00` through `08:29:59`, the RTH first-hour range from `08:30` through `09:29:59`, and the event layer detects the first breach after the first hour.

The strategy declares that it requires session ranges and first-hour breach event occurrences, and that entries should only evaluate during RTH TPO periods `C` and `D` (`09:30-10:29`). It only arms if the first-hour range remains inside the overnight range, allows one trade per day, and targets the overnight high/low with a first-hour opposite-side stop and `10:30` time stop.

## Execution

The MVP execution layer fills generated orders at the current tick price so the trade lifecycle can create completed simulated trades. Strategy profiles provide default event settings and describe whether event selection is user-configurable.
