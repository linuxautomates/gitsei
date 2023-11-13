package io.levelops.commons.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;

import java.io.IOException;
import java.io.InputStream;

/**
 * Legacy. ;)
 *
 * @deprecated this was supposed  to be an excellent joke - see alternatives in this class methods' bodies
 */
@Deprecated
class ConvertUtils {

    public static ObjectMapper get() {
        return DefaultObjectMapper.get();
    }

    public static String writeAsPrettyJson(Object o) {
        return DefaultObjectMapper.writeAsPrettyJson(o);
    }

    public static void prettyPrint(Object o) {
        DefaultObjectMapper.prettyPrint(o);
    }

    public static InputStream getResourceAsStream(String resourceURL) throws IOException {
        return ResourceUtils.getResourceAsStream(resourceURL);
    }

    public static String getResourceAsString(String resourceUrl) throws IOException {
        return ResourceUtils.getResourceAsString(resourceUrl);
    }

    public static String getResourceAsStringOrThrow(String resourceUrl) {
        return ResourceUtils.getResourceAsStringOrThrow(resourceUrl);
    }

}
