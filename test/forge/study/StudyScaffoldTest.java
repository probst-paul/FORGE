package forge.study;

import forge.condition.FirstHourBreachCondition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StudyScaffoldTest {
    @Test
    void firstHourBreachStudyDefinesResearchSetupMetadata() {
        FirstHourBreachStudy study = new FirstHourBreachStudy();

        assertEquals(FirstHourBreachCondition.EVENT_NAME, study.getName());
        assertEquals("First Hour Breach Frequency", study.getDisplayName());
        assertEquals(
                "Counts how often price breaches the first-hour RTH high or low after the first hour completes.",
                study.getDescription()
        );
    }

    @Test
    void studyCatalogFindsSupportedStudies() {
        StudyCatalog catalog = new StudyCatalog();

        assertEquals(List.of(FirstHourBreachCondition.EVENT_NAME), catalog.findAvailableStudyNames());
        assertEquals(FirstHourBreachCondition.EVENT_NAME, catalog.getStudy(FirstHourBreachCondition.EVENT_NAME).getName());
        assertThrows(IllegalArgumentException.class, () -> catalog.getStudy("UNKNOWN_STUDY"));
    }

    @Test
    void facadeExposesSingletonStudyAccess() {
        FacadeForgeStudy facade = FacadeForgeStudy.getTheInstance();

        assertSame(facade, FacadeForgeStudy.getTheInstance());
        assertEquals(List.of(FirstHourBreachCondition.EVENT_NAME), facade.forgeStudyAccess().getSupportedStudyNames());
    }
}
