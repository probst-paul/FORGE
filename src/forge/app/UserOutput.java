package forge.app;

public interface UserOutput {
    void printLine(String text);

    default void printBlankLine() {
        printLine("");
    }

    default void printStatusLine(String text) {
        printLine(text);
    }

    default void finishStatusLine() {
        // Default output implementations print status lines as ordinary lines.
    }
}
