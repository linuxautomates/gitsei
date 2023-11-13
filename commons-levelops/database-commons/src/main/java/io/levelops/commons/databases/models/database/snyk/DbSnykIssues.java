package io.levelops.commons.databases.models.database.snyk;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.integrations.snyk.models.SnykIssues;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.lang3.time.DateUtils;

import javax.validation.constraints.NotNull;
import java.util.Calendar;
import java.util.Date;

@Value
@Builder(toBuilder = true)
@JsonDeserialize
public class DbSnykIssues {

    @JsonProperty("integration_id")
    String integrationId;

    @JsonProperty("org_id")
    String orgId;

    @JsonProperty("org")
    String org;

    @JsonProperty("project_id")
    String projectId;

    @JsonProperty("project_name")
    String projectName;

    @JsonProperty("issues")
    SnykIssues.Issues issues;

    @JsonProperty("package_manager")
    String packageManager;

    @JsonProperty("ingested_at")
    Date ingestedAt;

    public static DbSnykIssues fromIssues(@NotNull SnykIssues snykIssues, String integrationId, Date fetchTime) {
        Date truncatedDate = DateUtils.truncate(fetchTime, Calendar.DATE);
        return DbSnykIssues.builder()
                .integrationId(integrationId)
                .ingestedAt(truncatedDate)
                .orgId(snykIssues.getOrgId())
                .org(snykIssues.getOrg())
                .projectId(snykIssues.getProjectId())
                .issues(snykIssues.getIssues())
                .packageManager(snykIssues.getPackageManager())
                .projectName(snykIssues.getProjectName())
                .build();
    }
}
