package forge.strategy;

public interface TradingStrategy {
    String getName();

    StrategyDecision evaluate(StrategyContext strategyContext);

    default StrategyRequirements getRequirements() {
        return StrategyRequirements.none();
    }

    default void onBacktestStart() {
        // Optional lifecycle hook for strategies that maintain state.
    }
}
