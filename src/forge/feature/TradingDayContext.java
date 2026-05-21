package forge.feature;

import java.time.LocalDate;

public class TradingDayContext {
    private final LocalDate tradingDay;
    private final TradingSession session;

    public TradingDayContext(LocalDate tradingDay, TradingSession session) {
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
        return session == TradingSession.FIRST_HOUR || session == TradingSession.RTH;
    }
}
