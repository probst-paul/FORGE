package forge.data.market;

public interface TradeTickStreamProcessor {
    void onTick(TradeTick tick);

    default void onComplete() {
    }
}
