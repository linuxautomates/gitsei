package io.levelops.commons.utils;

import java.util.Set;

public class SetUtils {

    public static <T> boolean contains(Set<T> set, T object) {
        return set != null && set.contains(object);
    }

}
