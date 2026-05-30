package forge.feature;

import java.time.LocalDate;

public class TradingDayContext {
    private final LocalDate tradingDay;
    private final TradingSession session;

    public TradingDayContext(LocalDate tradingDay, TradingSession session) {
        /*
         * Intent: Pair a timestamp's FORGE trading day with its classified session.
         * Precondition: Trading day and session must be non-null.
         * Returns: A constructed TradingDayContext instance.
         * Postcondition: Context is immutable.
         */
        if (tradingDay == null) {
            throw new IllegalArgumentException("tradingDay is required");
        }
        if (session == null) {
            throw new IllegalArgumentException("session is required");
        }
        this.tradingDay = tradingDay;
        this.session = session;
    }

    public LocalDate getTradingDay() {
        return tradingDay;
    }

    public TradingSession getSession() {
        return session;
    }

    public boolean isOvernight() {
        return session == TradingSession.OVERNIGHT;
    }

    public boolean isFirstHour() {
        return session == TradingSession.FIRST_HOUR;
    }

    public boolean isRth() {
        /*
         * Intent: Treat first-hour as part of the broader regular trading hours session.
         * Precondition: Context must be constructed.
         * Returns: True for FIRST_HOUR or RTH.
         * Postcondition: Context state is unchanged.
         */
        return session == TradingSession.FIRST_HOUR || session == TradingSession.RTH;
    }
}
