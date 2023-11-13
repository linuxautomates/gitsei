package io.levelops.commons.databases.models.database.scm;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Log4j2
public class DbScmFileCommitDetails {

    @JsonProperty("id")
    private String id;

    @JsonProperty("file_id")
    private String fileId;

    @JsonProperty("integration_id")
    private String integrationId;

    @JsonProperty("commit_sha")
    private String commitSha;

    @JsonProperty("project")
    private String project;

    @JsonProperty("repo")
    private String repo;

    @JsonProperty("file_name")
    private String fileName;

    @JsonProperty("file_type")
    private String fileType;

    @JsonProperty("change")
    private Integer change;

    @JsonProperty("addition")
    private Integer addition;

    @JsonProperty("deletion")
    private Integer deletion;

    @JsonProperty("total_change")
    private Integer totalChange;

    @JsonProperty("committed_at")
    private Long committedAt;

    @JsonProperty("previous_committed_at")
    private Long previousCommittedAt;

    @JsonProperty("file_created_at")
    private Long fileCreatedAt;

    @JsonProperty("createdAt")
    private Long createdAt;
}
