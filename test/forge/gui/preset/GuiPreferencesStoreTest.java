package forge.gui.preset;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuiPreferencesStoreTest {
    @TempDir
    Path tempDir;

    @Test
    void loadReturnsDefaultsWhenFileDoesNotExist() {
        GuiPreferencesStore store = new GuiPreferencesStore(tempDir.resolve("missing.dat"));

        GuiUserPreferences preferences = store.load();

        assertEquals("localhost", preferences.getDatabaseHost());
        assertEquals(5432, preferences.getDatabasePort());
        assertEquals("forge", preferences.getDatabaseName());
        assertEquals("postgres", preferences.getMaintenanceDatabaseName());
        assertEquals("postgres", preferences.getDatabaseUsername());
        assertTrue(preferences.shouldBenchmarkRebuildExistingContract());
        assertTrue(preferences.shouldBenchmarkRebuildDerivedData());
    }

    @Test
    void saveAndLoadRoundTripsSerializablePreferences() {
        Path preferencesPath = tempDir.resolve("nested").resolve("gui-preferences.dat");
        GuiPreferencesStore store = new GuiPreferencesStore(preferencesPath);
        GuiUserPreferences preferences = new GuiUserPreferences();
        preferences.setDatabaseHost("db.local");
        preferences.setDatabasePort(15432);
        preferences.setDatabaseName("forge_test");
        preferences.setMaintenanceDatabaseName("postgres_test");
        preferences.setDatabaseUsername("forge_user");
        preferences.setLastScidDirectory("/tmp/scid");
        preferences.setImportScidFilePath("/tmp/scid/ESU25.scid");
        preferences.setBenchmarkScidFilePath("/tmp/scid/YMU25.scid");
        preferences.setBenchmarkRebuildExistingContract(false);
        preferences.setBenchmarkRebuildDerivedData(false);

        store.save(preferences);
        GuiUserPreferences loaded = store.load();

        assertEquals("db.local", loaded.getDatabaseHost());
        assertEquals(15432, loaded.getDatabasePort());
        assertEquals("forge_test", loaded.getDatabaseName());
        assertEquals("postgres_test", loaded.getMaintenanceDatabaseName());
        assertEquals("forge_user", loaded.getDatabaseUsername());
        assertEquals("/tmp/scid", loaded.getLastScidDirectory());
        assertEquals("/tmp/scid/ESU25.scid", loaded.getImportScidFilePath());
        assertEquals("/tmp/scid/YMU25.scid", loaded.getBenchmarkScidFilePath());
        assertFalse(loaded.shouldBenchmarkRebuildExistingContract());
        assertFalse(loaded.shouldBenchmarkRebuildDerivedData());
    }
}
