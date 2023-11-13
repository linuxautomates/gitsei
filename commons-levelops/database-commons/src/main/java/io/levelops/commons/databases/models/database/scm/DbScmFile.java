package io.levelops.commons.databases.models.database.scm;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.models.database.IntegrationConfig;
import io.levelops.integrations.azureDevops.models.ChangeSet;
import io.levelops.integrations.azureDevops.models.Commit;
import io.levelops.integrations.azureDevops.models.Project;
import io.levelops.integrations.bitbucket.models.BitbucketCommit;
import io.levelops.integrations.bitbucket_server.models.BitbucketServerCommit;
import io.levelops.integrations.gerrit.models.RevisionInfo;
import io.levelops.integrations.github.models.GithubCommit;
import io.levelops.integrations.github.models.GithubCommitFile;
import io.levelops.integrations.gitlab.models.GitlabCommit;
import io.levelops.integrations.helixcore.models.HelixCoreChangeList;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Log4j2
public class DbScmFile {

    public final static String NO_FILE_TYPE = "NA";

    @JsonProperty("id")
    private String id;

    @JsonProperty("integration_id")
    private String integrationId;

    @JsonProperty("repo_id")
    private String repoId; //repository id -- something like: "levelops/api-levelops"

    @JsonProperty("project")
    private String project;

    @JsonProperty("filename")
    private String filename;

    @JsonProperty("filetype")
    private String filetype;

    @JsonProperty("created_at")
    private Long createdAt;

    //THIS field is only used when inserting the file and eventually when getting the file details
    @JsonProperty("file_commits")
    private List<DbScmFileCommit> fileCommits;

    //THE fields below will be returned during aggregations and not used for insertion
    @JsonProperty("total_changes")
    private Long totalChanges;

    @JsonProperty("total_additions")
    private Long totalAdditions;

    @JsonProperty("total_deletions")
    private Long totalDeletions;

    @JsonProperty("num_commits")
    private Long numCommits;

    @JsonProperty("commit_shas")
    private List<String> commitShas;

    @JsonProperty("jira_issue_keys")
    private List<String> jiraIssueKeys;

    @JsonProperty("num_issues")
    private Long numIssues;

    @JsonProperty("num_cases")
    private Long numCases;

    @JsonProperty("salesforce_case_numbers")
    private List<String> salesforceCaseNumbers;

    @JsonProperty("zendesk_ticket_ids")
    private List<String> zendeskTicketIds;

    @JsonProperty("zendesk_integration_id")
    String zendeskIntegrationId;

    @JsonProperty("zendesk_ticket_urls")
    List<String> zendeskTicketUrls;

    @JsonProperty("num_tickets")
    private Long numTickets;

    public static List<DbScmFile> fromGithubCommit(GithubCommit source,
                                                   String repoId,
                                                   String integrationId,
                                                   Long eventTime) {
        Optional<List<GithubCommitFile>> files = source.getFiles();
        if (files.isEmpty())
            return List.of();
        return files.get()
                .stream()
                .map(file -> DbScmFile.builder()
                        .filename(file.getFilename())
                        .filetype(StringUtils.defaultIfEmpty(FilenameUtils.getExtension(file.getFilename()), NO_FILE_TYPE))
                        .repoId(repoId)
                        .project(MoreObjects.firstNonNull(repoId, ""))
                        .integrationId(integrationId)
                        .fileCommits(
                                List.of(DbScmFileCommit.fromGithubCommitFile(
                                        file, source.getSha(), eventTime)))
                        .build())
                .collect(Collectors.toList());
    }

    public static List<DbScmFile> fromAzureDevopsCommitFiles(Project project,
                                                             Commit source,
                                                             String repoId,
                                                             String integrationId) {
        if (source.getChanges() == null)
            return List.of();
        return source.getChanges()
                .stream()
                .map(file -> DbScmFile.builder()
                        .filename(file.getItem().getPath())
                        .filetype(StringUtils.defaultIfEmpty(FilenameUtils.getExtension(file.getItem().getPath()), NO_FILE_TYPE))
                        .repoId(repoId)
                        .project(project.getName())
                        .integrationId(integrationId)
                        .fileCommits(
                                List.of(DbScmFileCommit.fromAzureDevopsCommitFileInfo(file, source.getCommitId(), source.getCommitter())))
                        .build())
                .collect(Collectors.toList());
    }

    public static List<DbScmFile> fromAzureDevopsChangeSetFiles(Project project,
                                                                ChangeSet changeSet,
                                                                String integrationId) {
        if (changeSet.getChangeSetChanges() == null)
            return List.of();
        return changeSet.getChangeSetChanges()
                .stream()
                .filter(changeSetChange -> changeSetChange.getItem() != null)
                .map(file -> DbScmFile.builder()
                        .filename(file.getItem().getPath())
                        .filetype(StringUtils.defaultIfEmpty(FilenameUtils.getExtension(file.getItem().getPath()), NO_FILE_TYPE))
                        .repoId(project.getId())
                        .project(project.getName())
                        .integrationId(integrationId)
                        .fileCommits(
                                List.of(DbScmFileCommit.fromAzureDevopsChangesetsFileInfo(changeSet)))
                        .build())
                .collect(Collectors.toList());
    }

