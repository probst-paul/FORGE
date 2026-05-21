package forge.app;

import forge.data.market.ContractTradeWindow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EventStatisticsRequest {
    private final List<ContractTradeWindow> contractWindows;
    private final String eventName;

    public EventStatisticsRequest(List<ContractTradeWindow> contractWindows, String eventName) {
        if (contractWindows == null || contractWindows.isEmpty()) {
            throw new IllegalArgumentException("at least one contract window is required");
        }
        if (eventName == null || eventName.trim().isEmpty()) {
            throw new IllegalArgumentException("eventName is required");
        }
        List<ContractTradeWindow> normalizedWindows = new ArrayList<>();
        for (ContractTradeWindow contractWindow : contractWindows) {
            if (contractWindow == null) {
                throw new IllegalArgumentException("contract windows cannot contain null values");
            }
            normalizedWindows.add(contractWindow);
        }
        this.contractWindows = Collections.unmodifiableList(normalizedWindows);
        this.eventName = eventName.trim();
    }

    public List<ContractTradeWindow> getContractWindows() {
        return contractWindows;
    }

    public String getEventName() {
        return eventName;
    }
}
