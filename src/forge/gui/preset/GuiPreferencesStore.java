package forge.gui.preset;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class GuiPreferencesStore {
    private static final Path DEFAULT_PATH = Path.of(
            System.getProperty("user.home"),
            ".forge",
            "gui-preferences.dat"
    );

    private final Path preferencesPath;

    public GuiPreferencesStore() {
        this(DEFAULT_PATH);
    }

    public GuiPreferencesStore(Path preferencesPath) {
        if (preferencesPath == null) {
            throw new IllegalArgumentException("preferencesPath is required");
        }
        this.preferencesPath = preferencesPath;
    }

    public GuiUserPreferences load() {
        if (!Files.exists(preferencesPath)) {
            return new GuiUserPreferences();
        }
        try (ObjectInputStream input = new ObjectInputStream(Files.newInputStream(preferencesPath))) {
            Object loaded = input.readObject();
            if (loaded instanceof GuiUserPreferences preferences) {
                return preferences;
            }
            return new GuiUserPreferences();
        } catch (IOException | ClassNotFoundException exception) {
            return new GuiUserPreferences();
        }
    }

    public void save(GuiUserPreferences preferences) {
        if (preferences == null) {
            throw new IllegalArgumentException("preferences is required");
        }
        try {
            Path parent = preferencesPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (ObjectOutputStream output = new ObjectOutputStream(Files.newOutputStream(preferencesPath))) {
                output.writeObject(preferences);
            }
        } catch (IOException exception) {
            throw new RuntimeException("Could not save GUI preferences.", exception);
        }
    }

    public Path getPreferencesPath() {
        return preferencesPath;
    }
}
