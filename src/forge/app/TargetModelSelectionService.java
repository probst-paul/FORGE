package forge.app;

import forge.config.TargetSettings;
import forge.target.TargetModel;
import forge.target.TargetModelCatalog;

import java.util.List;

public class TargetModelSelectionService {
    private final TargetModelCatalog targetModelCatalog;

    public TargetModelSelectionService(TargetModelCatalog targetModelCatalog) {
        this.targetModelCatalog = targetModelCatalog;
    }

    public Class<? extends TargetModel> selectTargetModel(UserInput input, UserOutput output) {
        List<Class<? extends TargetModel>> targetModels = targetModelCatalog.findAvailableTargetModels();
        if (targetModels.isEmpty()) {
            throw new IllegalStateException("No target models are available");
        }

        output.printLine("Available target models:");
        for (int i = 0; i < targetModels.size(); i++) {
            output.printLine((i + 1) + ". " + targetModelCatalog.getDisplayName(targetModels.get(i)));
        }

        int selectedIndex = input.readInt("Select target model") - 1;
        if (selectedIndex < 0 || selectedIndex >= targetModels.size()) {
            throw new IllegalArgumentException("Selected target model is not available");
        }
        return targetModels.get(selectedIndex);
    }

    public TargetSettings readTargetModelSettings(UserInput input, Class<? extends TargetModel> targetModel) {
        String targetModelName = getDisplayName(targetModel);
        if ("Fixed Risk/Reward".equals(targetModelName)) {
            double rewardRiskRatio = input.readDouble("Reward/risk ratio");
            return TargetSettings.fixedRiskReward(targetModelName, rewardRiskRatio);
        }

        int profitTargetTicks = input.readInt("Profit target ticks");
        return TargetSettings.fixedTarget(targetModelName, profitTargetTicks);
    }

    public String getDisplayName(Class<? extends TargetModel> targetModel) {
        return targetModelCatalog.getDisplayName(targetModel);
    }
}
