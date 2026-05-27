package forge.gui.viewmodel;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;

public class GuiWorkflowViewModel {
    private final StringProperty statusMessage = new SimpleStringProperty("");
    private final StringProperty resultSummary = new SimpleStringProperty("");
    private final StringProperty errorMessage = new SimpleStringProperty("");
    private final DoubleProperty progress = new SimpleDoubleProperty(0.0);
    private final LongProperty processedUnits = new SimpleLongProperty(0);
    private final LongProperty totalUnits = new SimpleLongProperty(0);
    private final BooleanProperty running = new SimpleBooleanProperty(false);

    public String getStatusMessage() {
        return statusMessage.get();
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage.set(statusMessage == null ? "" : statusMessage);
    }

    public StringProperty statusMessageProperty() {
        return statusMessage;
    }

    public String getResultSummary() {
        return resultSummary.get();
    }

    public void setResultSummary(String resultSummary) {
        this.resultSummary.set(resultSummary == null ? "" : resultSummary);
    }

    public StringProperty resultSummaryProperty() {
        return resultSummary;
    }

    public String getErrorMessage() {
        return errorMessage.get();
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage.set(errorMessage == null ? "" : errorMessage);
    }

    public StringProperty errorMessageProperty() {
        return errorMessage;
    }

    public double getProgress() {
        return progress.get();
    }

    public void setProgress(double progress) {
        if (progress < 0.0 || progress > 1.0) {
            throw new IllegalArgumentException("progress must be between 0.0 and 1.0");
        }
        this.progress.set(progress);
    }

    public DoubleProperty progressProperty() {
        return progress;
    }

    public long getProcessedUnits() {
        return processedUnits.get();
    }

    public long getTotalUnits() {
        return totalUnits.get();
    }

    public LongProperty processedUnitsProperty() {
        return processedUnits;
    }

    public LongProperty totalUnitsProperty() {
        return totalUnits;
    }

    public boolean isRunning() {
        return running.get();
    }

    public void setRunning(boolean running) {
        this.running.set(running);
    }

    public BooleanProperty runningProperty() {
        return running;
    }

    public void bindToTask(Task<?> task) {
        if (task == null) {
            throw new IllegalArgumentException("task is required");
        }
        unbindTask();
        statusMessage.bind(task.messageProperty());
        progress.bind(task.progressProperty());
        processedUnits.bind(task.workDoneProperty());
        totalUnits.bind(task.totalWorkProperty());
        running.bind(task.runningProperty());
    }

    public void unbindTask() {
        statusMessage.unbind();
        progress.unbind();
        processedUnits.unbind();
        totalUnits.unbind();
        running.unbind();
    }

    public void markStarted(String statusMessage) {
        unbindTask();
        setRunning(true);
        setProgress(0.0);
        this.processedUnits.set(0);
        this.totalUnits.set(0);
        setStatusMessage(statusMessage);
        setResultSummary("");
        setErrorMessage("");
    }

    public void markSucceeded(String statusMessage, String resultSummary) {
        unbindTask();
        setRunning(false);
        setProgress(1.0);
        if (getTotalUnits() > 0) {
            processedUnits.set(getTotalUnits());
        }
        setStatusMessage(statusMessage);
        setResultSummary(resultSummary);
        setErrorMessage("");
    }

    public void markFailed(String statusMessage, RuntimeException exception) {
        unbindTask();
        setRunning(false);
        setStatusMessage(statusMessage);
        setErrorMessage(exception == null ? "" : exception.getMessage());
    }

    public void updateProgress(long processed, long total) {
        if (processed < 0) {
            throw new IllegalArgumentException("processed cannot be negative");
        }
        if (total < 0) {
            throw new IllegalArgumentException("total cannot be negative");
        }
        if (processed > total) {
            throw new IllegalArgumentException("processed cannot exceed total");
        }
        this.processedUnits.set(processed);
        this.totalUnits.set(total);
        if (total <= 0) {
            setProgress(1.0);
            return;
        }
        setProgress(Math.min(1.0, Math.max(0.0, (double) processed / total)));
    }

    protected long requireNonNegative(long value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " cannot be negative");
        }
        return value;
    }
}
