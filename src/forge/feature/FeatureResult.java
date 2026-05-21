package forge.feature;

import java.util.Objects;

public class FeatureResult {
    private final String featureName;
    private final int featureVersion;

    public FeatureResult(String featureName, int featureVersion) {
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
        return Objects.hash(featureName, featureVersion);
    }
}
