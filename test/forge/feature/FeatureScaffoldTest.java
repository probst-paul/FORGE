package forge.feature;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FeatureScaffoldTest {
    @Nested
    class Facade {
        @Test
        void exposesSingletonAccessToSupportedFeatures() {
            FacadeForgeFeature facade = FacadeForgeFeature.getTheInstance();

            assertSame(facade, FacadeForgeFeature.getTheInstance());
            assertEquals(List.of(SessionRangeFeature.FEATURE_NAME), facade.forgeFeatureAccess().getSupportedFeatureNames());
        }
    }

    @Nested
    class SessionRange {
        @Test
        void storesTickRanges() {
            SessionRangeFeature feature = new SessionRangeFeature(
                    "esu25",
                    LocalDate.of(2025, 8, 1),
                    100,
                    120,
                    105,
                    115,
                    95,
                    130
            );

            assertEquals("ESU25", feature.getContractSymbol());
            assertEquals(LocalDate.of(2025, 8, 1), feature.getSessionDate());
            assertEquals(100, feature.getOvernightLowTicks());
            assertEquals(120, feature.getOvernightHighTicks());
            assertEquals(105, feature.getFirstHourLowTicks());
            assertEquals(115, feature.getFirstHourHighTicks());
            assertEquals(95, feature.getRthLowTicks());
            assertEquals(130, feature.getRthHighTicks());
        }

        @Test
        void rejectsInvalidRanges() {
            assertThrows(IllegalArgumentException.class, () -> new SessionRangeFeature(
                    "ESU25",
                    LocalDate.of(2025, 8, 1),
                    120,
                    100,
                    105,
                    115,
                    95,
                    130
            ));
        }
    }
}
