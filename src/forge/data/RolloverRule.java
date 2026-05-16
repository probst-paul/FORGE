package forge.data;

import java.util.Optional;

public interface RolloverRule {
    Optional<ContractRolloverWindow> resolveActiveWindow(FuturesContractCode contractCode);
}
