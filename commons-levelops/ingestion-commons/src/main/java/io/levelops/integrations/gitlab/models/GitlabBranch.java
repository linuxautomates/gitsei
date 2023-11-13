package io.levelops.integrations.gitlab.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = GitlabBranch.GitlabBranchBuilder.class)
public class GitlabBranch {
    @JsonProperty("name")
    String name;
    @JsonProperty("commit")
    GitlabCommit commit;
    @JsonProperty("merged")
    boolean merged;
    @JsonProperty("protected")
    boolean protect;
    @JsonProperty("developers_can_push")
    boolean developersCanPush;
    @JsonProperty("developers_can_merge")
    boolean developersCanMerge;
    @JsonProperty("can_push")
    boolean canPush;
    @JsonProperty("default")
    boolean defaulted;
    @JsonProperty("web_url")
    String webUrl;
}
