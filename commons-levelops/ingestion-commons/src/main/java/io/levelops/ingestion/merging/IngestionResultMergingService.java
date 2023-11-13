package io.levelops.ingestion.merging;

import io.levelops.commons.utils.MapUtils;
import io.levelops.ingestion.controllers.MergeableIngestionResult;
import io.levelops.ingestion.merging.strategies.ResultMerger;
import io.levelops.ingestion.merging.strategies.ResultMergingStrategy;
import io.levelops.ingestion.merging.strategies.StorageResultsListMergingStrategy;
import io.levelops.ingestion.merging.strategies.StorageResultsMergingStrategy;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Set;

public class IngestionResultMergingService implements ResultMerger {

    /**
     * See {@link MergeableIngestionResult}
     */
    private static final String MERGE_STRATEGY_FIELD = "merge_strategy";

    // TODO move this to Spring-enabled package and auto-wire this
    private static final Set<ResultMergingStrategy> STRATEGIES = Set.of(
            StorageResultsListMergingStrategy.getInstance(),
            StorageResultsMergingStrategy.getInstance()
    );

    public IngestionResultMergingService() {
    }

    public static boolean isMergeable(Map<String, Object> newResults) {
        return !MapUtils.isEmpty(newResults) && !getMergeStrategy(newResults).isEmpty();
    }

    public Map<String, Object> merge(Map<String, Object> previousResults, Map<String, Object> newResults) {
        if (MapUtils.isEmpty(newResults)) {
            return newResults;
        }
        if (MapUtils.isEmpty(previousResults)) {
            return newResults;
        }
        String mergeStrategy = getMergeStrategy(newResults);
        for (ResultMergingStrategy strategy : STRATEGIES) {
            if (strategy.getName().equalsIgnoreCase(mergeStrategy)) {
                return strategy.merge(previousResults, newResults);
            }
        }
        return newResults;
    }

    @Nonnull
    private static String getMergeStrategy(@Nonnull Map<String, Object> newResults) {
        return StringUtils.trimToEmpty((String) newResults.get(MERGE_STRATEGY_FIELD));
    }


}
