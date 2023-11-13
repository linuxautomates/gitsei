package io.levelops.commons.functional;

import java.util.function.Function;

public class FunctionUtils {

    /**
     * Allows for unchecked cast, otherwise use Java's Function.identity()
     */
    @SuppressWarnings("unchecked")
    public static <T, U> Function<T, U> identity() {
        //noinspection unchecked
        return x -> (U) x;
    }

}
