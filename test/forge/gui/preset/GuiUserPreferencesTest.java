package forge.gui.preset;

import org.junit.jupiter.api.Test;

import java.io.Serializable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class GuiUserPreferencesTest {
    @Test
    void defaultsMatchInitialGuiSettings() {
        GuiUserPreferences preferences = new GuiUserPreferences();

        assertEquals("", preferences.getLastScidDirectory());
        assertEquals("", preferences.getImportScidFilePath());
        assertInstanceOf(Serializable.class, preferences);
    }

    @Test
    void textSettersTrimValuesAndUseFallbacksForBlankInput() {
        GuiUserPreferences preferences = new GuiUserPreferences();

        preferences.setLastScidDirectory(" /tmp/scid ");
        preferences.setImportScidFilePath(" /tmp/scid/ESU25.scid ");

        assertEquals("/tmp/scid", preferences.getLastScidDirectory());
        assertEquals("/tmp/scid/ESU25.scid", preferences.getImportScidFilePath());

        preferences.setLastScidDirectory(null);
        preferences.setImportScidFilePath("");

        assertEquals("", preferences.getLastScidDirectory());
        assertEquals("", preferences.getImportScidFilePath());
    }
}
