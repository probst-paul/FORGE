# FORGE

**Futures Order Replay and Generalized Execution Engine**

FORGE is an early-stage Java futures backtesting project. The current code focuses on the setup/configuration model, futures contract modeling, strategy and target-model abstractions, and unit-tested behavior for the implemented classes.

It is not yet a complete historical market replay or backtesting engine.

## Currently Implemented

- Command-line backtest setup flow in `forge.app.Main`
- In-memory instrument/date catalog with sample futures instruments
- Futures contract model with symbol code, tick size, tick dollar amount, and expiration date
- Abstract `Instrument` base class and concrete `FuturesContract`
- Strategy interface with an implemented `RangeBreakoutStrategy`
- Trade trigger interface with an implemented no-op `OrderFlowExhaustionTrigger`
- Target model interface with:
  - `FixedRiskRewardTarget`
  - `FixedTarget`
- Basic `OrderRequest` modeling
- JUnit 5 tests for implemented behavior
- Mermaid class and sequence diagrams:
  - `class-model.md`
  - `setup-sequence.md`

## Current CLI Flow

```text
Select Instrument(s)
→ Select Date Range
→ Select Trading Strategy
→ Risk Settings
→ Select Trade Trigger
→ Select Target Model
→ Target Model Options
→ Build BacktestRequest
```

Order settings are currently defaulted internally and are not exposed in the CLI.

## Not Yet Implemented

- Real market data retrieval
- Persistent historical data storage
- Contract rollover handling
- Derived market analytics
- Full trade trigger evaluation against market data
- Backtest engine orchestration
- Order execution simulation
- Completed trade result calculation
- Performance reporting

## Project Structure

```text
src/forge/app        CLI entry point
src/forge/config     Backtest configuration objects
src/forge/data       Temporary in-memory instrument/date catalog
src/forge/engine     Market context
src/forge/execution  Order request and order enums
src/forge/model      Instrument and futures contract models
src/forge/strategy   Strategy interface, catalog, and range breakout strategy
src/forge/target     Target model interface, implementations, and results
src/forge/trigger    Trigger interface, catalog, and trigger result model
test/forge           JUnit 5 tests
```

## Object-Oriented Design

- **Abstract class:** `Instrument` stores common instrument identity and requires subclasses to provide `getInstrumentType()`.
- **Inheritance:** `FuturesContract` extends `Instrument`.
- **Interfaces:** `TradingStrategy`, `TradeTrigger`, and `TargetModel` define interchangeable behavior.
- **Polymorphism:** `FixedRiskRewardTarget` and `FixedTarget` both implement `TargetModel.calculateTarget(...)` with different behavior.
- **Upcasting:** `InstrumentDataCatalog` creates `FuturesContract` instances and stores them as `Instrument`.
- **Downcasting:** `InstrumentDataCatalog.AvailableInstrumentData` safely downcasts `Instrument` to `FuturesContract` when futures-specific details are needed.

## Run the Application

From the project root:

```bash
javac -d out $(find src -name '*.java')
java -cp out forge.app.Main
```

## Run Tests

The project uses JUnit 5. If the standalone JUnit runner is not already available, download it:

```bash
curl -L -o /private/tmp/junit-platform-console-standalone-1.10.2.jar https://repo1.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/1.10.2/junit-platform-console-standalone-1.10.2.jar
```

Then run:

```bash
javac -d out $(find src -name '*.java')
javac -cp out:/private/tmp/junit-platform-console-standalone-1.10.2.jar -d out/test $(find test -name '*.java')
java -jar /private/tmp/junit-platform-console-standalone-1.10.2.jar execute --class-path out:out/test --scan-class-path
```

## Status

FORGE is in early architectural development. The current implementation is intentionally small and focuses on clean object modeling, selection/configuration flow, and tested domain behavior before adding market data storage, replay, execution, and reporting.
