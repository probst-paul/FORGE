package forge.feature;

import forge.data.market.TradeTick;

import java.util.Collection;
import java.util.List;

public class FeatureBuildService {
    private final SessionRangeFeatureCalculator sessionRangeFeatureCalculator;

    public FeatureBuildService() {
        this(new SessionRangeFeatureCalculator());
    }

    public FeatureBuildService(SessionRangeFeatureCalculator sessionRangeFeatureCalculator) {
        if (sessionRangeFeatureCalculator == null) {
            throw new IllegalArgumentException("sessionRangeFeatureCalculator is required");
        }
        this.sessionRangeFeatureCalculator = sessionRangeFeatureCalculator;
    }

    public List<String> getSupportedFeatureNames() {
        return List.of(SessionRangeFeature.FEATURE_NAME);
    }

    public List<SessionRangeFeature> calculateSessionRanges(Collection<TradeTick> ticks) {
        return sessionRangeFeatureCalculator.calculate(ticks);
    }

    public SessionRangeFeatureCalculator.Accumulator newSessionRangeAccumulator() {
        return sessionRangeFeatureCalculator.newAccumulator();
    }
}
