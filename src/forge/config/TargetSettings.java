package forge.config;

public class TargetSettings {
    public static final String FIXED_RISK_REWARD = "Fixed Risk/Reward";
    public static final String FIXED_TARGET = "Target";

    private final String targetMode;
    private final Double rewardRiskRatio;
    private final Integer profitTargetTicks;

    public TargetSettings(
            String targetMode,
            Double rewardRiskRatio,
            Integer profitTargetTicks
    ) {
        if (targetMode == null || targetMode.trim().isEmpty()) {
            throw new IllegalArgumentException("targetMode is required");
        }
        if (rewardRiskRatio != null && rewardRiskRatio <= 0) {
            throw new IllegalArgumentException("rewardRiskRatio must be greater than zero");
        }
        if (profitTargetTicks != null && profitTargetTicks <= 0) {
            throw new IllegalArgumentException("profitTargetTicks must be greater than zero");
        }
        if (rewardRiskRatio == null && profitTargetTicks == null) {
            throw new IllegalArgumentException("target mode options are required");
        }

        this.targetMode = targetMode.trim();
        this.rewardRiskRatio = rewardRiskRatio;
        this.profitTargetTicks = profitTargetTicks;
    }

    public static TargetSettings fixedRiskReward(String targetMode, double rewardRiskRatio) {
        return new TargetSettings(targetMode, rewardRiskRatio, null);
    }

    public static TargetSettings fixedRiskReward(double rewardRiskRatio) {
        return fixedRiskReward(FIXED_RISK_REWARD, rewardRiskRatio);
    }

    public static TargetSettings fixedTarget(String targetMode, int profitTargetTicks) {
        if (profitTargetTicks <= 0) {
            throw new IllegalArgumentException("profitTargetTicks must be greater than zero");
        }
        return new TargetSettings(targetMode, null, profitTargetTicks);
    }

    public static TargetSettings fixedTarget(int profitTargetTicks) {
        return fixedTarget(FIXED_TARGET, profitTargetTicks);
    }

    public String getTargetMode() {
        return targetMode;
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
                "targetMode='" + targetMode + '\'' +
                ", rewardRiskRatio=" + rewardRiskRatio +
                ", profitTargetTicks=" + profitTargetTicks +
                '}';
    }
}
