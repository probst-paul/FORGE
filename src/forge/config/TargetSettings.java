package forge.config;

public class TargetSettings {
    private final String targetModel;
    private final Double rewardRiskRatio;
    private final Integer profitTargetTicks;

    public TargetSettings(
            String targetModel,
            Double rewardRiskRatio,
            Integer profitTargetTicks
    ) {
        if (targetModel == null || targetModel.trim().isEmpty()) {
            throw new IllegalArgumentException("targetModel is required");
        }
        if (rewardRiskRatio != null && rewardRiskRatio <= 0) {
            throw new IllegalArgumentException("rewardRiskRatio must be greater than zero");
        }
        if (profitTargetTicks != null && profitTargetTicks <= 0) {
            throw new IllegalArgumentException("profitTargetTicks must be greater than zero");
        }
        if (rewardRiskRatio == null && profitTargetTicks == null) {
            throw new IllegalArgumentException("target model options are required");
        }

        this.targetModel = targetModel.trim();
        this.rewardRiskRatio = rewardRiskRatio;
        this.profitTargetTicks = profitTargetTicks;
    }

    public static TargetSettings fixedRiskReward(String targetModel, double rewardRiskRatio) {
        return new TargetSettings(targetModel, rewardRiskRatio, null);
    }

    public static TargetSettings fixedTarget(String targetModel, int profitTargetTicks) {
        if (profitTargetTicks <= 0) {
            throw new IllegalArgumentException("profitTargetTicks must be greater than zero");
        }
        return new TargetSettings(targetModel, null, profitTargetTicks);
    }

    public String getTargetModel() {
        return targetModel;
    }

    public Double getRewardRiskRatio() {
        return rewardRiskRatio;
    }

    public Integer getProfitTargetTicks() {
        return profitTargetTicks;
    }

    @Override
    public String toString() {
        return "TargetSettings{" +
                "targetModel='" + targetModel + '\'' +
                ", rewardRiskRatio=" + rewardRiskRatio +
                ", profitTargetTicks=" + profitTargetTicks +
                '}';
    }
}
