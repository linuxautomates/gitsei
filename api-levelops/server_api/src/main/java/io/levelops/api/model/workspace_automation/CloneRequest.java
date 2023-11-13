package io.levelops.api.model.workspace_automation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CloneRequest {
    @JsonProperty(value = "original_workspace_id")
    private String originalWorkspaceId;

    @JsonProperty(value = "cloned_workspace_name")
    private String clonedWorkspaceName;

    @JsonProperty(value = "cloned_workspace_key_name")
    private String cloneWorkspaceKeyName;
}
