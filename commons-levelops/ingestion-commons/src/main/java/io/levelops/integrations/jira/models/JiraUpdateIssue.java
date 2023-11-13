package io.levelops.integrations.jira.models;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.integrations.jira.models.JiraIssueFields.JiraPriority;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.ObjectUtils;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = JiraUpdateIssue.JiraUpdateIssueBuilder.class)
public class JiraUpdateIssue {

    @JsonProperty("summary")
    List<Map<JiraUpdateOp, String>> summary; // set
    @JsonProperty("description")
    List<Map<JiraUpdateOp, String>> description; // set
    @JsonProperty("assignee")
    List<Map<JiraUpdateOp, JiraUser>> assignee; // set
    @JsonProperty("duedate")
    List<Map<JiraUpdateOp, String>> duedate; // set (format "2021-03-16")

    @JsonProperty("labels")
    List<Map<JiraUpdateOp, String>> labels; // add, remove

    @JsonProperty("components")
    List<Map<JiraUpdateOp, String>> components; // add, remove

    @JsonProperty("versions")
    List<Map<JiraUpdateOp, Map<String, String>>> versions;

    @JsonProperty("fixVersions")
    List<Map<JiraUpdateOp, Map<String, String>>> fixVersions;

    @JsonProperty("priority")
    List<Map<JiraUpdateOp, JiraPriority>> priority; // set

    // map of custom fields
    // NB: JsonUnwrapped only work with classes, for Maps use JsonAnyGetter
    @Singular
    Map<String, List<Map<JiraUpdateOp, Object>>> customFields; // set

    @JsonAnyGetter
    public Map<String, List<Map<JiraUpdateOp, Object>>> getCustomFields() {
        return customFields;
    }

    public enum JiraUpdateOp {
        SET, EDIT, ADD, REMOVE;

        @JsonValue
        @Override
        public String toString() {
            return super.toString().toLowerCase();
        }

        @Nullable
        @JsonCreator
        public static JiraUpdateOp fromString(@Nullable String value) {
            return EnumUtils.getEnumIgnoreCase(JiraUpdateOp.class, value);
        }
    }

    public static class JiraUpdateIssueBuilder {

        public JiraUpdateIssueBuilder setSummary(String summary) {
            return this.summary(List.of(Map.of(JiraUpdateOp.SET, summary)));
        }

        public JiraUpdateIssueBuilder setDescription(String description) {
            return this.description(List.of(Map.of(JiraUpdateOp.SET, description)));
        }

        public JiraUpdateIssueBuilder setAssignee(JiraUser assignee) {
            return this.assignee(List.of(Map.of(JiraUpdateOp.SET, assignee)));
        }

        public JiraUpdateIssueBuilder setDuedate(String duedate) {
            return this.duedate(List.of(Map.of(JiraUpdateOp.SET, duedate)));
        }

        public JiraUpdateIssueBuilder setCustomField(String customField, Object value) {
            value = ObjectUtils.isNotEmpty(value) ? value : null;
            return this.customField(customField, List.of(Map.of(JiraUpdateOp.SET, value)));
        }

        public JiraUpdateIssueBuilder setPriority(JiraPriority priority) {
            return this.priority(List.of(Map.of(JiraUpdateOp.SET, priority)));
        }

    }

}
