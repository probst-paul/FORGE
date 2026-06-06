package forge.gui.concurrency;

import forge.gui.viewmodel.GuiWorkflowType;
import javafx.concurrent.Task;

import java.util.Objects;
import java.util.concurrent.Future;

public class GuiWorkflowJob<T> {
    private final GuiWorkflowType workflowType;
    private final Task<T> task;
    private volatile GuiWorkflowStatus status;
    private volatile Future<?> future;

    public GuiWorkflowJob(GuiWorkflowType workflowType, Task<T> task) {
        if (workflowType == null) {
            throw new IllegalArgumentException("workflowType is required");
        }
        this.workflowType = workflowType;
        this.task = Objects.requireNonNull(task, "task is required");
        this.status = GuiWorkflowStatus.QUEUED;
    }

    public GuiWorkflowType getWorkflowType() {
        return workflowType;
    }

    public Task<T> getTask() {
        return task;
    }

    public GuiWorkflowStatus getStatus() {
        return status;
    }

    public Future<?> getFuture() {
        return future;
    }

    public void attachFuture(Future<?> future) {
        this.future = Objects.requireNonNull(future, "future is required");
    }

    public boolean cancel() {
        /*
         * Intent: Cancel this queued or running workflow if possible.
         * Precondition: The job may or may not have been submitted yet.
         * Returns: true when a submitted future accepted cancellation.
         * Postcondition: Task cancellation is requested and the job status is updated when applicable.
         */
        Future<?> currentFuture = future;
        task.cancel();
        if (currentFuture == null) {
            markCancelled();
            return false;
        }
        return currentFuture.cancel(true);
    }

    void markQueued() {
        status = GuiWorkflowStatus.QUEUED;
    }

    void markRunning() {
        status = GuiWorkflowStatus.RUNNING;
    }

    void markSucceeded() {
        status = GuiWorkflowStatus.SUCCEEDED;
    }

    void markFailed() {
        status = GuiWorkflowStatus.FAILED;
    }

    void markCancelled() {
        status = GuiWorkflowStatus.CANCELLED;
    }
}
