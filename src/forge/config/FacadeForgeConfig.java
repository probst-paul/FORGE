package forge.config;

import forge.data.market.ContractTradeWindow;
import forge.trade.OrderType;

import java.time.LocalDate;
import java.util.List;

public class FacadeForgeConfig {
    private static final FacadeForgeConfig THE_INSTANCE = new FacadeForgeConfig();

    private final ForgeConfigAccess access = new ForgeConfigAccess();

    public static FacadeForgeConfig getTheInstance() {
        return THE_INSTANCE;
    }

    public ForgeConfigAccess forgeConfigAccess() {
        return access;
    }

    public static class ForgeConfigAccess {
        public BacktestRequest createBacktestRequest(
                String strategyName,
                List<String> instruments,
                LocalDate startDate,
                LocalDate endDate,
                String conditionName,
                RiskSettings riskSettings,
                TargetSettings targetSettings
        ) {
            return createBacktestRequest(
                    new StrategyOptions(strategyName),
                    instruments,
                    startDate,
                    endDate,
                    new MarketConditionOptions(conditionName),
                    riskSettings,
                    targetSettings,
                    defaultOrderSettings()
            );
        }

        public BacktestRequest createBacktestRequest(
                String strategyName,
                List<ContractTradeWindow> contractWindows,
                String conditionName,
                RiskSettings riskSettings,
                TargetSettings targetSettings
        ) {
            return createBacktestRequest(
                    new StrategyOptions(strategyName),
                    contractWindows,
                    new MarketConditionOptions(conditionName),
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
                MarketConditionOptions marketConditionOptions,
                RiskSettings riskSettings,
                TargetSettings targetSettings,
                OrderSettings orderSettings
        ) {
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
                MarketConditionOptions marketConditionOptions,
                RiskSettings riskSettings,
                TargetSettings targetSettings,
                OrderSettings orderSettings
        ) {
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
            return new OrderSettings(OrderType.MARKET, 1, 0, 0);
        }
    }
}
