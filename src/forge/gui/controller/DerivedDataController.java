package forge.gui.controller;

import forge.data.FacadeForgeData;
import forge.data.build.DatabaseBuildPlan;
import forge.data.build.DatabaseBuildRequest;
import forge.data.build.DatabaseBuildResult;
import forge.data.build.DerivedDataBuildOption;
import forge.data.catalog.InstrumentDataCatalog.AvailableContractData;
import forge.data.market.ContractTradeWindow;
import forge.gui.viewmodel.DerivedDataViewModel;
import forge.gui.viewmodel.GuiProgressBindings;
import javafx.concurrent.Task;

import java.util.List;
import java.util.Set;

public class DerivedDataController {
    private final FacadeForgeData forgeData;
    private final DerivedDataViewModel viewModel;

    public DerivedDataController() {
        this(FacadeForgeData.getTheInstance(), new DerivedDataViewModel());
    }

    public DerivedDataController(FacadeForgeData forgeData, DerivedDataViewModel viewModel) {
        if (forgeData == null) {
            throw new IllegalArgumentException("forgeData is required");
        }
        if (viewModel == null) {
            throw new IllegalArgumentException("viewModel is required");
        }
        this.forgeData = forgeData;
        this.viewModel = viewModel;
    }

    public DerivedDataViewModel getViewModel() {
        return viewModel;
    }

    public List<AvailableContractData> getAvailableContracts() {
        return forgeData.forgeDataAccess().getAvailableContracts();
    }

    public DatabaseBuildPlan planBuild(
            List<ContractTradeWindow> contractWindows,
            Set<DerivedDataBuildOption> options,
            boolean rebuildExisting
    ) {
        /*
         * Intent: Preview derived-data work before launching a build.
         * Precondition: contractWindows and options must describe the user's selected rebuild scope.
         * Returns: Plan indicating whether any derived-data work needs to run.
         * Postcondition: No derived-data tables are modified by planning.
         */
        return forgeData.forgeDataAccess().planDatabaseBuild(new DatabaseBuildRequest(
                contractWindows,
                options,
                rebuildExisting
        ));
    }

    public DatabaseBuildResult runBuild(
            List<ContractTradeWindow> contractWindows,
            Set<DerivedDataBuildOption> options,
            boolean rebuildExisting
    ) {
        /*
         * Intent: Run a derived-data build synchronously for tests or non-task GUI callers.
         * Precondition: At least one contract window and one build option should be selected.
         * Returns: Completed build result.
         * Postcondition: The view model is marked succeeded or failed with build totals.
         */
        viewModel.setContractWindows(contractWindows);
        viewModel.setBuildSessionRanges(options.contains(DerivedDataBuildOption.SESSION_RANGES));
        viewModel.setBuildFirstHourBreachEvents(options.contains(DerivedDataBuildOption.FIRST_HOUR_BREACH_EVENTS));
        viewModel.setRebuildExisting(rebuildExisting);
        viewModel.markStarted("Building derived data...");
        try {
            DatabaseBuildResult result = forgeData.forgeDataAccess().runDatabaseBuild(
                    new DatabaseBuildRequest(contractWindows, options, rebuildExisting),
                    GuiProgressBindings.dataBuildProgress(viewModel, "Building derived data...")
            );
            applyBuildResult(result);
            return result;
        } catch (RuntimeException exception) {
            viewModel.markFailed("Could not build derived data.", exception);
            throw exception;
        }
    }

    public Task<DatabaseBuildResult> runBuildTask(
            List<ContractTradeWindow> contractWindows,
            Set<DerivedDataBuildOption> options,
            boolean rebuildExisting
    ) {
        /*
         * Intent: Create a JavaFX task for building derived data off the UI thread.
         * Precondition: At least one contract window and one build option should be selected.
         * Returns: Task that yields the derived-data build result.
         * Postcondition: Task progress callbacks update the view model through JavaFX bindings.
         */
        viewModel.setContractWindows(contractWindows);
        viewModel.setBuildSessionRanges(options.contains(DerivedDataBuildOption.SESSION_RANGES));
        viewModel.setBuildFirstHourBreachEvents(options.contains(DerivedDataBuildOption.FIRST_HOUR_BREACH_EVENTS));
        viewModel.setRebuildExisting(rebuildExisting);
        return GuiControllerTasks.create(
                viewModel,
                "Building derived data...",
                "Could not build derived data.",
                task -> forgeData.forgeDataAccess().runDatabaseBuild(
                        new DatabaseBuildRequest(contractWindows, options, rebuildExisting),
                        GuiProgressBindings.dataBuildProgress(task, "Building derived data...")
                ),
                this::applyBuildResult
        );
    }

    private void applyBuildResult(DatabaseBuildResult result) {
        /*
         * Intent: Copy completed derived-data totals into GUI state.
         * Precondition: result must be a successful build result.
         * Returns: Nothing.
         * Postcondition: The view model exposes tick counts, derived counts, and success text.
         */
        viewModel.setTicksRead(result.getTicksRead());
        viewModel.setSessionRangesBuilt(result.getSessionRangesBuilt());
        viewModel.setMarketEventOccurrencesBuilt(result.getMarketEventOccurrencesBuilt());
        viewModel.markSucceeded(
                "Derived data build complete.",
                "Read " + result.getTicksRead() + " ticks, built "
                        + result.getSessionRangesBuilt() + " session ranges and "
                        + result.getMarketEventOccurrencesBuilt() + " market event occurrences."
        );
    }
}
