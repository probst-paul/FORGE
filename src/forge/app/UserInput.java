package forge.app;

import java.time.LocalDate;

public interface UserInput {
    String readString(String label);

    default int readInt(String label) {
        return Integer.parseInt(readString(label));
    }

    default double readDouble(String label) {
        return Double.parseDouble(readString(label));
    }

    default String readStringOrDefault(String label, String defaultValue) {
        String value = readString(label);
        if (value.trim().isEmpty()) {
            return defaultValue;
        }
        return value;
    }

    default int readIntOrDefault(String label, int defaultValue) {
        String value = readString(label);
        if (value.trim().isEmpty()) {
            return defaultValue;
        }
        return Integer.parseInt(value);
    }

    default LocalDate readDateOrDefault(String label, LocalDate defaultDate) {
        String value = readString(label);
        if (value.trim().isEmpty()) {
            return defaultDate;
        }
        return LocalDate.parse(value);
    }
}
