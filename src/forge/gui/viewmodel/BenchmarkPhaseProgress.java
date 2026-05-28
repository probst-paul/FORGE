package forge.gui.viewmodel;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.time.Duration;

public class BenchmarkPhaseProgress {
    private final String phaseName;
    private final StringProperty statusMessage = new SimpleStringProperty("Not started.");
    private final StringProperty elapsedTime = new SimpleStringProperty("elapsed 0.000s");
    private final DoubleProperty progress = new SimpleDoubleProperty(0.0);
    private final LongProperty processedUnits = new SimpleLongProperty(0);
    private final LongProperty totalUnits = new SimpleLongProperty(0);
    private boolean running;
    private long startedAtNanos;

    public BenchmarkPhaseProgress(String phaseName) {
        if (phaseName == null || phaseName.trim().isEmpty()) {
            throw new IllegalArgumentException("phaseName is required");
        }
        this.phaseName = phaseName.trim();
    }

    public String getPhaseName() {
        return phaseName;
    }

    public String getStatusMessage() {
        return statusMessage.get();
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage.set(statusMessage == null ? "" : statusMessage);
    }

    public StringProperty statusMessageProperty() {
        return statusMessage;
    }

    public String getElapsedTime() {
        return elapsedTime.get();
    }

    public void setElapsedTime(Duration duration) {
        if (duration == null || duration.isNegative()) {
            throw new IllegalArgumentException("duration is required");
        }
        running = false;
        elapsedTime.set("elapsed " + formatDuration(duration));
    }

    public StringProperty elapsedTimeProperty() {
        return elapsedTime;
    }

    public double getProgress() {
        return progress.get();
    }

    public DoubleProperty progressProperty() {
        return progress;
    }

    public long getProcessedUnits() {
        return processedUnits.get();
    }

    public LongProperty processedUnitsProperty() {
        return processedUnits;
    }

    public long getTotalUnits() {
        return totalUnits.get();
    }

    public LongProperty totalUnitsProperty() {
        return totalUnits;
    }

    public void reset() {
        setStatusMessage("Not started.");
        elapsedTime.set("elapsed 0.000s");
        processedUnits.set(0);
        totalUnits.set(0);
        progress.set(0.0);
        running = false;
        startedAtNanos = 0;
    }

    public void updateProgress(long processed, long total, String statusMessage) {
        if (processed < 0) {
            throw new IllegalArgumentException("processed cannot be negative");
        }
        if (total < 0) {
            throw new IllegalArgumentException("total cannot be negative");
        }
        if (processed > total) {
            throw new IllegalArgumentException("processed cannot exceed total");
        }
        markStartedIfNeeded();
        processedUnits.set(processed);
        totalUnits.set(total);
        progress.set(total == 0 ? 1.0 : Math.min(1.0, Math.max(0.0, (double) processed / total)));
        setStatusMessage(statusMessage);
        refreshElapsedTime();
    }

    public void markComplete(long processed, long total, String statusMessage) {
        updateProgress(processed, total, statusMessage);
        running = false;
        progress.set(1.0);
    }

    public void stopElapsedTimer() {
        if (running) {
            refreshElapsedTime();
            running = false;
        }
    }

    public void refreshElapsedTime() {
        if (running && startedAtNanos > 0) {
            elapsedTime.set("elapsed " + formatDuration(Duration.ofNanos(System.nanoTime() - startedAtNanos)));
        }
    }

    private void markStartedIfNeeded() {
        if (!running && startedAtNanos == 0) {
            running = true;
            startedAtNanos = System.nanoTime();
        }
    }

    private String formatDuration(Duration duration) {
        return String.format("%.3fs", duration.toMillis() / 1000.0);
    }
}
