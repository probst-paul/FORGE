package forge.gui.viewmodel;

import forge.data.market.ContractTradeWindow;
import forge.util.ImmutableLists;

import java.util.Collections;
import java.util.List;

public class EventStatisticsViewModel extends GuiWorkflowViewModel {
    private List<ContractTradeWindow> contractWindows = Collections.emptyList();
    private String eventName = "";
    private int instrumentResultCount;
    private int contractResultCount;

    public List<ContractTradeWindow> getContractWindows() {
        return contractWindows;
    }

    public void setContractWindows(List<ContractTradeWindow> contractWindows) {
        this.contractWindows = ImmutableLists.copyOfRequired(contractWindows, "contractWindows");
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName == null ? "" : eventName;
    }

    public int getInstrumentResultCount() {
        return instrumentResultCount;
    }

    public void setInstrumentResultCount(int instrumentResultCount) {
        this.instrumentResultCount = (int) requireNonNegative(instrumentResultCount, "instrumentResultCount");
    }

    public int getContractResultCount() {
        return contractResultCount;
    }

    public void setContractResultCount(int contractResultCount) {
        this.contractResultCount = (int) requireNonNegative(contractResultCount, "contractResultCount");
    }
}
