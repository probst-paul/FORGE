package forge.feature;

import forge.data.market.TradeTick;

import java.util.Collection;
import java.util.List;

public class FacadeForgeFeature {
    private static final FacadeForgeFeature THE_INSTANCE = new FacadeForgeFeature();

    private final FeatureBuildService featureBuildService;
    private final ForgeFeatureAccess access = new ForgeFeatureAccess();

    public static FacadeForgeFeature getTheInstance() {
        return THE_INSTANCE;
    }

    public FacadeForgeFeature() {
        this(new FeatureBuildService());
    }

    public FacadeForgeFeature(FeatureBuildService featureBuildService) {
        if (featureBuildService == null) {
            throw new IllegalArgumentException("featureBuildService is required");
        }
        this.featureBuildService = featureBuildService;
    }

    public ForgeFeatureAccess forgeFeatureAccess() {
        return access;
    }

    public class ForgeFeatureAccess {
        public List<String> getSupportedFeatureNames() {
            return featureBuildService.getSupportedFeatureNames();
        }

        public List<SessionRangeFeature> calculateSessionRanges(Collection<TradeTick> ticks) {
            return featureBuildService.calculateSessionRanges(ticks);
        }
    }
}
