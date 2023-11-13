package io.levelops.commons.databases.models.response;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DbAggregationResultsMerger {
    public static List<DbAggregationResult> mergeResultsWithAndWithoutStacks(List<DbAggregationResult> resultWithoutStacks, List<DbAggregationResultStacksWrapper> resultWithStacks){
        Map<AcrossUniqueKey, List<DbAggregationResult>> resultWithStacksMap = resultWithStacks.stream()
                .collect(Collectors.groupingBy(x -> AcrossUniqueKey.fromDbAggregationResultStacksWrapper(x), Collectors.mapping(x -> x.getDbAggregationResult(), Collectors.toList())));

        List<DbAggregationResult> results = new ArrayList<>();
        for(DbAggregationResult currentResultWithoutStack : resultWithoutStacks){
            AcrossUniqueKey key = AcrossUniqueKey.fromDbAggregationResult(currentResultWithoutStack);
            if(resultWithStacksMap.containsKey(key)){
                List<DbAggregationResult> stacks = resultWithStacksMap.getOrDefault(key, null);
                results.add(currentResultWithoutStack.toBuilder().stacks(stacks).build());
            }
        }
        return results;
    }
}
