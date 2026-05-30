package forge.feature;

import forge.data.market.TradeTick;

import java.util.Collection;
import java.util.List;

public class FeatureBuildService {
    private final SessionRangeFeatureCalculator sessionRangeFeatureCalculator;

    public FeatureBuildService() {
        /*
         * Intent: Create the feature build service with the default session range calculator.
         * Precondition: Default feature dependencies must be available.
         * Returns: A constructed FeatureBuildService instance.
         * Postcondition: Service can calculate supported derived features.
         */
        this(new SessionRangeFeatureCalculator());
    }

    public FeatureBuildService(SessionRangeFeatureCalculator sessionRangeFeatureCalculator) {
        /*
         * Intent: Create the feature build service with an explicit session range calculator.
         * Precondition: Calculator must not be null.
         * Returns: A constructed FeatureBuildService instance.
         * Postcondition: Session range feature work delegates to the supplied calculator.
         */
        if (sessionRangeFeatureCalculator == null) {
            throw new IllegalArgumentException("sessionRangeFeatureCalculator is required");
        }
        this.sessionRangeFeatureCalculator = sessionRangeFeatureCalculator;
    }

    public List<String> getSupportedFeatureNames() {
        /*
         * Intent: Expose feature result types that can currently be built.
         * Precondition: None.
         * Returns: Immutable list of supported feature names.
         * Postcondition: Service state is unchanged.
         */
        return List.of(SessionRangeFeature.FEATURE_NAME);
    }

    public List<SessionRangeFeature> calculateSessionRanges(Collection<TradeTick> ticks) {
        /*
         * Intent: Calculate session range features from an in-memory tick collection.
         * Precondition: Tick collection must be non-null.
         * Returns: Complete session range features.
         * Postcondition: Source tick collection is not modified.
         */
        return sessionRangeFeatureCalculator.calculate(ticks);
    }

    public SessionRangeFeatureCalculator.Accumulator newSessionRangeAccumulator() {
        /*
         * Intent: Create a streaming accumulator for session range features.
         * Precondition: None.
         * Returns: SessionRangeFeatureCalculator accumulator.
         * Postcondition: No ticks have been consumed yet.
         */
        return sessionRangeFeatureCalculator.newAccumulator();
    }
}
