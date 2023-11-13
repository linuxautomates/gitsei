package io.levelops.integrations.github.models;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.io.Serializable;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = GithubCommitFile.GithubCommitFileBuilder.class)
public class GithubCommitFile implements Serializable {

    @JsonProperty("additions")
    private int additions;

    @JsonProperty("changes")
    private int changes;

    @JsonProperty("deletions")
    private int deletions;

    @JsonProperty("blob_url")
    private String blobUrl;

    @JsonProperty("filename")
    private String filename;

    @JsonProperty("patch")
    private String patch;

    @JsonProperty("raw_url")
    private String rawUrl;

    @JsonProperty("sha")
    private String sha;

    @JsonProperty("status")
    private String status;

}
