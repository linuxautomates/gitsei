package io.levelops.integrations.azureDevops.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;
import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = Commit.CommitBuilder.class)
public class Commit {

    @JsonProperty("commitId")
    String commitId;

    @JsonProperty("author")
    GitUserDate author;

    @JsonProperty("committer")
    GitUserDate committer;

    @JsonProperty("comment")
    String comment;

    @JsonProperty("commentTruncated")
    Boolean commentTruncated;

    @JsonProperty("changeCounts")
    ChangeCounts changeCounts;

    @JsonProperty("url")
    String url;

    @JsonProperty("remoteUrl")
    String remoteUrl;

    @JsonProperty("changes")
    List<Change> changes;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = GitUserDate.GitUserDateBuilder.class)
    public static class GitUserDate {
        @JsonProperty("name")
        String name;

        @JsonProperty("email")
        String email;

        @JsonProperty("date")
        String date;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = ChangeCounts.ChangeCountsBuilder.class)
    public static class ChangeCounts {
        @JsonProperty("Add")
        int add;

        @JsonProperty("Edit")
        int edit;

        @JsonProperty("Delete")
        int delete;
    }
}
