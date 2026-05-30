package forge.app;

import forge.data.market.ContractTradeWindow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EventStatisticsRequest {
    private final List<ContractTradeWindow> contractWindows;
    private final String eventName;
    private final EventStatisticsProgressListener progressListener;

    public EventStatisticsRequest(List<ContractTradeWindow> contractWindows, String eventName) {
        this(contractWindows, eventName, EventStatisticsProgressListener.NO_OP);
    }

    public EventStatisticsRequest(
            List<ContractTradeWindow> contractWindows,
            String eventName,
            EventStatisticsProgressListener progressListener
    ) {
        /*
         * Intent: Package the selected contract windows, statistic/event name, and progress callback for an event-statistics run.
         * Precondition: At least one non-null contract window is required; event name must be nonblank; progress listener must exist.
         * Returns: A constructed EventStatisticsRequest instance.
         * Postcondition: Contract windows are defensively copied into an immutable list and event name is trimmed.
         */
        if (contractWindows == null || contractWindows.isEmpty()) {
            throw new IllegalArgumentException("at least one contract window is required");
        }
        if (eventName == null || eventName.trim().isEmpty()) {
            throw new IllegalArgumentException("eventName is required");
        }
        if (progressListener == null) {
            throw new IllegalArgumentException("progressListener is required");
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
        this.progressListener = progressListener;
    }

    public List<ContractTradeWindow> getContractWindows() {
        return contractWindows;
    }

    public String getEventName() {
        return eventName;
    }

    public EventStatisticsProgressListener getProgressListener() {
        return progressListener;
    }
}
