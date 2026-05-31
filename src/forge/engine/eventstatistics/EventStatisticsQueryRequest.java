package forge.engine.eventstatistics;

import forge.app.EventStatisticsProgressListener;
import forge.data.market.ContractTradeWindow;
import forge.event.FirstHourBreachEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EventStatisticsQueryRequest {
    public static final int DEFAULT_BATCH_SIZE = 10_000;

    private final List<ContractTradeWindow> contractWindows;
    private final String eventName;
    private final int batchSize;
    private final EventStatisticsProgressListener progressListener;

    public EventStatisticsQueryRequest(List<ContractTradeWindow> contractWindows, String eventName) {
        /*
         * Intent: Create an event-statistics request with default batch size and no-op progress listener.
         * Precondition: Contract windows and event name must be valid.
         * Returns: A constructed EventStatisticsQueryRequest instance.
         * Postcondition: Request uses default batch size.
         */
        this(contractWindows, eventName, DEFAULT_BATCH_SIZE);
    }

    public EventStatisticsQueryRequest(List<ContractTradeWindow> contractWindows, String eventName, int batchSize) {
        /*
         * Intent: Create an event-statistics request with explicit batch size and no-op progress listener.
         * Precondition: Contract windows, event name, and batch size must be valid.
         * Returns: A constructed EventStatisticsQueryRequest instance.
         * Postcondition: Request uses the no-op progress listener.
         */
        this(contractWindows, eventName, batchSize, EventStatisticsProgressListener.NO_OP);
    }

    public EventStatisticsQueryRequest(
            List<ContractTradeWindow> contractWindows,
            String eventName,
            int batchSize,
            EventStatisticsProgressListener progressListener
    ) {
        /*
         * Intent: Store selected contract windows and event-statistics execution settings.
         * Precondition: Windows must be non-empty, event name supported, batch size positive, and listener non-null.
         * Returns: A constructed EventStatisticsQueryRequest instance.
         * Postcondition: Contract windows are defensively copied into an immutable list.
         */
        if (contractWindows == null || contractWindows.isEmpty()) {
            throw new IllegalArgumentException("at least one contract window is required");
        }
        if (eventName == null || eventName.trim().isEmpty()) {
            throw new IllegalArgumentException("eventName is required");
        }
        if (!FirstHourBreachEvent.EVENT_NAME.equals(eventName.trim())) {
            throw new IllegalArgumentException("Unsupported event statistics query: " + eventName);
        }
        if (batchSize < 1) {
            throw new IllegalArgumentException("batchSize must be positive");
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
        this.batchSize = batchSize;
        this.progressListener = progressListener;
    }

    public List<ContractTradeWindow> getContractWindows() {
        return contractWindows;
    }

    public String getEventName() {
        return eventName;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public EventStatisticsProgressListener getProgressListener() {
        return progressListener;
    }
}
