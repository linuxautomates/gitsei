package io.levelops.commons.databases.services.velocity;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.DatabaseTestUtils;
import io.levelops.commons.databases.models.database.CICDJob;
import io.levelops.commons.databases.models.database.CICDJobRun;
import io.levelops.commons.databases.models.database.CiCdScmMapping;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.IntegrationConfig;
import io.levelops.commons.databases.models.database.IntegrationTracker;
import io.levelops.commons.databases.models.database.jira.DbJiraField;
import io.levelops.commons.databases.models.database.jira.DbJiraIssue;
import io.levelops.commons.databases.models.database.jira.parsers.JiraIssueParser;
import io.levelops.commons.databases.models.database.repo.DbRepository;
import io.levelops.commons.databases.models.database.scm.DbScmCommit;
import io.levelops.commons.databases.models.database.scm.DbScmFile;
import io.levelops.commons.databases.models.database.scm.DbScmIssue;
import io.levelops.commons.databases.models.database.scm.DbScmPullRequest;
import io.levelops.commons.databases.models.database.velocity.VelocityConfigDTO;
import io.levelops.commons.databases.models.filters.CiCdJobRunsFilter;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
import io.levelops.commons.databases.models.filters.ScmCommitFilter;
import io.levelops.commons.databases.models.filters.ScmPrFilter;
import io.levelops.commons.databases.models.filters.VelocityFilter;
import io.levelops.commons.databases.models.filters.WorkItemsMilestoneFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.CiCdInstancesDatabaseService;
import io.levelops.commons.databases.services.CiCdJobRunsDatabaseService;
import io.levelops.commons.databases.services.CiCdJobsDatabaseService;
import io.levelops.commons.databases.services.CiCdMetadataConditionBuilder;
import io.levelops.commons.databases.services.CiCdPartialMatchConditionBuilder;
import io.levelops.commons.databases.services.CiCdScmMappingService;
import io.levelops.commons.databases.services.DatabaseSchemaService;
import io.levelops.commons.databases.services.GitRepositoryService;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.IntegrationTrackingService;
import io.levelops.commons.databases.services.JiraFieldService;
import io.levelops.commons.databases.services.JiraIssueService;
import io.levelops.commons.databases.services.ScmAggService;
import io.levelops.commons.databases.services.UserIdentityService;
import io.levelops.commons.databases.services.WorkItemFieldsMetaService;
import io.levelops.commons.databases.services.WorkItemsAgeReportService;
import io.levelops.commons.databases.services.WorkItemsFirstAssigneeReportService;
import io.levelops.commons.databases.services.WorkItemsReportService;
import io.levelops.commons.databases.services.WorkItemsResolutionTimeReportService;
import io.levelops.commons.databases.services.WorkItemsResponseTimeReportService;
import io.levelops.commons.databases.services.WorkItemsService;
import io.levelops.commons.databases.services.WorkItemsStageTimesReportService;
import io.levelops.commons.databases.services.organization.TeamMembersDatabaseService;
import io.levelops.commons.databases.services.scm.ScmCommitPRMappingService;
import io.levelops.commons.databases.services.scm.ScmCommitPullRequestMappingDBService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.github.models.GithubRepository;
import io.levelops.integrations.jira.models.JiraIssue;
import io.levelops.web.exceptions.BadRequestException;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class VelocityCustomFieldTest {
    private static final ObjectMapper m = DefaultObjectMapper.get();
    private static String company = "test";
    private static final WorkItemsType WORK_ITEM_TYPE = WorkItemsType.JIRA;

    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    private static DataSource dataSource;
    private static JiraIssueService jiraIssueService;
    private static IntegrationService integrationService;
    private static ScmAggService scmAggService;
    private static ScmCommitPullRequestMappingDBService scmCommitPullRequestMappingDBService;
    private static ScmCommitPRMappingService scmCommitPRMappingService;
    private static VelocityAggsDatabaseService velocityAggsDatabaseService;
    private static VelocityConfigsDatabaseService velocityConfigsDatabaseService;
    private static Date currentTime;
    private static GitRepositoryService repositoryService;
    private static CiCdJobsDatabaseService ciCdJobsDatabaseService;
    private static CiCdInstancesDatabaseService ciCdInstancesDatabaseService;
    private static CiCdScmMappingService mappingService;
    private static CiCdJobRunsDatabaseService jobRunsDatabaseService;
    private static UserIdentityService userIdentityService;
    private static TeamMembersDatabaseService teamMembersDatabaseService;
    private static IntegrationTrackingService integrationTrackingService;
    private static CiCdMetadataConditionBuilder ciCdMetadataConditionBuilder;
    private static CiCdPartialMatchConditionBuilder ciCdPartialMatchConditionBuilder;
    private static String integId, integId1;

    @BeforeClass
    public static void setup() throws SQLException, IOException {
        dataSource = DatabaseTestUtils.setUpDataSource(pg, company);
        DatabaseTestUtils.JiraTestDbs jiraTestDbs = DatabaseTestUtils.setUpJiraServices(dataSource, company);
        jiraIssueService = jiraTestDbs.getJiraIssueService();

        JiraFieldService jiraFieldService = jiraTestDbs.getJiraFieldService();
        integrationService = jiraTestDbs.getIntegrationService();

        integrationTrackingService = new IntegrationTrackingService(dataSource);
        WorkItemFieldsMetaService workItemFieldsMetaService = new WorkItemFieldsMetaService(dataSource);
        userIdentityService = jiraTestDbs.getUserIdentityService();
        scmAggService = new ScmAggService(dataSource, userIdentityService);
        scmCommitPullRequestMappingDBService = new ScmCommitPullRequestMappingDBService(dataSource);
        scmCommitPRMappingService = new ScmCommitPRMappingService(dataSource, scmCommitPullRequestMappingDBService, 100);
        ciCdMetadataConditionBuilder = new CiCdMetadataConditionBuilder();
        ciCdPartialMatchConditionBuilder = new CiCdPartialMatchConditionBuilder();
        velocityAggsDatabaseService = new VelocityAggsDatabaseService(dataSource, jiraTestDbs.getJiraConditionsBuilder(), scmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        ciCdJobsDatabaseService = new CiCdJobsDatabaseService(dataSource);
        teamMembersDatabaseService = new TeamMembersDatabaseService(dataSource, m);
        ciCdInstancesDatabaseService = new CiCdInstancesDatabaseService(dataSource);
        jobRunsDatabaseService = new CiCdJobRunsDatabaseService(DefaultObjectMapper.get(), dataSource);
        mappingService = new CiCdScmMappingService(dataSource);
        velocityConfigsDatabaseService = new VelocityConfigsDatabaseService(dataSource, DefaultObjectMapper.get(), null);
        repositoryService = new GitRepositoryService(dataSource);
        WorkItemsReportService workItemsReportService = new WorkItemsReportService(dataSource, workItemFieldsMetaService);
        WorkItemsStageTimesReportService workItemsStageTimesReportService = new WorkItemsStageTimesReportService(dataSource, workItemFieldsMetaService);
        WorkItemsAgeReportService workItemsAgeReportService = new WorkItemsAgeReportService(dataSource, workItemFieldsMetaService);
        WorkItemsResolutionTimeReportService workItemsResolutionTimeReportService = new WorkItemsResolutionTimeReportService(dataSource, workItemFieldsMetaService);
        WorkItemsResponseTimeReportService workItemsResponseTimeReportService = new WorkItemsResponseTimeReportService(dataSource, workItemFieldsMetaService);
        WorkItemsFirstAssigneeReportService workItemsFirstAssigneeReportService = new WorkItemsFirstAssigneeReportService(dataSource, workItemFieldsMetaService);
        WorkItemsService workItemService = new WorkItemsService(dataSource, workItemsReportService,
                workItemsStageTimesReportService, workItemsAgeReportService, workItemsResolutionTimeReportService,
                workItemsResponseTimeReportService, null, null, null,
                null, null, workItemsFirstAssigneeReportService, workItemFieldsMetaService, null);

        new DatabaseSchemaService(dataSource)
                .ensureSchemaExistence(company);
        integrationService.ensureTableExistence(company);
        integrationTrackingService.ensureTableExistence(company);
        jiraIssueService.ensureTableExistence(company);
        userIdentityService.ensureTableExistence(company);
        scmAggService.ensureTableExistence(company);
        scmCommitPullRequestMappingDBService.ensureTableExistence(company);
        ciCdInstancesDatabaseService.ensureTableExistence(company);
        ciCdJobsDatabaseService.ensureTableExistence(company);
        jobRunsDatabaseService.ensureTableExistence(company);
        mappingService.ensureTableExistence(company);
        workItemService.ensureTableExistence(company);
        jiraFieldService.ensureTableExistence(company);

        velocityAggsDatabaseService.ensureTableExistence(company);
        velocityConfigsDatabaseService.ensureTableExistence(company);
        repositoryService.ensureTableExistence(company);
        jiraFieldService.ensureTableExistence(company);
        teamMembersDatabaseService.ensureTableExistence(company);
        integId = integrationService.insert(company, Integration.builder()
                .application("jira")
                .name("jira test")
                .status("enabled")
                .build());
        integId1 = integrationService.insert(company, Integration.builder()
                .application("github")
                .name("github test")
                .status("enabled")
                .build());
        currentTime = DateUtils.truncate(new Date(Instant.parse("2021-05-05T15:00:00-08:00").getEpochSecond()), Calendar.DATE);
        String jiraIn = ResourceUtils.getResourceAsString("json/databases/jira_issues_custom.json");
        PaginatedResponse<JiraIssue> jissues = m.readValue(jiraIn,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, JiraIssue.class));
        jissues.getResponse().getRecords().forEach(issue -> {
            try {
                DbJiraIssue tmp = JiraIssueParser.parseJiraIssue(issue, integId, currentTime, JiraIssueParser.JiraParserConfig.builder()
                        .epicLinkField("customfield_10014")
                        .storyPointsField("customfield_10030")
                        .customFieldConfig(List.of(IntegrationConfig.ConfigEntry.builder().name("someCustom").key("customfield_11111").build()))
                        .build());

                if (jiraIssueService.get(company, tmp.getKey(), tmp.getIntegrationId(), tmp.getIngestedAt()).isPresent()) {
                    throw new RuntimeException("This issue shouldnt exist.");
                }

                jiraIssueService.insert(company, tmp);
                if (jiraIssueService.get(company, tmp.getKey(), tmp.getIntegrationId(), tmp.getIngestedAt()).isEmpty()) {
                    throw new RuntimeException("This issue should exist.");
                }
                if (jiraIssueService.get(company, tmp.getKey(), tmp.getIntegrationId(),
                        tmp.getIngestedAt() - 86400L).isPresent()) {
                    throw new RuntimeException("This issue shouldnt exist.");
                }
            } catch (SQLException throwable) {
                throwable.printStackTrace();
            }
        });
        integrationTrackingService.upsert(company, IntegrationTracker.builder().integrationId(integId).latestIngestedAt(1584000L).build());

        String input = ResourceUtils.getResourceAsString("json/databases/githubprs.json");
        PaginatedResponse<GithubRepository> prs = m.readValue(input,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, GithubRepository.class));
        List<DbRepository> repos = new ArrayList<>();
        prs.getResponse().getRecords().forEach(repo -> {
            repos.add(DbRepository.fromGithubRepository(repo, integId));
            repo.getPullRequests()
                    .forEach(review -> {
                        try {
                            DbScmPullRequest tmp = DbScmPullRequest
                                    .fromGithubPullRequest(review, repo.getId(), integId, null);
                            if (tmp.getMergeSha().equals("6ca5a1062809e836a0e8ac9dbc1b54eb7648eae6")) {
                                DbScmPullRequest.DbScmPullRequestBuilder tmp1 = tmp.toBuilder();
                                DbScmPullRequest tmp2 = tmp1.mergeSha("decf002f4fddc9ea62bfffabdbea74ac84b19467").build();
                                scmAggService.insert(company, tmp2);
                            } else {
                                scmAggService.insert(company, tmp);
                            }
                        } catch (SQLException throwable) {
                            throwable.printStackTrace();
                        }
                    });
        });

        input = ResourceUtils.getResourceAsString("json/databases/github_issues.json");
        PaginatedResponse<GithubRepository> issues = m.readValue(input,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, GithubRepository.class));
        issues.getResponse().getRecords().forEach(repo -> {
            repos.add(DbRepository.fromGithubRepository(repo, integId));
            repo.getIssues()
                    .forEach(issue -> {
                        DbScmIssue tmp = DbScmIssue
                                .fromGithubIssue(issue, repo.getId(), integId);
                        if (scmAggService.getIssue(company, tmp.getIssueId(), tmp.getRepoId(),
                                tmp.getIntegrationId()).isEmpty()) {
                            scmAggService.insertIssue(company, tmp);
                        }
                    });
        });

        input = ResourceUtils.getResourceAsString("json/databases/githubcommits.json");
        PaginatedResponse<GithubRepository> commits = m.readValue(input,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, GithubRepository.class));
        commits.getResponse().getRecords().forEach(repo -> {
            repos.add(DbRepository.fromGithubRepository(repo, integId));
            repo.getEvents().stream()
                    .filter(ev -> "PushEvent".equals(ev.getType()))
                    .flatMap(ev -> ev.getCommits().stream())
                    .forEach(commit -> {
                        DbScmCommit tmp = DbScmCommit
                                .fromGithubCommit(commit, repo.getId(), integId,
                                        currentTime.toInstant().getEpochSecond(), 0L);
                        if (scmAggService.getCommit(company, tmp.getCommitSha(), tmp.getRepoIds(),
                                tmp.getIntegrationId()).isEmpty()) {
                            String id = null;
                            try {
                                id = scmAggService.insertCommit(company, tmp);
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                            try {
                                CICDJob.CICDJobBuilder bldr = CICDJob.builder().jobName("job" + tmp.getId()).projectName("project-").jobFullName("branches")
                                        .jobNormalizedFullName("job/branch").branchName("branch").moduleName("module-name").scmUrl("url").scmUserId("user-git");
                                CICDJob cicdJob = bldr.build();
                                String jobId = ciCdJobsDatabaseService.insert(company, cicdJob);
                                String jobRunId = jobRunsDatabaseService.insert(company, CICDJobRun.builder().cicdJobId(UUID.fromString(jobId)).jobRunNumber(12345L).scmCommitIds(List.of()).build());
                                mappingService.insert(company, CiCdScmMapping.builder().commitId(UUID.fromString(id)).jobRunId(UUID.fromString(jobRunId)).build());
                            } catch (SQLException throwables) {
                                throwables.printStackTrace();
                            }
                            DbScmFile.fromGithubCommit(
                                            commit, repo.getId(), integId, currentTime.toInstant().getEpochSecond())
                                    .forEach(scmFile -> scmAggService.insertFile(company, scmFile));
                        }
                    });
        });
        repositoryService.batchUpsert(company, repos);
        jiraFieldService.batchUpsert(company, List.of(DbJiraField.builder()
                .name("Number Field")
                .custom(true)
                .integrationId("1")
                .fieldKey("customfield_11111")
                .fieldType("number")
                .build()));

        scmCommitPRMappingService.persistScmCommitPRMapping(company, null);
    }

    @Test
    public void test() throws BadRequestException {
        DbListResponse<List<DbAggregationResult>> result = velocityAggsDatabaseService.calculateVelocityWithoutStacks(company,
                VelocityConfigDTO.builder().build(),
                VelocityFilter.builder().calculation(VelocityFilter.CALCULATION.ticket_velocity).across(VelocityFilter.DISTINCT.velocity).page(0).pageSize(100).build(),
                WORK_ITEM_TYPE, JiraIssuesFilter.builder().customFields(Map.of("customfield_11111",
                        Map.of("$gt", "800", "$lt", "123199"))).integrationIds(List.of(integId, integId1)).projects(List.of("LEV")).excludeLabels(List.of("label1")).build(),
                null, WorkItemsMilestoneFilter.builder().build(), ScmPrFilter.builder().integrationIds(List.of(integId, integId1)).build(),
                ScmCommitFilter.builder().build(), CiCdJobRunsFilter.builder().build(), List.of(), null, false);
        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.getCount().intValue());
    }
}
