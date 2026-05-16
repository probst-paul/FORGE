package forge.app;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class ConsoleUserInput implements UserInput {
    private final Scanner scanner;

    public ConsoleUserInput(Scanner scanner) {
        this.scanner = scanner;
    }

    @Override
    public String readString(String label) {
        System.out.print(label + ": ");
        String value;
        try {
            value = scanner.nextLine();
        } catch (IllegalStateException | NoSuchElementException exception) {
            throw new UserQuitException();
        }
        requireNotQuit(value);
        return value;
    }

    @Override
    public int readInt(String label) {
        while (true) {
            String value = readString(label);
            try {
                return Integer.parseInt(value.trim());
            } catch (NumberFormatException exception) {
                System.out.println("Please enter a whole number, or enter 'quit' to exit program.");
            }
        }
    }

    @Override
    public double readDouble(String label) {
        while (true) {
            String value = readString(label);
            try {
                return Double.parseDouble(value.trim());
            } catch (NumberFormatException exception) {
                System.out.println("Please enter a number, or enter 'quit' to exit program.");
            }
        }
    }

    @Override
    public int readIntOrDefault(String label, int defaultValue) {
        while (true) {
            String value = readString(label);
            if (value.trim().isEmpty()) {
                return defaultValue;
            }
            try {
                return Integer.parseInt(value.trim());
            } catch (NumberFormatException exception) {
                System.out.println("Please enter a whole number, leave blank for the default, or enter 'quit' to exit program.");
            }
        }
    }

    @Override
    public LocalDate readDateOrDefault(String label, LocalDate defaultDate) {
        while (true) {
            String value = readString(label);
            if (value.trim().isEmpty()) {
                return defaultDate;
            }
            try {
                return LocalDate.parse(value.trim());
            } catch (DateTimeParseException exception) {
                System.out.println("Please enter a date as YYYY-MM-DD, leave blank for the default, or enter 'quit' to exit program.");
            }
        }
    }
}
