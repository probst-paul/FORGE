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
    private final DoubleProperty currentTaskProgress = new SimpleDoubleProperty(0.0);
    private final BooleanProperty taskIndicatorVisible = new SimpleBooleanProperty(false);

    public GuiWorkflowRunner() {
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
        task.addEventHandler(WorkerStateEvent.WORKER_STATE_RUNNING, event -> {
            job.markRunning();
            queuedTaskCount.set(Math.max(0, queuedTaskCount.get() - 1));
            runningTaskCount.set(runningTaskCount.get() + 1);
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
        finishTask(job.getStatus(), completedSuccessfully);
    }

    private void finishTask(GuiWorkflowStatus previousStatus, boolean completedSuccessfully) {
        if (previousStatus == GuiWorkflowStatus.QUEUED) {
            queuedTaskCount.set(Math.max(0, queuedTaskCount.get() - 1));
        } else {
            runningTaskCount.set(Math.max(0, runningTaskCount.get() - 1));
        }
        if (completedSuccessfully && runningTaskCount.get() + queuedTaskCount.get() == 0) {
            taskIndicatorVisible.set(true);
            setTaskDisplay("Task 1/1", "100%");
            PauseTransition pause = new PauseTransition(FINAL_TASK_DISPLAY_DURATION);
            pause.setOnFinished(event -> updateTaskIndicator());
            pause.play();
            return;
        }
        updateTaskIndicator();
    }

    private void updateTaskIndicator() {
        int activeTaskCount = runningTaskCount.get() + queuedTaskCount.get();
        boolean visible = activeTaskCount > 0;
        taskIndicatorVisible.set(visible);
        if (!visible) {
            setTaskDisplay("", "");
            currentTaskProgress.set(0.0);
            return;
        }
        setTaskDisplay("Task 1/" + activeTaskCount, progressPercent() + "%");
    }

    private void setTaskDisplay(String queueText, String percentText) {
        String safeQueueText = queueText == null ? "" : queueText;
        String safePercentText = percentText == null ? "" : percentText;
        taskQueueText.set(safeQueueText);
        taskPercentText.set(safePercentText);
        taskStatusText.set((safeQueueText + " " + safePercentText).trim());
    }

    private int progressPercent() {
        return (int) Math.round(normalizeProgress(currentTaskProgress.get()) * 100.0);
    }

    private double normalizeProgress(double progress) {
        if (Double.isNaN(progress) || progress < 0.0) {
            return 0.0;
        }
        return Math.min(1.0, Math.max(0.0, progress));
    }

    private ThreadFactory daemonThreadFactory() {
        return runnable -> {
            Thread thread = new Thread(runnable, "forge-gui-workflow-runner");
            thread.setDaemon(true);
            return thread;
        };
    }
}
