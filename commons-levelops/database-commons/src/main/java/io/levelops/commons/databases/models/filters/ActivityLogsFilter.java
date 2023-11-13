package io.levelops.commons.databases.models.filters;

import lombok.Builder;
import lombok.Value;
import org.apache.commons.lang3.EnumUtils;

import java.util.List;
import java.util.Map;

@Value
@Builder(toBuilder = true)
public class ActivityLogsFilter {

    List<String> targetItems;
    List<String> emails;
    List<String> actions;

    ActivityLogsFilter.DISTINCT across;
    Map<String, Map<String, String>> partialMatch;

    public enum DISTINCT {
        email,
        targetitem,
        none;

        public static ActivityLogsFilter.DISTINCT fromString(String across) {
            return EnumUtils.getEnumIgnoreCase(ActivityLogsFilter.DISTINCT.class, across);
        }
    }
}