package forge.risk;

import forge.config.RiskSettings;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FacadeForgeRiskTest {
    @Nested
    class Singleton {
        @Test
        void returnsSharedFacadeInstance() {
            assertSame(FacadeForgeRisk.getTheInstance(), FacadeForgeRisk.getTheInstance());
        }
    }

    @Nested
    class Access {
        @Test
        void createsRiskManagerAndDecisions() {
            FacadeForgeRisk.ForgeRiskAccess access = FacadeForgeRisk.getTheInstance().forgeRiskAccess();

            assertNotNull(access.createRiskManager(new RiskSettings(false, 0, false, 0)));
            assertFalse(access.holdDecision().shouldCloseTrade());
            assertTrue(access.closeTradeDecision("daily_risk").shouldCloseTrade());
        }
    }
}
