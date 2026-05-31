package forge.config;

import forge.data.market.ContractTradeWindow;
import forge.trade.OrderType;

import java.time.LocalDate;
import java.util.List;

public class FacadeForgeConfig {
    private static final FacadeForgeConfig THE_INSTANCE = new FacadeForgeConfig();

    private final ForgeConfigAccess access = new ForgeConfigAccess();

    public static FacadeForgeConfig getTheInstance() {
        /*
         * Intent: Provide the shared config facade used by application and CLI wiring.
         * Precondition: Static facade instance must have initialized successfully.
         * Returns: Singleton FacadeForgeConfig instance.
         * Postcondition: No new facade is created.
         */
        return THE_INSTANCE;
    }

    public ForgeConfigAccess forgeConfigAccess() {
        /*
         * Intent: Expose the public access object for config package operations.
         * Precondition: Facade must be constructed.
         * Returns: Stable ForgeConfigAccess instance.
         * Postcondition: Facade state is unchanged.
         */
        return access;
    }

    public static class ForgeConfigAccess {
        public BacktestRequest createBacktestRequest(
                String strategyName,
                List<String> instruments,
                LocalDate startDate,
                LocalDate endDate,
                String eventName,
                RiskSettings riskSettings,
                TargetSettings targetSettings
        ) {
            /*
             * Intent: Build a simple backtest request from primitive CLI-style selections.
             * Precondition: Strategy name, symbols, dates, condition name, risk, and target settings must be valid.
             * Returns: BacktestRequest using default order settings.
             * Postcondition: Inputs are wrapped into config value objects.
             */
            return createBacktestRequest(
                    new StrategyOptions(strategyName),
                    instruments,
                    startDate,
                    endDate,
                    new MarketEventOptions(eventName),
                    riskSettings,
                    targetSettings,
                    defaultOrderSettings()
            );
        }

        public BacktestRequest createBacktestRequest(
                String strategyName,
                List<ContractTradeWindow> contractWindows,
                String eventName,
                RiskSettings riskSettings,
                TargetSettings targetSettings
        ) {
            /*
             * Intent: Build a backtest request from selected contract windows and simple strategy/condition names.
             * Precondition: Strategy name, contract windows, condition name, risk, and target settings must be valid.
             * Returns: BacktestRequest using default order settings.
             * Postcondition: Names are wrapped into config value objects.
             */
            return createBacktestRequest(
                    new StrategyOptions(strategyName),
                    contractWindows,
                    new MarketEventOptions(eventName),
                    riskSettings,
                    targetSettings,
                    defaultOrderSettings()
            );
        }

        public BacktestRequest createBacktestRequest(
                StrategyOptions strategyOptions,
                List<String> instruments,
                LocalDate startDate,
                LocalDate endDate,
                MarketEventOptions marketConditionOptions,
                RiskSettings riskSettings,
                TargetSettings targetSettings,
                OrderSettings orderSettings
        ) {
            /*
             * Intent: Build a backtest request using explicit legacy instrument/date inputs and config objects.
             * Precondition: All config objects and legacy selection inputs must satisfy BacktestRequest validation.
             * Returns: BacktestRequest.
             * Postcondition: Request construction and validation are delegated to BacktestRequest.
             */
            return new BacktestRequest(
                    strategyOptions,
                    instruments,
                    startDate,
                    endDate,
                    marketConditionOptions,
                    riskSettings,
                    targetSettings,
                    orderSettings
            );
        }

        public BacktestRequest createBacktestRequest(
                StrategyOptions strategyOptions,
                List<ContractTradeWindow> contractWindows,
                MarketEventOptions marketConditionOptions,
                RiskSettings riskSettings,
                TargetSettings targetSettings,
                OrderSettings orderSettings
        ) {
            /*
             * Intent: Build the canonical backtest request from explicit contract windows and config objects.
             * Precondition: All inputs must satisfy BacktestRequest validation.
             * Returns: BacktestRequest.
             * Postcondition: Request construction and validation are delegated to BacktestRequest.
             */
            return new BacktestRequest(
                    strategyOptions,
                    contractWindows,
                    marketConditionOptions,
                    riskSettings,
                    targetSettings,
                    orderSettings
            );
        }

        public OrderSettings defaultOrderSettings() {
            /*
             * Intent: Provide MVP default order settings for request assembly.
             * Precondition: None.
             * Returns: Market-order settings with quantity one and no offsets.
             * Postcondition: A new immutable OrderSettings value is returned.
             */
            return new OrderSettings(OrderType.MARKET, 1, 0, 0);
        }
    }
}
