package forge.study;

import java.util.List;

public class FacadeForgeStudy {
    private static final FacadeForgeStudy THE_INSTANCE = new FacadeForgeStudy();

    private final StudyCatalog studyCatalog;
    private final ForgeStudyAccess access = new ForgeStudyAccess();

    public static FacadeForgeStudy getTheInstance() {
        return THE_INSTANCE;
    }

    public FacadeForgeStudy() {
        this(new StudyCatalog());
    }

    public FacadeForgeStudy(StudyCatalog studyCatalog) {
        if (studyCatalog == null) {
            throw new IllegalArgumentException("studyCatalog is required");
        }
        this.studyCatalog = studyCatalog;
    }

    public ForgeStudyAccess forgeStudyAccess() {
        return access;
    }

    public class ForgeStudyAccess {
        /*
         * Intent: Expose supported study names through the study facade.
         * Precondition: Study catalog must be initialized.
         * Returns: List of stable study names.
         * Postcondition: Callers do not need direct access to StudyCatalog.
         */
        public List<String> getSupportedStudyNames() {
            return studyCatalog.findAvailableStudyNames();
        }

        /*
         * Intent: Retrieve a study definition through the study facade.
         * Precondition: studyName must identify a supported study.
         * Returns: Matching MarketStudy.
         * Postcondition: Catalog implementation remains hidden from callers.
         */
        public MarketStudy getStudy(String studyName) {
            return studyCatalog.getStudy(studyName);
        }
    }
}
