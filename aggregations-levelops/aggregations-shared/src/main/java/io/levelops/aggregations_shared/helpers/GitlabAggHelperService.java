package io.levelops.aggregations_shared.helpers;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.levelops.commons.databases.models.database.CICDInstance;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.scm.DbScmCommit;
import io.levelops.commons.databases.models.database.scm.DbScmFile;
import io.levelops.commons.databases.models.database.scm.DbScmIssue;
import io.levelops.commons.databases.models.database.scm.DbScmPullRequest;
import io.levelops.commons.databases.models.database.scm.DbScmTag;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.databases.models.filters.CICDInstanceFilter;
import io.levelops.commons.databases.models.filters.CICD_TYPE;
import io.levelops.commons.databases.services.CiCdInstancesDatabaseService;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.ScmAggService;
import io.levelops.commons.databases.services.UserIdentityService;
import io.levelops.commons.models.DbListResponse;
import io.levelops.integrations.gitlab.models.GitlabCommit;
import io.levelops.integrations.gitlab.models.GitlabIssue;
import io.levelops.integrations.gitlab.models.GitlabMergeRequest;
import io.levelops.integrations.gitlab.models.GitlabPipeline;
import io.levelops.integrations.gitlab.models.GitlabProject;
import io.levelops.integrations.gitlab.models.GitlabTag;
import io.levelops.integrations.gitlab.models.GitlabUser;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Log4j2
@Service
public class GitlabAggHelperService {
    private final ScmAggService scmAggService;
    private final GitlabPipelineHelperService gitlabPipelineService;
    private final UserIdentityService userIdentityService;
    private final CiCdInstancesDatabaseService ciCdInstancesDatabaseService;
    private final IntegrationService integrationService;
    private static final Set<String> RELEVANT_TENANT_IDS = Set.of("broadcom");
    public static final int TOTAL_LINES_OF_CHANGE = 5000;


    @Autowired
    public GitlabAggHelperService(
            ScmAggService scmAggService,
            GitlabPipelineHelperService gitlabPipelineService,
            UserIdentityService userIdentityService,
            CiCdInstancesDatabaseService ciCdInstancesDatabaseService,
            IntegrationService integrationService) {
        this.scmAggService = scmAggService;
        this.gitlabPipelineService = gitlabPipelineService;
        this.userIdentityService = userIdentityService;
        this.ciCdInstancesDatabaseService = ciCdInstancesDatabaseService;
        this.integrationService = integrationService;
    }

    // In case the gitlab author or commiter info is not present, we try to create a new integration user with the name and email
    private Optional<DbScmUser> buildOverrideCommitOrAuthorUser(
            LoadingCache<String, Optional<DbScmUser>> integrationUserByCloudIdLoadingCache,
            LoadingCache<String, Optional<DbScmUser>> integrationUserByEmailLoadingCache,
            String customer,
            String integrationId,
            GitlabUser gitlabUser,
            String name,
            String email) {
        //If commit->committer_details/author_details is present, DbScmCommit.fromGitlabCommit has used it
        if (gitlabUser != null) {
            return Optional.empty();
        }
        //If commit->committer_details/author_details is NOT present
        //If commit->committer_name/author_name is empty, we cannot override
        if (StringUtils.isBlank(name)) {
            return Optional.empty();
        }

        // Try to look up existing integration users by the email and name. If none are found create a new integration user.
        Optional<DbScmUser> optional = Optional.empty();
        try {
            optional = integrationUserByEmailLoadingCache.get(email);
        } catch (ExecutionException e) {
            log.warn("Failed to load integrationUser for customer={}, integrationId={}, cloudId={}", customer, integrationId, name, e);
        }

        if (optional.isPresent()) {
            //If we find Integration user where cloud_id ILIKE commit->committer_name/author_name use it
            return optional;
        } else {
            try {
                optional = integrationUserByCloudIdLoadingCache.get(name);
            } catch (ExecutionException e) {
                log.warn("Failed to load integrationUser for customer={}, integrationId={}, email={}", customer, integrationId, email, e);
            }
            if (optional.isPresent()) {
                return optional;
            }
        }
        //Else create new Integration user where cloud_id = commit->committer_name/author_name & display_name/original = commit->committer_email/author_email (email helps identify users for name clash)
        return Optional.ofNullable(DbScmUser.builder()
                .integrationId(integrationId)
                .cloudId(name)
                .displayName(email)
                .originalDisplayName(email)
                .build());
    }

