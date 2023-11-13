package io.levelops.commons.tenant_management.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Map;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Offsets {
    @JsonProperty("latest_jira_issue_updated_ats")
    private Map<Integer,Long> latestJiraIssueUpdatedAts;
    @JsonProperty("latest_wi_updated_ats")
    private Map<Integer,Long> latestWiUpdatedAts;
    @JsonProperty("latest_scm_committed_ats")
    private Map<Integer,Long> latestScmCommittedAts;
    @JsonProperty("latest_scm_p_r_updated_ats")
    private Map<Integer,Long> latestScmPRUpdatedAts;
}
