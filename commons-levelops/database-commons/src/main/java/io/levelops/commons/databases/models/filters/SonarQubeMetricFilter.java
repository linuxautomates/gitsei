package io.levelops.commons.databases.models.filters;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.apache.commons.lang3.EnumUtils;

import java.util.List;
import java.util.Map;

@Value
@Builder(toBuilder = true)
public class SonarQubeMetricFilter {

    List<String> integrationIds;
    List<String> organizations;
    List<String> projects;
    List<String> visibilities;
    Map<String,String> complexityScore;

    @Builder.Default
    SCOPE scope = SCOPE.repo;

    List<String> pullRequests;
    List<String> prBranches;
    List<String> prTargetBranches;
    List<String> prBaseBranches;

    List<String> branches;

    List<String> metrics;
    List<String> dtypes;// for internal usage

    @NonNull
    Long ingestedAt;

    DISTINCT DISTINCT;

    public enum SCOPE {
        pull_request,
        branch,
        repo;

        public static SCOPE fromString(String parent) {
            return EnumUtils.getEnumIgnoreCase(SCOPE.class, parent);
        }
    }

    public enum DISTINCT {
        organization,
        project,
        visibility,
        pull_request,
        pr_branch,
        pr_target_branch,
        pr_base_branch,
        branch,
        metric,
        trend;

        public static DISTINCT fromString(String distinct) {
            return EnumUtils.getEnumIgnoreCase(DISTINCT.class, distinct);
        }
    }

}
