package io.levelops.commons.databases.models.database;

import org.apache.commons.lang3.EnumUtils;
import org.jetbrains.annotations.NotNull;

public enum SortingOrder {
    ASC,
    DESC;

    public static SortingOrder fromString(final String sortingOrder){
        return EnumUtils.<SortingOrder>getEnumIgnoreCase(SortingOrder.class, sortingOrder);
    }

    @NotNull
    public static String getNullsPosition(SortingOrder sortOrder) {
        return SortingOrder.DESC.equals(sortOrder) ? "LAST" : "FIRST";
    }
}