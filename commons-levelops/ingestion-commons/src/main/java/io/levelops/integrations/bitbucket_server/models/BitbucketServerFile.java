package io.levelops.integrations.bitbucket_server.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = BitbucketServerFile.BitbucketServerFileBuilder.class)
public class BitbucketServerFile {

    @JsonProperty("fileName")
    String name;

    @JsonProperty("source")
    FileRef sourceFile;

    @JsonProperty("destination")
    FileRef destinationFile;

    @JsonProperty("linesRemoved")
    Integer linesRemoved;

    @JsonProperty("linesAdded")
    Integer linesAdded;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = BitbucketServerFile.FileRef.FileRefBuilder.class)
    public static class FileRef {

        @JsonProperty("components")
        List<String> components;

        @JsonProperty("parent")
        String parent;

        @JsonProperty("name")
        String name;

        @JsonProperty("extension")
        String fileExtension;

        @JsonProperty("toString")
        String fileFullName;
    }
}
