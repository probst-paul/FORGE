package forge.feature;

/*
 * Intent: Define a generic contract for calculators that produce a specific FeatureResult subtype.
 * Precondition: Type parameter must extend FeatureResult.
 * Returns: Implementations expose their FeatureDefinition through getDefinition().
 * Postcondition: Generics preserve type safety between a calculator and the feature result family it supports.
 */
public interface FeatureCalculator<T extends FeatureResult> {
    FeatureDefinition getDefinition();
}
