package forge.strategy;

import forge.engine.MarketContext;
import forge.execution.OrderRequest;
import forge.execution.OrderSide;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class OpeningRangeContinuationStrategy implements TradingStrategy {
    private static final ZoneId CENTRAL_TIME = ZoneId.of("America/Chicago");
    private static final LocalTime OVERNIGHT_START = LocalTime.of(17, 0);
    private static final LocalTime RTH_START = LocalTime.of(8, 30);
    private static final LocalTime TRADE_START = LocalTime.of(9, 30);
    private static final LocalTime TRADE_END_EXCLUSIVE = LocalTime.of(10, 30);
    private static final int DEFAULT_QUANTITY = 1;
    private static final double DEFAULT_REWARD_RISK_RATIO = 2.0;

    private final int quantity;
    private final ExitStyle exitStyle;
    private final double rewardRiskRatio;
    private final Map<LocalDate, SessionState> sessions = new HashMap<>();
    private TradePlan lastTradePlan;

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
    public Optional<OrderRequest> evaluate(MarketContext marketContext) {
        LocalDateTime centralDateTime = toCentralTime(marketContext.getTimestamp());
        LocalDate sessionDate = sessionDateFor(centralDateTime);
        SessionState session = sessions.computeIfAbsent(sessionDate, ignored -> new SessionState());
        LocalTime time = centralDateTime.toLocalTime();
        long priceTicks = marketContext.getLastPriceTicks();

        if (isOvernight(time)) {
            session.overnightRange.include(priceTicks);
            return Optional.empty();
        }
        if (isFirstHour(time)) {
            session.firstHourRange.include(priceTicks);
            return Optional.empty();
        }
        if (!isTradeWindow(time) || marketContext.hasOpenPosition() || session.tradeTaken) {
            return Optional.empty();
        }
        if (!session.isSetupValid()) {
            return Optional.empty();
        }

        long firstHourHigh = session.firstHourRange.getHighPriceTicks();
        long firstHourLow = session.firstHourRange.getLowPriceTicks();
        if (priceTicks >= firstHourHigh) {
            return enterTrade(marketContext, session, OrderSide.BUY, firstHourLow);
        }
        if (priceTicks <= firstHourLow) {
            return enterTrade(marketContext, session, OrderSide.SELL, firstHourHigh);
        }
        return Optional.empty();
    }

    @Override
    public void onBacktestStart() {
        sessions.clear();
        lastTradePlan = null;
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

    public Optional<TradePlan> getLastTradePlan() {
        return Optional.ofNullable(lastTradePlan);
    }

    private Optional<OrderRequest> enterTrade(
            MarketContext marketContext,
            SessionState session,
            OrderSide side,
            long stopPriceTicks
    ) {
        session.tradeTaken = true;
        long targetPriceTicks = calculateTargetPriceTicks(side, marketContext.getLastPriceTicks(), stopPriceTicks, session);
        lastTradePlan = new TradePlan(side, targetPriceTicks, stopPriceTicks, TRADE_END_EXCLUSIVE);
        return Optional.of(OrderRequest.market(marketContext.getInstrumentSymbol(), side, quantity));
    }

    private long calculateTargetPriceTicks(
            OrderSide side,
            long entryPriceTicks,
            long stopPriceTicks,
            SessionState session
    ) {
        if (exitStyle == ExitStyle.RANGE) {
            return side == OrderSide.BUY
                    ? session.overnightRange.getHighPriceTicks()
                    : session.overnightRange.getLowPriceTicks();
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

    private LocalDateTime toCentralTime(LocalDateTime utcDateTime) {
        return utcDateTime
                .atZone(ZoneOffset.UTC)
                .withZoneSameInstant(CENTRAL_TIME)
                .toLocalDateTime();
    }

    private LocalDate sessionDateFor(LocalDateTime centralDateTime) {
        if (!centralDateTime.toLocalTime().isBefore(OVERNIGHT_START)) {
            return centralDateTime.toLocalDate().plusDays(1);
        }
        return centralDateTime.toLocalDate();
    }

    private boolean isOvernight(LocalTime time) {
        return !time.isBefore(OVERNIGHT_START) || time.isBefore(RTH_START);
    }

    private boolean isFirstHour(LocalTime time) {
        return !time.isBefore(RTH_START) && time.isBefore(TRADE_START);
    }

    private boolean isTradeWindow(LocalTime time) {
        return !time.isBefore(TRADE_START) && time.isBefore(TRADE_END_EXCLUSIVE);
    }

    public enum ExitStyle {
        RANGE,
        RISK_REWARD
    }

    public static class TradePlan {
        private final OrderSide side;
        private final long targetPriceTicks;
        private final long stopPriceTicks;
        private final LocalTime timeStop;

        private TradePlan(OrderSide side, long targetPriceTicks, long stopPriceTicks, LocalTime timeStop) {
            this.side = side;
            this.targetPriceTicks = targetPriceTicks;
            this.stopPriceTicks = stopPriceTicks;
            this.timeStop = timeStop;
        }

        public OrderSide getSide() {
            return side;
        }

        public long getTargetPriceTicks() {
            return targetPriceTicks;
        }

        public long getStopPriceTicks() {
            return stopPriceTicks;
        }

        public LocalTime getTimeStop() {
            return timeStop;
        }
    }

    private static class SessionState {
        private final RangeBuilder overnightRange = new RangeBuilder();
        private final RangeBuilder firstHourRange = new RangeBuilder();
        private boolean tradeTaken;

        private boolean isSetupValid() {
            return overnightRange.hasPrices()
                    && firstHourRange.hasPrices()
                    && firstHourRange.getHighPriceTicks() <= overnightRange.getHighPriceTicks()
                    && firstHourRange.getLowPriceTicks() >= overnightRange.getLowPriceTicks();
        }
    }

    private static class RangeBuilder {
        private Long lowPriceTicks;
        private Long highPriceTicks;

        private void include(long priceTicks) {
            if (lowPriceTicks == null || priceTicks < lowPriceTicks) {
                lowPriceTicks = priceTicks;
            }
            if (highPriceTicks == null || priceTicks > highPriceTicks) {
                highPriceTicks = priceTicks;
            }
        }

        private boolean hasPrices() {
            return lowPriceTicks != null;
        }

        private long getLowPriceTicks() {
            if (lowPriceTicks == null) {
                throw new IllegalStateException("range has no prices");
            }
            return lowPriceTicks;
        }

        private long getHighPriceTicks() {
            if (highPriceTicks == null) {
                throw new IllegalStateException("range has no prices");
            }
            return highPriceTicks;
        }
    }
}
