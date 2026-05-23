package forge.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ImmutableLists {
    private ImmutableLists() {
    }

    public static <T> List<T> copyOfRequired(List<? extends T> values, String name) {
        if (values == null) {
            throw new IllegalArgumentException(name + " is required");
        }
        return Collections.unmodifiableList(new ArrayList<>(values));
    }
}
