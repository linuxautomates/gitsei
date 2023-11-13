package io.levelops.commons.utils;

import com.fasterxml.jackson.databind.JavaType;
import com.google.common.io.Resources;
import io.levelops.commons.jackson.DefaultObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Objects;

public class ResourceUtils {

    public static InputStream getResourceAsStream(String resourceURL) throws IOException {
        //noinspection UnstableApiUsage
        return Resources.getResource(resourceURL).openStream();
    }

    public static String getResourceAsString(String resourceUrl) throws IOException {
        //noinspection UnstableApiUsage
        return Resources.toString(Resources.getResource(resourceUrl), Charset.defaultCharset());
    }

    public static String getResourceAsString(String resourceUrl, ClassLoader classLoader) throws IOException {
        return Resources.toString(Objects.requireNonNull(classLoader.getResource(resourceUrl)), Charset.defaultCharset());
    }

    public static String getResourceAsStringOrThrow(String resourceUrl) {
        try {
            return getResourceAsString(resourceUrl);
        } catch (IOException e) {
            throw new RuntimeException("Could not load resource " + resourceUrl, e);
        }
    }

    public static <T> T getResourceAsObject(String resourceUrl, Class<T> clazz) throws IOException {
        return DefaultObjectMapper.get().readValue(getResourceAsString(resourceUrl), clazz);
    }

    public static <T> T getResourceAsObject(String resourceUrl, JavaType type) throws IOException {
        return DefaultObjectMapper.get().readValue(getResourceAsString(resourceUrl), type);
    }

    public static <T> List<T> getResourceAsList(String resourceUrl, Class<T> clazz) throws IOException {
        return DefaultObjectMapper.get().readValue(getResourceAsString(resourceUrl), DefaultObjectMapper.get().getTypeFactory().constructCollectionType(List.class, clazz));
    }

}
