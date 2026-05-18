package forge.app;

import java.time.LocalDate;

public interface UserInput {
    String QUIT_COMMAND = "quit";

    String readString(String label);

    default int readInt(String label) {
        String value = readString(label);
        requireNotQuit(value);
        return Integer.parseInt(value);
    }

    default long readLong(String label) {
        String value = readString(label);
        requireNotQuit(value);
        return Long.parseLong(value);
    }

    default double readDouble(String label) {
        String value = readString(label);
        requireNotQuit(value);
        return Double.parseDouble(value);
    }

    default double readDoubleOrDefault(String label, double defaultValue) {
        String value = readString(label);
        requireNotQuit(value);
        if (value.trim().isEmpty()) {
            return defaultValue;
        }
        return Double.parseDouble(value);
    }

    default String readStringOrDefault(String label, String defaultValue) {
        String value = readString(label);
        requireNotQuit(value);
        if (value.trim().isEmpty()) {
            return defaultValue;
        }
        return value;
    }

    default int readIntOrDefault(String label, int defaultValue) {
        String value = readString(label);
        requireNotQuit(value);
        if (value.trim().isEmpty()) {
            return defaultValue;
        }
        return Integer.parseInt(value);
    }

    default LocalDate readDateOrDefault(String label, LocalDate defaultDate) {
        String value = readString(label);
        requireNotQuit(value);
        if (value.trim().isEmpty()) {
            return defaultDate;
        }
        return LocalDate.parse(value);
    }

    default boolean isQuitCommand(String value) {
        return value != null && QUIT_COMMAND.equalsIgnoreCase(value.trim());
    }

    default void requireNotQuit(String value) {
        if (isQuitCommand(value)) {
            throw new UserQuitException();
        }
    }
}
