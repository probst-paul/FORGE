package forge.cli;

import forge.app.UserInput;
import forge.app.UserOutput;
import forge.config.TargetSettings;
import forge.strategy.StrategyConfigurationProfile;

import java.util.List;

public class TargetSettingsSelectionService {
    /*
     * Intent: Select a target mode while honoring the selected strategy's configuration profile.
     * Precondition: Strategy profile must define default and allowed target modes.
     * Returns: Default target mode when selection is locked, otherwise the user's selected allowed mode.
     * Postcondition: User cannot select a target mode outside the strategy profile.
     */
    public String selectTargetMode(
            UserInput input,
            UserOutput output,
            StrategyConfigurationProfile strategyProfile
    ) {
        List<String> targetModes = strategyProfile.getAllowedTargets();
        if (!strategyProfile.isTargetSelectionAllowed()) {
            String targetMode = strategyProfile.getDefaultTarget();
            output.printLine("Using target mode: " + getDisplayName(targetMode));
            return targetMode;
        }

        output.printLine("Available target modes:");
        for (int i = 0; i < targetModes.size(); i++) {
            String targetMode = targetModes.get(i);
            String defaultMarker = targetMode.equals(strategyProfile.getDefaultTarget()) ? " (default)" : "";
            output.printLine((i + 1) + ". " + getDisplayName(targetMode) + defaultMarker);
        }

        while (true) {
            int selectedIndex = input.readInt("Select target mode") - 1;
            if (selectedIndex >= 0 && selectedIndex < targetModes.size()) {
                return targetModes.get(selectedIndex);
            }
            output.printLine("Selected target mode is not available for this strategy. Please select an available target mode, or enter 'quit' to exit program.");
        }
    }

    /*
     * Intent: Read CLI target settings for the selected target mode, using strategy defaults where possible.
     * Precondition: Target mode must be supported by the strategy profile.
     * Returns: TargetSettings for fixed target or fixed risk/reward mode.
     * Postcondition: Invalid numeric settings are rejected and reprompted without changing data state.
     */
    public TargetSettings readTargetSettings(
            UserInput input,
            UserOutput output,
            String targetMode,
            StrategyConfigurationProfile strategyProfile
    ) {
        TargetSettings defaults = strategyProfile.getDefaultTargetSettings(targetMode);
        if (!strategyProfile.isTargetSelectionAllowed()) {
            return defaults;
        }
        String targetModeName = getDisplayName(targetMode);
        while (true) {
            try {
                if (TargetSettings.FIXED_RISK_REWARD.equals(targetModeName)) {
                    double defaultRewardRiskRatio = defaults.getRewardRiskRatio() == null ? 2.0 : defaults.getRewardRiskRatio();
                    double rewardRiskRatio = input.readDoubleOrDefault(
                            "Reward/risk ratio [" + defaultRewardRiskRatio + "]",
                            defaultRewardRiskRatio
                    );
                    return TargetSettings.fixedRiskReward(rewardRiskRatio);
                }

                int defaultProfitTargetTicks = defaults.getProfitTargetTicks() == null ? 8 : defaults.getProfitTargetTicks();
                int profitTargetTicks = input.readIntOrDefault(
                        "Profit target ticks [" + defaultProfitTargetTicks + "]",
                        defaultProfitTargetTicks
                );
                return TargetSettings.fixedTarget(profitTargetTicks);
            } catch (IllegalArgumentException exception) {
                output.printLine(exception.getMessage() + ". Please enter valid target settings, or enter 'quit' to exit program.");
            }
        }
    }

    public String getDisplayName(String targetMode) {
        return targetMode;
    }
}
