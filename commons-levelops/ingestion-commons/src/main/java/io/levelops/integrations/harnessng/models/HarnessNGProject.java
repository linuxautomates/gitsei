package io.levelops.integrations.harnessng.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = HarnessNGProject.HarnessNGProjectBuilder.class)
public class HarnessNGProject {

    @JsonProperty("project")
    Project project;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Project.ProjectBuilder.class)
    public static class Project {

        @JsonProperty("identifier")
        String identifier;

        @JsonProperty("name")
        String name;

        @JsonProperty("orgIdentifier")
        String orgIdentifier;

    }
}