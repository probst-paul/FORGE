package forge.app;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class ConsoleUserInput implements UserInput {
    private final Scanner scanner;

    public ConsoleUserInput(Scanner scanner) {
        /*
         * Intent: Adapt Scanner-based console input to the UserInput interface.
         * Precondition: Scanner should be open and connected to the desired input stream.
         * Returns: A constructed ConsoleUserInput instance.
         * Postcondition: Future reads are delegated to the provided scanner.
         */
        this.scanner = scanner;
    }

    @Override
    public String readString(String label) {
        /*
         * Intent: Prompt for one line of console text while honoring the global quit command.
         * Precondition: Label should describe the requested value and scanner must be readable.
         * Returns: Raw user-entered text, except the quit command.
         * Postcondition: One input line is consumed or UserQuitException is thrown.
         */
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
        /*
         * Intent: Read and validate a whole-number console value.
         * Precondition: Label should describe the requested integer.
         * Returns: Parsed integer.
         * Postcondition: Invalid input is rejected with a reprompt; quit exits via UserQuitException.
         */
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
        /*
         * Intent: Read and validate a decimal numeric console value.
         * Precondition: Label should describe the requested number.
         * Returns: Parsed double.
         * Postcondition: Invalid input is rejected with a reprompt; quit exits via UserQuitException.
         */
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
        /*
         * Intent: Read an optional whole number, using a default when the user leaves input blank.
         * Precondition: Label should describe the requested integer and default value should be acceptable to the caller.
         * Returns: Parsed integer or the supplied default.
         * Postcondition: Invalid nonblank input is rejected with a reprompt; quit exits via UserQuitException.
         */
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
        /*
         * Intent: Read an optional ISO local date, using a default when the user leaves input blank.
         * Precondition: Label should describe the requested date and default date should be acceptable to the caller.
         * Returns: Parsed LocalDate or the supplied default.
         * Postcondition: Invalid nonblank input is rejected with a reprompt; quit exits via UserQuitException.
         */
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
