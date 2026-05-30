package forge.feature;

import java.util.Objects;

public class FeatureResult {
    private final String featureName;
    private final int featureVersion;

    public FeatureResult(String featureName, int featureVersion) {
        /*
         * Intent: Store stable feature identity shared by derived feature result types.
         * Precondition: Feature name must be nonblank and version must be positive.
         * Returns: A constructed FeatureResult instance.
         * Postcondition: Feature identity is immutable.
         */
        if (featureName == null || featureName.trim().isEmpty()) {
            throw new IllegalArgumentException("featureName is required");
        }
        if (featureVersion < 1) {
            throw new IllegalArgumentException("featureVersion must be positive");
        }
        this.featureName = featureName.trim();
        this.featureVersion = featureVersion;
    }

    public String getFeatureName() {
        return featureName;
    }

    public int getFeatureVersion() {
        return featureVersion;
    }

    @Override
    public boolean equals(Object other) {
        /*
         * Intent: Compare feature results by feature name and version identity.
         * Precondition: Other object may be any type.
         * Returns: True when both objects identify the same feature/version.
         * Postcondition: Neither object is modified.
         */
        if (this == other) {
            return true;
        }
        if (!(other instanceof FeatureResult that)) {
            return false;
        }
        return featureVersion == that.featureVersion && featureName.equals(that.featureName);
    }

    @Override
    public int hashCode() {
        /*
         * Intent: Produce a hash code consistent with FeatureResult equality.
         * Precondition: Feature identity must be initialized.
         * Returns: Hash code for collection lookup.
         * Postcondition: FeatureResult state is unchanged.
         */
        return Objects.hash(featureName, featureVersion);
    }
}
