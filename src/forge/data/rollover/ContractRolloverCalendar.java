package forge.data.rollover;

import forge.data.contract.ContractNameResolver;
import forge.data.contract.FuturesContractCode;

import java.util.List;
import java.util.Optional;

public class ContractRolloverCalendar {
    private final ContractNameResolver contractNameResolver;
    private final List<RolloverRule> rolloverRules;

    public ContractRolloverCalendar() {
        /*
         * Intent: Create the default rollover calendar for currently supported futures families.
         * Precondition: Default rollover rules must be available.
         * Returns: A constructed ContractRolloverCalendar instance.
         * Postcondition: Calendar can resolve equity index and crude oil active windows.
         */
        this(new ContractNameResolver(), List.of(
                new EquityIndexRolloverRule(),
                new CrudeOilRolloverRule()
        ));
    }

    public ContractRolloverCalendar(ContractNameResolver contractNameResolver, List<RolloverRule> rolloverRules) {
        /*
         * Intent: Create a rollover calendar from explicit contract parsing and rule dependencies.
         * Precondition: Resolver and rule list must be non-null.
         * Returns: A constructed ContractRolloverCalendar instance.
         * Postcondition: Rule list is defensively copied.
         */
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
        /*
         * Intent: Resolve the date window where a contract should be treated as front-month active.
         * Precondition: Contract symbol must be parseable by the resolver.
         * Returns: Active window when a matching rollover rule supports the contract root.
         * Postcondition: Calendar state is unchanged.
         */
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
