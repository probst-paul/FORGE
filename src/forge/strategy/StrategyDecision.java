package forge.strategy;

import forge.trade.OrderRequest;
import forge.trade.TradePlan;

import java.util.Objects;
import java.util.Optional;

public class StrategyDecision {
    private static final StrategyDecision NO_ACTION = new StrategyDecision(null, null);

    private final OrderRequest orderRequest;
    private final TradePlan tradePlan;

    private StrategyDecision(OrderRequest orderRequest, TradePlan tradePlan) {
        this.orderRequest = orderRequest;
        this.tradePlan = tradePlan;
    }

    public static StrategyDecision noAction() {
        return NO_ACTION;
    }

    public static StrategyDecision signal(OrderRequest orderRequest) {
        return new StrategyDecision(Objects.requireNonNull(orderRequest, "orderRequest is required"), null);
    }

    public static StrategyDecision trade(OrderRequest orderRequest, TradePlan tradePlan) {
        return new StrategyDecision(
                Objects.requireNonNull(orderRequest, "orderRequest is required"),
                Objects.requireNonNull(tradePlan, "tradePlan is required")
        );
    }

    public boolean hasOrderRequest() {
        return orderRequest != null;
    }

    public Optional<OrderRequest> getOrderRequest() {
        return Optional.ofNullable(orderRequest);
    }

    public Optional<TradePlan> getTradePlan() {
        return Optional.ofNullable(tradePlan);
    }
}
