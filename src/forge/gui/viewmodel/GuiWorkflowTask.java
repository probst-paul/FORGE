package forge.gui.viewmodel;

import javafx.concurrent.Task;

public abstract class GuiWorkflowTask<T> extends Task<T> {
    public void publishProgress(long processed, long total) {
        updateProgress(processed, total);
    }

    public void publishStatusMessage(String message) {
        updateMessage(message == null ? "" : message);
    }
}
