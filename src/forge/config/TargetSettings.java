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
        /*
         * Intent: Store target configuration in either fixed risk/reward or fixed target-ticks mode.
         * Precondition: Target mode must be named and exactly enough positive target parameters must be supplied.
         * Returns: A constructed TargetSettings instance.
         * Postcondition: Target settings are immutable and validated for strategy use.
         */
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
        /*
         * Intent: Create target settings for a named risk/reward target mode.
         * Precondition: Target mode must be nonblank and reward/risk ratio must be positive.
         * Returns: TargetSettings configured with a reward/risk ratio.
         * Postcondition: Profit-target ticks are intentionally absent.
         */
        return new TargetSettings(targetMode, rewardRiskRatio, null);
    }

    public static TargetSettings fixedRiskReward(double rewardRiskRatio) {
        /*
         * Intent: Create default fixed risk/reward target settings.
         * Precondition: Reward/risk ratio must be positive.
         * Returns: TargetSettings using the standard fixed risk/reward mode name.
         * Postcondition: Profit-target ticks are intentionally absent.
         */
        return fixedRiskReward(FIXED_RISK_REWARD, rewardRiskRatio);
    }

    public static TargetSettings fixedTarget(String targetMode, int profitTargetTicks) {
        /*
         * Intent: Create target settings for a named fixed target-ticks mode.
         * Precondition: Target mode must be nonblank and target ticks must be positive.
         * Returns: TargetSettings configured with fixed profit target ticks.
         * Postcondition: Reward/risk ratio is intentionally absent.
         */
        if (profitTargetTicks <= 0) {
            throw new IllegalArgumentException("profitTargetTicks must be greater than zero");
        }
        return new TargetSettings(targetMode, null, profitTargetTicks);
    }

    public static TargetSettings fixedTarget(int profitTargetTicks) {
        /*
         * Intent: Create default fixed target-ticks settings.
         * Precondition: Target ticks must be positive.
         * Returns: TargetSettings using the standard fixed target mode name.
         * Postcondition: Reward/risk ratio is intentionally absent.
         */
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
        /*
         * Intent: Provide a readable diagnostic summary of target settings.
         * Precondition: TargetSettings must be constructed.
         * Returns: String representation of target settings.
         * Postcondition: TargetSettings state is unchanged.
         */
        return "TargetSettings{" +
                "targetMode='" + targetMode + '\'' +
                ", rewardRiskRatio=" + rewardRiskRatio +
                ", profitTargetTicks=" + profitTargetTicks +
                '}';
    }
}