    private DbScmCommit buildDbScmCommitFromGitlabCommit(
            LoadingCache<String, Optional<DbScmUser>> integrationUserByCloudIdLoadingCache,
            LoadingCache<String, Optional<DbScmUser>> integrationUserByEmailLoadingCache,
            String customer,
            String integrationId,
            GitlabProject project,
            GitlabCommit commit
    ) {
        DbScmCommit dbScmCommit = DbScmCommit.fromGitlabCommit(commit, project.getPathWithNamespace(), integrationId);
        log.info("gitlab dbScmCommit before, customer = {}, integrationId = {}, commitSha = {}, committer = {}, author = {}", customer, integrationId, dbScmCommit.getCommitSha(), dbScmCommit.getCommitterInfo(), dbScmCommit.getAuthorInfo());

        DbScmCommit.DbScmCommitBuilder bldr = dbScmCommit.toBuilder();
        Optional<DbScmUser> overrideCommitter = buildOverrideCommitOrAuthorUser(
                integrationUserByCloudIdLoadingCache,
                integrationUserByEmailLoadingCache,
                customer,
                integrationId,
                commit.getCommitterDetails(),
                commit.getCommitterName(),
                commit.getCommitterEmail());
        if (overrideCommitter.isPresent()) {
            log.info("Github committer info missing. Overriding with username/email for customer={}, integrationId={}, commit={}", customer, integrationId, dbScmCommit);
            bldr.committerInfo(overrideCommitter.get());
        }
        Optional<DbScmUser> overrideAuthor = buildOverrideCommitOrAuthorUser(
                integrationUserByCloudIdLoadingCache,
                integrationUserByEmailLoadingCache,
                customer,
                integrationId,
                commit.getAuthorDetails(),
                commit.getAuthorName(),
                commit.getAuthorEmail());
        if (overrideAuthor.isPresent()) {
            log.info("Github author info missing. Overriding with username/email for customer={}, integrationId={}, commit={}", customer, integrationId, dbScmCommit);
            bldr.authorInfo(overrideAuthor.get());
        }
        dbScmCommit = bldr.build();

        log.info("gitlab dbScmCommit after, customer = {}, integrationId = {}, commitSha = {}, committer = {}, author = {}", customer, integrationId, dbScmCommit.getCommitSha(), dbScmCommit.getCommitterInfo(), dbScmCommit.getAuthorInfo());
        return dbScmCommit;
    }

    private Optional<DbScmUser> findIntegrationUserByCloudId(String customer, String integrationId, String cloudId) {
        if (StringUtils.isBlank(cloudId)) {
            return Optional.empty();
        }
        try {
            return userIdentityService.getUserByCloudId(customer, integrationId, cloudId);
        } catch (Exception e) {
            log.warn("Failed to lookup Integration User by cloudId", e);
            return Optional.empty();
        }
    }

    private Optional<DbScmUser> findIntegrationUserByEmail(String customer, String integrationId, String email) {
        if (StringUtils.isBlank(email)) {
            return Optional.empty();
        }
        try {
            return userIdentityService.getUserByEmail(customer, integrationId, email);
        } catch (Exception e) {
            log.warn("Failed to lookup Integration User by cloudId", e);
            return Optional.empty();
        }
    }

