package io.levelops.commons.databases.issue_management;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DbScmCommitWorkItemMapping {

    @JsonProperty("id")
    private UUID id;

    @JsonProperty("scm_integration_id")
    private Integer scmIntegrationId;

    @JsonProperty("commit_sha")
    private String commitSha;

    @JsonProperty("workitem_id")
    private Integer workitemId;
}
