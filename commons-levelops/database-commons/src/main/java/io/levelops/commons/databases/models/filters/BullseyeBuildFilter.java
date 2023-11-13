package io.levelops.commons.databases.models.filters;

import lombok.Builder;
import lombok.Value;
import org.apache.commons.lang3.EnumUtils;

import java.util.List;
import java.util.Map;

@Value
@Builder(toBuilder = true)
public class BullseyeBuildFilter {

    List<String> names;
    List<String> directories;
    List<String> cicdJobRunIds;
    List<String> projects;
    List<String> buildIds;
    List<String> jobNames;
    List<String> jobFullNames;
    List<String> jobNormalizedFullNames;
    List<String> fileHashes;
    Map<String, String> functionsCovered;
    Map<String, String> totalFunctions;
    Map<String, String> decisionsCovered;
    Map<String, String> totalDecisions;
    Map<String, String> conditionsCovered;
    Map<String, String> totalConditions;
    Map<String, String> builtAtRange;
    Map<String, Map<String, String>> partialMatch;

    Distinct across;
    Calculation calculation;

    public enum Distinct {
        project,
        directory,
        name,
        trend,
        job_run_id,
        job_name,
        job_full_name,
        job_normalized_full_name,
        none;

        public static Distinct fromString(String st) {
            return EnumUtils.getEnumIgnoreCase(Distinct.class, st);
        }
    }

    public enum Calculation {
        total_coverage;

        public static Calculation fromString(String output) {
            return EnumUtils.getEnumIgnoreCase(Calculation.class, output);
        }
    }
}
