package forge.gui.viewmodel;

import forge.data.market.ContractTradeWindow;
import forge.util.ImmutableLists;

import java.util.Collections;
import java.util.List;

public class BacktestViewModel extends GuiWorkflowViewModel {
    private List<ContractTradeWindow> contractWindows = Collections.emptyList();
    private String strategyName = "";
    private long ticksProcessed;
    private long orderSignalsGenerated;

    public List<ContractTradeWindow> getContractWindows() {
        return contractWindows;
    }

    public void setContractWindows(List<ContractTradeWindow> contractWindows) {
        this.contractWindows = ImmutableLists.copyOfRequired(contractWindows, "contractWindows");
    }

    public String getStrategyName() {
        return strategyName;
    }

    public void setStrategyName(String strategyName) {
        this.strategyName = strategyName == null ? "" : strategyName;
    }

    public long getTicksProcessed() {
        return ticksProcessed;
    }

    public void setTicksProcessed(long ticksProcessed) {
        this.ticksProcessed = requireNonNegative(ticksProcessed, "ticksProcessed");
    }

    public long getOrderSignalsGenerated() {
        return orderSignalsGenerated;
    }

    public void setOrderSignalsGenerated(long orderSignalsGenerated) {
        this.orderSignalsGenerated = requireNonNegative(orderSignalsGenerated, "orderSignalsGenerated");
    }
}
