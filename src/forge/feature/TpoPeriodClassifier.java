package forge.feature;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;

public class TpoPeriodClassifier {
    private static final ZoneId CENTRAL_TIME = ZoneId.of("America/Chicago");
    private static final LocalTime RTH_START = LocalTime.of(8, 30);
    private static final LocalTime RTH_END = LocalTime.of(17, 0);
    private static final int MINUTES_PER_PERIOD = 30;

    public TpoPeriod classify(Instant tradeDateTime) {
        /*
         * Intent: Classify an RTH timestamp into a 30-minute TPO period.
         * Precondition: Timestamp must be non-null and is interpreted in America/Chicago time.
         * Returns: TPO period A-Q during RTH, or OUTSIDE_RTH outside 08:30-17:00 Central.
         * Postcondition: Classifier state is unchanged.
         */
        if (tradeDateTime == null) {
            throw new IllegalArgumentException("tradeDateTime is required");
        }

        LocalTime centralTime = tradeDateTime.atZone(CENTRAL_TIME).toLocalTime();
        if (centralTime.isBefore(RTH_START) || !centralTime.isBefore(RTH_END)) {
            return TpoPeriod.OUTSIDE_RTH;
        }

        int minutesFromRthOpen = (centralTime.getHour() * 60 + centralTime.getMinute())
                - (RTH_START.getHour() * 60 + RTH_START.getMinute());
        int periodIndex = minutesFromRthOpen / MINUTES_PER_PERIOD;
        TpoPeriod[] periods = TpoPeriod.values();
        if (periodIndex < 0 || periodIndex >= TpoPeriod.OUTSIDE_RTH.ordinal()) {
            return TpoPeriod.OUTSIDE_RTH;
        }
        return periods[periodIndex];
    }
}