    public void processCommit(
            String customer,
            String integrationId,
            GitlabProject project,
            GitlabCommit commit,
            LoadingCache<String, Optional<DbScmUser>> integrationUserByCloudIdLoadingCache,
            LoadingCache<String, Optional<DbScmUser>> integrationUserByEmailLoadingCache) {
        if (project.getPathWithNamespace() == null) {
            log.warn("Could not insert gitlab commit without projectId: commitId={}", commit.getId());
            return;
        }
        DbScmCommit dbScmCommit = buildDbScmCommitFromGitlabCommit(
                integrationUserByCloudIdLoadingCache, integrationUserByEmailLoadingCache, customer, integrationId, project, commit);
        Optional<DbScmCommit> existingDbGitCommit = scmAggService.getCommit(customer, dbScmCommit.getId(), project.getPathWithNamespace(), integrationId);
        if (existingDbGitCommit.isEmpty()) {
            List<DbScmFile> dbScmFiles = DbScmFile.fromGitlabCommit(
                    commit, project.getPathWithNamespace(), integrationId);
            try {
                if (ScmAggUtils.isRelevant(customer, RELEVANT_TENANT_IDS)) {
                    if (ScmAggUtils.isChangeVolumeLessThanXLines(dbScmCommit, TOTAL_LINES_OF_CHANGE)) {
                        scmAggService.insert(customer, dbScmCommit, dbScmFiles);
                    } else {
                        log.info("Commit not inserted as lines of change greater than {} for Customer {}, integrationId {}," +
                                " commitSha {} commit ", TOTAL_LINES_OF_CHANGE, customer, integrationId, dbScmCommit.getCommitSha());
                    }
                } else {
                    scmAggService.insert(customer, dbScmCommit, dbScmFiles);
                }
            } catch (SQLException e) {
                log.error("Failed to insert SCM commits and files for customer={}, integrationId={}", customer, integrationId, e);
            }
        }
    }

    public void processCommits(
            String customer,
            String integrationId,
            GitlabProject project
    ) {
        LoadingCache<String, Optional<DbScmUser>> integrationUserByCloudIdLoadingCache = CacheBuilder.from("maximumSize=1000")
                .build(CacheLoader.from(cloudId -> findIntegrationUserByCloudId(customer, integrationId, cloudId)));

        LoadingCache<String, Optional<DbScmUser>> integrationUserByEmailLoadingCache = CacheBuilder.from("maximumSize=1000")
                .build(CacheLoader.from(email -> findIntegrationUserByEmail(customer, integrationId, email)));

        project.getCommits()
                .stream()
                .filter(Objects::nonNull)
                .forEach(commit -> processCommit(customer, integrationId, project, commit, integrationUserByCloudIdLoadingCache, integrationUserByEmailLoadingCache));
    }

    public void processMergeRequest(
            String customer,
            String integrationId,
            GitlabProject project,
            GitlabMergeRequest mergeRequest,
            Integration integration
    ) {
        if (project.getPathWithNamespace() == null) {
            log.warn("Could not insert gitlab merge request without projectId: mrId={}", mergeRequest.getId());
            return;
        }
        DbScmPullRequest gitlabMergeRequest = DbScmPullRequest.fromGitlabMergeRequest(
                mergeRequest, project.getPathWithNamespace(), integrationId, integration);
        try {
            Optional<DbScmPullRequest> existingPr = scmAggService.getPr(customer, gitlabMergeRequest.getId(),
                    project.getPathWithNamespace(), integrationId);
            if (existingPr.isEmpty() || existingPr.get().getPrUpdatedAt() < gitlabMergeRequest.getPrUpdatedAt()) {
                scmAggService.insert(customer, gitlabMergeRequest);
            }
        } catch (SQLException e) {
            log.error("Failed to insert SCM pr for customer={}, integrationId={}", customer, integrationId, e);
        }
    }

    public void processMergeRequests(
            String customer,
            String integrationId,
            GitlabProject project
    ) {
        Integration integration = integrationService.get(customer, integrationId).orElseThrow();
        project.getMergeRequests()
                .stream()
                .filter(Objects::nonNull)
                .forEach(mergeRequest -> processMergeRequest(customer, integrationId, project, mergeRequest, integration));
    }

    private void processCommitForDirectMerge(
            String customer,
            String integrationId,
            GitlabProject project,
            GitlabCommit commit
    ) {
        if (project.getPathWithNamespace() == null) {
            log.warn("Could not update gitlab commit for direct merges without projectId: commitId={}", commit.getId());
            return;
        }
        Optional<DbScmPullRequest> optionalDbScmPullRequest = scmAggService.checkCommitForDirectMerge(customer, commit.getId(), project.getPathWithNamespace(), integrationId);
        boolean directMerge = optionalDbScmPullRequest.isEmpty();
        Optional<DbScmCommit> opt = scmAggService.getCommit(customer, commit.getId(), project.getPathWithNamespace(), integrationId);
        if (opt.isPresent()) {
            try {
                scmAggService.updateDirectMergeForCommit(customer, UUID.fromString(opt.get().getId()), directMerge);
            } catch (SQLException e) {
                log.error("Failed to update direct_merge of SCM commits for customer={}, integrationId={}", customer, integrationId, e);
            }
        }
    }

