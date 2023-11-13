package io.levelops.commons.databases.models.database;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = CICDJob.CICDJobBuilder.class)
public class CICDJob {
    private static final Pattern PATTERN_PERIOD_GIT_TWICE = Pattern.compile(".*\\.git\\.git$");
    private static final String PERIOD_GIT_ONCE = ".git";

    @JsonProperty("id")
    private final UUID id;

    @JsonProperty("cicd_instance_id")
    private final UUID cicdInstanceId;

    @JsonProperty("project_name")
    private final String projectName;

    @JsonProperty("job_name")
    private final String jobName;

    @JsonProperty("job_full_name")
    private final String jobFullName;

    @JsonProperty("job_normalized_full_name")
    private final String jobNormalizedFullName;

    @JsonProperty("branch_name")
    private final String branchName;

    @JsonProperty("module_name")
    private final String moduleName;

    @JsonProperty("scm_url")
    private final String scmUrl;

    @JsonProperty("scm_user_id")
    private final String scmUserId;

    @JsonProperty("created_at")
    private Instant createdAt;

    @JsonProperty("updated_at")
    private Instant updatedAt;

    public static String sanitizeScmUrl(final String scmUrl){
        if(StringUtils.isBlank(scmUrl)) {
            return scmUrl;
        }
        Matcher matcher = PATTERN_PERIOD_GIT_TWICE.matcher(scmUrl);
        if(matcher.matches()) {
            return scmUrl.substring(0, scmUrl.length() - PERIOD_GIT_ONCE.length());
        } else {
            return scmUrl;
        }
    }
}
