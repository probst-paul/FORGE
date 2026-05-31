package forge.strategy;

import forge.event.EventSide;
import forge.event.FirstHourBreachEvent;
import forge.event.MarketEventOccurrence;
import forge.trade.OrderRequest;
import forge.trade.OrderSide;
import forge.feature.SessionRangeFeature;
import forge.feature.TpoPeriod;
import forge.feature.TradingSession;
import forge.trade.TradePlan;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

public class OpeningRangeContinuationStrategy implements TradingStrategy {
    private static final ZoneId CENTRAL_TIME = ZoneId.of("America/Chicago");
    private static final LocalTime TRADE_START = LocalTime.of(9, 30);
    private static final LocalTime TRADE_END_EXCLUSIVE = LocalTime.of(10, 30);
    private static final int DEFAULT_QUANTITY = 1;
    private static final double DEFAULT_REWARD_RISK_RATIO = 2.0;

    private final int quantity;
    private final ExitStyle exitStyle;
    private final double rewardRiskRatio;
    private final Map<SessionKey, Boolean> tradeTakenBySession = new HashMap<>();

    public OpeningRangeContinuationStrategy() {
        this(DEFAULT_QUANTITY);
    }

    public OpeningRangeContinuationStrategy(int quantity) {
        this(quantity, ExitStyle.RANGE, DEFAULT_REWARD_RISK_RATIO);
    }

