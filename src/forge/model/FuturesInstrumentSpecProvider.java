package forge.model;

public interface FuturesInstrumentSpecProvider {
    FuturesInstrumentSpec getBySymbol(String symbol);

    boolean supports(String symbol);
}
