package forge.app;

public class ConsoleUserOutput implements UserOutput {
    @Override
    public void printLine(String text) {
        System.out.println(text);
    }
}
