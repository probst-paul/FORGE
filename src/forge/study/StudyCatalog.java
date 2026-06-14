package forge.study;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StudyCatalog {
    private final List<MarketStudy> studies;

    public StudyCatalog() {
        this(List.of(new FirstHourBreachStudy()));
    }

    public StudyCatalog(List<MarketStudy> studies) {
        /*
         * Intent: Create an immutable catalog of supported market studies.
         * Precondition: studies must contain at least one non-null study.
         * Returns: Constructed StudyCatalog.
         * Postcondition: Study list is defensively copied and cannot be mutated through this catalog.
         */
        if (studies == null || studies.isEmpty()) {
            throw new IllegalArgumentException("studies must contain at least one market study");
        }
        List<MarketStudy> normalized = new ArrayList<>();
        for (MarketStudy study : studies) {
            if (study == null) {
                throw new IllegalArgumentException("studies cannot contain null values");
            }
            normalized.add(study);
        }
        this.studies = Collections.unmodifiableList(normalized);
    }

    public List<MarketStudy> findAvailableStudies() {
        return studies;
    }

    public List<String> findAvailableStudyNames() {
        /*
         * Intent: List study identifiers available to GUI selectors.
         * Precondition: Catalog must have been constructed with valid studies.
         * Returns: Unmodifiable list of study names.
         * Postcondition: Catalog state is unchanged.
         */
        List<String> names = new ArrayList<>();
        for (MarketStudy study : studies) {
            names.add(study.getName());
        }
        return Collections.unmodifiableList(names);
    }

    public MarketStudy getStudy(String studyName) {
        /*
         * Intent: Retrieve a supported study by its stable name.
         * Precondition: studyName must be non-null and non-blank.
         * Returns: Matching MarketStudy.
         * Postcondition: Unsupported study names are rejected before execution.
         */
        if (studyName == null || studyName.trim().isEmpty()) {
            throw new IllegalArgumentException("studyName is required");
        }
        String normalizedName = studyName.trim();
        for (MarketStudy study : studies) {
            if (study.getName().equals(normalizedName)) {
                return study;
            }
        }
        throw new IllegalArgumentException("Unsupported market study: " + studyName);
    }
}
