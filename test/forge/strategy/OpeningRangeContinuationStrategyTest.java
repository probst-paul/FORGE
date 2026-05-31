package forge.strategy;

import forge.data.market.TradeTick;
import forge.engine.MarketContext;
import forge.event.EventSide;
import forge.event.FirstHourBreachEvent;
import forge.event.MarketEventOccurrence;
import forge.trade.OrderRequest;
import forge.trade.OrderSide;
import forge.feature.SessionRangeFeature;
import forge.feature.TpoPeriodClassifier;
import forge.feature.TradingDayClassifier;
import forge.feature.TpoPeriod;
import forge.feature.TradingSession;
import forge.trade.TradePlan;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpeningRangeContinuationStrategyTest {
    private static final ZoneId CENTRAL_TIME = ZoneId.of("America/Chicago");
    private static final LocalDate SESSION_DATE = LocalDate.of(2025, 1, 6);

    @Nested
    class Constructor {
        @Test
        void storesQuantityAndName() {
            OpeningRangeContinuationStrategy strategy = new OpeningRangeContinuationStrategy(2);

            assertEquals("OpeningRangeContinuation", strategy.getName());
            assertEquals(2, strategy.getQuantity());
        }

        @Test
        void rejectsInvalidConfiguration() {
            assertThrows(IllegalArgumentException.class, () -> new OpeningRangeContinuationStrategy(0));
        }
    }

    @Nested
    class Requirements {
        @Test
        void requiresSessionRangesFirstHourBreachesAndRthTpoPeriods() {
            StrategyRequirements requirements = new OpeningRangeContinuationStrategy().getRequirements();

            assertTrue(requirements.requiresFeature(SessionRangeFeature.FEATURE_NAME));
            assertTrue(requirements.requiresEvent(FirstHourBreachEvent.EVENT_NAME));
            assertTrue(requirements.getEvaluationSessions().contains(TradingSession.RTH));
            assertTrue(requirements.getEvaluationTpoPeriods().contains(TpoPeriod.C));
            assertTrue(requirements.getEvaluationTpoPeriods().contains(TpoPeriod.D));
        }
    }

    @Nested
    class Evaluate {
        @Test
        void entersLongWhenFirstHourHighIsCrossedAfterInsideFirstHourRange() {
            OpeningRangeContinuationStrategy strategy = new OpeningRangeContinuationStrategy(2);

            StrategyDecision decision = strategy.evaluate(context(LocalTime.of(9, 30), 98, false, EventSide.LONG));

            assertTrue(decision.hasOrderRequest());
            OrderRequest orderRequest = decision.getOrderRequest().orElseThrow();
            assertEquals(OrderSide.BUY, orderRequest.getSide());
            assertEquals(2, orderRequest.getQuantity());
            TradePlan plan = decision.getTradePlan().orElseThrow();
            assertEquals(OrderSide.BUY, plan.getSide());
            assertEquals(100, plan.getTargetPriceTicks());
            assertEquals(92, plan.getStopPriceTicks());
            assertEquals(LocalTime.of(10, 30), plan.getTimeStop());
        }

        @Test
        void entersShortWhenFirstHourLowIsCrossedAfterInsideFirstHourRange() {
            OpeningRangeContinuationStrategy strategy = new OpeningRangeContinuationStrategy();

            StrategyDecision decision = strategy.evaluate(context(LocalTime.of(9, 30), 92, false, EventSide.SHORT));

            assertTrue(decision.hasOrderRequest());
            assertEquals(OrderSide.SELL, decision.getOrderRequest().orElseThrow().getSide());
            TradePlan plan = decision.getTradePlan().orElseThrow();
            assertEquals(OrderSide.SELL, plan.getSide());
            assertEquals(90, plan.getTargetPriceTicks());
            assertEquals(98, plan.getStopPriceTicks());
            assertEquals(LocalTime.of(10, 30), plan.getTimeStop());
        }

        @Test
        void doesNotTradeWhenFirstHourRangeExceedsOvernightRange() {
            OpeningRangeContinuationStrategy strategy = new OpeningRangeContinuationStrategy();

            StrategyDecision decision = strategy.evaluate(context(
                    LocalTime.of(9, 30),
                    101,
                    false,
                    EventSide.LONG,
                    feature(90, 100, 92, 101)
            ));

            assertFalse(decision.hasOrderRequest());
        }

        @Test
        void onlyAllowsOneTradePerSessionDate() {
            OpeningRangeContinuationStrategy strategy = new OpeningRangeContinuationStrategy();

            assertTrue(strategy.evaluate(context(LocalTime.of(9, 30), 98, false, EventSide.LONG)).hasOrderRequest());
            assertFalse(strategy.evaluate(context(LocalTime.of(9, 31), 92, false, EventSide.SHORT)).hasOrderRequest());
        }

        @Test
        void onlyTradesDuringAllowedWindow() {
            OpeningRangeContinuationStrategy strategy = new OpeningRangeContinuationStrategy();

            assertFalse(strategy.evaluate(context(LocalTime.of(10, 30), 98, false, EventSide.LONG)).hasOrderRequest());
        }

        @Test
        void doesNotTradeWhenPositionIsAlreadyOpen() {
            OpeningRangeContinuationStrategy strategy = new OpeningRangeContinuationStrategy();

            assertFalse(strategy.evaluate(context(LocalTime.of(9, 30), 98, true, EventSide.LONG)).hasOrderRequest());
        }

        @Test
        void doesNotTradeWithoutCurrentBreachEvent() {
            OpeningRangeContinuationStrategy strategy = new OpeningRangeContinuationStrategy();

            assertFalse(strategy.evaluate(context(LocalTime.of(9, 30), 98, false, null)).hasOrderRequest());
        }

        @Test
        void clearsStateOnBacktestStart() {
            OpeningRangeContinuationStrategy strategy = new OpeningRangeContinuationStrategy();

            assertTrue(strategy.evaluate(context(LocalTime.of(9, 30), 98, false, EventSide.LONG)).hasOrderRequest());

            strategy.onBacktestStart();

            assertTrue(strategy.evaluate(context(LocalTime.of(9, 31), 98, false, EventSide.LONG)).hasOrderRequest());
        }
    }

    private StrategyContext context(
            LocalTime centralTime,
            long priceTicks,
            boolean hasOpenPosition,
            EventSide eventSide
    ) {
        return context(centralTime, priceTicks, hasOpenPosition, eventSide, feature(90, 100, 92, 98));
    }

    private StrategyContext context(
            LocalTime centralTime,
            long priceTicks,
            boolean hasOpenPosition,
            EventSide eventSide,
            SessionRangeFeature feature
    ) {
        Instant instant = instantAtCentral(SESSION_DATE, centralTime);
        MarketContext marketContext = new MarketContext(
                "ES",
                LocalDateTime.ofInstant(instant, ZoneOffset.UTC),
                priceTicks,
                0.25,
                12.50,
                hasOpenPosition
        );
        TradeTick tick = new TradeTick("ESU25", instant, priceTicks, priceTicks - 1, priceTicks + 1, 1, 1, 1);
        return new StrategyContext(
                marketContext,
                tick,
                new TradingDayClassifier().classify(instant),
                new TpoPeriodClassifier().classify(instant),
                feature,
                eventSide == null ? List.of() : List.of(event(instant, priceTicks, eventSide))
        );
    }

    private SessionRangeFeature feature(
            long overnightLowTicks,
            long overnightHighTicks,
            long firstHourLowTicks,
            long firstHourHighTicks
    ) {
        return new SessionRangeFeature(
                "ESU25",
                SESSION_DATE,
                overnightLowTicks,
                overnightHighTicks,
                firstHourLowTicks,
                firstHourHighTicks,
                firstHourLowTicks,
                firstHourHighTicks
        );
    }

    private MarketEventOccurrence event(Instant instant, long priceTicks, EventSide side) {
        return new MarketEventOccurrence(
                "ESU25",
                SESSION_DATE,
                FirstHourBreachEvent.EVENT_NAME,
                FirstHourBreachEvent.EVENT_VERSION,
                side,
                instant,
                priceTicks
        );
    }

    private Instant instantAtCentral(LocalDate date, LocalTime time) {
        return LocalDateTime.of(date, time)
                .atZone(CENTRAL_TIME)
                .withZoneSameInstant(ZoneOffset.UTC)
                .toInstant();
    }
}