    public void processCommitsForDirectMerge(
            String customer,
            String integrationId,
            GitlabProject project
    ) {
        project.getCommits()
                .stream()
                .filter(Objects::nonNull)
                .forEach(commit -> {
                    processCommitForDirectMerge(customer, integrationId, project, commit);
                });
    }

    public void processIssue(
            String customer,
            String integrationId,
            GitlabIssue gitlabIssue
    ) {
        DbScmIssue gitIssue = DbScmIssue.fromGitlabIssue(gitlabIssue, gitlabIssue.getProjectId(), integrationId);
        DbScmIssue oldIssue = scmAggService.getIssue(customer, gitIssue.getIssueId(),
                String.valueOf(gitlabIssue.getProjectId()), integrationId).orElse(null);
        if (oldIssue == null || oldIssue.getIssueUpdatedAt() < gitIssue.getIssueUpdatedAt()) {
            scmAggService.insertIssue(customer, gitIssue);
        }
    }

    private UUID getCiCdInstanceId(String company, String integrationId) throws SQLException {
        DbListResponse<CICDInstance> dbListResponse = ciCdInstancesDatabaseService
                .list(company,
                        CICDInstanceFilter.builder()
                                .integrationIds(List.of(integrationId))
                                .types(List.of(CICD_TYPE.gitlab))
                                .build(), null, null, null);
        if (CollectionUtils.isNotEmpty(dbListResponse.getRecords())) {
            return dbListResponse.getRecords().get(0).getId();
        } else {
            log.warn("CiCd instance response is empty for company " + company + "and integration id" + integrationId);
            throw new RuntimeException("Error listing the cicd instances for integration id " + integrationId + " type "
                    + CICD_TYPE.gitlab);
        }
    }

    public void processPipeline(
            String customer,
            String integrationId,
            GitlabProject project,
            GitlabPipeline pipeline,
            UUID instanceId
    ) {
        gitlabPipelineService.insert(customer, integrationId, instanceId, pipeline.toBuilder()
                .projectName(project.getName())
                .projectId(project.getId())
                .pathWithNamespace(project.getPathWithNamespace())
                .httpUrlToRepo(project.getHttpUrlToRepo()).build());
    }

    public void processPipelines(
            String customer,
            String integrationId,
            GitlabProject project
    ) throws SQLException {
        UUID instanceId = getCiCdInstanceId(customer, integrationId);
        project.getPipelines()
                .stream()
                .filter(Objects::nonNull)
                .forEach(pipeline -> {
                    processPipeline(customer, integrationId, project, pipeline, instanceId);
                });
    }

    public void processTag(
            String customer,
            String integrationId,
            GitlabProject project,
            GitlabTag tag) {
        if (project.getId() == null) {
            log.warn("Could not insert the tag for customer:{}, integrationId:{}, tag:{} for null repo id", customer, integrationId, tag.getName());
            return;
        }
        DbScmTag dbScmTag = DbScmTag.fromGitLabTag(tag, project.getId(), integrationId);
        try {
            scmAggService.insertTag(customer, dbScmTag);
        } catch (SQLException e) {
            log.error("Failed to insert the tag for customer:{}, integrationId:{}, repo:{}, tag:{}", customer, integrationId, project.getId(), tag.getName());
        }
    }

    public void processTags(
            String customer,
            String integrationId,
            GitlabProject project
    ) {
        project.getTags()
                .stream()
                .filter(Objects::nonNull)
                .forEach(tag -> {
                    processTag(customer, integrationId, project, tag);
                });
    }

    public Long getOldestJobRunStartTime(String company, String integrationId) {
        return gitlabPipelineService.getOldestJobRunStartTime(company, integrationId);
    }
}
