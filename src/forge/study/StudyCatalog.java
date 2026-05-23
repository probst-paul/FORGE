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
        List<String> names = new ArrayList<>();
        for (MarketStudy study : studies) {
            names.add(study.getName());
        }
        return Collections.unmodifiableList(names);
    }

    public MarketStudy getStudy(String studyName) {
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