    public static List<DbScmFile> fromBitbucketCommit(BitbucketCommit source,
                                                      String repoId,
                                                      String integrationId,
                                                      Long eventTime) {
        if (source.getDiffStats() == null)
            return List.of();
        return source.getDiffStats()
                .stream()
                .map(stat -> {
                    if (stat.getOldFile() == null && stat.getNewFile() == null)
                        return null;
                    String fileName = stat.getNewFile() != null ?
                            stat.getNewFile().getPath() : stat.getOldFile().getPath();
                    return DbScmFile.builder()
                            .filename(fileName)
                            .filetype(StringUtils.defaultIfEmpty(FilenameUtils.getExtension(fileName), NO_FILE_TYPE))
                            .repoId(repoId)
                            .project(MoreObjects.firstNonNull(repoId, ""))
                            .integrationId(integrationId)
                            .totalChanges((long) (stat.getLinesAdded() + stat.getLinesRemoved()))
                            .fileCommits(
                                    List.of(DbScmFileCommit.fromBitbucketStat(
                                            stat, source.getHash(), eventTime)))
                            .build();
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public static List<DbScmFile> fromBitbucketServerCommit(BitbucketServerCommit source,
                                                            String integrationId,
                                                            Long eventTime) {
        if (source.getFiles() == null)
            return List.of();
        return source.getFiles()
                .stream()
                .map(file -> DbScmFile.builder()
                        .filename(file.getName())
                        .filetype(StringUtils.defaultIfEmpty(FilenameUtils.getExtension(file.getName()), NO_FILE_TYPE))
                        .repoId(source.getRepoName())
                        .project(MoreObjects.firstNonNull(source.getProjectName(), source.getRepoName()))
                        .integrationId(integrationId)
                        .totalChanges((long) (file.getLinesAdded() + file.getLinesRemoved()))
                        .fileCommits(
                                List.of(DbScmFileCommit.fromBitbucketServerCommitFile(
                                        file, source.getId(), eventTime)))
                        .build())
                .collect(Collectors.toList());
    }

    public static List<DbScmFile> fromHelixCoreChangeList(HelixCoreChangeList source, String integrationId,
                                                          List<IntegrationConfig.RepoConfigEntry> configEntries) {
        List<DbScmFile> dbScmFiles = new ArrayList<>();
        final RepoConfigEntryMatcher repoConfigEntryMatcher = new RepoConfigEntryMatcher(configEntries);
        if (CollectionUtils.isNotEmpty(configEntries) && source.getFiles() != null) {
            source.getFiles()
                    .stream()
                    .map(value -> {
                        String repoId = repoConfigEntryMatcher.matchPrefix(value.getDepotPathString());
                        if(StringUtils.isBlank(repoId)) {
                            return null;
                        }
                        return DbScmFile.builder()
                                .filename(value.getName())
                                .filetype(StringUtils.defaultIfEmpty(FilenameUtils.getExtension(value.getName()), NO_FILE_TYPE))
                                .repoId(repoId)
                                .project(MoreObjects.firstNonNull(repoId, ""))
                                .integrationId(integrationId)
                                .totalChanges((long) (value.getAdditions() + value.getDeletions() + value.getChanges()))
                                .fileCommits(List.of(DbScmFileCommit.fromHelixCoreCommitFile(value,
                                        String.valueOf(value.getChangeListId()),
                                        value.getCommitDate().toInstant().getEpochSecond())))
                                .build();
                    })
                    .filter(Objects::nonNull)
                    .forEach(dbScmFiles::add);
        }
        log.debug("DbScmFile:fromHelixCoreChangeList returns {} files for changelist id {}, " +
                        "integration id {} and {} config entries",
                dbScmFiles.size(), source.getId(), integrationId, (configEntries != null) ? configEntries.size() : 0);
        return dbScmFiles;
    }

    public static List<DbScmFile> fromGitlabCommit(GitlabCommit source,
                                                   String projectId,
                                                   String integrationId) {
        if (source.getChanges() == null)
            return List.of();
        return source.getChanges()
                .stream()
                .map(stat -> {
                    if (stat.getOldPath() == null && stat.getNewPath() == null)
                        return null;
                    String fileName = stat.getNewPath() != null ?
                            stat.getNewPath() : stat.getOldPath();
                    return DbScmFile.builder()
                            .filename(fileName)
                            .filetype(StringUtils.defaultIfEmpty(FilenameUtils.getExtension(fileName), NO_FILE_TYPE))
                            .repoId(projectId)
                            .project(MoreObjects.firstNonNull(projectId, ""))
                            .integrationId(integrationId)
                            .fileCommits(
                                    List.of(DbScmFileCommit.fromGitlabCommitFile(
                                            stat, source.getId(), source.getCreatedAt()
                                                    .toInstant().getEpochSecond())))
                            .build();
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
