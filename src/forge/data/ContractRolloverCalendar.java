package forge.data;

import java.util.List;
import java.util.Optional;

public class ContractRolloverCalendar {
    private final ContractNameResolver contractNameResolver;
    private final List<RolloverRule> rolloverRules;

    public ContractRolloverCalendar() {
        this(new ContractNameResolver(), List.of(
                new EquityIndexRolloverRule(),
                new CrudeOilRolloverRule()
        ));
    }

    public ContractRolloverCalendar(ContractNameResolver contractNameResolver, List<RolloverRule> rolloverRules) {
        if (contractNameResolver == null) {
            throw new IllegalArgumentException("contractNameResolver is required");
        }
        if (rolloverRules == null) {
            throw new IllegalArgumentException("rolloverRules is required");
        }
        this.contractNameResolver = contractNameResolver;
        this.rolloverRules = List.copyOf(rolloverRules);
    }

    public Optional<ContractRolloverWindow> findActiveWindow(String contractSymbol) {
        FuturesContractCode contractCode = contractNameResolver.resolveContractCode(contractSymbol);
        for (RolloverRule rolloverRule : rolloverRules) {
            Optional<ContractRolloverWindow> activeWindow = rolloverRule.resolveActiveWindow(contractCode);
            if (activeWindow.isPresent()) {
                return activeWindow;
            }
        }
        return Optional.empty();
    }
}
