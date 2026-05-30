package forge.gui.viewmodel;

import forge.app.BacktestProgressListener;
import forge.app.EventStatisticsProgressListener;
import forge.app.ImportProgressListener;
import forge.data.build.DataBuildProgressListener;

public final class GuiProgressBindings {
    private GuiProgressBindings() {
    }

    public static ImportProgressListener importProgress(
            GuiWorkflowViewModel viewModel,
            String statusMessage
    ) {
        /*
         * Intent: Adapt import progress events to direct view model updates.
         * Precondition: viewModel must be writable from the calling thread.
         * Returns: Listener that updates progress and import status text.
         * Postcondition: GUI import progress reflects the latest import callback.
         */
        requireViewModel(viewModel);
        return progress -> {
            viewModel.updateProgress(progress.getProcessedRecords(), progress.getTotalRecords());
            viewModel.setStatusMessage(statusMessage + " " + progress.getContractSymbol() + "...");
        };
    }

    public static ImportProgressListener importProgress(
            GuiWorkflowTask<?> task,
            String statusMessage
    ) {
        /*
         * Intent: Adapt import progress events to JavaFX Task updates.
         * Precondition: task must be the active background workflow task.
         * Returns: Listener that publishes task progress and message updates.
         * Postcondition: Bound JavaFX controls are updated through task-safe APIs.
         */
        requireTask(task);
        return progress -> {
            task.publishProgress(progress.getProcessedRecords(), progress.getTotalRecords());
            task.publishStatusMessage(statusMessage + " " + progress.getContractSymbol() + "...");
        };
    }

    public static DataBuildProgressListener dataBuildProgress(
            GuiWorkflowViewModel viewModel,
            String statusMessage
    ) {
        /*
         * Intent: Adapt derived-data progress events to direct view model updates.
         * Precondition: viewModel must be writable from the calling thread.
         * Returns: Listener that updates processed/total tick counts and status text.
         * Postcondition: GUI derived-data progress reflects the latest build callback.
         */
        requireViewModel(viewModel);
        return progress -> {
            viewModel.updateProgress(progress.getProcessedTicks(), progress.getTotalTicks());
            viewModel.setStatusMessage(statusMessage);
        };
    }

    public static DataBuildProgressListener dataBuildProgress(
            GuiWorkflowTask<?> task,
            String statusMessage
    ) {
        /*
         * Intent: Adapt derived-data progress events to JavaFX Task updates.
         * Precondition: task must be the active background workflow task.
         * Returns: Listener that publishes task progress and message updates.
         * Postcondition: Bound JavaFX controls are updated through task-safe APIs.
         */
        requireTask(task);
        return progress -> {
            task.publishProgress(progress.getProcessedTicks(), progress.getTotalTicks());
            task.publishStatusMessage(statusMessage);
        };
    }

    public static EventStatisticsProgressListener eventStatisticsProgress(
            GuiWorkflowViewModel viewModel,
            String statusMessage
    ) {
        /*
         * Intent: Adapt event-statistics progress events to direct view model updates.
         * Precondition: viewModel must be writable from the calling thread.
         * Returns: Listener that updates processed/total tick counts and status text.
         * Postcondition: GUI statistics progress reflects the latest engine callback.
         */
        requireViewModel(viewModel);
        return progress -> {
            viewModel.updateProgress(progress.getProcessedTicks(), progress.getTotalTicks());
            viewModel.setStatusMessage(statusMessage);
        };
    }

    public static EventStatisticsProgressListener eventStatisticsProgress(
            GuiWorkflowTask<?> task,
            String statusMessage
    ) {
        /*
         * Intent: Adapt event-statistics progress events to JavaFX Task updates.
         * Precondition: task must be the active background workflow task.
         * Returns: Listener that publishes task progress and message updates.
         * Postcondition: Bound JavaFX controls are updated through task-safe APIs.
         */
        requireTask(task);
        return progress -> {
            task.publishProgress(progress.getProcessedTicks(), progress.getTotalTicks());
            task.publishStatusMessage(statusMessage);
        };
    }

    public static BacktestProgressListener backtestProgress(
            GuiWorkflowViewModel viewModel,
            String statusMessage
    ) {
        /*
         * Intent: Adapt backtest progress events to direct view model updates.
         * Precondition: viewModel must be writable from the calling thread.
         * Returns: Listener that updates processed/total tick counts and status text.
         * Postcondition: GUI backtest progress reflects the latest engine callback.
         */
        requireViewModel(viewModel);
        return progress -> {
            viewModel.updateProgress(progress.getProcessedTicks(), progress.getTotalTicks());
            viewModel.setStatusMessage(statusMessage);
        };
    }

    public static BacktestProgressListener backtestProgress(
            GuiWorkflowTask<?> task,
            String statusMessage
    ) {
        /*
         * Intent: Adapt backtest progress events to JavaFX Task updates.
         * Precondition: task must be the active background workflow task.
         * Returns: Listener that publishes task progress and message updates.
         * Postcondition: Bound JavaFX controls are updated through task-safe APIs.
         */
        requireTask(task);
        return progress -> {
            task.publishProgress(progress.getProcessedTicks(), progress.getTotalTicks());
            task.publishStatusMessage(statusMessage);
        };
    }

    private static void requireViewModel(GuiWorkflowViewModel viewModel) {
        if (viewModel == null) {
            throw new IllegalArgumentException("viewModel is required");
        }
    }

    private static void requireTask(GuiWorkflowTask<?> task) {
        if (task == null) {
            throw new IllegalArgumentException("task is required");
        }
    }
}
