package forge.data.rollover;

import forge.data.contract.FuturesContractCode;

import java.util.Optional;

public interface RolloverRule {
    Optional<ContractRolloverWindow> resolveActiveWindow(FuturesContractCode contractCode);
}
