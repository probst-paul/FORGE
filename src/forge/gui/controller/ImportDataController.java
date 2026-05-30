package forge.gui.controller;

import forge.app.DataImportRequest;
import forge.app.FacadeForgeApplication;
import forge.data.importing.DataImportPlan;
import forge.data.importing.DataImportResult;
import forge.gui.viewmodel.GuiProgressBindings;
import forge.gui.viewmodel.ImportDataViewModel;
import javafx.concurrent.Task;

public class ImportDataController {
    private final FacadeForgeApplication forgeApplication;
    private final ImportDataViewModel viewModel;

    public ImportDataController() {
        this(FacadeForgeApplication.getTheInstance(), new ImportDataViewModel());
    }

    public ImportDataController(FacadeForgeApplication forgeApplication, ImportDataViewModel viewModel) {
        if (forgeApplication == null) {
            throw new IllegalArgumentException("forgeApplication is required");
        }
        if (viewModel == null) {
            throw new IllegalArgumentException("viewModel is required");
        }
        this.forgeApplication = forgeApplication;
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
        /*
         * Intent: Run SCID import synchronously for tests or non-task GUI callers.
         * Precondition: scidFilePath must be valid and rebuildExistingContract must reflect user confirmation.
         * Returns: Completed import result.
         * Postcondition: The view model is marked succeeded or failed with import details.
         */
        viewModel.setScidFilePath(scidFilePath);
        viewModel.setRebuildExistingContract(rebuildExistingContract);
        viewModel.markStarted("Importing data...");
        try {
            DataImportResult result = forgeApplication.forgeApplicationAccess().importData(new DataImportRequest(
                    scidFilePath,
                    rebuildExistingContract,
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
        /*
         * Intent: Create a JavaFX task for importing SCID data off the UI thread.
         * Precondition: scidFilePath must be valid and rebuildExistingContract must reflect user confirmation.
         * Returns: Task that yields the import result.
         * Postcondition: Task progress callbacks update the view model through JavaFX bindings.
         */
        viewModel.setScidFilePath(scidFilePath);
        viewModel.setRebuildExistingContract(rebuildExistingContract);
        return GuiControllerTasks.create(
                viewModel,
                "Importing data...",
                "Could not import data.",
                task -> forgeApplication.forgeApplicationAccess().importData(new DataImportRequest(
                        scidFilePath,
                        rebuildExistingContract,
                        GuiProgressBindings.importProgress(task, "Importing")
                )),
                this::applyImportResult
        );
    }

    private void applyImportResult(DataImportResult result) {
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
                "Imported " + result.getImportedRows() + " rows into " + result.getTableName() + "."
        );
    }
}
