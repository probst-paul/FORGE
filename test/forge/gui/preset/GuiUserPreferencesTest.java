package forge.gui.preset;

import org.junit.jupiter.api.Test;

import java.io.Serializable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuiUserPreferencesTest {
    @Test
    void defaultsMatchInitialGuiSettings() {
        GuiUserPreferences preferences = new GuiUserPreferences();

        assertEquals("localhost", preferences.getDatabaseHost());
        assertEquals(5432, preferences.getDatabasePort());
        assertEquals("forge", preferences.getDatabaseName());
        assertEquals("postgres", preferences.getMaintenanceDatabaseName());
        assertEquals("postgres", preferences.getDatabaseUsername());
        assertEquals("", preferences.getLastScidDirectory());
        assertEquals("", preferences.getImportScidFilePath());
        assertEquals("", preferences.getBenchmarkScidFilePath());
        assertTrue(preferences.shouldBenchmarkRebuildExistingContract());
        assertTrue(preferences.shouldBenchmarkRebuildDerivedData());
        assertInstanceOf(Serializable.class, preferences);
    }

    @Test
    void textSettersTrimValuesAndUseFallbacksForBlankInput() {
        GuiUserPreferences preferences = new GuiUserPreferences();

        preferences.setDatabaseHost(" db.local ");
        preferences.setDatabaseName(" forge_replay ");
        preferences.setMaintenanceDatabaseName(" postgres_admin ");
        preferences.setDatabaseUsername(" forge_user ");
        preferences.setLastScidDirectory(" /tmp/scid ");
        preferences.setImportScidFilePath(" /tmp/scid/ESU25.scid ");
        preferences.setBenchmarkScidFilePath(" /tmp/scid/YMU25.scid ");

        assertEquals("db.local", preferences.getDatabaseHost());
        assertEquals("forge_replay", preferences.getDatabaseName());
        assertEquals("postgres_admin", preferences.getMaintenanceDatabaseName());
        assertEquals("forge_user", preferences.getDatabaseUsername());
        assertEquals("/tmp/scid", preferences.getLastScidDirectory());
        assertEquals("/tmp/scid/ESU25.scid", preferences.getImportScidFilePath());
        assertEquals("/tmp/scid/YMU25.scid", preferences.getBenchmarkScidFilePath());

        preferences.setDatabaseHost(" ");
        preferences.setDatabaseName(null);
        preferences.setMaintenanceDatabaseName("");
        preferences.setDatabaseUsername("   ");
        preferences.setLastScidDirectory(null);
        preferences.setImportScidFilePath("");
        preferences.setBenchmarkScidFilePath(" ");

        assertEquals("localhost", preferences.getDatabaseHost());
        assertEquals("forge", preferences.getDatabaseName());
        assertEquals("postgres", preferences.getMaintenanceDatabaseName());
        assertEquals("postgres", preferences.getDatabaseUsername());
        assertEquals("", preferences.getLastScidDirectory());
        assertEquals("", preferences.getImportScidFilePath());
        assertEquals("", preferences.getBenchmarkScidFilePath());
    }

    @Test
    void portMustBeInTcpPortRange() {
        GuiUserPreferences preferences = new GuiUserPreferences();

        preferences.setDatabasePort(1);
        assertEquals(1, preferences.getDatabasePort());
        preferences.setDatabasePort(65535);
        assertEquals(65535, preferences.getDatabasePort());

        assertThrows(IllegalArgumentException.class, () -> preferences.setDatabasePort(0));
        assertThrows(IllegalArgumentException.class, () -> preferences.setDatabasePort(65536));
    }

    @Test
    void benchmarkFlagsCanBeDisabled() {
        GuiUserPreferences preferences = new GuiUserPreferences();

        preferences.setBenchmarkRebuildExistingContract(false);
        preferences.setBenchmarkRebuildDerivedData(false);

        assertFalse(preferences.shouldBenchmarkRebuildExistingContract());
        assertFalse(preferences.shouldBenchmarkRebuildDerivedData());
    }
}
