package io.levelops.integrations.bitbucket.models;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = BitbucketCommitDiffStat.BitbucketCommitDiffStatBuilder.class)
public class BitbucketCommitDiffStat {
    @JsonProperty("status")
    String status;
    @JsonProperty("old")
    FileRef oldFile;
    @JsonProperty("lines_removed")
    Integer linesRemoved;
    @JsonProperty("lines_added")
    Integer linesAdded;
    @JsonProperty("new")
    FileRef newFile;
    @JsonProperty("type")
    String type;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = FileRef.FileRefBuilder.class)
    public static final class FileRef{
        @JsonProperty("path")
        String path;
        @JsonProperty("type")
        String type;
    }
}
