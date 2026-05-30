package forge.app;

public interface UserOutput {
    void printLine(String text);

    /*
     * Intent: Print a blank output line through the active output implementation.
     * Precondition: Output target must be writable.
     * Returns: Nothing.
     * Postcondition: One blank line is emitted.
     */
    default void printBlankLine() {
        printLine("");
    }

    /*
     * Intent: Print progress/status text without requiring every output implementation to support same-line updates.
     * Precondition: Output target must be writable.
     * Returns: Nothing.
     * Postcondition: Default implementation emits status as a normal line.
     */
    default void printStatusLine(String text) {
        printLine(text);
    }

    /*
     * Intent: Finish a status-line update sequence for output implementations that need explicit line termination.
     * Precondition: May be called after zero or more status-line updates.
     * Returns: Nothing.
     * Postcondition: Default implementation leaves output unchanged because status lines are already ordinary lines.
     */
    default void finishStatusLine() {
        // Default output implementations print status lines as ordinary lines.
    }
}
