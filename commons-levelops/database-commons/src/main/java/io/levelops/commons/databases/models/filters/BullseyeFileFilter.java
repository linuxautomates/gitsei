package io.levelops.commons.databases.models.filters;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;

@Value
@Builder(toBuilder = true)
public class BullseyeFileFilter {

    List<String> names;
    Map<String, String> functionsCovered;
    Map<String, String> totalFunctions;
    Map<String, String> decisionsCovered;
    Map<String, String> totalDecisions;
    Map<String, String> conditionsCovered;
    Map<String, String> totalConditions;
    Map<String, String> modificationTimeRange;
}
