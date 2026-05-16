package forge.data.importing;

import forge.data.contract.ContractNameResolver;
import forge.data.postgres.PostgresDatabaseSettings;
import forge.data.postgres.PostgresTradeRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Execution(ExecutionMode.CONCURRENT)
class ScidDataImportServiceTest {
    private final ScidDataImportService importService = new ScidDataImportService(
            new ContractNameResolver(),
            new PostgresTradeRepository(new PostgresDatabaseSettings(
                    "localhost",
                    5432,
                    "forge",
                    "postgres",
                    "postgres",
                    ""
            ))
    );

    @Nested
    class PlanImport {
        @Test
        void rejectsUnsupportedContractMonthBeforeDatabaseWork() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> importService.planImport("/data/ESK25_FUT_CME.scid")
            );

            assertTrue(exception.getMessage().contains("Unsupported contract month K for ES"));
        }
    }
}
