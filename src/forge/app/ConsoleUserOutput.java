package forge.app;

public class ConsoleUserOutput implements UserOutput {
    /*
     * Intent: Print one complete line to the console.
     * Precondition: Console output stream must be writable.
     * Returns: Nothing.
     * Postcondition: Text is written followed by a newline.
     */
    @Override
    public void printLine(String text) {
        System.out.println(text);
    }

    /*
     * Intent: Render progress/status text on a reusable console line.
     * Precondition: Console output should support carriage-return line updates.
     * Returns: Nothing.
     * Postcondition: Current console line is overwritten with the supplied text where supported.
     */
    @Override
    public void printStatusLine(String text) {
        System.out.print("\r" + text);
    }

    /*
     * Intent: Complete a same-line status update sequence.
     * Precondition: May be called after zero or more printStatusLine calls.
     * Returns: Nothing.
     * Postcondition: Console cursor advances to the next line.
     */
    @Override
    public void finishStatusLine() {
        System.out.println();
    }
}
