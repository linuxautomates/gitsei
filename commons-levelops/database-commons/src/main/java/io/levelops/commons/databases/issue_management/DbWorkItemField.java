package io.levelops.commons.databases.issue_management;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.levelops.commons.databases.utils.IssueMgmtCustomFieldUtils;
import io.levelops.integrations.azureDevops.models.WorkItemField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DbWorkItemField {
    @JsonProperty("id")
    private UUID id;

    @JsonProperty("custom")
    private Boolean custom;

    @JsonProperty("integration_id")
    private String integrationId;

    @JsonProperty(value = "name")
    private String name;

    @JsonProperty(value = "field_key")
    private String fieldKey;

    @JsonProperty(value = "items_type")
    private String itemsType;

    @JsonProperty(value = "field_type")
    private String fieldType;

    @JsonProperty("created_at")
    private Instant createdAt;

    public static DbWorkItemField fromAzureDevopsWorkItemField(String integrationId, WorkItemField workItemField) {
        return DbWorkItemField.builder()
                .custom(IssueMgmtCustomFieldUtils.isCustomField(workItemField.getReferenceName()))
                .integrationId(integrationId)
                .name(workItemField.getName())
                .fieldKey(workItemField.getReferenceName())
                .fieldType(workItemField.getType())
                .itemsType(null)
                .build();
    }
}
