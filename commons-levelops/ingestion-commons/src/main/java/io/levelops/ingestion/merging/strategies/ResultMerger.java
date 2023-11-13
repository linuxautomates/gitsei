package io.levelops.ingestion.merging.strategies;

import java.util.Map;

public interface ResultMerger {

    Map<String, Object> merge(Map<String, Object> previousResults, Map<String, Object> newResults);

}
