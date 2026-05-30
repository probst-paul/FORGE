package forge.strategy;

public interface TradingStrategy {
    String getName();

    /*
     * Intent: Evaluate the strategy against the current market and derived-data context.
     * Precondition: strategyContext must contain the inputs required by getRequirements().
     * Returns: StrategyDecision describing no action, an order signal, or a complete trade.
     * Postcondition: Strategy implementations may update their own run-local state.
     */
    StrategyDecision evaluate(StrategyContext strategyContext);

    default StrategyRequirements getRequirements() {
        return StrategyRequirements.none();
    }

    /*
     * Intent: Let stateful strategies reset themselves before a backtest starts.
     * Precondition: Called before the first tick of a new backtest run.
     * Returns: Nothing.
     * Postcondition: Strategy-specific state from prior runs should be cleared by overrides.
     */
    default void onBacktestStart() {
        // Optional lifecycle hook for strategies that maintain state.
    }
}
