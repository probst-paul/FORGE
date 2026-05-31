package forge.risk;

import forge.config.RiskSettings;

public class FacadeForgeRisk {
    private static final FacadeForgeRisk THE_INSTANCE = new FacadeForgeRisk();

    private final ForgeRiskAccess access = new ForgeRiskAccess();

    public static FacadeForgeRisk getTheInstance() {
        /*
         * Intent: Provide the shared facade for risk package operations.
         * Precondition: Static facade instance must have initialized successfully.
         * Returns: Singleton FacadeForgeRisk instance.
         * Postcondition: No new facade is created.
         */
        return THE_INSTANCE;
    }

    public ForgeRiskAccess forgeRiskAccess() {
        /*
         * Intent: Expose the public access object for risk package operations.
         * Precondition: Facade must be constructed.
         * Returns: Stable ForgeRiskAccess instance.
         * Postcondition: Facade state is unchanged.
         */
        return access;
    }

    public static class ForgeRiskAccess {
        /*
         * Intent: Create a fresh risk manager for one backtest/run scope.
         * Precondition: Risk settings must be valid.
         * Returns: New RiskManager with isolated per-run daily state.
         * Postcondition: Callers do not depend directly on RiskManager construction.
         */
        public RiskManager createRiskManager(RiskSettings riskSettings) {
            return new RiskManager(riskSettings);
        }

        /*
         * Intent: Create a no-action risk decision through the risk facade.
         * Precondition: None.
         * Returns: RiskDecision that does not close an open trade.
         * Postcondition: No state is changed.
         */
        public RiskDecision holdDecision() {
            return RiskDecision.hold();
        }

        /*
         * Intent: Create a close-trade risk decision through the risk facade.
         * Precondition: Exit reason must be nonblank.
         * Returns: RiskDecision that requests immediate trade close.
         * Postcondition: No state is changed.
         */
        public RiskDecision closeTradeDecision(String exitReason) {
            return RiskDecision.closeTrade(exitReason);
        }
    }
}
