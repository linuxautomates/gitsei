package io.levelops.integrations.circleci.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = CircleCIScmInfo.CircleCIScmInfoBuilder.class)
public class CircleCIScmInfo {

    @JsonProperty("commit_url")
    String vcsUrl;

    @JsonProperty("commit")
    String vcsRevision;

    @JsonProperty("committer_name")
    String committerName;

    @JsonProperty("committer_email")
    String committerEmail;

    @JsonProperty("subject")
    String subject;
}
