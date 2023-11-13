package io.levelops.aggregations.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import io.levelops.aggregations.models.jira.JiraAggByIssueType;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.HashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Setter
@Getter
@ToString
@SuperBuilder(toBuilder = true)
@JsonDeserialize(builder = JiraAggData.JiraAggDataBuilderImpl.class)
public class JiraAggData extends AggData {
    @JsonProperty("agg_by_issue_type")
    @Default
    private Map<String, JiraAggByIssueType> aggByIssueType = new HashMap<>();

    @JsonProperty("issue_types_by_release")
    @Default
    private Map<String, Map<String, Integer>> typesByRelease = new HashMap<>();

    @JsonProperty("total_vuln_count")
    private Integer vulnerabilityCount;

    @JsonProperty("duplicate_vuln_count")
    @Default
    private Map<String, Integer> duplicateVulnerabilityCount = new HashMap<>();

    //clearly not threadsafe.
    public void addReleaseCountForType(String type, String release, Integer count) {
        Map<String, Integer> typeMap = typesByRelease.getOrDefault(type, new HashMap<>());
        typeMap.put(release, count);
        typesByRelease.put(type, typeMap);
    }

    @JsonPOJOBuilder(withPrefix = "")
    static final class JiraAggDataBuilderImpl extends JiraAggDataBuilder<JiraAggData, JiraAggDataBuilderImpl> {
    }

}
