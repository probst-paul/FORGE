package forge.reporting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BacktestResult {
    private final String strategyName;
    private final List<String> contractSymbols;
    private final long ticksProcessed;
    private final long orderSignalsGenerated;

    public BacktestResult() {
        this("Not Run", Collections.emptyList(), 0, 0);
    }

    public BacktestResult(
            String strategyName,
            List<String> contractSymbols,
            long ticksProcessed,
            long orderSignalsGenerated
    ) {
        if (strategyName == null || strategyName.trim().isEmpty()) {
            throw new IllegalArgumentException("strategyName is required");
        }
        if (contractSymbols == null) {
            throw new IllegalArgumentException("contractSymbols is required");
        }
        if (ticksProcessed < 0) {
            throw new IllegalArgumentException("ticksProcessed cannot be negative");
        }
        if (orderSignalsGenerated < 0) {
            throw new IllegalArgumentException("orderSignalsGenerated cannot be negative");
        }
        List<String> normalizedContractSymbols = new ArrayList<>();
        for (String contractSymbol : contractSymbols) {
            if (contractSymbol == null || contractSymbol.trim().isEmpty()) {
                throw new IllegalArgumentException("contract symbols cannot be blank");
            }
            normalizedContractSymbols.add(contractSymbol.trim().toUpperCase());
        }
        this.strategyName = strategyName.trim();
        this.contractSymbols = Collections.unmodifiableList(normalizedContractSymbols);
        this.ticksProcessed = ticksProcessed;
        this.orderSignalsGenerated = orderSignalsGenerated;
    }

    public String getStrategyName() {
        return strategyName;
    }

    public List<String> getContractSymbols() {
        return contractSymbols;
    }

    public long getTicksProcessed() {
        return ticksProcessed;
    }

    public long getOrderSignalsGenerated() {
        return orderSignalsGenerated;
    }

    @Override
    public String toString() {
        return "BacktestResult{" +
                "strategyName='" + strategyName + '\'' +
                ", contractSymbols=" + contractSymbols +
                ", ticksProcessed=" + ticksProcessed +
                ", orderSignalsGenerated=" + orderSignalsGenerated +
                '}';
    }
}
