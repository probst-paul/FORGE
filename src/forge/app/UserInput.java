package forge.app;

import java.time.LocalDate;

public interface UserInput {
    String QUIT_COMMAND = "quit";

    String readString(String label);

    /*
     * Intent: Read a required whole number through the active input implementation.
     * Precondition: Input text must be parseable as an int and must not be the quit command.
     * Returns: Parsed integer.
     * Postcondition: One input value is consumed; UserQuitException is thrown for quit.
     */
    default int readInt(String label) {
        String value = readString(label);
        requireNotQuit(value);
        return Integer.parseInt(value);
    }

    /*
     * Intent: Read a required long integer through the active input implementation.
     * Precondition: Input text must be parseable as a long and must not be the quit command.
     * Returns: Parsed long.
     * Postcondition: One input value is consumed; UserQuitException is thrown for quit.
     */
    default long readLong(String label) {
        String value = readString(label);
        requireNotQuit(value);
        return Long.parseLong(value);
    }

    /*
     * Intent: Read a required decimal number through the active input implementation.
     * Precondition: Input text must be parseable as a double and must not be the quit command.
     * Returns: Parsed double.
     * Postcondition: One input value is consumed; UserQuitException is thrown for quit.
     */
    default double readDouble(String label) {
        String value = readString(label);
        requireNotQuit(value);
        return Double.parseDouble(value);
    }

    /*
     * Intent: Read an optional decimal number, using a default for blank input.
     * Precondition: Nonblank input must be parseable as a double and must not be the quit command.
     * Returns: Parsed double or supplied default.
     * Postcondition: One input value is consumed; UserQuitException is thrown for quit.
     */
    default double readDoubleOrDefault(String label, double defaultValue) {
        String value = readString(label);
        requireNotQuit(value);
        if (value.trim().isEmpty()) {
            return defaultValue;
        }
        return Double.parseDouble(value);
    }

    /*
     * Intent: Read optional text, using a default for blank input.
     * Precondition: Input must not be the quit command.
     * Returns: User text or supplied default.
     * Postcondition: One input value is consumed; UserQuitException is thrown for quit.
     */
    default String readStringOrDefault(String label, String defaultValue) {
        String value = readString(label);
        requireNotQuit(value);
        if (value.trim().isEmpty()) {
            return defaultValue;
        }
        return value;
    }

    /*
     * Intent: Read an optional whole number, using a default for blank input.
     * Precondition: Nonblank input must be parseable as an int and must not be the quit command.
     * Returns: Parsed integer or supplied default.
     * Postcondition: One input value is consumed; UserQuitException is thrown for quit.
     */
    default int readIntOrDefault(String label, int defaultValue) {
        String value = readString(label);
        requireNotQuit(value);
        if (value.trim().isEmpty()) {
            return defaultValue;
        }
        return Integer.parseInt(value);
    }

    /*
     * Intent: Read an optional ISO local date, using a default for blank input.
     * Precondition: Nonblank input must be parseable by LocalDate.parse and must not be the quit command.
     * Returns: Parsed LocalDate or supplied default.
     * Postcondition: One input value is consumed; UserQuitException is thrown for quit.
     */
    default LocalDate readDateOrDefault(String label, LocalDate defaultDate) {
        String value = readString(label);
        requireNotQuit(value);
        if (value.trim().isEmpty()) {
            return defaultDate;
        }
        return LocalDate.parse(value);
    }

    /*
     * Intent: Detect the shared command for graceful user exit.
     * Precondition: Value may be null or any user-entered text.
     * Returns: True when value equals quit after trimming and ignoring case.
     * Postcondition: Input value is not modified.
     */
    default boolean isQuitCommand(String value) {
        return value != null && QUIT_COMMAND.equalsIgnoreCase(value.trim());
    }

    /*
     * Intent: Enforce graceful exit handling for any input path.
     * Precondition: Value may be null or any user-entered text.
     * Returns: Nothing.
     * Postcondition: UserQuitException is thrown when the quit command is entered.
     */
    default void requireNotQuit(String value) {
        if (isQuitCommand(value)) {
            throw new UserQuitException();
        }
    }
}
