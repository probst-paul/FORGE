package forge.gui.preset;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GuiPreferencesStoreTest {
    @TempDir
    Path tempDir;

    @Test
    void defaultStoreUsesProjectRuntimeDirectory() {
        GuiPreferencesStore store = new GuiPreferencesStore();

        assertEquals(Path.of(System.getProperty("user.dir"), "runtime", "gui-preferences.dat"),
                store.getPreferencesPath());
    }

    @Test
    void loadReturnsDefaultsWhenFileDoesNotExist() {
        GuiPreferencesStore store = new GuiPreferencesStore(tempDir.resolve("missing.dat"));

        GuiUserPreferences preferences = store.load();

        assertEquals("", preferences.getLastScidDirectory());
        assertEquals("", preferences.getImportScidFilePath());
    }

    @Test
    void saveAndLoadRoundTripsSerializablePreferences() {
        Path preferencesPath = tempDir.resolve("nested").resolve("gui-preferences.dat");
        GuiPreferencesStore store = new GuiPreferencesStore(preferencesPath);
        GuiUserPreferences preferences = new GuiUserPreferences();
        preferences.setLastScidDirectory("/tmp/scid");
        preferences.setImportScidFilePath("/tmp/scid/ESU25.scid");

        store.save(preferences);
        GuiUserPreferences loaded = store.load();

        assertEquals("/tmp/scid", loaded.getLastScidDirectory());
        assertEquals("/tmp/scid/ESU25.scid", loaded.getImportScidFilePath());
    }
}
