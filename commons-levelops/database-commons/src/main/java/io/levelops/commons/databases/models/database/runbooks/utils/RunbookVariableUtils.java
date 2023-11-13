package io.levelops.commons.databases.models.database.runbooks.utils;

import io.levelops.commons.databases.models.database.runbooks.RunbookVariable;
import org.apache.commons.collections4.MapUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RunbookVariableUtils {

    public static RunbookVariable fromJsonObject(@Nonnull String name, @Nullable Object value) {
        RunbookVariable.RunbookValueType valueType = parseValueType(value);
        Object finalValue = value;
        if (valueType == RunbookVariable.RunbookValueType.STRING && !(value instanceof String) && value != null) {
            finalValue = value.toString();
        }
        return RunbookVariable.builder()
                .name(name)
                .valueType(valueType)
                .value(finalValue)
                .build();
    }

    public static List<RunbookVariable> fromKvMap(@Nullable Map<String, Object> kv) {
        return MapUtils.emptyIfNull(kv).entrySet().stream()
                .map(entry -> fromJsonObject(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    public static RunbookVariable.RunbookValueType parseValueType(Object value) {
        if (value == null) {
            return RunbookVariable.RunbookValueType.NONE;
        }
        if (value instanceof Map) {
            return RunbookVariable.RunbookValueType.JSON_BLOB;
        }
        if (value instanceof List) {
            return RunbookVariable.RunbookValueType.JSON_ARRAY;
        }
        return RunbookVariable.RunbookValueType.STRING;
    }

}
