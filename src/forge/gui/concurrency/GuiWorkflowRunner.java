package forge.gui.concurrency;

import forge.gui.viewmodel.GuiWorkflowType;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.animation.PauseTransition;
import javafx.util.Duration;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class GuiWorkflowRunner {
    private static final Duration FINAL_TASK_DISPLAY_DURATION = Duration.seconds(2);

    private final BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>();
    private final ExecutorService executor;
    private final IntegerProperty runningTaskCount = new SimpleIntegerProperty(0);
    private final IntegerProperty queuedTaskCount = new SimpleIntegerProperty(0);
    private final StringProperty taskStatusText = new SimpleStringProperty("");
    private final StringProperty taskQueueText = new SimpleStringProperty("");
    private final StringProperty taskPercentText = new SimpleStringProperty("");
    private final StringProperty currentTaskQueueText = new SimpleStringProperty("");
    private final DoubleProperty currentTaskProgress = new SimpleDoubleProperty(0.0);
    private final BooleanProperty taskIndicatorVisible = new SimpleBooleanProperty(false);

    public GuiWorkflowRunner() {
        /*
         * Intent: Create the shared single-lane executor used by GUI workflows.
         * Precondition: Must be constructed from the JavaFX application setup path.
         * Returns: New runner instance.
         * Postcondition: Background workflow tasks run on a daemon thread instead of the UI thread.
         */
        this.executor = new ThreadPoolExecutor(
                1,
                1,
                0L,
                TimeUnit.MILLISECONDS,
                workQueue,
                daemonThreadFactory()
        );
    }

    public <T> GuiWorkflowJob<T> submit(GuiWorkflowType workflowType, Task<T> task) {
        /*
         * Intent: Queue a JavaFX workflow task on the shared GUI background executor.
         * Precondition: workflowType and task must identify one GUI workflow operation.
         * Returns: Job wrapper containing status and Future information.
         * Postcondition: The task is queued and will run off the JavaFX application thread.
         */
        GuiWorkflowJob<T> job = new GuiWorkflowJob<>(workflowType, task);
        return submit(job);
    }

    public <T> GuiWorkflowJob<T> submit(GuiWorkflowJob<T> job) {
        /*
         * Intent: Submit a prepared workflow job to the single shared GUI workflow queue.
         * Precondition: job must contain a JavaFX Task that has not already completed.
         * Returns: The same job with its Future attached.
         * Postcondition: Job status transitions are bound to JavaFX task lifecycle events.
         */
        Objects.requireNonNull(job, "job is required");
        job.markQueued();
        queuedTaskCount.set(queuedTaskCount.get() + 1);
        updateTaskIndicator();
        Task<T> task = job.getTask();
        task.progressProperty().addListener((observable, oldValue, newValue) -> {
            if (job.getStatus() == GuiWorkflowStatus.RUNNING) {
                currentTaskProgress.set(normalizeProgress(newValue.doubleValue()));
                updateTaskIndicator();
            }
        });
        task.titleProperty().addListener((observable, oldValue, newValue) -> {
            if (job.getStatus() == GuiWorkflowStatus.RUNNING) {
                currentTaskQueueText.set(newValue == null ? "" : newValue);
                updateTaskIndicator();
            }
        });
        task.addEventHandler(WorkerStateEvent.WORKER_STATE_RUNNING, event -> {
            job.markRunning();
            queuedTaskCount.set(Math.max(0, queuedTaskCount.get() - 1));
            runningTaskCount.set(runningTaskCount.get() + 1);
            currentTaskQueueText.set(task.getTitle() == null ? "" : task.getTitle());
            currentTaskProgress.set(normalizeProgress(task.getProgress()));
            updateTaskIndicator();
        });
        task.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED, event -> {
            job.markSucceeded();
            currentTaskProgress.set(1.0);
            finishTask(job, true);
        });
        task.addEventHandler(WorkerStateEvent.WORKER_STATE_FAILED, event -> {
            GuiWorkflowStatus previousStatus = job.getStatus();
            job.markFailed();
            finishTask(previousStatus, false);
        });
        task.addEventHandler(WorkerStateEvent.WORKER_STATE_CANCELLED, event -> {
            GuiWorkflowStatus previousStatus = job.getStatus();
            job.markCancelled();
            finishTask(previousStatus, false);
        });
        Future<?> future = executor.submit(task);
        job.attachFuture(future);
        return job;
    }

    public ReadOnlyStringProperty taskStatusTextProperty() {
        return taskStatusText;
    }

    public ReadOnlyStringProperty taskQueueTextProperty() {
        return taskQueueText;
    }

    public ReadOnlyStringProperty taskPercentTextProperty() {
        return taskPercentText;
    }

    public ReadOnlyDoubleProperty currentTaskProgressProperty() {
        return currentTaskProgress;
    }

    public ReadOnlyBooleanProperty taskIndicatorVisibleProperty() {
        return taskIndicatorVisible;
    }

    public int getQueuedTaskCount() {
        return workQueue.size();
    }

    public void shutdown() {
        /*
         * Intent: Stop any queued GUI workflow work when the GUI is closing.
         * Precondition: May be called once or repeatedly during application shutdown.
         * Returns: Nothing.
         * Postcondition: Running tasks are interrupted and queued tasks are discarded.
         */
        executor.shutdownNow();
        runningTaskCount.set(0);
        queuedTaskCount.set(0);
        updateTaskIndicator();
    }

    private void finishTask(GuiWorkflowJob<?> job, boolean completedSuccessfully) {
        /*
         * Intent: Finish a task using its current tracked workflow status.
         * Precondition: job must have completed, failed, or been cancelled.
         * Returns: Nothing.
         * Postcondition: Queue/running counts are reconciled with task completion.
         */
        finishTask(job.getStatus(), completedSuccessfully);
    }

    private void finishTask(GuiWorkflowStatus previousStatus, boolean completedSuccessfully) {
        /*
         * Intent: Update aggregate GUI task state after one workflow leaves the queue or runner.
         * Precondition: previousStatus must describe whether the job was queued or running.
         * Returns: Nothing.
         * Postcondition: Task indicator either advances, briefly shows final success, or hides.
         */
        if (previousStatus == GuiWorkflowStatus.QUEUED) {
            queuedTaskCount.set(Math.max(0, queuedTaskCount.get() - 1));
        } else {
            runningTaskCount.set(Math.max(0, runningTaskCount.get() - 1));
        }
        if (completedSuccessfully && runningTaskCount.get() + queuedTaskCount.get() == 0) {
            taskIndicatorVisible.set(true);
            String queueText = currentTaskQueueText.get() == null || currentTaskQueueText.get().isBlank()
                    ? "Task 1/1"
                    : currentTaskQueueText.get();
            setTaskDisplay(queueText, "100%");
            PauseTransition pause = new PauseTransition(FINAL_TASK_DISPLAY_DURATION);
            pause.setOnFinished(event -> updateTaskIndicator());
            pause.play();
            return;
        }
        updateTaskIndicator();
    }

    private void updateTaskIndicator() {
        /*
         * Intent: Recompute the footer task indicator from queued/running workflow counts.
         * Precondition: Count properties must reflect current executor state.
         * Returns: Nothing.
         * Postcondition: Footer visibility, queue text, percent text, and progress value are consistent.
         */
        int activeTaskCount = runningTaskCount.get() + queuedTaskCount.get();
        boolean visible = activeTaskCount > 0;
        taskIndicatorVisible.set(visible);
        if (!visible) {
            setTaskDisplay("", "");
            currentTaskQueueText.set("");
            currentTaskProgress.set(0.0);
            return;
        }
        String queueText = currentTaskQueueText.get() == null || currentTaskQueueText.get().isBlank()
                ? "Task 1/" + activeTaskCount
                : currentTaskQueueText.get();
        setTaskDisplay(queueText, progressPercent() + "%");
    }

    private void setTaskDisplay(String queueText, String percentText) {
        /*
         * Intent: Publish task display text through JavaFX observable properties.
         * Precondition: Values may be null when the indicator should be cleared.
         * Returns: Nothing.
         * Postcondition: Bound footer labels receive normalized non-null text.
         */
        String safeQueueText = queueText == null ? "" : queueText;
        String safePercentText = percentText == null ? "" : percentText;
        taskQueueText.set(safeQueueText);
        taskPercentText.set(safePercentText);
        taskStatusText.set((safeQueueText + " " + safePercentText).trim());
    }

    private int progressPercent() {
        /*
         * Intent: Convert normalized task progress into a whole-number percentage.
         * Precondition: currentTaskProgress may contain any JavaFX task progress value.
         * Returns: Clamped percentage from 0 through 100.
         * Postcondition: Progress state is unchanged.
         */
        return (int) Math.round(normalizeProgress(currentTaskProgress.get()) * 100.0);
    }

    private double normalizeProgress(double progress) {
        /*
         * Intent: Clamp JavaFX progress values into a footer-safe display range.
         * Precondition: progress may be indeterminate, negative, or greater than complete.
         * Returns: 0.0 through 1.0.
         * Postcondition: No observable state is changed.
         */
        if (Double.isNaN(progress) || progress < 0.0) {
            return 0.0;
        }
        return Math.min(1.0, Math.max(0.0, progress));
    }

    private ThreadFactory daemonThreadFactory() {
        /*
         * Intent: Create named daemon threads for GUI background workflow execution.
         * Precondition: Runnable will be supplied by the executor.
         * Returns: ThreadFactory that creates daemon workflow threads.
         * Postcondition: GUI shutdown is not blocked by the workflow executor thread.
         */
        return runnable -> {
            Thread thread = new Thread(runnable, "forge-gui-workflow-runner");
            thread.setDaemon(true);
            return thread;
        };
    }
}