    public OpeningRangeContinuationStrategy(int quantity, ExitStyle exitStyle, double rewardRiskRatio) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be greater than zero");
        }
        if (exitStyle == null) {
            throw new NullPointerException("exitStyle is required");
        }
        if (rewardRiskRatio <= 0) {
            throw new IllegalArgumentException("rewardRiskRatio must be greater than zero");
        }
        this.quantity = quantity;
        this.exitStyle = exitStyle;
        this.rewardRiskRatio = rewardRiskRatio;
    }

    @Override
    public String getName() {
        return "OpeningRangeContinuation";
    }

    @Override
    public StrategyRequirements getRequirements() {
        return StrategyRequirements.builder()
                .requireFeature(SessionRangeFeature.FEATURE_NAME)
                .requireEvent(FirstHourBreachEvent.EVENT_NAME)
                .evaluateDuring(TradingSession.RTH)
                .evaluateDuring(TpoPeriod.C)
                .evaluateDuring(TpoPeriod.D)
                .build();
    }

    @Override
    public StrategyDecision evaluate(StrategyContext strategyContext) {
        /*
         * Intent: Generate at most one opening-range continuation trade per contract session.
         * Precondition: strategyContext must contain the current tick, session range feature, and current events.
         * Returns: Trade decision when a valid first-hour breach occurs in the trade window; otherwise no action.
         * Postcondition: Session is marked traded only after a trade decision is created.
         */
        if (strategyContext == null) {
            throw new NullPointerException("strategyContext is required");
        }
        if (strategyContext.hasOpenPosition()) {
            return StrategyDecision.noAction();
        }
        SessionRangeFeature feature = strategyContext.getSessionRangeFeature().orElse(null);
        if (feature == null || !isSetupValid(feature)) {
            return StrategyDecision.noAction();
        }
        SessionKey sessionKey = new SessionKey(feature.getContractSymbol(), feature.getSessionDate());
        if (tradeTakenBySession.containsKey(sessionKey)) {
            return StrategyDecision.noAction();
        }
        MarketEventOccurrence breachEvent = firstHourBreachEvent(strategyContext);
        if (breachEvent == null || !isTradeWindow(breachEvent)) {
            return StrategyDecision.noAction();
        }

        OrderSide side = breachEvent.getSide() == EventSide.LONG ? OrderSide.BUY : OrderSide.SELL;
        long stopPriceTicks = side == OrderSide.BUY
                ? feature.getFirstHourLowTicks()
                : feature.getFirstHourHighTicks();
        long targetPriceTicks = calculateTargetPriceTicks(
                side,
                strategyContext.getMarketContext().getLastPriceTicks(),
                stopPriceTicks,
                feature
        );
        tradeTakenBySession.put(sessionKey, Boolean.TRUE);
        TradePlan tradePlan = new TradePlan(side, targetPriceTicks, stopPriceTicks, TRADE_END_EXCLUSIVE, CENTRAL_TIME);
        return StrategyDecision.trade(
                OrderRequest.market(strategyContext.getMarketContext().getInstrumentSymbol(), side, quantity),
                tradePlan
        );
    }

    @Override
    public void onBacktestStart() {
        /*
         * Intent: Reset per-session trade tracking before a new backtest run.
         * Precondition: None.
         * Returns: Nothing.
         * Postcondition: Prior run state cannot suppress trades in the new run.
         */
        tradeTakenBySession.clear();
    }

    public int getQuantity() {
        return quantity;
    }

    public ExitStyle getExitStyle() {
        return exitStyle;
    }

    public double getRewardRiskRatio() {
        return rewardRiskRatio;
    }

    private long calculateTargetPriceTicks(
            OrderSide side,
            long entryPriceTicks,
            long stopPriceTicks,
            SessionRangeFeature feature
    ) {
        /*
         * Intent: Calculate target price in ticks using the selected exit style.
         * Precondition: Side, entry, stop, and feature must describe a valid trade setup.
         * Returns: Target price ticks for the generated TradePlan.
         * Postcondition: Strategy state is unchanged.
         */
        if (exitStyle == ExitStyle.RANGE) {
            return side == OrderSide.BUY
                    ? feature.getOvernightHighTicks()
                    : feature.getOvernightLowTicks();
        }

        long riskTicks = Math.abs(entryPriceTicks - stopPriceTicks);
        if (riskTicks == 0) {
            throw new IllegalStateException("riskTicks must be greater than zero");
        }
        long rewardTicks = Math.round(riskTicks * rewardRiskRatio);
        return side == OrderSide.BUY
                ? entryPriceTicks + rewardTicks
                : entryPriceTicks - rewardTicks;
    }

    private boolean isTradeWindow(MarketEventOccurrence event) {
        /*
         * Intent: Restrict entries to the configured morning trade window.
         * Precondition: event must have a valid timestamp.
         * Returns: true when event time is from 9:30 inclusive to 10:30 exclusive Central Time.
         * Postcondition: Event state is unchanged.
         */
        LocalTime time = event.getEventTime().atZone(CENTRAL_TIME).toLocalTime();
        return !time.isBefore(TRADE_START) && time.isBefore(TRADE_END_EXCLUSIVE);
    }

    private boolean isSetupValid(SessionRangeFeature feature) {
        /*
         * Intent: Require the first-hour range to stay inside the overnight range.
         * Precondition: feature must contain overnight and first-hour tick ranges.
         * Returns: true when both first-hour boundaries are inside overnight boundaries.
         * Postcondition: Feature state is unchanged.
         */
        return feature.getFirstHourHighTicks() <= feature.getOvernightHighTicks()
                && feature.getFirstHourLowTicks() >= feature.getOvernightLowTicks();
    }

    private MarketEventOccurrence firstHourBreachEvent(StrategyContext context) {
        /*
         * Intent: Find the current tick's first-hour breach event, if one exists.
         * Precondition: context must contain current tick and current event list.
         * Returns: Matching directional breach event, or null when none applies.
         * Postcondition: Event list is not modified.
         */
        for (MarketEventOccurrence event : context.getCurrentEvents()) {
            if (FirstHourBreachEvent.EVENT_NAME.equals(event.getEventName())
                    && event.getEventTime().equals(context.getCurrentTick().getTradeDateTime())
                    && event.getContractSymbol().equals(context.getCurrentTick().getContractSymbol())
                    && (event.getSide() == EventSide.LONG || event.getSide() == EventSide.SHORT)) {
                return event;
            }
        }
        return null;
    }

    public enum ExitStyle {
        RANGE,
        RISK_REWARD
    }

    private static class SessionKey {
        private final String contractSymbol;
        private final LocalDate sessionDate;

        private SessionKey(String contractSymbol, LocalDate sessionDate) {
            this.contractSymbol = contractSymbol;
            this.sessionDate = sessionDate;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof SessionKey)) {
                return false;
            }
            SessionKey that = (SessionKey) other;
            return contractSymbol.equals(that.contractSymbol) && sessionDate.equals(that.sessionDate);
        }

        @Override
        public int hashCode() {
            int result = contractSymbol.hashCode();
            result = 31 * result + sessionDate.hashCode();
            return result;
        }
    }
}
