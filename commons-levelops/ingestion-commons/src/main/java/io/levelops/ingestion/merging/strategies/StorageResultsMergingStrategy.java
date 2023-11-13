package io.levelops.ingestion.merging.strategies;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.jackson.ParsingUtils;
import io.levelops.commons.utils.ListUtils;
import io.levelops.integrations.storage.models.StorageResult;

import javax.annotation.Nullable;
import java.util.Map;

public class StorageResultsMergingStrategy implements ResultMergingStrategy {

    public static final String NAME = "StorageResult";
    private static volatile StorageResultsMergingStrategy instance = null;
    private final ObjectMapper objectMapper;

    // TODO move this to Spring-enabled package and auto-wire this
    public static StorageResultsMergingStrategy getInstance() {
        if (instance != null) {
            return instance;
        }
        synchronized (StorageResultsMergingStrategy.class) {
            if (instance != null) {
                return instance;
            }
            instance = new StorageResultsMergingStrategy();
            return instance;
        }
    }

    private StorageResultsMergingStrategy() {
        this.objectMapper = DefaultObjectMapper.get();
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Map<String, Object> merge(Map<String, Object> previousResults, Map<String, Object> newResults) {
        return ParsingUtils.toJsonObject(objectMapper,
                merge(parseStorageResult(previousResults),
                        parseStorageResult(newResults)));
    }

    @Nullable
    private StorageResult parseStorageResult(Map<String, Object> results) {
        if (results == null) {
            return null;
        }
        return objectMapper.convertValue(results, StorageResult.class);
    }

    @Nullable
    public static StorageResult merge(@Nullable StorageResult previousResult, @Nullable StorageResult newResult) {
        if (newResult == null) {
            return previousResult;
        }
        if (previousResult == null) {
            return newResult;
        }
        return newResult.toBuilder()
                .records(ListUtils.union(previousResult.getRecords(), newResult.getRecords()))
                .ingestionFailures(ListUtils.union(previousResult.getIngestionFailures(), newResult.getIngestionFailures()))
                .build();
    }

}
