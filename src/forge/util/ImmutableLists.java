package forge.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ImmutableLists {
    private ImmutableLists() {
    }

    /*
     * Intent: Create a defensive immutable copy of a required list.
     * Precondition: values must not be null; name should identify the argument for error messages.
     * Returns: An unmodifiable List containing the same elements in the same order.
     * Postcondition: Future changes to the source list cannot affect the returned list.
     */
    public static <T> List<T> copyOfRequired(List<? extends T> values, String name) {
        if (values == null) {
            throw new IllegalArgumentException(name + " is required");
        }
        return Collections.unmodifiableList(new ArrayList<>(values));
    }
}
