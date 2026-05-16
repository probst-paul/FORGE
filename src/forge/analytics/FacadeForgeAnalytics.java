package forge.analytics;

public class FacadeForgeAnalytics {
    private static final FacadeForgeAnalytics THE_INSTANCE = new FacadeForgeAnalytics();

    private final ForgeAnalyticsAccess access = new ForgeAnalyticsAccess();

    public static FacadeForgeAnalytics getTheInstance() {
        return THE_INSTANCE;
    }

    public ForgeAnalyticsAccess forgeAnalyticsAccess() {
        return access;
    }

    public static class ForgeAnalyticsAccess {
        public FeatureSet createFeatureSet() {
            return new FeatureSet();
        }

        public MarketFeature createMarketFeature() {
            return new MarketFeature();
        }
    }
}
