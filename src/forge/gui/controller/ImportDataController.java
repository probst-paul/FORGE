package forge.gui.controller;

import forge.app.DataImportRequest;
import forge.app.FacadeForgeApplication;
import forge.data.FacadeForgeData;
import forge.data.build.DatabaseBuildRequest;
import forge.data.build.DatabaseBuildResult;
import forge.data.build.DerivedDataBuildOption;
import forge.data.catalog.InstrumentDataCatalog.AvailableContractData;
import forge.data.importing.DataImportPlan;
import forge.data.importing.DataImportResult;
import forge.data.importing.DataImportMode;
import forge.data.market.ContractTradeWindow;
import forge.gui.viewmodel.GuiProgressBindings;
import forge.gui.viewmodel.ImportDataViewModel;
import javafx.concurrent.Task;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public class ImportDataController {
    private final FacadeForgeApplication forgeApplication;
    private final FacadeForgeData forgeData;
    private final ImportDataViewModel viewModel;

    public ImportDataController() {
        this(FacadeForgeApplication.getTheInstance(), FacadeForgeData.getTheInstance(), new ImportDataViewModel());
    }

    public ImportDataController(FacadeForgeApplication forgeApplication, ImportDataViewModel viewModel) {
        this(forgeApplication, FacadeForgeData.getTheInstance(), viewModel);
    }

    public ImportDataController(
            FacadeForgeApplication forgeApplication,
            FacadeForgeData forgeData,
            ImportDataViewModel viewModel
    ) {
        if (forgeApplication == null) {
            throw new IllegalArgumentException("forgeApplication is required");
        }
        if (forgeData == null) {
            throw new IllegalArgumentException("forgeData is required");
        }
        if (viewModel == null) {
            throw new IllegalArgumentException("viewModel is required");
        }
        this.forgeApplication = forgeApplication;
        this.forgeData = forgeData;
        this.viewModel = viewModel;
    }

    public ImportDataViewModel getViewModel() {
        return viewModel;
    }

    public DataImportPlan planImport(String scidFilePath) {
        /*
         * Intent: Inspect the selected SCID file before import so the GUI can confirm rebuilds.
         * Precondition: scidFilePath must identify a readable SCID file.
         * Returns: Import plan describing the target contract and existing data state.
         * Postcondition: The view model remembers the selected SCID path.
         */
        viewModel.setScidFilePath(scidFilePath);
        return forgeApplication.forgeApplicationAccess().planDataImport(new DataImportRequest(scidFilePath));
    }

    public DataImportResult importData(String scidFilePath, boolean rebuildExistingContract) {
        return importData(
                scidFilePath,
                rebuildExistingContract ? DataImportMode.OVERWRITE_OVERLAP : DataImportMode.FILL_MISSING
        );
    }

    public DataImportResult importData(String scidFilePath, DataImportMode importMode) {
        /*
         * Intent: Run SCID import synchronously for tests or non-task GUI callers.
         * Precondition: scidFilePath must be valid and importMode must reflect user selection.
         * Returns: Completed import result.
         * Postcondition: The view model is marked succeeded or failed with import details.
         */
        viewModel.setScidFilePath(scidFilePath);
        viewModel.setRebuildExistingContract(importMode == DataImportMode.OVERWRITE_OVERLAP);
        viewModel.markStarted("Importing data...");
        try {
            DataImportResult result = forgeApplication.forgeApplicationAccess().importData(new DataImportRequest(
                    scidFilePath,
                    importMode,
                    GuiProgressBindings.importProgress(viewModel, "Importing")
            ));
            applyImportResult(result);
            return result;
        } catch (RuntimeException exception) {
            viewModel.markFailed("Could not import data.", exception);
            throw exception;
        }
    }

    public Task<DataImportResult> importDataTask(String scidFilePath, boolean rebuildExistingContract) {
        return importDataTask(
                scidFilePath,
                rebuildExistingContract ? DataImportMode.OVERWRITE_OVERLAP : DataImportMode.FILL_MISSING,
                Collections.emptySet()
        );
    }

    public Task<DataImportResult> importDataTask(
            String scidFilePath,
            boolean rebuildExistingContract,
            Set<DerivedDataBuildOption> derivedDataOptions
    ) {
        return importDataTask(
                scidFilePath,
                rebuildExistingContract ? DataImportMode.OVERWRITE_OVERLAP : DataImportMode.FILL_MISSING,
                derivedDataOptions
        );
    }

    public Task<DataImportResult> importDataTask(
            String scidFilePath,
            DataImportMode importMode,
            Set<DerivedDataBuildOption> derivedDataOptions
    ) {
        /*
         * Intent: Create a JavaFX task for importing SCID data and optional post-import derived-data builds.
         * Precondition: scidFilePath must be valid and importMode must reflect user selection.
         * Returns: Task that yields the import result.
         * Postcondition: Task progress callbacks update the view model through JavaFX bindings.
         */
        DataImportMode normalizedImportMode = importMode == null ? DataImportMode.FILL_MISSING : importMode;
        Set<DerivedDataBuildOption> normalizedOptions = normalizeDerivedDataOptions(derivedDataOptions);
        AtomicReference<DatabaseBuildResult> buildResult = new AtomicReference<>();
        viewModel.setScidFilePath(scidFilePath);
        viewModel.setRebuildExistingContract(normalizedImportMode == DataImportMode.OVERWRITE_OVERLAP);
        return GuiControllerTasks.create(
                viewModel,
                "Importing data...",
                "Could not import data.",
                task -> {
                    DataImportResult importResult = forgeApplication.forgeApplicationAccess().importData(new DataImportRequest(
                            scidFilePath,
                            normalizedImportMode,
                            GuiProgressBindings.importProgress(task, "Importing")
                    ));
                    if (!normalizedOptions.isEmpty()) {
                        ContractTradeWindow window = importedContractWindow(importResult.getContractSymbol());
                        buildResult.set(forgeData.forgeDataAccess().runDatabaseBuild(
                                new DatabaseBuildRequest(List.of(window), normalizedOptions, true),
                                GuiProgressBindings.dataBuildProgress(task, "Building derived data...")
                        ));
                    }
                    return importResult;
                },
                result -> applyImportResult(result, buildResult.get())
        );
    }

    private void applyImportResult(DataImportResult result) {
        applyImportResult(result, null);
    }

    private void applyImportResult(DataImportResult result, DatabaseBuildResult buildResult) {
        /*
         * Intent: Copy completed import details into GUI state.
         * Precondition: result must be a successful import result.
         * Returns: Nothing.
         * Postcondition: The view model exposes row counts, contract information, and success text.
         */
        viewModel.setContractSymbol(result.getContractSymbol());
        viewModel.setTableName(result.getTableName());
        viewModel.setRowsImported(result.getImportedRows());
        viewModel.setNullSideRowsImported(result.getNullSideRowsImported());
        viewModel.setSkippedOutsideFrontMonthRows(result.getSkippedOutsideFrontMonthRows());
        viewModel.markSucceeded(
                "Import complete.",
                importSummary(result, buildResult)
        );
    }

    private String importSummary(DataImportResult importResult, DatabaseBuildResult buildResult) {
        /*
         * Intent: Build the GUI summary for an import run and any chained derived-data build.
         * Precondition: importResult must describe a completed import.
         * Returns: Multi-line summary text for the import view.
         * Postcondition: Result objects are unchanged.
         */
        String summary = "Imported " + importResult.getImportedRows() + " rows into " + importResult.getTableName() + "."
                + System.lineSeparator()
                + "Duplicate rows skipped: " + importResult.getDuplicateRowsSkipped()
                + System.lineSeparator()
                + "Overlapping stored rows removed: " + importResult.getOverlappingRowsRemoved()
                + System.lineSeparator()
                + "Rows skipped outside front-month window: " + importResult.getSkippedOutsideFrontMonthRows() + ".";
        if (buildResult == null) {
            return summary;
        }
        return summary
                + System.lineSeparator()
                + "Built derived data from " + buildResult.getTicksRead() + " tick rows: "
                + buildResult.getSessionRangesBuilt() + " session range(s), "
                + buildResult.getMarketEventOccurrencesBuilt() + " market event occurrence(s).";
    }

    private ContractTradeWindow importedContractWindow(String contractSymbol) {
        /*
         * Intent: Resolve the imported contract into its front-month-valid derived-data build window.
         * Precondition: The import has completed and catalog metadata can see the imported contract.
         * Returns: ContractTradeWindow for the imported contract.
         * Postcondition: Catalog state is unchanged.
         */
        for (AvailableContractData contractData : forgeData.forgeDataAccess().getAvailableContracts()) {
            if (contractData.getContractSymbol().equalsIgnoreCase(contractSymbol)) {
                return new ContractTradeWindow(
                        contractData.getContractSymbol(),
                        contractData.getStartDate(),
                        contractData.getEndDate()
                );
            }
        }
        throw new IllegalStateException("Imported contract is not available for derived data: " + contractSymbol);
    }

    private Set<DerivedDataBuildOption> normalizeDerivedDataOptions(Set<DerivedDataBuildOption> derivedDataOptions) {
        /*
         * Intent: Defensively copy optional post-import derived-data selections.
         * Precondition: Input may be null or empty.
         * Returns: Immutable empty set or immutable EnumSet-backed copy.
         * Postcondition: Later UI changes cannot alter the running task's selected options.
         */
        if (derivedDataOptions == null || derivedDataOptions.isEmpty()) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(EnumSet.copyOf(derivedDataOptions));
    }
}
