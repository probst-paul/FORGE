package forge.strategy;

import forge.config.TargetSettings;
import forge.target.FixedRiskRewardTarget;
import forge.target.FixedTarget;
import forge.target.TargetModel;
import forge.trigger.OrderFlowExhaustionTrigger;
import forge.trigger.PriceCrossoverTrigger;
import forge.trigger.TradeTrigger;

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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class StrategyCatalog {
    private static final String STRATEGY_PACKAGE = "forge.strategy";
    private static final String STRATEGY_PATH = "forge/strategy";

    public List<Class<? extends TradingStrategy>> findAvailableStrategies() {
        List<Class<? extends TradingStrategy>> strategies = new ArrayList<>();

        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Enumeration<URL> resources = classLoader.getResources(STRATEGY_PATH);

            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                if ("file".equals(resource.getProtocol())) {
                    addFileSystemStrategies(resource, strategies);
                } else if ("jar".equals(resource.getProtocol())) {
                    addJarStrategies(resource, strategies);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load available strategies", e);
        }

        strategies.sort(Comparator.comparing(Class::getSimpleName));
        return Collections.unmodifiableList(strategies);
    }

    public String getDisplayName(Class<? extends TradingStrategy> strategyClass) {
        String simpleName = strategyClass.getSimpleName();
        if (simpleName.endsWith("Strategy")) {
            return simpleName.substring(0, simpleName.length() - "Strategy".length());
        }
        return simpleName;
    }

    public StrategyConfigurationProfile getConfigurationProfile(Class<? extends TradingStrategy> strategyClass) {
        if (RangeBreakoutStrategy.class.equals(strategyClass)) {
            List<Class<? extends TradeTrigger>> allowedTriggers = List.of(OrderFlowExhaustionTrigger.class, PriceCrossoverTrigger.class);
            List<Class<? extends TargetModel>> allowedTargets = List.of(FixedRiskRewardTarget.class, FixedTarget.class);
            Map<Class<? extends TargetModel>, TargetSettings> defaultTargetSettings = new LinkedHashMap<>();
            defaultTargetSettings.put(FixedRiskRewardTarget.class, TargetSettings.fixedRiskReward("Fixed Risk/Reward", 2.0));
            defaultTargetSettings.put(FixedTarget.class, TargetSettings.fixedTarget("Target", 8));
            return new StrategyConfigurationProfile(
                    strategyClass,
                    allowedTriggers,
                    OrderFlowExhaustionTrigger.class,
                    true,
                    allowedTargets,
                    FixedRiskRewardTarget.class,
                    true,
                    defaultTargetSettings
            );
        }
        throw new IllegalArgumentException("No configuration profile is defined for " + strategyClass.getSimpleName());
    }

    private void addFileSystemStrategies(URL resource, List<Class<? extends TradingStrategy>> strategies) {
        try {
            Path strategyDirectory = Paths.get(resource.toURI());
            try (java.util.stream.Stream<Path> files = Files.list(strategyDirectory)) {
                files.filter(path -> path.getFileName().toString().endsWith(".class"))
                        .map(path -> classNameFromFile(path.getFileName().toString()))
                        .forEach(className -> addIfStrategy(className, strategies));
            }
        } catch (IOException | URISyntaxException e) {
            throw new IllegalStateException("Unable to inspect strategy directory", e);
        }
    }

    private void addJarStrategies(URL resource, List<Class<? extends TradingStrategy>> strategies) {
        try {
            JarURLConnection connection = (JarURLConnection) resource.openConnection();
            try (JarFile jarFile = connection.getJarFile()) {
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (name.startsWith(STRATEGY_PATH + "/") && name.endsWith(".class")) {
                        addIfStrategy(classNameFromJarEntry(name), strategies);
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to inspect strategy jar", e);
        }
    }

    private String classNameFromFile(String fileName) {
        return STRATEGY_PACKAGE + "." + fileName.substring(0, fileName.length() - ".class".length());
    }

    private String classNameFromJarEntry(String entryName) {
        return entryName.substring(0, entryName.length() - ".class".length()).replace('/', '.');
    }

    @SuppressWarnings("unchecked")
    private void addIfStrategy(String className, List<Class<? extends TradingStrategy>> strategies) {
        try {
            Class<?> candidate = Class.forName(className);
            int modifiers = candidate.getModifiers();
            if (TradingStrategy.class.isAssignableFrom(candidate)
                    && !candidate.isInterface()
                    && !Modifier.isAbstract(modifiers)) {
                Class<? extends TradingStrategy> strategyClass = (Class<? extends TradingStrategy>) candidate;
                if (!strategies.contains(strategyClass)) {
                    strategies.add(strategyClass);
                }
            }
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Unable to load strategy class " + className, e);
        }
    }
}
