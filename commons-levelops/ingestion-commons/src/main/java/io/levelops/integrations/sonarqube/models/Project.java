package io.levelops.integrations.sonarqube.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.ToString;
import lombok.Value;

import java.util.Date;
import java.util.List;

@Value
@Builder(toBuilder = true)
@ToString
@JsonDeserialize(builder = Project.ProjectBuilder.class)
public class Project {

    @JsonProperty("organization")
    String organization;

    @JsonProperty("key")
    String key;

    @JsonProperty("name")
    String name;

    @JsonProperty("qualifier")
    String qualifier;

    @JsonProperty("visibility")
    String visibility;

    @JsonProperty("lastAnalysisDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssZ")
    Date lastAnalysisDate;

    @JsonProperty("revision")
    String revision;

    @JsonProperty("uuid")
    String uuid;

    @JsonProperty("path")
    String path;

    @JsonProperty("longName")
    String longName;

    @JsonProperty("enabled")
    Boolean enabled;

    @JsonProperty("measures")
    List<Measure> measures; // enriched

    @JsonProperty("branches")
    List<Branch> branches;  // enriched

    @JsonProperty("pullRequests")
    List<PullRequest> pullRequests;  // enriched

    @JsonProperty("analyses")
    List<Analyse> analyses;  //enriched

    @JsonPOJOBuilder(withPrefix = "")
    public static final class ProjectBuilder {
        String organization;

        String key;

        String name;

        String qualifier;

        String visibility;

        @JsonDeserialize(using=CustomDeserializer.class)
        Date lastAnalysisDate;

        String revision;

        String uuid;

        String path;

        String longName;

        Boolean enabled;

        List<Measure> measures;


        List<Branch> branches;  // enriched


        List<PullRequest> pullRequests;  // enriched


        List<Analyse> analyses;  //enriched


        List<Issue> issues;  //enriched

    }
}
