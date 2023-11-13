package io.levelops.integrations.blackduck.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;
import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = BlackDuckIssue.BlackDuckIssueBuilder.class)
public class BlackDuckIssue {

    @JsonProperty("componentName")
    String componentName;

    @JsonProperty("componentVersionName")
    String componentVersionName;

    @JsonProperty("componentVersionOriginName")
    String componentVersionOriginName;

    @JsonProperty("componentVersionOriginId")
    String componentVersionOriginId;

    @JsonProperty("vulnerabilityWithRemediation")
    BlackDuckVulnerability blackDuckVulnerability;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = BlackDuckVulnerability.BlackDuckVulnerabilityBuilder.class)
    public static class BlackDuckVulnerability {
        @JsonProperty("vulnerabilityName")
        String vulnerabilityName;

        @JsonProperty("description")
        String description;

        @JsonProperty("vulnerabilityPublishedDate")
        Date vulnerabilityPublishedDate;

        @JsonProperty("vulnerabilityUpdatedDate")
        Date vulnerabilityUpdatedDate;

        @JsonProperty("componentVersionOriginId")
        String componentVersionOriginId;

        @JsonProperty("baseScore")
        Float baseScore;

        @JsonProperty("overallScore")
        Float overallScore;

        @JsonProperty("exploitabilitySubscore")
        Float exploitabilitySubScore;

        @JsonProperty("impactSubscore")
        Float impactSubScore;

        @JsonProperty("source")
        String source;

        @JsonProperty("severity")
        String severity;

        @JsonProperty("remediationStatus")
        String remediationStatus;

        @JsonProperty("cweId")
        String cweId;

        @JsonProperty("remediationCreatedAt")
        Date remediationCreatedAt;

        @JsonProperty("remediationUpdatedAt")
        Date remediationUpdatedAt;

        @JsonProperty("relatedVulnerability")
        String relatedVulnerability;

        @JsonProperty("bdsaTags")
        List<String> bdsaTags;

    }

}
