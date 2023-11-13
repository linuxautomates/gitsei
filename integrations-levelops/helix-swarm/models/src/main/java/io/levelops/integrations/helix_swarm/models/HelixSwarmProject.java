package io.levelops.integrations.helix_swarm.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = HelixSwarmProject.HelixSwarmProjectBuilder.class)
public class HelixSwarmProject {

    @JsonProperty("id")
    String id;

    @JsonProperty("deleted")
    Boolean deleted;

    @JsonProperty("description")
    String description;

    @JsonProperty("jobView")
    String jobView;

    @JsonProperty("minimumUpVotes")
    Integer minimumUpVotes;

    @JsonProperty("name")
    String name;

    @JsonProperty("private")
    Boolean isPrivate;

    @JsonProperty("retainDefaultReviewers")
    Boolean retainDefaultReviewers;

    @JsonProperty("branches")
    List<Branch> branches;

    @JsonProperty("deploy")
    Deploy deploy;

    @JsonProperty("members")
    List<String> members;

    @JsonProperty("owners")
    List<String> owners;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Branch.BranchBuilder.class)
    public static class Branch {

        @JsonProperty("id")
        String id;

        @JsonProperty("name")
        String name;

        @JsonProperty("workflow")
        String workflow;

        @JsonProperty("paths")
        List<String> paths;

        @JsonProperty("minimumUpVotes")
        Integer minimumUpVotes;

        @JsonProperty("retainDefaultReviewers")
        Boolean retainDefaultReviewers;

        @JsonProperty("moderators")
        List<String> moderators;

        @JsonProperty("moderators-groups")
        List<String> moderatorGroups;
    }


    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Deploy.DeployBuilder.class)
    public static class Deploy {

        Boolean enabled;

        String url;
    }
}
