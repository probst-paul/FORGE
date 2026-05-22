package forge.strategy;

import forge.data.market.TradeTick;
import forge.engine.MarketContext;
import forge.feature.TpoPeriod;
import forge.feature.TradingDayContext;
import forge.feature.TradingSession;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StrategyRequirementsTest {
    @Test
    void filtersBySessionAndTpoPeriod() {
        StrategyRequirements requirements = StrategyRequirements.builder()
                .evaluateDuring(TradingSession.RTH)
                .evaluateDuring(TpoPeriod.C)
                .build();

        assertTrue(requirements.shouldEvaluate(context(TradingSession.RTH, TpoPeriod.C)));
        assertFalse(requirements.shouldEvaluate(context(TradingSession.RTH, TpoPeriod.D)));
        assertFalse(requirements.shouldEvaluate(context(TradingSession.OVERNIGHT, TpoPeriod.OUTSIDE_RTH)));
    }

    @Test
    void tracksRequiredFeaturesAndEvents() {
        StrategyRequirements requirements = StrategyRequirements.builder()
                .requireFeature("SESSION_RANGE")
                .requireEvent("FIRST_HOUR_BREACH")
                .build();

        assertTrue(requirements.requiresFeature("SESSION_RANGE"));
        assertTrue(requirements.requiresEvent("FIRST_HOUR_BREACH"));
        assertFalse(requirements.requiresFeature("OTHER"));
        assertFalse(requirements.requiresEvent("OTHER"));
    }

    @Test
    void rejectsBlankRequirementNames() {
        assertThrows(IllegalArgumentException.class, () -> StrategyRequirements.builder().requireFeature(" "));
        assertThrows(IllegalArgumentException.class, () -> StrategyRequirements.builder().requireEvent(""));
    }

    private StrategyContext context(TradingSession session, TpoPeriod period) {
        Instant instant = Instant.parse("2025-01-06T15:30:00Z");
        MarketContext marketContext = new MarketContext(
                "ES",
                LocalDateTime.of(2025, 1, 6, 15, 30),
                100,
                0.25,
                12.50,
                false
        );
        TradeTick tick = new TradeTick("ESU25", instant, 100, 99L, 101L, 1, 1, 1);
        return new StrategyContext(
                marketContext,
                tick,
                new TradingDayContext(LocalDate.of(2025, 1, 6), session),
                period,
                null,
                List.of()
        );
    }
}
