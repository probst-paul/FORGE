package forge.cli;

import forge.app.UserInput;
import forge.app.UserOutput;
import forge.data.FacadeForgeData;
import forge.data.catalog.InstrumentDataCatalog.AvailableDateRange;
import forge.data.catalog.InstrumentDataCatalog.AvailableInstrumentData;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class InstrumentSelectionService {
    private final FacadeForgeData facadeData;

    public InstrumentSelectionService(FacadeForgeData facadeData) {
        this.facadeData = facadeData;
    }

    public List<String> selectInstruments(UserInput input, UserOutput output) {
        List<AvailableInstrumentData> instruments = facadeData.forgeDataAccess().getAvailableInstruments();
        if (instruments.isEmpty()) {
            throw new IllegalStateException("No instrument data is available");
        }

        output.printLine("Available instruments:");
        for (int i = 0; i < instruments.size(); i++) {
            output.printLine((i + 1) + ". " + instruments.get(i).getSymbol());
        }

        while (true) {
            String rawSelections = input.readString("Select instruments (comma separated numbers)");
            try {
                List<String> selectedSymbols = Arrays.stream(rawSelections.split(","))
                        .map(String::trim)
                        .filter(selection -> !selection.isEmpty())
                        .map(selection -> {
                            int selectedIndex = Integer.parseInt(selection) - 1;
                            if (selectedIndex < 0 || selectedIndex >= instruments.size()) {
                                throw new IllegalArgumentException("Selected instrument is not available");
                            }
                            return instruments.get(selectedIndex).getSymbol();
                        })
                        .collect(Collectors.toList());
                if (selectedSymbols.isEmpty()) {
                    throw new IllegalArgumentException("At least one instrument must be selected");
                }
                return selectedSymbols;
            } catch (IllegalArgumentException exception) {
                output.printLine(exception.getMessage() + ". Please enter available instrument numbers, or enter 'quit' to exit program.");
            }
        }
    }

    public LocalDate[] selectDateRange(UserInput input, UserOutput output, List<String> instruments) {
        AvailableDateRange availableDateRange = facadeData.forgeDataAccess().getSharedDateRange(instruments);
        output.printLine("Available date range for selected instruments: " + availableDateRange);

        while (true) {
            try {
                LocalDate startDate = input.readDateOrDefault(
                        "Start date (YYYY-MM-DD, blank for earliest available)",
                        availableDateRange.getStartDate()
                );
                LocalDate endDate = input.readDateOrDefault(
                        "End date (YYYY-MM-DD, blank for latest available)",
                        availableDateRange.getEndDate()
                );
                facadeData.forgeDataAccess().validateDateRange(instruments, startDate, endDate);
                return new LocalDate[]{startDate, endDate};
            } catch (IllegalArgumentException exception) {
                output.printLine(exception.getMessage() + ". Please enter dates within the available range, or enter 'quit' to exit program.");
            }
        }
    }
}
