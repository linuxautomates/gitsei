package io.levelops.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.Optional;

public class JsonPathUtils {

    public static boolean isSubFolder(@Nullable String path, String basePath) {
        return path != null && path.startsWith(StringUtils.appendIfMissing(basePath, "/"));
    }

    public static Optional<String> getSubFolderPath(String path, String basePath) {
        basePath = StringUtils.appendIfMissing(basePath, "/");
        if (!isSubFolder(path, basePath)) {
            return Optional.empty();
        }
        int indexOfSlash = path.indexOf("/", basePath.length());
        if (indexOfSlash < 0) {
            return Optional.of(path);
        }
        return Optional.of(path.substring(0, indexOfSlash));
    }

    public static boolean isTopLevelSubFolder(@Nullable String path, String basePath) {
        basePath = StringUtils.appendIfMissing(basePath, "/");
        if (!isSubFolder(path, basePath)) {
            return false;
        }
        String pathPostfix = path.substring(basePath.length());
        return !pathPostfix.contains("/");
    }

    @Nonnull
    public static JsonNode getData(@Nullable String path, @Nullable JsonNode source) {
        if (Strings.isEmpty(path) || !path.startsWith("/")) {
            return NullNode.getInstance();
        }

        String[] segments = path.split("/");
        JsonNode currentNode = source;
        for (String segment : segments) {
            if (currentNode == null) {
                break;
            }
            if (Strings.isEmpty(segment)) {
                continue;
            }
            currentNode = currentNode.get(JsonPathUtils.unescapeSegement(segment));
        }
        if (currentNode == null) {
            return NullNode.getInstance();
        }
        return currentNode;
    }

    public static String unescapeSegement(String pathSegment) {
        return pathSegment
                .replaceAll("~0", "~")
                .replaceAll("~1", "/");
    }

}
