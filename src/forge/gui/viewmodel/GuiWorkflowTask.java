package forge.gui.viewmodel;

import javafx.concurrent.Task;

public abstract class GuiWorkflowTask<T> extends Task<T> {
    /*
     * Intent: Let controller progress listeners update JavaFX Task progress with domain units.
     * Precondition: processed and total should represent the same unit type for the running workflow.
     * Returns: Nothing.
     * Postcondition: Bound JavaFX progress controls receive the latest task progress.
     */
    public void publishProgress(long processed, long total) {
        updateProgress(processed, total);
    }

    /*
     * Intent: Publish a non-null status message from a background workflow.
     * Precondition: message may be null.
     * Returns: Nothing.
     * Postcondition: Bound JavaFX labels receive a safe string value.
     */
    public void publishStatusMessage(String message) {
        updateMessage(message == null ? "" : message);
    }
}
