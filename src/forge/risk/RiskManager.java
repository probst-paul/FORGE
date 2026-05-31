package forge.risk;

import forge.config.RiskSettings;
import forge.data.market.TradeTick;
import forge.trade.TradeLifecycleEngine;
import forge.trade.TradeResult;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class RiskManager {
    public static final String EXIT_REASON_PER_TRADE_RISK = "PER_TRADE_RISK";
    public static final String EXIT_REASON_DAILY_RISK = "DAILY_RISK";

    private final RiskSettings riskSettings;
    private final Map<String, DailyRiskState> statesByInstrument = new HashMap<>();

    public RiskManager(RiskSettings riskSettings) {
        /*
         * Intent: Track strategy-independent per-trade and daily loss controls during a backtest.
         * Precondition: Risk settings must be valid.
         * Returns: A constructed RiskManager.
         * Postcondition: Risk state starts empty and resets per instrument/trading day as ticks arrive.
         */
        this.riskSettings = Objects.requireNonNull(riskSettings, "riskSettings is required");
    }

    public boolean canOpenTrade(String instrumentSymbol, LocalDate tradingDay) {
        /*
         * Intent: Decide whether a new trade may be opened under daily loss controls.
         * Precondition: Instrument and trading day must identify the current strategy context.
         * Returns: False when daily risk is enabled and the instrument is locked for the day.
         * Postcondition: Risk state may reset if a new trading day is observed.
         */
        if (!riskSettings.isDailyRiskEnabled()) {
            return true;
        }
        return !stateFor(instrumentSymbol, tradingDay).dailyLocked;
    }

    public RiskDecision evaluateOpenTrade(
            String instrumentSymbol,
            LocalDate tradingDay,
            TradeLifecycleEngine lifecycleEngine,
            TradeTick tick
    ) {
        /*
         * Intent: Check open-trade and daily loss limits against current unrealized P/L.
         * Precondition: Lifecycle engine and tick must describe the current instrument tick.
         * Returns: Decision to hold or close the trade with a risk exit reason.
         * Postcondition: Daily state may reset for a new trading day but realized P/L is unchanged.
         */
        Objects.requireNonNull(lifecycleEngine, "lifecycleEngine is required");
        Objects.requireNonNull(tick, "tick is required");
        if (!lifecycleEngine.hasOpenPosition()) {
            return RiskDecision.hold();
        }

        DailyRiskState state = stateFor(instrumentSymbol, tradingDay);
        double unrealizedDollars = lifecycleEngine.unrealizedDollars(tick);
        if (riskSettings.isPerTradeRiskEnabled()
                && unrealizedDollars <= -riskSettings.getRiskPerTrade()) {
            return RiskDecision.closeTrade(EXIT_REASON_PER_TRADE_RISK);
        }
        if (riskSettings.isDailyRiskEnabled()
                && state.realizedDollars + unrealizedDollars <= -riskSettings.getMaxDailyLoss()) {
            state.dailyLocked = true;
            return RiskDecision.closeTrade(EXIT_REASON_DAILY_RISK);
        }
        return RiskDecision.hold();
    }

    public void recordClosedTrade(String instrumentSymbol, LocalDate tradingDay, TradeResult trade) {
        /*
         * Intent: Add realized trade P/L to the current trading day and apply daily lockout if needed.
         * Precondition: Trade must be completed for the supplied instrument/trading day.
         * Returns: Nothing.
         * Postcondition: Daily realized P/L increases and may lock further entries for the day.
         */
        Objects.requireNonNull(trade, "trade is required");
        DailyRiskState state = stateFor(instrumentSymbol, tradingDay);
        state.realizedDollars += trade.getGrossDollars();
        if (riskSettings.isDailyRiskEnabled()
                && state.realizedDollars <= -riskSettings.getMaxDailyLoss()) {
            state.dailyLocked = true;
        }
    }

    private DailyRiskState stateFor(String instrumentSymbol, LocalDate tradingDay) {
        String key = normalizeInstrumentSymbol(instrumentSymbol);
        LocalDate day = Objects.requireNonNull(tradingDay, "tradingDay is required");
        DailyRiskState state = statesByInstrument.computeIfAbsent(key, ignored -> new DailyRiskState(day));
        if (!state.tradingDay.equals(day)) {
            state.tradingDay = day;
            state.realizedDollars = 0.0;
            state.dailyLocked = false;
        }
        return state;
    }

    private String normalizeInstrumentSymbol(String instrumentSymbol) {
        if (instrumentSymbol == null || instrumentSymbol.trim().isEmpty()) {
            throw new IllegalArgumentException("instrumentSymbol is required");
        }
        return instrumentSymbol.trim().toUpperCase();
    }

    private static class DailyRiskState {
        private LocalDate tradingDay;
        private double realizedDollars;
        private boolean dailyLocked;

        private DailyRiskState(LocalDate tradingDay) {
            this.tradingDay = tradingDay;
        }
    }
}
