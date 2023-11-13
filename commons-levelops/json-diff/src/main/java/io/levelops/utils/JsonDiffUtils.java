package io.levelops.utils;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.flipkart.zjsonpatch.DiffFlags;
import com.flipkart.zjsonpatch.JsonDiff;
import io.levelops.commons.utils.MapUtils;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.lang3.Validate;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class JsonDiffUtils {

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = PathDiff.PathDiffBuilder.class)
    public static class PathDiff {
        @JsonProperty("op")
        String op;
        @JsonProperty("path")
        String path;
        @JsonProperty("value")
        JsonNode value;
    }

    public static List<PathDiff> diff(ObjectMapper objectMapper, String beforeJson, String afterJson) throws IOException {
        JsonNode before = objectMapper.readTree(beforeJson);
        JsonNode after = objectMapper.readTree(afterJson);
        return JsonDiffUtils.diff(before, after);
    }

    public static JsonNode diffAsJson(JsonNode beforeNode, JsonNode afterNode) {
        return JsonDiff.asJson(beforeNode, afterNode, DiffFlags.dontNormalizeOpIntoMoveAndCopy());
    }

    public static List<PathDiff> diff(JsonNode beforeNode, JsonNode afterNode) {
        JsonNode jsonNode = diffAsJson(beforeNode, afterNode);
        if (jsonNode == null || !jsonNode.isArray() || !jsonNode.elements().hasNext()) {
            return Collections.emptyList();
        }
        List<PathDiff> diffList = new ArrayList<>();
        Iterator<JsonNode> elements = jsonNode.elements();
        while (elements.hasNext()) {
            JsonNode element = elements.next();
            if (element == null) {
                continue;
            }
            diffList.add(convertDiff(element));
        }
        return diffList;
    }

    private static PathDiff convertDiff(JsonNode diff) {
        String op = Optional.ofNullable(diff.get("op"))
                .map(JsonNode::asText)
                .orElse("");
        String path = Optional.ofNullable(diff.get("path"))
                .map(JsonNode::asText)
                .orElse("");
        return PathDiff.builder()
                .op(op)
                .path(path)
                .value(diff.get("value"))
                .build();
    }

    public static Map<String, List<PathDiff>> groupBy(List<PathDiff> diffList, String basePath) {
        Map<String, List<PathDiff>> groupBy = new HashMap<>();
        for (PathDiff diff : diffList) {
            JsonPathUtils.getSubFolderPath(diff.getPath(), basePath).ifPresent(
                    key -> MapUtils.mutate(groupBy, key, ArrayList::new,
                            group -> group.add(diff)));
        }
        return groupBy;
    }

    public static Map<String, List<JsonNode>> groupBy(JsonNode diffArray, String basePath) {
        Validate.isTrue(diffArray.isArray(), "Diff must be an array");

        Map<String, List<JsonNode>> groupBy = new HashMap<>();
        diffArray.forEach(elem -> {
            String path = JsonDiffUtils.getPathFromDiff(elem);
            JsonPathUtils.getSubFolderPath(path, basePath)
                    .ifPresent(key -> MapUtils.mutate(groupBy, key, ArrayList::new,
                            group -> group.add(elem)));
        });
        return groupBy;
    }

    @Nullable
    public static String getPathFromDiff(@Nullable JsonNode diff) {
        return Optional.ofNullable(diff)
                .map(d -> d.get("path"))
                .map(JsonNode::asText)
                .orElse(null);
    }

    public static Optional<String> getOpFromDiff(@Nullable JsonNode diff) {
        return Optional.ofNullable(diff)
                .map(d -> d.get("op"))
                .map(JsonNode::asText);
    }

}
