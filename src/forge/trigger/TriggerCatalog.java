package forge.trigger;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class TriggerCatalog {
    private static final String TRIGGER_PACKAGE = "forge.trigger";
    private static final String TRIGGER_PATH = "forge/trigger";

    public List<Class<? extends TradeTrigger>> findAvailableTriggers() {
        List<Class<? extends TradeTrigger>> triggers = new ArrayList<>();

        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Enumeration<URL> resources = classLoader.getResources(TRIGGER_PATH);

            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                if ("file".equals(resource.getProtocol())) {
                    addFileSystemTriggers(resource, triggers);
                } else if ("jar".equals(resource.getProtocol())) {
                    addJarTriggers(resource, triggers);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load available triggers", e);
        }

        triggers.sort(Comparator.comparing(Class::getSimpleName));
        return Collections.unmodifiableList(triggers);
    }

    public String getDisplayName(Class<? extends TradeTrigger> triggerClass) {
        String simpleName = triggerClass.getSimpleName();
        if (simpleName.endsWith("Trigger")) {
            return simpleName.substring(0, simpleName.length() - "Trigger".length());
        }
        return simpleName;
    }

    private void addFileSystemTriggers(URL resource, List<Class<? extends TradeTrigger>> triggers) {
        try {
            Path triggerDirectory = Paths.get(resource.toURI());
            try (java.util.stream.Stream<Path> files = Files.list(triggerDirectory)) {
                files.filter(path -> path.getFileName().toString().endsWith(".class"))
                        .map(path -> classNameFromFile(path.getFileName().toString()))
                        .forEach(className -> addIfTrigger(className, triggers));
            }
        } catch (IOException | URISyntaxException e) {
            throw new IllegalStateException("Unable to inspect trigger directory", e);
        }
    }

    private void addJarTriggers(URL resource, List<Class<? extends TradeTrigger>> triggers) {
        try {
            JarURLConnection connection = (JarURLConnection) resource.openConnection();
            try (JarFile jarFile = connection.getJarFile()) {
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (name.startsWith(TRIGGER_PATH + "/") && name.endsWith(".class")) {
                        addIfTrigger(classNameFromJarEntry(name), triggers);
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to inspect trigger jar", e);
        }
    }

    private String classNameFromFile(String fileName) {
        return TRIGGER_PACKAGE + "." + fileName.substring(0, fileName.length() - ".class".length());
    }

    private String classNameFromJarEntry(String entryName) {
        return entryName.substring(0, entryName.length() - ".class".length()).replace('/', '.');
    }

    @SuppressWarnings("unchecked")
    private void addIfTrigger(String className, List<Class<? extends TradeTrigger>> triggers) {
        try {
            Class<?> candidate = Class.forName(className);
            int modifiers = candidate.getModifiers();
            if (TradeTrigger.class.isAssignableFrom(candidate)
                    && !candidate.isInterface()
                    && !Modifier.isAbstract(modifiers)) {
                Class<? extends TradeTrigger> triggerClass = (Class<? extends TradeTrigger>) candidate;
                if (!triggers.contains(triggerClass)) {
                    triggers.add(triggerClass);
                }
            }
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Unable to load trigger class " + className, e);
        }
    }
}
