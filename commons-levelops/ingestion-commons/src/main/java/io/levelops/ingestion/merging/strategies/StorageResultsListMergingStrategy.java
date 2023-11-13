package io.levelops.ingestion.merging.strategies;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.jackson.ParsingUtils;
import io.levelops.commons.utils.ListUtils;
import io.levelops.ingestion.controllers.ControllerIngestionResult;
import io.levelops.ingestion.controllers.generic.ControllerIngestionResultList;
import io.levelops.integrations.storage.models.StorageResult;
import org.apache.commons.collections4.SetUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class StorageResultsListMergingStrategy implements ResultMergingStrategy {

    public static final String NAME = "ControllerIngestionResultList<StorageResult>";
    private static volatile StorageResultsListMergingStrategy instance = null;
    private final ObjectMapper objectMapper;

    // TODO move this to Spring-enabled package and auto-wire this
    public static StorageResultsListMergingStrategy getInstance() {
        if (instance != null) {
            return instance;
        }
        synchronized (StorageResultsListMergingStrategy.class) {
            if (instance != null) {
                return instance;
            }
            instance = new StorageResultsListMergingStrategy();
            return instance;
        }
    }

    private StorageResultsListMergingStrategy() {
        this.objectMapper = DefaultObjectMapper.get();
    }

    @Override
    public String getName() {
        return NAME;
    }

    public Map<String, Object> merge(Map<String, Object> previousResults, Map<String, Object> newResults) {
        List<StorageResult> previousRecords = parseStorageResultList(previousResults.get("records"));
        List<StorageResult> newRecords = parseStorageResultList(newResults.get("records"));


        Map<String, StorageResult> previousResultsByType = groupStorageResultsByType(previousRecords);
        Map<String, StorageResult> newResultsByType = groupStorageResultsByType(newRecords);
        Set<String> mergedTypes = SetUtils.union(previousResultsByType.keySet(), newResultsByType.keySet()).toSet();

        List<ControllerIngestionResult> mergedResults = new ArrayList<>();
        for (String type : mergedTypes) {
            mergedResults.add(StorageResultsMergingStrategy.merge(previousResultsByType.get(type), newResultsByType.get(type)));
        }
        return ParsingUtils.toJsonObject(objectMapper, new ControllerIngestionResultList(mergedResults));
    }

    private List<StorageResult> parseStorageResultList(Object records) {
        if (records == null) {
            return List.of();
        }
        return objectMapper.convertValue(records, objectMapper.getTypeFactory().constructCollectionLikeType(List.class, StorageResult.class));
    }

    private Map<String, StorageResult> groupStorageResultsByType(List<StorageResult> results) {
        return ListUtils.emptyIfNull(results).stream()
                .filter(Objects::nonNull)
                .filter(r -> r.getStorageMetadata() != null && r.getStorageMetadata().getDataType() != null)
                .collect(Collectors.toMap(r -> r.getStorageMetadata().getDataType(), r -> r, (a, b) -> b));
    }
}
