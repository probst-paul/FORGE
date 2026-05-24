package forge.condition;

import forge.data.market.TradeTick;
import forge.feature.SessionRangeFeature;
import forge.feature.SessionRangeFeatureCalculator;

import java.util.Collection;
import java.util.List;

public class ConditionBuildService {
    private final FirstHourBreachConditionDetector firstHourBreachConditionDetector;

    public ConditionBuildService() {
        this(new FirstHourBreachConditionDetector());
    }

    public ConditionBuildService(FirstHourBreachConditionDetector firstHourBreachConditionDetector) {
        if (firstHourBreachConditionDetector == null) {
            throw new IllegalArgumentException("firstHourBreachConditionDetector is required");
        }
        this.firstHourBreachConditionDetector = firstHourBreachConditionDetector;
    }

    public List<String> getSupportedConditionNames() {
        return List.of(FirstHourBreachCondition.EVENT_NAME);
    }

    public List<MarketConditionOccurrence> detectFirstHourBreachConditions(
            Collection<SessionRangeFeature> sessionRangeFeatures,
            Collection<TradeTick> ticks
    ) {
        return firstHourBreachConditionDetector.detect(sessionRangeFeatures, ticks);
    }

    public FirstHourBreachConditionDetector.Accumulator newFirstHourBreachAccumulator(
            Collection<SessionRangeFeature> sessionRangeFeatures
    ) {
        return firstHourBreachConditionDetector.newAccumulator(sessionRangeFeatures);
    }

    public FirstHourBreachConditionDetector.LiveAccumulator newLiveFirstHourBreachAccumulator(
            SessionRangeFeatureCalculator.Accumulator sessionRangeAccumulator
    ) {
        return firstHourBreachConditionDetector.newLiveAccumulator(sessionRangeAccumulator);
    }
}
