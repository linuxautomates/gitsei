package io.levelops.commons.comparison;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public class ComparisonUtils {

    /**
     * Check if value U of object T has changed.
     * Ignores null values in the updated object.
     * Example:
     * {@code hasChanged(dbRunbook, updatedRunbook, Runbook::getName) }
     *
     * @return true there is an updated
     */
    public static <T, U> boolean hasChanged(T previous, T updated, Function<T, ? extends U> mapper) {
        // if there is no updated value, don't update
        Optional<? extends U> updatedValue = Optional.ofNullable(updated).map(mapper);
        if (updatedValue.isEmpty()) {
            return false;
        }
        // if there is no previous value, do update
        Optional<? extends U> previousValue = Optional.ofNullable(previous).map(mapper);
        if (previousValue.isEmpty()) {
            return true;
        }
        // only update if updated value is different from previous value
        return !Objects.equals(previousValue.get(), updatedValue.get());
    }


}
