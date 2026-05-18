package forge.stop;

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

public class StopModelCatalog {
    private static final String STOP_PACKAGE = "forge.stop";
    private static final String STOP_PATH = "forge/stop";

    public List<Class<? extends StopModel>> findAvailableStopModels() {
        List<Class<? extends StopModel>> stopModels = new ArrayList<>();

        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Enumeration<URL> resources = classLoader.getResources(STOP_PATH);

            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                if ("file".equals(resource.getProtocol())) {
                    addFileSystemStopModels(resource, stopModels);
                } else if ("jar".equals(resource.getProtocol())) {
                    addJarStopModels(resource, stopModels);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load available stop models", e);
        }

        stopModels.sort(Comparator.comparing(Class::getSimpleName));
        return Collections.unmodifiableList(stopModels);
    }

    public String getDisplayName(Class<? extends StopModel> stopModelClass) {
        String simpleName = stopModelClass.getSimpleName();
        if ("PriceBasedStop".equals(simpleName)) {
            return "Price Based";
        }
        if ("TimeBasedStop".equals(simpleName)) {
            return "Time Based";
        }
        if (simpleName.endsWith("Stop")) {
            return simpleName.substring(0, simpleName.length() - "Stop".length());
        }
        return simpleName;
    }

    private void addFileSystemStopModels(URL resource, List<Class<? extends StopModel>> stopModels) {
        try {
            Path stopDirectory = Paths.get(resource.toURI());
            try (java.util.stream.Stream<Path> files = Files.list(stopDirectory)) {
                files.filter(path -> path.getFileName().toString().endsWith(".class"))
                        .map(path -> classNameFromFile(path.getFileName().toString()))
                        .forEach(className -> addIfStopModel(className, stopModels));
            }
        } catch (IOException | URISyntaxException e) {
            throw new IllegalStateException("Unable to inspect stop model directory", e);
        }
    }

    private void addJarStopModels(URL resource, List<Class<? extends StopModel>> stopModels) {
        try {
            JarURLConnection connection = (JarURLConnection) resource.openConnection();
            try (JarFile jarFile = connection.getJarFile()) {
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (name.startsWith(STOP_PATH + "/") && name.endsWith(".class")) {
                        addIfStopModel(classNameFromJarEntry(name), stopModels);
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to inspect stop model jar", e);
        }
    }

    private String classNameFromFile(String fileName) {
        return STOP_PACKAGE + "." + fileName.substring(0, fileName.length() - ".class".length());
    }

    private String classNameFromJarEntry(String entryName) {
        return entryName.substring(0, entryName.length() - ".class".length()).replace('/', '.');
    }

    @SuppressWarnings("unchecked")
    private void addIfStopModel(String className, List<Class<? extends StopModel>> stopModels) {
        try {
            Class<?> candidate = Class.forName(className);
            int modifiers = candidate.getModifiers();
            if (StopModel.class.isAssignableFrom(candidate)
                    && !candidate.isInterface()
                    && !Modifier.isAbstract(modifiers)) {
                Class<? extends StopModel> stopModelClass = (Class<? extends StopModel>) candidate;
                if (!stopModels.contains(stopModelClass)) {
                    stopModels.add(stopModelClass);
                }
            }
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Unable to load stop model class " + className, e);
        }
    }
}
