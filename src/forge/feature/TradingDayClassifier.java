package forge.feature;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class TradingDayClassifier {
    public static final ZoneId CENTRAL_TIME = ZoneId.of("America/Chicago");

    private static final LocalTime OVERNIGHT_START = LocalTime.of(17, 0);
    private static final LocalTime RTH_START = LocalTime.of(8, 30);
    private static final LocalTime FIRST_HOUR_END = LocalTime.of(9, 30);
    private static final LocalTime RTH_END = LocalTime.of(17, 0);

    public TradingDayContext classify(Instant tradeDateTime) {
        /*
         * Intent: Assign a tick timestamp to the FORGE trading day and session model.
         * Precondition: Timestamp must be non-null and is interpreted in America/Chicago time.
         * Returns: TradingDayContext for overnight, first-hour, or RTH session.
         * Postcondition: Evening overnight ticks are attributed to the following trading day.
         */
        if (tradeDateTime == null) {
            throw new IllegalArgumentException("tradeDateTime is required");
        }

        ZonedDateTime centralDateTime = tradeDateTime.atZone(CENTRAL_TIME);
        LocalTime centralTime = centralDateTime.toLocalTime();

        if (!centralTime.isBefore(OVERNIGHT_START)) {
            return new TradingDayContext(
                    centralDateTime.toLocalDate().plusDays(1),
                    TradingSession.OVERNIGHT
            );
        }

        if (centralTime.isBefore(RTH_START)) {
            return new TradingDayContext(centralDateTime.toLocalDate(), TradingSession.OVERNIGHT);
        }

        if (centralTime.isBefore(FIRST_HOUR_END)) {
            return new TradingDayContext(centralDateTime.toLocalDate(), TradingSession.FIRST_HOUR);
        }

        if (centralTime.isBefore(RTH_END)) {
            return new TradingDayContext(centralDateTime.toLocalDate(), TradingSession.RTH);
        }

        return new TradingDayContext(
                centralDateTime.toLocalDate().plusDays(1),
                TradingSession.OVERNIGHT
        );
    }
}
