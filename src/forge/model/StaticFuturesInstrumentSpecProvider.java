package forge.model;

import java.util.Collections;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class StaticFuturesInstrumentSpecProvider implements FuturesInstrumentSpecProvider {
    private final Map<String, FuturesInstrumentSpec> specsBySymbol;

    public StaticFuturesInstrumentSpecProvider() {
        Map<String, FuturesInstrumentSpec> specs = new LinkedHashMap<>();
        Set<String> quarterlyMonths = new LinkedHashSet<>(Arrays.asList("H", "M", "U", "Z"));
        add(specs, new FuturesInstrumentSpec("ES", "E-mini S&P 500", 0.25, 12.50, quarterlyMonths));
        add(specs, new FuturesInstrumentSpec("NQ", "E-mini Nasdaq-100", 0.25, 5.00, quarterlyMonths));
        add(specs, new FuturesInstrumentSpec("YM", "E-mini Dow", 1.00, 5.00, quarterlyMonths));
        add(specs, new FuturesInstrumentSpec("RTY", "E-mini Russell 2000", 0.10, 5.00, quarterlyMonths));
        add(specs, new FuturesInstrumentSpec("CL", "Crude Oil", 0.01, 10.00, FuturesInstrumentSpec.ALL_MONTH_CODES));
        this.specsBySymbol = Collections.unmodifiableMap(specs);
    }

    @Override
    public FuturesInstrumentSpec getBySymbol(String symbol) {
        String normalizedSymbol = normalizeSymbol(symbol);
        FuturesInstrumentSpec spec = specsBySymbol.get(normalizedSymbol);
        if (spec == null) {
            throw new IllegalArgumentException("Unsupported futures instrument: " + normalizedSymbol);
        }
        return spec;
    }

    @Override
    public boolean supports(String symbol) {
        return specsBySymbol.containsKey(normalizeSymbol(symbol));
    }

    private void add(Map<String, FuturesInstrumentSpec> specs, FuturesInstrumentSpec spec) {
        specs.put(spec.getSymbolCode(), spec);
    }

    private String normalizeSymbol(String symbol) {
        if (symbol == null || symbol.trim().isEmpty()) {
            throw new IllegalArgumentException("symbol is required");
        }
        return symbol.trim().toUpperCase(Locale.ROOT);
    }
}
