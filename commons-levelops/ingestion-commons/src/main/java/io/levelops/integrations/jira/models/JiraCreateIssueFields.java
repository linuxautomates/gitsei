package io.levelops.integrations.jira.models;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.integrations.jira.models.JiraIssueFields.JiraPriority;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import org.apache.commons.lang3.ObjectUtils;

import java.util.List;
import java.util.Map;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = JiraCreateIssueFields.JiraCreateIssueFieldsBuilder.class)
public class JiraCreateIssueFields {

    @JsonProperty("summary")
    String summary; // REQUIRED

    @JsonProperty("description")
    String description;

    @JsonProperty("project")
    JiraProject project; // only id needed - REQUIRED

    @JsonProperty("issuetype")
    JiraIssueType issueType; // only id needed - REQUIRED

    @JsonProperty("assignee")
    JiraUser assignee; // only name (Jira Server) or accountId (Jira Cloud) needed

//    JiraUser reporter; // only name  (Jira Server) or accountId (Jira Cloud) needed - NOT USING IT TO PREVENT PERMISSION ISSUES

    @JsonProperty("labels")
    List<String> labels;

    @JsonProperty("components")
    List<JiraComponent> components; // only id

    @JsonProperty("versions")
    List<JiraVersion> versions;

    @JsonProperty("fixVersions")
    List<JiraVersion> fixVersions;

    @JsonProperty("priority")
    JiraPriority priority; // only id needed

    @Singular
    Map<String, Object> customFields;

    @JsonAnyGetter
    public Map<String, Object> getCustomFields() {
        return customFields;
    }

    public static class JiraCreateIssueFieldsBuilder {

        public void setCustomField(String customField, Object value) {
            value = ObjectUtils.isNotEmpty(value) ? value : null;
            this.customField(customField, value);
        }
    }

        // parent { key : "lev-123" } // requires task type to be a valid sub task
}
