package forge.cli;

import forge.app.UserInput;
import forge.app.UserOutput;
import forge.config.TargetSettings;
import forge.target.FacadeForgeTarget;
import forge.target.TargetModel;

import java.util.List;

public class TargetModelSelectionService {
    private final FacadeForgeTarget facadeTarget;

    public TargetModelSelectionService(FacadeForgeTarget facadeTarget) {
        this.facadeTarget = facadeTarget;
    }

    public Class<? extends TargetModel> selectTargetModel(UserInput input, UserOutput output) {
        List<Class<? extends TargetModel>> targetModels = facadeTarget.forgeTargetAccess().findAvailableTargetModels();
        if (targetModels.isEmpty()) {
            throw new IllegalStateException("No target models are available");
        }

        output.printLine("Available target models:");
        for (int i = 0; i < targetModels.size(); i++) {
            output.printLine((i + 1) + ". " + facadeTarget.forgeTargetAccess().getDisplayName(targetModels.get(i)));
        }

        while (true) {
            int selectedIndex = input.readInt("Select target model") - 1;
            if (selectedIndex >= 0 && selectedIndex < targetModels.size()) {
                return targetModels.get(selectedIndex);
            }
            output.printLine("Selected target model is not available. Please select an available target model, or enter 'quit' to exit program.");
        }
    }

    public TargetSettings readTargetModelSettings(UserInput input, UserOutput output, Class<? extends TargetModel> targetModel) {
        String targetModelName = getDisplayName(targetModel);
        while (true) {
            try {
                if ("Fixed Risk/Reward".equals(targetModelName)) {
                    double rewardRiskRatio = input.readDouble("Reward/risk ratio");
                    return facadeTarget.forgeTargetAccess().createFixedRiskRewardSettings(targetModel, rewardRiskRatio);
                }

                int profitTargetTicks = input.readInt("Profit target ticks");
                return facadeTarget.forgeTargetAccess().createFixedTargetSettings(targetModel, profitTargetTicks);
            } catch (IllegalArgumentException exception) {
                output.printLine(exception.getMessage() + ". Please enter valid target settings, or enter 'quit' to exit program.");
            }
        }
    }

    public String getDisplayName(Class<? extends TargetModel> targetModel) {
        return facadeTarget.forgeTargetAccess().getDisplayName(targetModel);
    }
}
