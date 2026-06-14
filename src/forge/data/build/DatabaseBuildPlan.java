package forge.data.build;

import forge.data.market.ContractTradeWindow;
import forge.util.ImmutableLists;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class DatabaseBuildPlan {
    private final List<ContractTradeWindow> contractWindows;
    private final Set<DerivedDataBuildOption> requestedOptions;
    private final boolean rebuildExisting;
    private final long totalTicks;
    private final boolean sessionRangesAlreadyBuilt;
    private final boolean firstHourBreachEventsAlreadyBuilt;
    private final boolean willBuildSessionRanges;
    private final boolean willBuildFirstHourBreachEvents;

    public DatabaseBuildPlan(
            List<ContractTradeWindow> contractWindows,
            Set<DerivedDataBuildOption> requestedOptions,
            boolean rebuildExisting,
            long totalTicks,
            boolean sessionRangesAlreadyBuilt,
            boolean firstHourBreachEventsAlreadyBuilt,
            boolean willBuildSessionRanges,
            boolean willBuildFirstHourBreachEvents
    ) {
        /*
         * Intent: Capture the derived-data build decision before running any work.
         * Precondition: Contract windows/options must be present and total tick count cannot be negative.
         * Returns: A constructed DatabaseBuildPlan instance.
         * Postcondition: Plan is immutable and can be used by GUI workflows to preview work.
         */
        if (contractWindows == null || contractWindows.isEmpty()) {
            throw new IllegalArgumentException("at least one contract window is required");
        }
        if (requestedOptions == null || requestedOptions.isEmpty()) {
            throw new IllegalArgumentException("at least one requested option is required");
        }
        if (totalTicks < 0) {
            throw new IllegalArgumentException("totalTicks cannot be negative");
        }
        this.contractWindows = ImmutableLists.copyOfRequired(contractWindows, "contractWindows");
        this.requestedOptions = Collections.unmodifiableSet(EnumSet.copyOf(requestedOptions));
        this.rebuildExisting = rebuildExisting;
        this.totalTicks = totalTicks;
        this.sessionRangesAlreadyBuilt = sessionRangesAlreadyBuilt;
        this.firstHourBreachEventsAlreadyBuilt = firstHourBreachEventsAlreadyBuilt;
        this.willBuildSessionRanges = willBuildSessionRanges;
        this.willBuildFirstHourBreachEvents = willBuildFirstHourBreachEvents;
    }

    public List<ContractTradeWindow> getContractWindows() {
        return contractWindows;
    }

    public Set<DerivedDataBuildOption> getRequestedOptions() {
        return requestedOptions;
    }

    public boolean isRebuildExisting() {
        return rebuildExisting;
    }

    public long getTotalTicks() {
        return totalTicks;
    }

    public boolean isSessionRangesAlreadyBuilt() {
        return sessionRangesAlreadyBuilt;
    }

    public boolean isFirstHourBreachEventsAlreadyBuilt() {
        return firstHourBreachEventsAlreadyBuilt;
    }

    public boolean willBuildSessionRanges() {
        return willBuildSessionRanges;
    }

    public boolean willBuildFirstHourBreachEvents() {
        return willBuildFirstHourBreachEvents;
    }

    public boolean hasWorkToRun() {
        /*
         * Intent: Determine whether the plan requires any derived-data processing.
         * Precondition: Plan must be constructed.
         * Returns: True when at least one derived-data artifact will be built.
         * Postcondition: Plan state is unchanged.
         */
        return willBuildSessionRanges || willBuildFirstHourBreachEvents;
    }
}
