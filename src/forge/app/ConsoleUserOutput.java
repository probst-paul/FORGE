package forge.app;

public class ConsoleUserOutput implements UserOutput {
    @Override
    public void printLine(String text) {
        System.out.println(text);
    }

    @Override
    public void printStatusLine(String text) {
        System.out.print("\r" + text);
    }

    @Override
    public void finishStatusLine() {
        System.out.println();
    }
}
