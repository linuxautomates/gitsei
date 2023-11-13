package io.levelops.commons.databases.models.database.jira;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.integrations.jira.models.JiraUser;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.lang3.ObjectUtils;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DbJiraField.DbJiraFieldBuilder.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DbJiraUser {

    @JsonProperty("id")
    private String id;

    @JsonProperty("jira_id")
    private String jiraId;

    @JsonProperty("active")
    private Boolean active;

    @JsonProperty("account_type")
    private String accountType;

    @JsonProperty("display_name")
    private String displayName;

    @JsonProperty("integration_id")
    private String integrationId;

    @JsonProperty("created_at")
    private Long createdAt;

    public static DbJiraUser fromJiraUser(JiraUser source, String integrationId) {
        return DbJiraUser.builder()
                .active(!Boolean.FALSE.equals(source.getActive())) //we mark null active as true
                .integrationId(integrationId)
                .displayName(source.getDisplayName())
                .jiraId(ObjectUtils.firstNonNull(source.getAccountId(), source.getName(), ""))
                .accountType(source.getAccountType())
                .build();
    }
}
