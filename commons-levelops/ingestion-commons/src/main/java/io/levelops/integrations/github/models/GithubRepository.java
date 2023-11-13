package io.levelops.integrations.github.models;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = GithubRepository.GithubRepositoryBuilder.class)
public class GithubRepository implements Serializable {

    @JsonProperty("id")
    public String getId() {
        if (owner == null || StringUtils.isEmpty(owner.getLogin()) || StringUtils.isEmpty(name)) {
            return null;
        }
        return owner.getLogin() + "/" + name;
    }

    @JsonProperty("name")
    String name;

    @JsonProperty("full_name")
    String fullName;

    @JsonProperty("description")
    String description;

    @JsonProperty("owner")
    GithubUser owner;

    @JsonProperty("html_url")
    String htmlUrl;

    @JsonProperty("master_branch")
    String masterBranch;

    @JsonProperty("created_at")
    Date createdAt;

    @JsonProperty("pushed_at")
    Date pushedAt;

    @JsonProperty("updated_at")
    Date updatedAt;

    @JsonProperty("size")
    int size;

    @JsonProperty("is_private")
    Boolean isPrivate;

    @JsonProperty("language")
    String language;
    
    @JsonProperty("languages")
    Map<String, Long> languages; // enriched

    @JsonProperty("events")
    List<GithubEvent> events; // enriched

    @JsonProperty("pull_requests")
    List<GithubPullRequest> pullRequests; // enriched

    @JsonProperty("tags")
    List<GithubTag> tags; // enriched

    @JsonProperty("issues")
    List<GithubIssue> issues; // enriched

    @JsonProperty("issue_events")
    List<GithubIssueEvent> issueEvents; // enriched
}
