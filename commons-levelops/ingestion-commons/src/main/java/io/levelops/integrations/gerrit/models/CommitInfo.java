package io.levelops.integrations.gerrit.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * Bean describing a Group from https://gerrit-documentation.storage.googleapis.com/Documentation/3.2.2/rest-api-groups.html#group-info
 */
@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = CommitInfo.CommitInfoBuilder.class)
public class CommitInfo {

    @JsonProperty
    String commitId;

    @JsonProperty
    List<CommitInfo> parents;

    @JsonProperty
    ProjectInfo.TagInfo.GitPersonInfo author;

    @JsonProperty
    ProjectInfo.TagInfo.GitPersonInfo committer;

    @JsonProperty
    String subject;

    @JsonProperty
    String message;

    @JsonProperty("web_links")
    List<ProjectInfo.WebLinkInfo> webLinks;
}
