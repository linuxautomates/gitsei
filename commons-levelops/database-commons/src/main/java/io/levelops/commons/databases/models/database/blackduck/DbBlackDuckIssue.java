package io.levelops.commons.databases.models.database.blackduck;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Date;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
public class DbBlackDuckIssue {

    @JsonProperty("id")
    private String id;

    @JsonProperty("version_id")
    private String versionId;

    @JsonProperty("description")
    private String description;

    @JsonProperty("componentName")
    String componentName;

    @JsonProperty("componentVersionName")
    String componentVersionName;

    @JsonProperty("vulnerability_name")
    private String vulnerabilityName;

    @JsonProperty("vulnerability_published_at")
    private Date vulnerabilityPublishedAt;

    @JsonProperty("vulnerability_updated_at")
    private Date vulnerabilityUpdatedAt;

    @JsonProperty("base_score")
    private Float baseScore;

    @JsonProperty("overall_score")
    private Float overallScore;

    @JsonProperty("exploitability_subscore")
    private Float exploitabilitySubScore;

    @JsonProperty("impact_subscore")
    private Float impactSubScore;

    @JsonProperty("source")
    private String source;

    @JsonProperty("severity")
    private String severity;

    @JsonProperty("remediation_status")
    private String remediationStatus;

    @JsonProperty("cwe_id")
    private String cwdId;

    @JsonProperty("bdsa_tags")
    private List<String> bdsaTags;

    @JsonProperty("related_vulnerability")
    private String relatedVulnerability;

    @JsonProperty("remediation_created_at")
    private Date remediationCreatedAt;

    @JsonProperty("remediation_updated_at")
    private Date remediationUpdatedAt;

    @JsonProperty("created_at")
    private Long createdAt;

    @JsonProperty("updated_at")
    private Long updatedAt;

}
