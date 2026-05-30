package forge.data.build;

import forge.data.market.ContractTradeWindow;
import forge.util.ImmutableLists;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class DatabaseBuildRequest {
    public static final int DEFAULT_BATCH_SIZE = 100_000;

    private final List<ContractTradeWindow> contractWindows;
    private final Set<DerivedDataBuildOption> options;
    private final boolean rebuildExisting;
    private final int batchSize;

    public DatabaseBuildRequest(
            List<ContractTradeWindow> contractWindows,
            Set<DerivedDataBuildOption> options,
            boolean rebuildExisting
    ) {
        /*
         * Intent: Create a derived-data build request using the default streaming batch size.
         * Precondition: Contract windows and build options must be valid.
         * Returns: A constructed DatabaseBuildRequest instance.
         * Postcondition: Request uses the standard batch size for database reads.
         */
        this(contractWindows, options, rebuildExisting, DEFAULT_BATCH_SIZE);
    }

    public DatabaseBuildRequest(
            List<ContractTradeWindow> contractWindows,
            Set<DerivedDataBuildOption> options,
            boolean rebuildExisting,
            int batchSize
    ) {
        /*
         * Intent: Store selected contract windows and derived-data build choices.
         * Precondition: At least one contract window, one build option, and a positive batch size are required.
         * Returns: A constructed DatabaseBuildRequest instance.
         * Postcondition: Windows and options are defensively copied into immutable collections.
         */
        if (contractWindows == null || contractWindows.isEmpty()) {
            throw new IllegalArgumentException("at least one contract window is required");
        }
        if (options == null || options.isEmpty()) {
            throw new IllegalArgumentException("at least one build option is required");
        }
        if (batchSize < 1) {
            throw new IllegalArgumentException("batchSize must be positive");
        }
        this.contractWindows = ImmutableLists.copyOfRequired(contractWindows, "contractWindows");
        this.options = Collections.unmodifiableSet(EnumSet.copyOf(options));
        this.rebuildExisting = rebuildExisting;
        this.batchSize = batchSize;
    }

    public List<ContractTradeWindow> getContractWindows() {
        return contractWindows;
    }

    public Set<DerivedDataBuildOption> getOptions() {
        return options;
    }

    public boolean shouldBuild(DerivedDataBuildOption option) {
        /*
         * Intent: Check whether a specific derived-data option was requested.
         * Precondition: Option should be non-null.
         * Returns: True when the option is included in this request.
         * Postcondition: Request state is unchanged.
         */
        return options.contains(option);
    }

    public boolean isRebuildExisting() {
        return rebuildExisting;
    }

    public int getBatchSize() {
        return batchSize;
    }
}
