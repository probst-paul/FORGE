package forge.target;

import forge.config.TargetSettings;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class FacadeForgeTarget {
    private static final FacadeForgeTarget THE_INSTANCE = new FacadeForgeTarget();

    private final TargetModelCatalog targetModelCatalog;
    private final ForgeTargetAccess access = new ForgeTargetAccess();

    public static FacadeForgeTarget getTheInstance() {
        return THE_INSTANCE;
    }

    public FacadeForgeTarget() {
        this(new TargetModelCatalog());
    }

    public FacadeForgeTarget(TargetModelCatalog targetModelCatalog) {
        this.targetModelCatalog = targetModelCatalog;
    }

    public ForgeTargetAccess forgeTargetAccess() {
        return access;
    }

    public class ForgeTargetAccess {
        public List<Class<? extends TargetModel>> findAvailableTargetModels() {
            return targetModelCatalog.findAvailableTargetModels();
        }

        public String getDisplayName(Class<? extends TargetModel> targetModel) {
            return targetModelCatalog.getDisplayName(targetModel);
        }

        public TargetSettings createFixedRiskRewardSettings(
                Class<? extends TargetModel> targetModel,
                double rewardRiskRatio
        ) {
            return TargetSettings.fixedRiskReward(getDisplayName(targetModel), rewardRiskRatio);
        }

        public TargetSettings createFixedTargetSettings(
                Class<? extends TargetModel> targetModel,
                int profitTargetTicks
        ) {
            return TargetSettings.fixedTarget(getDisplayName(targetModel), profitTargetTicks);
        }

        public TargetModel createTargetModel(Class<? extends TargetModel> targetModel) {
            try {
                return targetModel.getDeclaredConstructor().newInstance();
            } catch (InstantiationException
                     | IllegalAccessException
                     | InvocationTargetException
                     | NoSuchMethodException e) {
                throw new IllegalStateException("Unable to create target model " + targetModel.getSimpleName(), e);
            }
        }
    }
}
