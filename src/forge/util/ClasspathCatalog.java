package forge.util;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ClasspathCatalog<T> {
    private final String packageName;
    private final String packagePath;
    private final Class<T> baseType;

    public ClasspathCatalog(String packageName, Class<T> baseType) {
        this.packageName = requireText(packageName, "packageName");
        this.packagePath = this.packageName.replace('.', '/');
        this.baseType = Objects.requireNonNull(baseType, "baseType is required");
    }

    public List<Class<? extends T>> findImplementations() {
        List<Class<? extends T>> implementations = new ArrayList<>();

        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Enumeration<URL> resources = classLoader.getResources(packagePath);

            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                if ("file".equals(resource.getProtocol())) {
                    addFileSystemImplementations(resource, implementations);
                } else if ("jar".equals(resource.getProtocol())) {
                    addJarImplementations(resource, implementations);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load implementations from " + packageName, e);
        }

        implementations.sort(Comparator.comparing(Class::getSimpleName));
        return ImmutableLists.copyOfRequired(implementations, "implementations");
    }

    private void addFileSystemImplementations(URL resource, List<Class<? extends T>> implementations) {
        try {
            Path directory = Paths.get(resource.toURI());
            try (java.util.stream.Stream<Path> files = Files.list(directory)) {
                files.filter(path -> path.getFileName().toString().endsWith(".class"))
                        .map(path -> classNameFromFile(path.getFileName().toString()))
                        .forEach(className -> addIfImplementation(className, implementations));
            }
        } catch (IOException | URISyntaxException e) {
            throw new IllegalStateException("Unable to inspect package directory " + packageName, e);
        }
    }

    private void addJarImplementations(URL resource, List<Class<? extends T>> implementations) {
        try {
            JarURLConnection connection = (JarURLConnection) resource.openConnection();
            try (JarFile jarFile = connection.getJarFile()) {
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (name.startsWith(packagePath + "/") && name.endsWith(".class")) {
                        addIfImplementation(classNameFromJarEntry(name), implementations);
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to inspect package jar " + packageName, e);
        }
    }

    private String classNameFromFile(String fileName) {
        return packageName + "." + fileName.substring(0, fileName.length() - ".class".length());
    }

    private String classNameFromJarEntry(String entryName) {
        return entryName.substring(0, entryName.length() - ".class".length()).replace('/', '.');
    }

    private void addIfImplementation(String className, List<Class<? extends T>> implementations) {
        try {
            Class<?> candidate = Class.forName(className);
            int modifiers = candidate.getModifiers();
            if (baseType.isAssignableFrom(candidate)
                    && !candidate.isInterface()
                    && !Modifier.isAbstract(modifiers)) {
                Class<? extends T> implementationClass = candidate.asSubclass(baseType);
                if (!implementations.contains(implementationClass)) {
                    implementations.add(implementationClass);
                }
            }
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Unable to load class " + className, e);
        }
    }

    private String requireText(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value.trim();
    }
}
