package io.levelops.commons.jackson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;

import org.apache.logging.log4j.util.Strings;

import java.io.IOException;
import java.sql.Array;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Log4j2
public class ParsingUtils {

    public static String serialize(String name, Object object, String defaultValue) {
        return serialize(DefaultObjectMapper.get(), name, object, defaultValue);
    }

    public static String serialize(ObjectMapper objectMapper, String name, Object object, String defaultValue) {
        try {
            return serializeOrThrow(objectMapper, object, defaultValue);
        } catch (IOException e) {
            log.warn("Failed to serialize '{}': {}", name, object, e);
            return defaultValue;
        }
    }

    public static String serializeOrThrow(Object object, String defaultValue) throws JsonProcessingException {
        return serializeOrThrow(DefaultObjectMapper.get(), object, defaultValue);
    }

    public static String serializeOrThrow(ObjectMapper objectMapper, Object object, String defaultValue) throws JsonProcessingException {
        if (object == null) {
            return defaultValue;
        }
        return objectMapper.writeValueAsString(object);
    }

    public static Map<String, Object> parseJsonObject(ObjectMapper objectMapper, String name, String json) {
        return parseMap(objectMapper, name, String.class, Object.class, json);
    }

    public static List<Map<String, Object>> parseJsonList(ObjectMapper objectMapper, String name, String json) {
        //noinspection unchecked
        return parseParametricObject(objectMapper, name, List.class, objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class), json);
    }

    public static <K, V> Map<K, V> parseMap(ObjectMapper objectMapper, String name, Class<K> keyClass, Class<V> valueClass, String json) {
        if (Strings.isEmpty(json)) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(json, objectMapper.getTypeFactory()
                    .constructMapType(Map.class, keyClass, valueClass));
        } catch (IOException e) {
            log.warn("Failed to parse {} from: {}", name, json, e);
            return null;
        }
    }

    public static <K, V> Map<K, V> parseMap(ObjectMapper objectMapper, String name, JavaType keyType, JavaType valueType, String json) {
        if (Strings.isEmpty(json)) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(json, objectMapper.getTypeFactory()
                    .constructMapType(Map.class, keyType, valueType));
        } catch (IOException e) {
            log.warn("Failed to parse {} from: {}", name, json, e);
            return null;
        }
    }

    public static <T> List<T> parseList(ObjectMapper objectMapper, String name, Class<T> clazz, String json) {
        if (Strings.isEmpty(json)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, objectMapper.getTypeFactory()
                    .constructCollectionLikeType(List.class, clazz));
        } catch (IOException e) {
            log.warn("Failed to parse {} from: {}", name, json, e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> Set<T> parseSet(String name, Class<T> clazz, Array array) {
        try {
            if (array == null || array.getArray() == null) {
                return Collections.emptySet();
            }
            return (Set<T>) Arrays.stream((T[]) array.getArray()).filter(item -> item != null)
                .collect(Collectors.toSet());
        } catch (SQLException e) {
            log.warn("Failed to parse {} from: {}", name, array, e);
            return null;
        }
    }

    public static <T> Set<T> parseSet(ObjectMapper objectMapper, String name, Class<T> clazz, String json) {
        if (Strings.isEmpty(json)) {
            return Collections.emptySet();
        }
        try {
            return objectMapper.readValue(json, objectMapper.getTypeFactory()
                    .constructCollectionType(Set.class, clazz));
        } catch (IOException e) {
            log.warn("Failed to parse {} from: {}", name, json, e);
            return null;
        }
    }

    public static <T> T parseObject(ObjectMapper objectMapper, String name, Class<T> clazz, String json) {
        if (Strings.isEmpty(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, clazz);
        } catch (IOException e) {
            log.warn("Failed to parse {} from: {}", name, json, e);
            return null;
        }
    }

    public static <T> T parseParametricObject(ObjectMapper objectMapper, String name, Class<T> baseType, JavaType parameterType, String json) {
        if (Strings.isEmpty(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, objectMapper.getTypeFactory()
                    .constructParametricType(baseType, parameterType));
        } catch (IOException e) {
            log.warn("Failed to parse {} from: {}", name, json, e);
            return null;
        }
    }

    public static <T> Map<String, Object> toJsonObject(ObjectMapper objectMapper, T object) {
        if (object == null) {
            return null;
        }
        return objectMapper.convertValue(object, objectMapper.getTypeFactory()
                .constructMapType(Map.class, String.class, Object.class));
    }

}
