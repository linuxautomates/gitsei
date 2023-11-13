package io.levelops;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.models.JsonDiff;
import io.levelops.models.JsonDiff.Operation;
import io.levelops.utils.JsonDiffUtils;
import io.levelops.utils.JsonDiffUtils.PathDiff;
import io.levelops.utils.JsonPathUtils;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.levelops.utils.JsonDiffUtils.groupBy;

public class JsonDiffService {

    private final ObjectMapper objectMapper;

    public JsonDiffService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Map<String, JsonDiff> diff(String beforeJson, String afterJson) throws IOException {
        return diff(beforeJson, afterJson, null);
    }

    public Map<String, JsonDiff> diff(String beforeJson, String afterJson, List<String> basePathList) throws IOException {
        JsonNode before = Strings.isNotEmpty(beforeJson)? objectMapper.readTree(beforeJson) : objectMapper.createObjectNode();
        JsonNode after = Strings.isNotEmpty(afterJson)? objectMapper.readTree(afterJson) : objectMapper.createObjectNode();
        List<PathDiff> diff = JsonDiffUtils.diff(before, after);
        basePathList = CollectionUtils.isNotEmpty(basePathList) ? basePathList : List.of("/");
        return groupByManyAndAnalyzeDiff(before, after, diff, basePathList);
    }

    private Map<String, JsonDiff> groupByManyAndAnalyzeDiff(JsonNode beforeNode, JsonNode afterNode, List<PathDiff> diff, List<String> basePathList) {
        return basePathList.stream()
                .collect(Collectors.toMap(
                        path -> StringUtils.appendIfMissing(path, "/"),
                        path -> groupByAndAnalyzeDiff(beforeNode, afterNode, diff, path)));
    }

    private JsonDiff groupByAndAnalyzeDiff(JsonNode beforeNode, JsonNode afterNode, List<PathDiff> diff, String basePath) {
        Map<String, List<PathDiff>> diffsByGroup = groupBy(diff, basePath);
        return analyseGroupDiff(beforeNode, afterNode, diffsByGroup, basePath);
    }

    private JsonDiff analyseGroupDiff(JsonNode beforeNode, JsonNode afterNode, Map<String, List<PathDiff>> diffsByGroup, String basePath) {
        String base = StringUtils.appendIfMissing(basePath, "/");
        List<String> added = new ArrayList<>();
        List<String> removed = new ArrayList<>();
        List<String> changed = new ArrayList<>();
        Map<String, JsonDiff.DataChange> dataChanges = new LinkedHashMap<>();

        // for each group, find out what the overall operation was
        diffsByGroup.entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .forEach(entry -> {
                    String key = entry.getKey();
                    Operation groupOp = detectGroupOp(entry.getValue(), base);
                    switch (groupOp) {
                        case REMOVED:
                            removed.add(key);
                            break;
                        case ADDED:
                            added.add(key);
                            break;
                        case CHANGED:
                            changed.add(key);
                            break;
                    }
                    dataChanges.put(key, getDataBeforeAndAfter(key, groupOp, beforeNode, afterNode));
                });

        return JsonDiff.builder()
                .basePath(base)
                .added(added)
                .removed(removed)
                .changed(changed)
                .dataChanges(dataChanges)
                .build();
    }

    private static JsonDiff.DataChange getDataBeforeAndAfter(String path, Operation operation, JsonNode before, JsonNode after) {
        return JsonDiff.DataChange.builder()
                .operation(operation)
                .before(JsonPathUtils.getData(path, before))
                .after(JsonPathUtils.getData(path, after))
                .build();
    }

    private static Operation detectGroupOp(List<PathDiff> groupDiffs, String basePath) {
        if (CollectionUtils.size(groupDiffs) != 1) {
            return Operation.CHANGED;
        }
        PathDiff diff = groupDiffs.get(0);
        if (!JsonPathUtils.isTopLevelSubFolder(diff.getPath(), basePath)) {
            return Operation.CHANGED;
        }
        switch (diff.getOp()) {
            case "remove":
                return Operation.REMOVED;
            case "add":
                return Operation.ADDED;
            default:
                return Operation.CHANGED;
        }
    }




}
