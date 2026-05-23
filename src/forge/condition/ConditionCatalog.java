package forge.condition;

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

public class ConditionCatalog {
    private static final String CONDITION_PACKAGE = "forge.condition";
    private static final String CONDITION_PATH = "forge/condition";

    public List<Class<? extends MarketCondition>> findAvailableConditions() {
        List<Class<? extends MarketCondition>> conditions = new ArrayList<>();

        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Enumeration<URL> resources = classLoader.getResources(CONDITION_PATH);

            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                if ("file".equals(resource.getProtocol())) {
                    addFileSystemConditions(resource, conditions);
                } else if ("jar".equals(resource.getProtocol())) {
                    addJarConditions(resource, conditions);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load available conditions", e);
        }

        conditions.sort(Comparator.comparing(Class::getSimpleName));
        return Collections.unmodifiableList(conditions);
    }

    public String getDisplayName(Class<? extends MarketCondition> conditionClass) {
        String simpleName = conditionClass.getSimpleName();
        if (simpleName.endsWith("Condition")) {
            return simpleName.substring(0, simpleName.length() - "Condition".length());
        }
        return simpleName;
    }

    private void addFileSystemConditions(URL resource, List<Class<? extends MarketCondition>> conditions) {
        try {
            Path conditionDirectory = Paths.get(resource.toURI());
            try (java.util.stream.Stream<Path> files = Files.list(conditionDirectory)) {
                files.filter(path -> path.getFileName().toString().endsWith(".class"))
                        .map(path -> classNameFromFile(path.getFileName().toString()))
                        .forEach(className -> addIfCondition(className, conditions));
            }
        } catch (IOException | URISyntaxException e) {
            throw new IllegalStateException("Unable to inspect condition directory", e);
        }
    }

    private void addJarConditions(URL resource, List<Class<? extends MarketCondition>> conditions) {
        try {
            JarURLConnection connection = (JarURLConnection) resource.openConnection();
            try (JarFile jarFile = connection.getJarFile()) {
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (name.startsWith(CONDITION_PATH + "/") && name.endsWith(".class")) {
                        addIfCondition(classNameFromJarEntry(name), conditions);
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to inspect condition jar", e);
        }
    }

    private String classNameFromFile(String fileName) {
        return CONDITION_PACKAGE + "." + fileName.substring(0, fileName.length() - ".class".length());
    }

    private String classNameFromJarEntry(String entryName) {
        return entryName.substring(0, entryName.length() - ".class".length()).replace('/', '.');
    }

    @SuppressWarnings("unchecked")
    private void addIfCondition(String className, List<Class<? extends MarketCondition>> conditions) {
        try {
            Class<?> candidate = Class.forName(className);
            int modifiers = candidate.getModifiers();
            if (MarketCondition.class.isAssignableFrom(candidate)
                    && !candidate.isInterface()
                    && !Modifier.isAbstract(modifiers)) {
                Class<? extends MarketCondition> conditionClass = (Class<? extends MarketCondition>) candidate;
                if (!conditions.contains(conditionClass)) {
                    conditions.add(conditionClass);
                }
            }
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Unable to load condition class " + className, e);
        }
    }
}
