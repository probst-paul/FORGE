package forge.target;

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

public class TargetModelCatalog {
    private static final String TARGET_PACKAGE = "forge.target";
    private static final String TARGET_PATH = "forge/target";

    public List<Class<? extends TargetModel>> findAvailableTargetModels() {
        List<Class<? extends TargetModel>> targetModels = new ArrayList<>();

        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Enumeration<URL> resources = classLoader.getResources(TARGET_PATH);

            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                if ("file".equals(resource.getProtocol())) {
                    addFileSystemTargetModels(resource, targetModels);
                } else if ("jar".equals(resource.getProtocol())) {
                    addJarTargetModels(resource, targetModels);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load available target models", e);
        }

        targetModels.sort(Comparator.comparing(Class::getSimpleName));
        return Collections.unmodifiableList(targetModels);
    }

    public String getDisplayName(Class<? extends TargetModel> targetModelClass) {
        String simpleName = targetModelClass.getSimpleName();
        if ("FixedRiskRewardTarget".equals(simpleName)) {
            return "Fixed Risk/Reward";
        }
        if ("FixedTarget".equals(simpleName)) {
            return "Target";
        }
        if (simpleName.endsWith("Target")) {
            return simpleName.substring(0, simpleName.length() - "Target".length());
        }
        return simpleName;
    }

    private void addFileSystemTargetModels(URL resource, List<Class<? extends TargetModel>> targetModels) {
        try {
            Path targetDirectory = Paths.get(resource.toURI());
            try (java.util.stream.Stream<Path> files = Files.list(targetDirectory)) {
                files.filter(path -> path.getFileName().toString().endsWith(".class"))
                        .map(path -> classNameFromFile(path.getFileName().toString()))
                        .forEach(className -> addIfTargetModel(className, targetModels));
            }
        } catch (IOException | URISyntaxException e) {
            throw new IllegalStateException("Unable to inspect target model directory", e);
        }
    }

    private void addJarTargetModels(URL resource, List<Class<? extends TargetModel>> targetModels) {
        try {
            JarURLConnection connection = (JarURLConnection) resource.openConnection();
            try (JarFile jarFile = connection.getJarFile()) {
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (name.startsWith(TARGET_PATH + "/") && name.endsWith(".class")) {
                        addIfTargetModel(classNameFromJarEntry(name), targetModels);
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to inspect target model jar", e);
        }
    }

    private String classNameFromFile(String fileName) {
        return TARGET_PACKAGE + "." + fileName.substring(0, fileName.length() - ".class".length());
    }

    private String classNameFromJarEntry(String entryName) {
        return entryName.substring(0, entryName.length() - ".class".length()).replace('/', '.');
    }

    @SuppressWarnings("unchecked")
    private void addIfTargetModel(String className, List<Class<? extends TargetModel>> targetModels) {
        try {
            Class<?> candidate = Class.forName(className);
            int modifiers = candidate.getModifiers();
            if (TargetModel.class.isAssignableFrom(candidate)
                    && !candidate.isInterface()
                    && !Modifier.isAbstract(modifiers)) {
                Class<? extends TargetModel> targetModelClass = (Class<? extends TargetModel>) candidate;
                if (!targetModels.contains(targetModelClass)) {
                    targetModels.add(targetModelClass);
                }
            }
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Unable to load target model class " + className, e);
        }
    }
}
