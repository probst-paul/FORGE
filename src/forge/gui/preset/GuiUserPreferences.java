package forge.gui.preset;

import java.io.Serial;
import java.io.Serializable;

public class GuiUserPreferences implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String lastScidDirectory = "";
    private String importScidFilePath = "";

    /*
     * Intent: Normalize optional text preference values before persistence.
     * Precondition: value may be null or blank; fallback should be a valid default.
     * Returns: Trimmed value or fallback.
     * Postcondition: Stored preferences do not contain null or accidental blank values.
     */
    private String clean(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }

    public String getLastScidDirectory() {
        return lastScidDirectory;
    }

    public void setLastScidDirectory(String lastScidDirectory) {
        this.lastScidDirectory = clean(lastScidDirectory, "");
    }

    public String getImportScidFilePath() {
        return importScidFilePath;
    }

    public void setImportScidFilePath(String importScidFilePath) {
        this.importScidFilePath = clean(importScidFilePath, "");
    }

}
