package io.levelops.commons.databases.services;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.scm.DbScmCommit;
import io.levelops.commons.databases.models.database.scm.DbScmFile;
import io.levelops.commons.databases.models.database.scm.DbScmPullRequest;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.databases.models.filters.ScmPrFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.organization.TeamMembersDatabaseService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.gitlab.models.GitlabProject;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.assertj.core.api.Assertions;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.services.ScmCommitUtils.arrayUniq;

@SuppressWarnings("unused")
public class ScmAggService3Test {
    private static final String company = "test";
    private static final ObjectMapper m = DefaultObjectMapper.get();

    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;
    private static ScmAggService scmAggService;
    private static JiraIssueService jiraIssueService;
    private static ZendeskTicketService zendeskTicketService;
    private static String gitlabIntegrationId;
    private static GitRepositoryService repositoryService;
    private static UserIdentityService userIdentityService;
    private static TeamMembersDatabaseService teamMembersDatabaseService;
    final DbScmUser testScmUser = DbScmUser.builder()
            .integrationId(gitlabIntegrationId)
            .cloudId("viraj-levelops")
            .displayName("viraj-levelops")
            .originalDisplayName("viraj-levelops")
            .build();

    @BeforeClass
    public static void setup() throws Exception {
        if (dataSource != null)
            return;

        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        IntegrationService integrationService = new IntegrationService(dataSource);
        userIdentityService = new UserIdentityService(dataSource);
        scmAggService = new ScmAggService(dataSource, userIdentityService);
        WorkItemFieldsMetaService workItemFieldsMetaService = new WorkItemFieldsMetaService(dataSource);
        repositoryService = new GitRepositoryService(dataSource);
        teamMembersDatabaseService = new TeamMembersDatabaseService(dataSource, DefaultObjectMapper.get());
        new DatabaseSchemaService(dataSource).ensureSchemaExistence(company);
        integrationService.ensureTableExistence(company);
        gitlabIntegrationId = integrationService.insert(company, Integration.builder()
                .application("gitlab")
                .name("gitlac test")
                .status("enabled")
                .build());
        userIdentityService.ensureTableExistence(company);
        scmAggService.ensureTableExistence(company);
        repositoryService.ensureTableExistence(company);
        teamMembersDatabaseService.ensureTableExistence(company);

        String input = ResourceUtils.getResourceAsString("json/databases/gitlab_prs_2021.json");
        PaginatedResponse<GitlabProject> gitlabPRs = m.readValue(input,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, GitlabProject.class));
        gitlabPRs.getResponse().getRecords().forEach(project -> project.getMergeRequests()
                .forEach(mergeRequest -> {
                    DbScmPullRequest gitlabMergeRequest = DbScmPullRequest.fromGitlabMergeRequest(
                            mergeRequest, project.getPathWithNamespace(), gitlabIntegrationId,null);
                    try {
                        if (scmAggService.getPr(company, gitlabMergeRequest.getId(),
                                project.getId(), gitlabIntegrationId)
                                .isEmpty()) {
                            scmAggService.insert(company, gitlabMergeRequest);
                        }
                    } catch (SQLException throwables) {
                        throwables.printStackTrace();
                    }
                }));

