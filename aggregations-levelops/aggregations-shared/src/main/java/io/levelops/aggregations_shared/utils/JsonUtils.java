package io.levelops.aggregations_shared.utils;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.ParsingUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

// TODO: Move to commons, this is copied from ingestion code
@Log4j2
public class JsonUtils {

    public static Optional<Object> getJsonValue(Object o) {
        if (!(o instanceof LinkedHashMap)) {
            return Optional.empty();
        }
        LinkedHashMap map = (LinkedHashMap) o;
        if (!"jsonb".equals(map.get("type"))) {
            return Optional.empty();
        }
        Object value = map.get("value");
//        if (!(value instanceof LinkedHashMap)) {
//            return Optional.empty();
//        }
        return Optional.of(value);
    }

    public static Object parseJson(ObjectMapper objectMapper, Object o) {
        return getJsonValue(o).map(Object::toString).map((String str) -> {
            try {
                return (Object) objectMapper.readValue(str, Map.class);
            } catch (IOException e) {
                return null;
            }
        }).filter(Objects::nonNull).orElse(o);
    }

    @Nullable
    public static Object parseJsonNullable(ObjectMapper objectMapper, Object o) {
        return getJsonValue(o).map(Object::toString).map((String str) -> {
            try {
                return (Object) objectMapper.readValue(str, Map.class);
            } catch (IOException e) {
                return null;
            }
        }).filter(Objects::nonNull).orElse(null);
    }

    @Nullable
    public static Object parseJsonField(ObjectMapper objectMapper, ResultSet rs, String field) throws SQLException {
        String value = rs.getString(field);
        if (StringUtils.isEmpty(value)) {
            return null;
        }
        try {
            return objectMapper.readValue(value, Object.class);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse JSON data from DB field '" + field + "'", e);
            return null;
        }
    }

    @Nullable
    public static Map<String, Object> parseJsonObjectField(ObjectMapper objectMapper, ResultSet rs, String field) throws SQLException {
        String value = rs.getString(field);
        if (StringUtils.isEmpty(value)) {
            return null;
        }
        return ParsingUtils.parseJsonObject(objectMapper, "field", value);
    }

    public static String serializeJson(ObjectMapper objectMapper, Object o, String defaultValue, String debugName) {
        if (o == null) {
            return defaultValue;
        }
        try {
            return objectMapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize {}: {}", debugName, o);
        }
        return defaultValue;
    }
}
