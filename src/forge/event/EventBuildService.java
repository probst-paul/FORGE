package forge.event;

import forge.data.market.TradeTick;
import forge.feature.SessionRangeFeature;
import forge.feature.SessionRangeFeatureCalculator;

import java.util.Collection;
import java.util.List;

public class EventBuildService {
    private final FirstHourBreachEventDetector firstHourBreachEventDetector;

    public EventBuildService() {
        /*
         * Intent: Create the condition build service with the default first-hour breach detector.
         * Precondition: Default detector dependencies must be available.
         * Returns: A constructed EventBuildService instance.
         * Postcondition: Service can build supported market event occurrences.
         */
        this(new FirstHourBreachEventDetector());
    }

    public EventBuildService(FirstHourBreachEventDetector firstHourBreachEventDetector) {
        /*
         * Intent: Create the condition build service with an explicit detector dependency.
         * Precondition: Detector must not be null.
         * Returns: A constructed EventBuildService instance.
         * Postcondition: Future first-hour breach work is delegated to the supplied detector.
         */
        if (firstHourBreachEventDetector == null) {
            throw new IllegalArgumentException("firstHourBreachEventDetector is required");
        }
        this.firstHourBreachEventDetector = firstHourBreachEventDetector;
    }

    public List<String> getSupportedEventNames() {
        /*
         * Intent: Expose event occurrence types that can be built into derived data.
         * Precondition: None.
         * Returns: Immutable list of supported event names.
         * Postcondition: Service state is unchanged.
         */
        return List.of(FirstHourBreachEvent.EVENT_NAME);
    }

    public List<MarketEventOccurrence> detectFirstHourBreachEvents(
            Collection<SessionRangeFeature> sessionRangeFeatures,
            Collection<TradeTick> ticks
    ) {
        /*
         * Intent: Detect first-hour breaches from existing session range features and trade ticks.
         * Precondition: Feature and tick collections must be non-null and represent matching contracts/sessions.
         * Returns: Market event occurrences for the first breach per contract/session.
         * Postcondition: Inputs are not modified; detection work is delegated to the detector.
         */
        return firstHourBreachEventDetector.detect(sessionRangeFeatures, ticks);
    }

    public FirstHourBreachEventDetector.Accumulator newFirstHourBreachAccumulator(
            Collection<SessionRangeFeature> sessionRangeFeatures
    ) {
        /*
         * Intent: Create a streaming accumulator backed by prebuilt session range features.
         * Precondition: Session range features must be non-null.
         * Returns: Accumulator that can consume ordered trade ticks.
         * Postcondition: No ticks have been consumed yet.
         */
        return firstHourBreachEventDetector.newAccumulator(sessionRangeFeatures);
    }

    public FirstHourBreachEventDetector.LiveAccumulator newLiveFirstHourBreachAccumulator(
            SessionRangeFeatureCalculator.Accumulator sessionRangeAccumulator
    ) {
        /*
         * Intent: Create a streaming accumulator that detects breaches while session ranges are being built.
         * Precondition: Live session range accumulator must be non-null.
         * Returns: LiveAccumulator that can consume ordered trade ticks.
         * Postcondition: Breach detection will use ranges as soon as the live feature accumulator exposes them.
         */
        return firstHourBreachEventDetector.newLiveAccumulator(sessionRangeAccumulator);
    }
}
