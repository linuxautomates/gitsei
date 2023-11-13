package io.levelops.integrations.snyk.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = SnykIssueRevised.SnykIssueRevisedBuilder.class)
public class SnykIssueRevised {

    @JsonProperty("issues")
    private final List<IssueData> issues;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = SnykIssueRevised.IssueData.IssueDataBuilder.class)
    public static final class IssueData {
        @JsonProperty("id")
        private final String id;
        @JsonProperty("issueType")
        private final String issueType;
        @JsonProperty("issueData")
        private final SnykIssuesData issueData;
        @JsonProperty("isIgnored")
        private final Boolean ignored;
        @JsonProperty("isPatched")
        private final Boolean patched;
        @JsonProperty("pkgName")
        private String packageName;
        @JsonProperty("pkgVersions")
        private List<String> version;
    }
}
