package forge.risk;

public class RiskDecision {
    private final boolean closeTrade;
    private final String exitReason;

    private RiskDecision(boolean closeTrade, String exitReason) {
        this.closeTrade = closeTrade;
        this.exitReason = exitReason;
    }

    public static RiskDecision hold() {
        return new RiskDecision(false, null);
    }

    public static RiskDecision closeTrade(String exitReason) {
        if (exitReason == null || exitReason.trim().isEmpty()) {
            throw new IllegalArgumentException("exitReason is required");
        }
        return new RiskDecision(true, exitReason.trim().toUpperCase());
    }

    public boolean shouldCloseTrade() {
        return closeTrade;
    }

    public String getExitReason() {
        return exitReason;
    }
}
