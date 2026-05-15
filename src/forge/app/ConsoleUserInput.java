package forge.app;

import java.util.Scanner;

public class ConsoleUserInput implements UserInput {
    private final Scanner scanner;

    public ConsoleUserInput(Scanner scanner) {
        this.scanner = scanner;
    }

    @Override
    public String readString(String label) {
        System.out.print(label + ": ");
        return scanner.nextLine();
    }
}
