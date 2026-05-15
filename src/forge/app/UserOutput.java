package forge.app;

public interface UserOutput {
    void printLine(String text);

    default void printBlankLine() {
        printLine("");
    }
}
