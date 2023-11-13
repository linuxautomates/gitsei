package io.levelops.commons.databases.models.filters;

import lombok.Builder;
import lombok.Value;
import org.apache.commons.lang3.EnumUtils;

import java.util.List;

@Value
@Builder(toBuilder = true)
public class GitlabPipelineFilter {

    DISTINCT across;
    CALCULATION calculation;
    List<String> repoIds;
    List<String> pipelineIds;
    List<String> integrationIds;
    List<String> branches;
    List<String> commitShas;
    List<String> statuses;

    public enum DISTINCT {
        status,
        branch,
        repo_id,
        none;

        public static DISTINCT fromString(String st) {
            return EnumUtils.getEnumIgnoreCase(DISTINCT.class, st);
        }
    }

    public enum CALCULATION {
        duration,
        count;

        public static CALCULATION fromString(String st) {
            return EnumUtils.getEnumIgnoreCase(CALCULATION.class, st);
        }
    }
}
