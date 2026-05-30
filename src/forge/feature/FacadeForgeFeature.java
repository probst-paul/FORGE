package forge.feature;

import forge.data.market.TradeTick;

import java.util.Collection;
import java.util.List;

public class FacadeForgeFeature {
    private static final FacadeForgeFeature THE_INSTANCE = new FacadeForgeFeature();

    private final FeatureBuildService featureBuildService;
    private final ForgeFeatureAccess access = new ForgeFeatureAccess();

    public static FacadeForgeFeature getTheInstance() {
        /*
         * Intent: Provide the shared feature facade used by other layers.
         * Precondition: Static facade instance must have initialized successfully.
         * Returns: Singleton FacadeForgeFeature instance.
         * Postcondition: No new facade is created.
         */
        return THE_INSTANCE;
    }

    public FacadeForgeFeature() {
        /*
         * Intent: Create the feature facade with the default build service.
         * Precondition: Default feature build service dependencies must be available.
         * Returns: A constructed FacadeForgeFeature instance.
         * Postcondition: Facade can expose supported feature operations.
         */
        this(new FeatureBuildService());
    }

    public FacadeForgeFeature(FeatureBuildService featureBuildService) {
        /*
         * Intent: Create the feature facade with an explicit build service.
         * Precondition: Feature build service must not be null.
         * Returns: A constructed FacadeForgeFeature instance.
         * Postcondition: Facade delegates feature work to the supplied service.
         */
        if (featureBuildService == null) {
            throw new IllegalArgumentException("featureBuildService is required");
        }
        this.featureBuildService = featureBuildService;
    }

    public ForgeFeatureAccess forgeFeatureAccess() {
        /*
         * Intent: Expose the public access object for feature package operations.
         * Precondition: Facade must be constructed.
         * Returns: Stable ForgeFeatureAccess instance.
         * Postcondition: Facade state is unchanged.
         */
        return access;
    }

    public class ForgeFeatureAccess {
        public List<String> getSupportedFeatureNames() {
            /*
             * Intent: List feature types currently supported by the feature package.
             * Precondition: Build service must be configured.
             * Returns: Supported feature names.
             * Postcondition: No feature calculations are run.
             */
            return featureBuildService.getSupportedFeatureNames();
        }

        public List<SessionRangeFeature> calculateSessionRanges(Collection<TradeTick> ticks) {
            /*
             * Intent: Calculate session range features through the feature facade.
             * Precondition: Tick collection must be non-null.
             * Returns: Complete session range features.
             * Postcondition: Source tick collection is not modified.
             */
            return featureBuildService.calculateSessionRanges(ticks);
        }
    }
}
