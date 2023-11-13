package io.levelops.commons.databases.models.filters;

import lombok.Builder;
import lombok.Value;
import org.apache.commons.lang3.EnumUtils;

import java.util.List;

@Value
@Builder(toBuilder = true)
public class GitlabJobFilter {

    DISTINCT across;
    CALCULATION calculation;
    List<String> repoIds;
    List<String> statuses;
    List<String> commitShas;
    List<String> branches;
    List<String> usernames;
    List<String> stages;
    List<String> integrationIds;
    List<String> pipelineIds;
    List<String> pipelineStatuses;
    List<String> jobNames;

    public enum DISTINCT {
        username,
        branch,
        status,
        pipeline_status,
        job_name,
        stage,
        pipeline_id,
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
