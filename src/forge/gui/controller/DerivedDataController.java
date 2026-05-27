package forge.gui.controller;

import forge.data.FacadeForgeData;
import forge.data.build.DatabaseBuildPlan;
import forge.data.build.DatabaseBuildRequest;
import forge.data.build.DatabaseBuildResult;
import forge.data.build.DerivedDataBuildOption;
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

    public DatabaseBuildPlan planBuild(
            List<ContractTradeWindow> contractWindows,
            Set<DerivedDataBuildOption> options,
            boolean rebuildExisting
    ) {
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
        viewModel.setTicksRead(result.getTicksRead());
        viewModel.setSessionRangesBuilt(result.getSessionRangesBuilt());
        viewModel.setMarketConditionOccurrencesBuilt(result.getMarketConditionOccurrencesBuilt());
        viewModel.markSucceeded(
                "Derived data build complete.",
                "Read " + result.getTicksRead() + " ticks, built "
                        + result.getSessionRangesBuilt() + " session ranges and "
                        + result.getMarketConditionOccurrencesBuilt() + " market condition occurrences."
        );
    }
}