        input = ResourceUtils.getResourceAsString("json/databases/gitlab_commits.json");
        PaginatedResponse<GitlabProject> response = m.readValue(input,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, GitlabProject.class));
        response.getResponse().getRecords().forEach(project -> project.getCommits()
                .forEach(commit -> {
                    DbScmCommit gitlabCommit = DbScmCommit.fromGitlabCommit(
                            commit, project.getPathWithNamespace(), gitlabIntegrationId);
                    if (scmAggService.getCommit(
                            company, gitlabCommit.getId(), project.getPathWithNamespace(), gitlabIntegrationId)
                            .isEmpty()) {
                        try {
                            scmAggService.insertCommit(company, gitlabCommit);
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                        DbScmFile.fromGitlabCommit(
                                commit, project.getPathWithNamespace(), gitlabIntegrationId)
                                .forEach(scmFile -> scmAggService.insertFile(company, scmFile));
                    }
                }));
        dataSource.getConnection().prepareStatement(arrayUniq)
                .execute();
    }

    @Test
    public void testCodeChangesGroupBy() throws SQLException {
        DbListResponse<DbAggregationResult> resultDbListResponse = scmAggService.groupByAndCalculatePrs(
                company, ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.code_change)
                        .codeChangeUnit("lines")
                        .codeChangeSizeConfig(Map.of())
                        .commentDensitySizeConfig(Map.of())
                        .sort(Map.of())
                        .build(), null);
        DefaultObjectMapper.prettyPrint(resultDbListResponse);
        Assertions.assertThat(resultDbListResponse.getRecords()).isNotEmpty();
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(1);
        Assertions.assertThat(resultDbListResponse.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("small");

        resultDbListResponse = scmAggService.groupByAndCalculatePrs(
                company, ScmPrFilter.builder()
                        .assignees(List.of(userIdentityService.getUser(company, gitlabIntegrationId, "ctlo2020")))
                        .across(ScmPrFilter.DISTINCT.code_change)
                        .codeChangeUnit("lines")
                        .codeChangeSizeConfig(Map.of())
                        .commentDensitySizeConfig(Map.of())
                        .sort(Map.of())
                        .build(), null);
        DefaultObjectMapper.prettyPrint(resultDbListResponse);
        Assertions.assertThat(resultDbListResponse.getRecords()).isNotEmpty();
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(1);
        Assertions.assertThat(resultDbListResponse.getRecords().stream().map(DbAggregationResult::getLinesAddedCount).collect(Collectors.toList()))
                .containsExactlyInAnyOrder(891L);

        resultDbListResponse = scmAggService.groupByAndCalculatePrs(
                company, ScmPrFilter.builder()
                        .assignees(List.of("ctlo 2021"))
                        .across(ScmPrFilter.DISTINCT.code_change)
                        .codeChangeUnit("lines")
                        .codeChangeSizeConfig(Map.of())
                        .commentDensitySizeConfig(Map.of())
                        .sort(Map.of())
                        .build(), null);
        Assertions.assertThat(resultDbListResponse.getRecords()).isEmpty();

        resultDbListResponse = scmAggService.groupByAndCalculatePrs(
                company, ScmPrFilter.builder()
                        .assignees(List.of(userIdentityService.getUser(company, gitlabIntegrationId, "ctlo2020")))
                        .sourceBranches(List.of("trends-test"))
                        .across(ScmPrFilter.DISTINCT.code_change)
                        .codeChangeUnit("lines")
                        .codeChangeSizeConfig(Map.of())
                        .commentDensitySizeConfig(Map.of())
                        .sort(Map.of())
                        .build(), null);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(1);
        Assertions.assertThat(resultDbListResponse.getRecords().stream().map(DbAggregationResult::getLinesAddedCount).collect(Collectors.toList()))
                .containsExactlyInAnyOrder(35L);

        resultDbListResponse = scmAggService.groupByAndCalculatePrs(
                company, ScmPrFilter.builder()
                        .excludeSourceBranches(List.of("trends-test"))
                        .across(ScmPrFilter.DISTINCT.code_change)
                        .codeChangeUnit("lines")
                        .codeChangeSizeConfig(Map.of())
                        .commentDensitySizeConfig(Map.of())
                        .sort(Map.of())
                        .build(), null);
        DefaultObjectMapper.prettyPrint(resultDbListResponse);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(1);
        Assertions.assertThat(resultDbListResponse.getRecords().stream().map(DbAggregationResult::getLinesAddedCount).collect(Collectors.toList()))
                .containsExactlyInAnyOrder(856L);
    }

    @Test
    public void testCommentDensityGroupBy() throws SQLException {
        DbListResponse<DbAggregationResult> resultDbListResponse = scmAggService.groupByAndCalculatePrs(
                company, ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.approval_status)
                        .codeChangeUnit("lines")
                        .codeChangeSizeConfig(Map.of())
                        .commentDensitySizeConfig(Map.of())
                        .sort(Map.of())
                        .build(), null);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(3);
        Assertions.assertThat(resultDbListResponse.getRecords().stream().map(DbAggregationResult::getCount).collect(Collectors.toList()))
                .containsExactlyInAnyOrder(2L, 1L, 1L);

        resultDbListResponse = scmAggService.groupByAndCalculatePrs(
                company, ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.approval_status)
                        .codeChangeUnit("lines")
                        .codeChangeSizeConfig(Map.of())
                        .commentDensitySizeConfig(Map.of())
                        .excludeSourceBranches(List.of("trends-test"))
                        .sort(Map.of())
                        .build(), null);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(2);
        Assertions.assertThat(resultDbListResponse.getRecords().stream().map(DbAggregationResult::getCount).collect(Collectors.toList()))
                .containsExactlyInAnyOrder(2L, 1L);

        resultDbListResponse = scmAggService.groupByAndCalculatePrs(
                company, ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.approval_status)
                        .codeChangeUnit("lines")
                        .codeChangeSizeConfig(Map.of())
                        .commentDensitySizeConfig(Map.of())
                        .excludeSourceBranches(List.of("trends-test"))
                        .sort(Map.of())
                        .build(), null);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(2);
        Assertions.assertThat(resultDbListResponse.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("pending review", "rejected");
    }

    @Test
    public void testApproversAcrossGroupBy() throws SQLException {
        String user = userIdentityService.getUser(company, gitlabIntegrationId, "ctlo2020");
        DbListResponse<DbAggregationResult> resultDbListResponse = scmAggService.groupByAndCalculatePrs(
                company, ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.approver)
                        .codeChangeUnit("lines")
                        .codeChangeSizeConfig(Map.of())
                        .commentDensitySizeConfig(Map.of())
                        .sort(Map.of())
                        .build(), null);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(2);
        resultDbListResponse = scmAggService.groupByAndCalculatePrs(
                company, ScmPrFilter.builder()
                        .reviewers(List.of(user))
                        .codeChangeUnit("lines")
                        .codeChangeSizeConfig(Map.of())
                        .commentDensitySizeConfig(Map.of())
                        .across(ScmPrFilter.DISTINCT.reviewer)
                        .sort(Map.of())
                        .build(), null);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(3);
        resultDbListResponse = scmAggService.groupByAndCalculatePrs(
                company, ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.reviewer)
                        .codeChangeUnit("lines")
                        .codeChangeSizeConfig(Map.of())
                        .commentDensitySizeConfig(Map.of())
                        .sort(Map.of())
                        .build(), null);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(3);
        resultDbListResponse = scmAggService.groupByAndCalculatePrs(
                company, ScmPrFilter.builder()
                        .approvers(List.of(user))
                        .across(ScmPrFilter.DISTINCT.approver)
                        .codeChangeUnit("lines")
                        .codeChangeSizeConfig(Map.of())
                        .commentDensitySizeConfig(Map.of())
                        .sort(Map.of())
                        .build(), null);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(1);
        Assertions.assertThat(resultDbListResponse.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("ctlo2020");

        resultDbListResponse = scmAggService.groupByAndCalculatePrs(
                company, ScmPrFilter.builder()
                        .approvers(List.of(user))
                        .codeChangeUnit("lines")
                        .codeChangeSizeConfig(Map.of())
                        .commentDensitySizeConfig(Map.of())
                        .across(ScmPrFilter.DISTINCT.state)
                        .sort(Map.of())
                        .build(), null);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(1);
        Assertions.assertThat(resultDbListResponse.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("merged");

        resultDbListResponse = scmAggService.groupByAndCalculatePrs(
                company, ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.state)
                        .codeChangeUnit("lines")
                        .codeChangeSizeConfig(Map.of())
                        .commentDensitySizeConfig(Map.of())
                        .sort(Map.of())
                        .build(), null);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(3);
        Assertions.assertThat(resultDbListResponse.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("opened", "merged", "closed");

        resultDbListResponse = scmAggService.groupByAndCalculatePrs(
                company, ScmPrFilter.builder()
                        .commentDensities(List.of("good"))
                        .commentDensitySizeConfig(Map.of("good", "4"))
                        .across(ScmPrFilter.DISTINCT.comment_density)
                        .codeChangeUnit("lines")
                        .codeChangeSizeConfig(Map.of())
                        .commentDensitySizeConfig(Map.of())
                        .sort(Map.of())
                        .build(), null);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(0);

        resultDbListResponse = scmAggService.groupByAndCalculatePrs(
                company, ScmPrFilter.builder()
                        .commentDensities(List.of("shallow"))
                        .commentDensitySizeConfig(Map.of("shallow", "4"))
                        .across(ScmPrFilter.DISTINCT.comment_density)
                        .codeChangeUnit("lines")
                        .codeChangeSizeConfig(Map.of())
                        .commentDensitySizeConfig(Map.of())
                        .sort(Map.of())
                        .build(), null);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(1);

        resultDbListResponse = scmAggService.groupByAndCalculatePrs(
                company, ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.approver)
                        .codeChangeUnit("lines")
                        .codeChangeSizeConfig(Map.of())
                        .commentDensitySizeConfig(Map.of())
                        .reviewerCount(ImmutablePair.of(0L, 5L))
                        .sort(Map.of())
                        .build(), null);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(2);
        Assertions.assertThat(resultDbListResponse.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("ctlo2020", "NONE");

        resultDbListResponse = scmAggService.groupByAndCalculatePrs(
                company, ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.creator)
                        .states(List.of("merged"))
                        .codeChangeUnit("lines")
                        .codeChangeSizeConfig(Map.of())
                        .commentDensitySizeConfig(Map.of())
                        .sort(Map.of())
                        .build(), null);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(1);
        Assertions.assertThat(resultDbListResponse.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("ctlo 2020");

        resultDbListResponse = scmAggService.groupByAndCalculatePrs(
                company, ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.approval_status)
                        .sort(Map.of())
                        .build(), null);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(3);
        Assertions.assertThat(resultDbListResponse.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("pending review", "rejected", "self approved");

        resultDbListResponse = scmAggService.groupByAndCalculatePrs(
                company, ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.approval_status)
                        .approvalStatuses(List.of("rejected"))
                        .sort(Map.of())
                        .build(), null);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(1);
        Assertions.assertThat(resultDbListResponse.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("rejected");

        resultDbListResponse = scmAggService.groupByAndCalculatePrs(
                company, ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.reviewer)
                        .sort(Map.of())
                        .build(), null);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(3);
        Assertions.assertThat(resultDbListResponse.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("ctlo 2020", "ctlo 2020", "ctlo2020");

        resultDbListResponse = scmAggService.groupByAndCalculatePrs(
                company, ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.reviewer)
                        .reviewers(List.of(user))
                        .sort(Map.of())
                        .build(), null);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(3);
        Assertions.assertThat(resultDbListResponse.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("ctlo 2020", "ctlo 2020", "ctlo2020");

        resultDbListResponse = scmAggService.groupByAndCalculatePrs(
                company, ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.reviewer)
                        .reviewers(List.of("NONE-FALSE"))
                        .sort(Map.of())
                        .build(), null);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(0);
        resultDbListResponse = scmAggService.groupByAndCalculatePrs(
                company, ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.repo_id)
                        .reviewers(List.of(user))
                        .sort(Map.of())
                        .build(), null);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(2);
        Assertions.assertThat(resultDbListResponse.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("ctlo2020/test-velocity-cog", "ctlo2020/test-velocity-cog-2");

    }

    @Test
    public void testCountAcrossGroupBy() throws SQLException, JsonProcessingException {
        String user = userIdentityService.getUser(company, gitlabIntegrationId, "ctlo2020");
        DbListResponse<DbAggregationResult> resultDbListResponse = scmAggService.groupByAndCalculatePrs(
                company, ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.approver_count)
                        .codeChangeUnit("lines")
                        .codeChangeSizeConfig(Map.of())
                        .commentDensitySizeConfig(Map.of())
                        .sort(Map.of())
                        .build(), null);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(2);
        Assertions.assertThat(resultDbListResponse.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("1", "0");
        Assertions.assertThat(resultDbListResponse.getRecords().stream().map(DbAggregationResult::getCount).collect(Collectors.toList()))
                .containsExactlyInAnyOrder(3L, 1L);

        resultDbListResponse = scmAggService.groupByAndCalculatePrs(
                company, ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.approver_count)
                        .approvers(List.of(user))
                        .codeChangeUnit("lines")
                        .codeChangeSizeConfig(Map.of())
                        .commentDensitySizeConfig(Map.of())
                        .sort(Map.of())
                        .build(), null);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(1);
        Assertions.assertThat(resultDbListResponse.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("1");
        Assertions.assertThat(resultDbListResponse.getRecords().stream().map(DbAggregationResult::getCount).collect(Collectors.toList()))
                .containsExactlyInAnyOrder(1L);

        resultDbListResponse = scmAggService.groupByAndCalculatePrs(
                company, ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.reviewer_count)
                        .codeChangeUnit("lines")
                        .codeChangeSizeConfig(Map.of())
                        .commentDensitySizeConfig(Map.of())
                        .sort(Map.of())
                        .build(), null);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(2);
        Assertions.assertThat(resultDbListResponse.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("1", "2");
        Assertions.assertThat(resultDbListResponse.getRecords().stream().map(DbAggregationResult::getCount).collect(Collectors.toList()))
                .containsExactlyInAnyOrder(2L, 2L);

        resultDbListResponse = scmAggService.groupByAndCalculatePrs(
                company, ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.reviewer_count)
                        .codeChangeUnit("lines")
                        .codeChangeSizeConfig(Map.of())
                        .commentDensitySizeConfig(Map.of())
                        .sort(Map.of())
                        .build(), null);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(2);
        Assertions.assertThat(resultDbListResponse.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("2", "1");
        Assertions.assertThat(resultDbListResponse.getRecords().stream().map(DbAggregationResult::getCount).collect(Collectors.toList()))
                .containsExactlyInAnyOrder(2L, 2L);

        resultDbListResponse = scmAggService.groupByAndCalculatePrs(
                company, ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.reviewer_count)
                        .codeChangeUnit("lines")
                        .hasIssueKeys("true")
                        .codeChangeSizeConfig(Map.of())
                        .commentDensitySizeConfig(Map.of())
                        .sort(Map.of())
                        .build(), null);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(2);
        resultDbListResponse = scmAggService.groupByAndCalculatePrs(
                company, ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.reviewer_count)
                        .codeChangeUnit("lines")
                        .hasIssueKeys("false")
                        .codeChangeSizeConfig(Map.of())
                        .commentDensitySizeConfig(Map.of())
                        .sort(Map.of())
                        .build(), null);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(1);
        Assertions.assertThat(resultDbListResponse.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("2");
        Assertions.assertThat(resultDbListResponse.getRecords().stream().map(DbAggregationResult::getCount).collect(Collectors.toList()))
                .containsExactlyInAnyOrder(1L);

        resultDbListResponse = scmAggService.groupByAndCalculatePrs(
                company, ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.reviewer_count)
                        .codeChangeUnit("files")
                        .hasIssueKeys("true")
                        .codeChangeSizeConfig(Map.of())
                        .commentDensitySizeConfig(Map.of())
                        .sort(Map.of())
                        .build(), null);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(2);

        resultDbListResponse = scmAggService.groupByAndCalculatePrs(
                company, ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.reviewer_count)
                        .codeChangeUnit("files")
                        .hasIssueKeys("false")
                        .codeChangeSizeConfig(Map.of())
                        .commentDensitySizeConfig(Map.of())
                        .sort(Map.of())
                        .build(), null);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(1);
        Assertions.assertThat(resultDbListResponse.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("2");
        Assertions.assertThat(resultDbListResponse.getRecords().stream().map(DbAggregationResult::getCount).collect(Collectors.toList()))
                .containsExactlyInAnyOrder(1L);

        for (ScmPrFilter.DISTINCT a : List.of(ScmPrFilter.DISTINCT.values())) {
            System.out.println("a: " + a + " data: " +
                    m.writeValueAsString(
                            scmAggService.groupByAndCalculatePrs(company, ScmPrFilter.builder().across(a).build(), null)
                ));
        }
        resultDbListResponse = scmAggService.groupByAndCalculatePrs(
                company, ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.branch)
                        .codeChangeSizeConfig(Map.of())
                        .commentDensitySizeConfig(Map.of())
                        .sort(Map.of())
                        .build(), null);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(3);
        Assertions.assertThat(resultDbListResponse.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("test", "test2", "trends-test");
        resultDbListResponse = scmAggService.groupByAndCalculatePrs(
                company, ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.branch)
                        .codeChangeSizeConfig(Map.of())
                        .hasIssueKeys("true")
                        .commentDensitySizeConfig(Map.of())
                        .sort(Map.of())
                        .build(), null);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(2);
        resultDbListResponse = scmAggService.groupByAndCalculatePrs(
                company, ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.branch)
                        .codeChangeSizeConfig(Map.of())
                        .reviewers(List.of(user))
                        .commentDensitySizeConfig(Map.of())
                        .sort(Map.of())
                        .build(), null);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(3);
        Assertions.assertThat(resultDbListResponse.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("test", "test2", "trends-test");

        resultDbListResponse = scmAggService.groupByAndCalculatePrs(
                company, ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.branch)
                        .codeChangeSizeConfig(Map.of())
                        .states(List.of("closed"))
                        .commentDensitySizeConfig(Map.of())
                        .sort(Map.of())
                        .build(), null);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(1);
        Assertions.assertThat(resultDbListResponse.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("test");
        resultDbListResponse = scmAggService.groupByAndCalculatePrs(
                company, ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.project)
                        .codeChangeSizeConfig(Map.of())
                        .commentDensitySizeConfig(Map.of())
                        .calculation(ScmPrFilter.CALCULATION.merge_time)
                        .sort(Map.of())
                        .build(), null);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(1);
        Assertions.assertThat(resultDbListResponse.getRecords().stream().map(DbAggregationResult::getMedian).collect(Collectors.toList())).isNotNull();
        Assertions.assertThat(resultDbListResponse.getRecords().stream().map(DbAggregationResult::getMean).collect(Collectors.toList())).isNotNull();
        Assertions.assertThat(resultDbListResponse.getRecords().stream().map(DbAggregationResult::getSum).collect(Collectors.toList())).isNotNull();
        Assertions.assertThat(resultDbListResponse.getRecords().stream().map(DbAggregationResult::getMax).collect(Collectors.toList())).isNotNull();

        resultDbListResponse = scmAggService.groupByAndCalculatePrs(
                company, ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.repo_id)
                        .codeChangeSizeConfig(Map.of())
                        .commentDensitySizeConfig(Map.of())
                        .calculation(ScmPrFilter.CALCULATION.merge_time)
                        .sort(Map.of())
                        .build(), null);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(1);
        Assertions.assertThat(resultDbListResponse.getRecords().stream().map(DbAggregationResult::getMedian).collect(Collectors.toList())).isNotNull();
        Assertions.assertThat(resultDbListResponse.getRecords().stream().map(DbAggregationResult::getMean).collect(Collectors.toList())).isNotNull();
        Assertions.assertThat(resultDbListResponse.getRecords().stream().map(DbAggregationResult::getSum).collect(Collectors.toList())).isNotNull();
        Assertions.assertThat(resultDbListResponse.getRecords().stream().map(DbAggregationResult::getMax).collect(Collectors.toList())).isNotNull();

        resultDbListResponse = scmAggService.groupByAndCalculatePrs(
                company, ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.project)
                        .codeChangeSizeConfig(Map.of())
                        .commentDensitySizeConfig(Map.of())
                        .calculation(ScmPrFilter.CALCULATION.merge_time)
                        .sort(Map.of())
                        .build(), null);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(1);
        Assertions.assertThat(resultDbListResponse.getRecords().stream().map(DbAggregationResult::getMedian).collect(Collectors.toList())).isNotNull();
        Assertions.assertThat(resultDbListResponse.getRecords().stream().map(DbAggregationResult::getMean).collect(Collectors.toList())).isNotNull();
        Assertions.assertThat(resultDbListResponse.getRecords().stream().map(DbAggregationResult::getSum).collect(Collectors.toList())).isNotNull();
        Assertions.assertThat(resultDbListResponse.getRecords().stream().map(DbAggregationResult::getMax).collect(Collectors.toList())).isNotNull();

        resultDbListResponse = scmAggService.groupByAndCalculatePrs(
                company, ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.creator)
                        .codeChangeSizeConfig(Map.of())
                        .commentDensitySizeConfig(Map.of())
                        .calculation(ScmPrFilter.CALCULATION.merge_time)
                        .sort(Map.of())
                        .build(), null);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(1);
        Assertions.assertThat(resultDbListResponse.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList())).containsExactlyInAnyOrder("ctlo 2020");
        Assertions.assertThat(resultDbListResponse.getRecords().stream().map(DbAggregationResult::getMean).collect(Collectors.toList())).isNotNull();
        Assertions.assertThat(resultDbListResponse.getRecords().stream().map(DbAggregationResult::getSum).collect(Collectors.toList())).isNotNull();
        Assertions.assertThat(resultDbListResponse.getRecords().stream().map(DbAggregationResult::getMax).collect(Collectors.toList())).isNotNull();

        resultDbListResponse = scmAggService.groupByAndCalculatePrs(
                company, ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.approval_status)
                        .codeChangeSizeConfig(Map.of())
                        .commentDensitySizeConfig(Map.of())
                        .calculation(ScmPrFilter.CALCULATION.merge_time)
                        .sort(Map.of())
                        .build(), null);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(1);
        Assertions.assertThat(resultDbListResponse.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList())).containsExactlyInAnyOrder("self approved");
        Assertions.assertThat(resultDbListResponse.getRecords().stream().map(DbAggregationResult::getMean).collect(Collectors.toList())).isNotNull();
        Assertions.assertThat(resultDbListResponse.getRecords().stream().map(DbAggregationResult::getSum).collect(Collectors.toList())).isNotNull();
        Assertions.assertThat(resultDbListResponse.getRecords().stream().map(DbAggregationResult::getMax).collect(Collectors.toList())).isNotNull();

        resultDbListResponse = scmAggService.groupByAndCalculatePrs(
                company, ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.review_type)
                        .codeChangeSizeConfig(Map.of())
                        .commentDensitySizeConfig(Map.of())
                        .sort(Map.of())
                        .build(), null);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(2);
        Assertions.assertThat(resultDbListResponse.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("NOT_REVIEWED", "SELF_REVIEWED");
        Assertions.assertThat(resultDbListResponse.getRecords().stream().map(DbAggregationResult::getCount).collect(Collectors.toList()))
                .containsExactlyInAnyOrder(2L, 2L);

        resultDbListResponse = scmAggService.groupByAndCalculatePrs(
                company, ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.review_type)
                        .reviewers(List.of(user))
                        .codeChangeSizeConfig(Map.of())
                        .commentDensitySizeConfig(Map.of())
                        .sort(Map.of())
                        .build(), null);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(2);
        Assertions.assertThat(resultDbListResponse.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("NOT_REVIEWED", "SELF_REVIEWED");
        Assertions.assertThat(resultDbListResponse.getRecords().stream().map(DbAggregationResult::getCount).collect(Collectors.toList()))
                .containsExactlyInAnyOrder(2L, 2L);
        resultDbListResponse = scmAggService.groupByAndCalculatePrs(
                company, ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.review_type)
                        .excludeReviewers(List.of(user))
                        .codeChangeSizeConfig(Map.of())
                        .commentDensitySizeConfig(Map.of())
                        .sort(Map.of())
                        .build(), null);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(0);
        resultDbListResponse = scmAggService.groupByAndCalculatePrs(
                company, ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.review_type)
                        .approvers(List.of(user))
                        .codeChangeSizeConfig(Map.of())
                        .commentDensitySizeConfig(Map.of())
                        .sort(Map.of())
                        .build(), null);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(1);
        Assertions.assertThat(resultDbListResponse.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("SELF_REVIEWED");
        Assertions.assertThat(resultDbListResponse.getRecords().stream().map(DbAggregationResult::getCount).collect(Collectors.toList()))
                .containsExactlyInAnyOrder(1L);
        resultDbListResponse = scmAggService.groupByAndCalculatePrs(
                company, ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.review_type)
                        .excludeApprovers(List.of(user))
                        .codeChangeSizeConfig(Map.of())
                        .commentDensitySizeConfig(Map.of())
                        .sort(Map.of())
                        .build(), null);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(2);
        Assertions.assertThat(resultDbListResponse.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("NOT_REVIEWED", "SELF_REVIEWED");
        Assertions.assertThat(resultDbListResponse.getRecords().stream().map(DbAggregationResult::getCount).collect(Collectors.toList()))
                .containsExactlyInAnyOrder(2L, 1L);
        resultDbListResponse = scmAggService.groupByAndCalculatePrs(
                company, ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.approver)
                        .excludeApprovers(List.of(user))
                        .codeChangeSizeConfig(Map.of())
                        .commentDensitySizeConfig(Map.of())
                        .sort(Map.of())
                        .build(), null);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(1);
        Assertions.assertThat(resultDbListResponse.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("NONE");
        Assertions.assertThat(resultDbListResponse.getRecords().stream().map(DbAggregationResult::getCount).collect(Collectors.toList()))
                .containsExactlyInAnyOrder(3L);
        resultDbListResponse =  scmAggService.groupByAndCalculatePrs(
                company, ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.approval_status)
                        .excludeApprovalStatuses(List.of("rejected"))
                        .codeChangeSizeConfig(Map.of())
                        .commentDensitySizeConfig(Map.of())
                        .sort(Map.of())
                        .build(), null);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(2);
        Assertions.assertThat(resultDbListResponse.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("pending review", "self approved");
        Assertions.assertThat(resultDbListResponse.getRecords().stream().map(DbAggregationResult::getCount).collect(Collectors.toList()))
                .containsExactlyInAnyOrder(1L, 2L);

        resultDbListResponse = scmAggService.groupByAndCalculatePrsDuration(
                company, ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.pr_created)
                        .codeChangeUnit("lines")
                        .calculation(ScmPrFilter.CALCULATION.first_review_time)
                        .codeChangeSizeConfig(Map.of())
                        .commentDensitySizeConfig(Map.of())
                        .sort(Map.of())
                        .build(), null);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(2);
        resultDbListResponse = scmAggService.groupByAndCalculatePrsDuration(
                company, ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.pr_updated)
                        .codeChangeUnit("lines")
                        .calculation(ScmPrFilter.CALCULATION.first_review_to_merge_time)
                        .codeChangeSizeConfig(Map.of())
                        .commentDensitySizeConfig(Map.of())
                        .sort(Map.of())
                        .build(), null);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(1);
    }

    @Test
    public void testListWithFilters() throws SQLException {
        String user = userIdentityService.getUser(company, gitlabIntegrationId, "ctlo2020");
        DbListResponse<DbScmPullRequest> resultDbListResponse = scmAggService.list(company, ScmPrFilter.builder()
                .codeChangeUnit("lines")
                .codeChanges(List.of("small"))
                .codeChangeSizeConfig(Map.of("small", "500"))
                .commentDensitySizeConfig(Map.of())
                .build(), Map.of(), null, 0, 10000);
        Assertions.assertThat(resultDbListResponse).isNotNull();
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(4);

        resultDbListResponse = scmAggService.list(company, ScmPrFilter.builder()
                .locRange(ImmutablePair.of(0L, 600L))
                .build(), Map.of(), null, 0, 10000);
        Assertions.assertThat(resultDbListResponse).isNotNull();
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(3);

        resultDbListResponse = scmAggService.list(company, ScmPrFilter.builder()
                .locRange(ImmutablePair.of(300L, 700L))
                .excludeLocRange(ImmutablePair.of(200L, 800L))
                .build(), Map.of(), null, 0, 10000);
        Assertions.assertThat(resultDbListResponse).isNotNull();
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(0);

        resultDbListResponse = scmAggService.list(company, ScmPrFilter.builder()
                .excludeLocRange(ImmutablePair.of(200L, 800L))
                .build(), Map.of(), null, 0, 10000);
        Assertions.assertThat(resultDbListResponse).isNotNull();
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(3);

        resultDbListResponse = scmAggService.list(company, ScmPrFilter.builder()
                .locRange(ImmutablePair.of(300L, 600L))
                .build(), Map.of(), null, 0, 10000);
        Assertions.assertThat(resultDbListResponse).isNotNull();
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(0);


        resultDbListResponse = scmAggService.list(company, ScmPrFilter.builder()
                .codeChanges(List.of("medium"))
                .codeChangeUnit("lines")
                .commentDensitySizeConfig(Map.of())
                .codeChangeSizeConfig(Map.of("medium", "100"))
                .build(), Map.of(), null, 0, 10000);
        Assertions.assertThat(resultDbListResponse).isNotNull();
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(0);

        resultDbListResponse = scmAggService.list(company, ScmPrFilter.builder()
                .approvalStatuses(List.of("rejected"))
                .codeChangeUnit("lines")
                .codeChangeSizeConfig(Map.of())
                .commentDensitySizeConfig(Map.of())
                .build(), Map.of(), null, 0, 10000);
        Assertions.assertThat(resultDbListResponse).isNotNull();
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(1);
        Assertions.assertThat(resultDbListResponse.getRecords().stream().map(DbScmPullRequest::getProject).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("ctlo2020/test-velocity-cog-2");

        resultDbListResponse = scmAggService.list(company, ScmPrFilter.builder()
                .approvalStatuses(List.of("pending review"))
                .codeChangeUnit("lines")
                .codeChangeSizeConfig(Map.of())
                .commentDensitySizeConfig(Map.of())
                .build(), Map.of(), null, 0, 10000);
        Assertions.assertThat(resultDbListResponse).isNotNull();
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(2);
        Assertions.assertThat(resultDbListResponse.getRecords().stream().map(DbScmPullRequest::getProject).collect(Collectors.toList()))
                .contains("ctlo2020/test-velocity-cog-2");

        resultDbListResponse = scmAggService.list(company, ScmPrFilter.builder()
                .reviewers(List.of(user))
                .codeChangeUnit("lines")
                .codeChangeSizeConfig(Map.of())
                .commentDensitySizeConfig(Map.of())
                .build(), Map.of(), null, 0, 10000);
        Assertions.assertThat(resultDbListResponse).isNotNull();
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(4);

        resultDbListResponse = scmAggService.list(company, ScmPrFilter.builder()
                .approvers(List.of(user))
                .codeChangeUnit("lines")
                .codeChangeSizeConfig(Map.of())
                .commentDensitySizeConfig(Map.of())
                .build(), Map.of(), null, 0, 10000);
        Assertions.assertThat(resultDbListResponse).isNotNull();
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(1);

        resultDbListResponse = scmAggService.list(company, ScmPrFilter.builder()
                .codeChangeSizeConfig(Map.of())
                .codeChangeSizeConfig(Map.of("small", "50", "medium", "200"))
                .creators(List.of("ctlo 2020"))
                .commentDensitySizeConfig(Map.of())
                .build(), Map.of(), null, 0, 10000);
        Assertions.assertThat(resultDbListResponse).isNotNull();
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(0);

        resultDbListResponse = scmAggService.list(company, ScmPrFilter.builder()
                .codeChangeSizeConfig(Map.of())
                .codeChangeSizeConfig(Map.of("small", "50", "medium", "200"))
                .hasIssueKeys("false")
                .commentDensitySizeConfig(Map.of())
                .build(), Map.of(), null, 0, 10000);

        Assertions.assertThat(resultDbListResponse).isNotNull();
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(1);

        resultDbListResponse = scmAggService.list(company, ScmPrFilter.builder()
                .commitTitles(List.of("abc"))
                .build(), Map.of(), null, 0, 10000);

        System.out.println("results: " + resultDbListResponse.getRecords().get(0));
        Assertions.assertThat(Integer.parseInt(resultDbListResponse.getRecords().get(0).getLinesAdded())).isEqualTo(0);
        Assertions.assertThat(Integer.parseInt(resultDbListResponse.getRecords().get(0).getLinesChanged())).isEqualTo(0);
        Assertions.assertThat(Integer.parseInt(resultDbListResponse.getRecords().get(0).getLinesDeleted())).isEqualTo(0);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(4);

        resultDbListResponse = scmAggService.list(company, ScmPrFilter.builder()
                .locRange(ImmutablePair.of(0L, 10L))
                .commitTitles(List.of("abc"))
                .build(), Map.of(), null, 0, 10000);

        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(0);
    }

    @Test
    public void testCommentCodeChanges() throws SQLException {
        DbListResponse<DbAggregationResult> resultDbListResponse = scmAggService.groupByAndCalculatePrs(
                company, ScmPrFilter.builder()
                        .codeChangeSizeConfig(Map.of("small", "4"))
                        .across(ScmPrFilter.DISTINCT.code_change)
                        .codeChangeUnit("lines")
                        .commentDensitySizeConfig(Map.of())
                        .sort(Map.of())
                        .build(), null);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(1);
        Assertions.assertThat(resultDbListResponse.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .contains("small");
        Assertions.assertThat(resultDbListResponse.getRecords().stream().map(DbAggregationResult::getCount).collect(Collectors.toList()))
                .contains(4L);

        resultDbListResponse = scmAggService.groupByAndCalculatePrs(
                company, ScmPrFilter.builder()
                        .codeChanges(List.of("medium"))
                        .codeChangeSizeConfig(Map.of("small", "4"))
                        .across(ScmPrFilter.DISTINCT.code_change)
                        .codeChangeUnit("lines")
                        .commentDensitySizeConfig(Map.of())
                        .sort(Map.of())
                        .build(), null);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(0);

        resultDbListResponse = scmAggService.groupByAndCalculatePrs(
                company, ScmPrFilter.builder()
                        .commentDensitySizeConfig(Map.of("shallow", "4"))
                        .across(ScmPrFilter.DISTINCT.comment_density)
                        .codeChangeUnit("lines")
                        .codeChangeSizeConfig(Map.of())
                        .sort(Map.of())
                        .build(), null);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(1);
        Assertions.assertThat(resultDbListResponse.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .contains("shallow");
        Assertions.assertThat(resultDbListResponse.getRecords().stream().map(DbAggregationResult::getCount).collect(Collectors.toList()))
                .contains(4L);

        resultDbListResponse = scmAggService.groupByAndCalculatePrs(
                company, ScmPrFilter.builder()
                        .commentDensities(List.of("good"))
                        .commentDensitySizeConfig(Map.of("shallow", "4"))
                        .across(ScmPrFilter.DISTINCT.comment_density)
                        .codeChangeUnit("lines")
                        .codeChangeSizeConfig(Map.of())
                        .sort(Map.of())
                        .build(), null);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(0);

        DbListResponse<DbScmPullRequest> listResponse = scmAggService.list(company, ScmPrFilter.builder()
                .codeChangeUnit("lines")
                .codeChanges(List.of("small"))
                .commentDensities(List.of("shallow"))
                .codeChangeSizeConfig(Map.of())
                .commentDensitySizeConfig(Map.of())
                .build(), Map.of(), null, 0, 10000);
        Assertions.assertThat(listResponse).isNotNull();
        Assertions.assertThat(listResponse.getTotalCount()).isEqualTo(4);

        listResponse = scmAggService.list(company, ScmPrFilter.builder()
                .codeChangeUnit("lines")
                .codeChanges(List.of("small"))
                .codeChangeSizeConfig(Map.of())
                .commentDensitySizeConfig(Map.of())
                .build(), Map.of(), null, 0, 10000);
        Assertions.assertThat(listResponse).isNotNull();
        Assertions.assertThat(listResponse.getTotalCount()).isEqualTo(4);

        listResponse = scmAggService.list(company, ScmPrFilter.builder()
                .codeChangeUnit("lines")
                .commentDensities(List.of("shallow"))
                .codeChangeSizeConfig(Map.of())
                .commentDensitySizeConfig(Map.of())
                .build(), Map.of(), null, 0, 10000);
        Assertions.assertThat(listResponse).isNotNull();
        Assertions.assertThat(listResponse.getTotalCount()).isEqualTo(4);

        listResponse = scmAggService.list(company, ScmPrFilter.builder()
                .codeChangeUnit("lines")
                .commentDensities(List.of("good"))
                .codeChangeSizeConfig(Map.of())
                .commentDensitySizeConfig(Map.of())
                .build(), Map.of(), null, 0, 10000);
        Assertions.assertThat(listResponse).isNotNull();
        Assertions.assertThat(listResponse.getTotalCount()).isEqualTo(0);

        listResponse = scmAggService.list(company, ScmPrFilter.builder()
                .codeChangeUnit("lines")
                .commentDensities(List.of("shallow"))
                .codeChanges(List.of("large"))
                .codeChangeSizeConfig(Map.of())
                .commentDensitySizeConfig(Map.of())
                .build(), Map.of(), null, 0, 10000);
        Assertions.assertThat(listResponse).isNotNull();
        Assertions.assertThat(listResponse.getTotalCount()).isEqualTo(0);

        listResponse = scmAggService.list(company, ScmPrFilter.builder()
                .codeChangeUnit("lines")
                .projects(List.of("ctlo2020/test-velocity-cog"))
                .codeChangeSizeConfig(Map.of())
                .commentDensitySizeConfig(Map.of())
                .build(), Map.of(), null, 0, 10000);
        Assertions.assertThat(listResponse).isNotNull();
        Assertions.assertThat(listResponse.getTotalCount()).isEqualTo(1);

        listResponse = scmAggService.list(company, ScmPrFilter.builder()
                .codeChangeUnit("lines")
                .projects(List.of("ctlo2020/test-velocity-cog"))
                .codeChangeSizeConfig(Map.of())
                .commentDensitySizeConfig(Map.of())
                .build(), Map.of(), null, 0, 10000);
        Assertions.assertThat(listResponse).isNotNull();
        Assertions.assertThat(listResponse.getTotalCount()).isEqualTo(1);
        listResponse = scmAggService.list(company, ScmPrFilter.builder()
                .codeChangeUnit("lines")
                .states(List.of("merged"))
                .codeChangeSizeConfig(Map.of())
                .commentDensitySizeConfig(Map.of())
                .build(), Map.of(), null, 0, 10000);
        Assertions.assertThat(listResponse).isNotNull();
        Assertions.assertThat(listResponse.getTotalCount()).isEqualTo(1);
        listResponse = scmAggService.list(company, ScmPrFilter.builder()
                .codeChangeUnit("lines")
                .codeChangeSizeConfig(Map.of())
                .commentDensitySizeConfig(Map.of())
                .build(), Map.of(), null, 0, 10000);
        Assertions.assertThat(listResponse).isNotNull();
        Assertions.assertThat(listResponse.getRecords().stream().map(DbScmPullRequest::getReviewType).collect(Collectors.toList()))
                .containsAnyOf("SELF_REVIEWED", "PEER_REVIEWED", "PEER_REVIEWED");

        listResponse = scmAggService.list(company, ScmPrFilter.builder()
                .codeChangeUnit("lines")
                .codeChangeSizeConfig(Map.of())
                .commentDensitySizeConfig(Map.of())
                .build(), Map.of(), null, 0, 10000);
        Assertions.assertThat(listResponse).isNotNull();
        Assertions.assertThat(listResponse.getRecords().stream().map(DbScmPullRequest::getReviewers).findAny().orElseThrow())
                .containsAnyOf("ctlo 2020");

        listResponse = scmAggService.list(company, ScmPrFilter.builder()
                .codeChangeUnit("lines")
                .codeChangeSizeConfig(Map.of())
                .commentDensitySizeConfig(Map.of())
                .build(), Map.of(), null, 0, 10000);
        Assertions.assertThat(listResponse).isNotNull();
        Assertions.assertThat(listResponse.getRecords().stream().map(DbScmPullRequest::getApprovers).findAny().orElseThrow())
                .containsAnyElementsOf(List.of("NONE", "ctlo2020"));

        listResponse = scmAggService.list(company, ScmPrFilter.builder()
                .codeChangeUnit("lines")
                .codeChangeSizeConfig(Map.of())
                .reviewerCount(ImmutablePair.of(0L,2L))
                .commentDensitySizeConfig(Map.of())
                .build(), Map.of(), null, 0, 10000);
        Assertions.assertThat(listResponse).isNotNull();
        Assertions.assertThat(listResponse.getTotalCount()).isEqualTo(2);
        Assertions.assertThat(listResponse.getRecords().stream().map(DbScmPullRequest::getReviewers).findAny().orElseThrow())
                .containsAnyOf("ctlo 2020");

        listResponse = scmAggService.list(company, ScmPrFilter.builder()
                .codeChangeUnit("lines")
                .codeChangeSizeConfig(Map.of())
                .reviewerCount(ImmutablePair.of(0L,5L))
                .commentDensitySizeConfig(Map.of())
                .build(), Map.of(), null, 0, 10000);
        Assertions.assertThat(listResponse).isNotNull();
        Assertions.assertThat(listResponse.getTotalCount()).isEqualTo(4);

        listResponse = scmAggService.list(company, ScmPrFilter.builder()
                .codeChangeUnit("lines")
                .codeChangeSizeConfig(Map.of())
                .prUpdatedRange(ImmutablePair.of(0L, 1729507440L))
                .commentDensitySizeConfig(Map.of())
                .build(), Map.of(), null, 0, 10000);
        Assertions.assertThat(listResponse).isNotNull();
        Assertions.assertThat(listResponse.getTotalCount()).isEqualTo(4);
    }

    @Test
    public void testUpdateCommitProject() throws SQLException {
        Optional<DbScmCommit> optCommit = scmAggService.getCommit(company, "8753e00810527aaf9c75b77d64a82e651a736bcd", "ctlo2020/test-velocity-cog-2", gitlabIntegrationId);
        org.junit.jupiter.api.Assertions.assertTrue(optCommit.isPresent());
        DbScmCommit dbScmCommit = optCommit.get();
        scmAggService.updateCommitProject(company, UUID.fromString(dbScmCommit.getId()), dbScmCommit.toBuilder().project("levelops").build());
        optCommit = scmAggService.getCommit(company, "8753e00810527aaf9c75b77d64a82e651a736bcd", "ctlo2020/test-velocity-cog-2", gitlabIntegrationId);
        org.junit.jupiter.api.Assertions.assertTrue(optCommit.isPresent());
        Assertions.assertThat(optCommit.get().getProject()).isEqualTo("levelops");
    }
}