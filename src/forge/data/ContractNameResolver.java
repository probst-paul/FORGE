package forge.data;

import java.nio.file.Path;
import java.util.Locale;

public class ContractNameResolver {
    public String resolveFromScidPath(String scidFilePath) {
        if (scidFilePath == null || scidFilePath.trim().isEmpty()) {
            throw new IllegalArgumentException("SCID file path is required");
        }

        String normalizedPath = scidFilePath.trim().replace('\\', '/');
        String fileName = Path.of(normalizedPath).getFileName().toString();
        String upperFileName = fileName.toUpperCase(Locale.ROOT);
        if (!upperFileName.endsWith(".SCID")) {
            throw new IllegalArgumentException("SCID file path must end with .scid");
        }

        String baseName = fileName.substring(0, fileName.length() - ".scid".length());
        int underscoreIndex = baseName.indexOf('_');
        int dotIndex = baseName.indexOf('.');
        int separatorIndex = firstPresentIndex(underscoreIndex, dotIndex);
        String contract = separatorIndex >= 0 ? baseName.substring(0, separatorIndex) : baseName;
        contract = contract.toUpperCase(Locale.ROOT);

        if (!contract.matches("[A-Z]{1,3}[FGHJKMNQUVXZ][0-9]{1,2}")) {
            throw new IllegalArgumentException("Could not derive a futures contract from SCID file name: " + fileName);
        }

        return contract;
    }

    private int firstPresentIndex(int first, int second) {
        if (first < 0) {
            return second;
        }
        if (second < 0) {
            return first;
        }
        return Math.min(first, second);
    }
}
