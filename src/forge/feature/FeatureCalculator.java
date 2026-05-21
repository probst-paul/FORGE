package forge.feature;

public interface FeatureCalculator<T extends FeatureResult> {
    FeatureDefinition getDefinition();
}
