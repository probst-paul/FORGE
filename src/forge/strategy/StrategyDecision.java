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
        /*
         * Intent: Represent a strategy evaluation that produced no order.
         * Precondition: None.
         * Returns: Shared no-action decision.
         * Postcondition: No new decision object is allocated for no-action cases.
         */
        return NO_ACTION;
    }

    public static StrategyDecision signal(OrderRequest orderRequest) {
        /*
         * Intent: Represent an order signal that does not yet include full trade lifecycle rules.
         * Precondition: orderRequest must be non-null.
         * Returns: StrategyDecision containing the order request.
         * Postcondition: Caller can inspect the order request via Optional.
         */
        return new StrategyDecision(Objects.requireNonNull(orderRequest, "orderRequest is required"), null);
    }

    public static StrategyDecision trade(OrderRequest orderRequest, TradePlan tradePlan) {
        /*
         * Intent: Represent a complete trade decision with entry order and exit plan.
         * Precondition: orderRequest and tradePlan must be non-null.
         * Returns: StrategyDecision containing both order request and trade plan.
         * Postcondition: Backtest engine can open and manage a simulated trade from the decision.
         */
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
