package io.levelops.commons.databases.services.velocity;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.DatabaseTestUtils;
import io.levelops.commons.databases.issue_management.DbWorkItemField;
import io.levelops.commons.databases.models.database.CICDJob;
import io.levelops.commons.databases.models.database.CICDJobRun;
import io.levelops.commons.databases.models.database.CiCdScmMapping;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.IntegrationTracker;
import io.levelops.commons.databases.models.database.jira.DbJiraIssue;
import io.levelops.commons.databases.models.database.jira.DbJiraUser;
import io.levelops.commons.databases.models.database.jira.parsers.JiraIssueParser;
import io.levelops.commons.databases.models.database.repo.DbRepository;
import io.levelops.commons.databases.models.database.scm.DbScmCommit;
import io.levelops.commons.databases.models.database.scm.DbScmFile;
import io.levelops.commons.databases.models.database.scm.DbScmIssue;
import io.levelops.commons.databases.models.database.scm.DbScmPullRequest;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.databases.models.database.velocity.VelocityConfigDTO;
import io.levelops.commons.databases.models.filters.CiCdJobRunsFilter;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
import io.levelops.commons.databases.models.filters.ScmCommitFilter;
import io.levelops.commons.databases.models.filters.ScmPrFilter;
import io.levelops.commons.databases.models.filters.VelocityFilter;
import io.levelops.commons.databases.models.filters.WorkItemsFilter;
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
import io.levelops.commons.databases.services.JiraProjectService;
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
import io.levelops.integrations.jira.models.JiraUser;
import io.levelops.web.exceptions.BadRequestException;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
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
import java.util.Optional;
import java.util.UUID;

public class VelocityAggsDatabaseServiceTest {
    private static final ObjectMapper m = DefaultObjectMapper.get();
    private static String company = "test";
    private static final WorkItemsType WORK_ITEM_TYPE = WorkItemsType.JIRA;
    private static final WorkItemsMilestoneFilter WORKITEMS_MILESTONE_FILTER = WorkItemsMilestoneFilter.builder().build();
    private static final List<DbWorkItemField> WORKITEM_CUSTOM_FIELDS = List.of(); 

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

    private Optional<String> userIdOf(String displayName) {
        return userIdentityService.getUserByDisplayName(company, "1", displayName);
    }

    @BeforeClass
    public static void setup() throws SQLException, IOException {
        dataSource =  DatabaseTestUtils.setUpDataSource(pg, company);
        DatabaseTestUtils.JiraTestDbs jiraTestDbs = DatabaseTestUtils.setUpJiraServices(dataSource, company);
        jiraIssueService = jiraTestDbs.getJiraIssueService();

        JiraFieldService jiraFieldService = jiraTestDbs.getJiraFieldService();
        JiraProjectService jiraProjectService = jiraTestDbs.getJiraProjectService();
        integrationService = jiraTestDbs.getIntegrationService();
        integrationTrackingService = new IntegrationTrackingService(dataSource);
        WorkItemFieldsMetaService workItemFieldsMetaService = new WorkItemFieldsMetaService(dataSource);
        userIdentityService = jiraTestDbs.getUserIdentityService();
        scmAggService = new ScmAggService(dataSource, userIdentityService);
        scmCommitPullRequestMappingDBService = new ScmCommitPullRequestMappingDBService(dataSource);
        scmCommitPRMappingService = new ScmCommitPRMappingService(dataSource, scmCommitPullRequestMappingDBService, 100);
        ciCdMetadataConditionBuilder = new CiCdMetadataConditionBuilder();
        ciCdPartialMatchConditionBuilder = new CiCdPartialMatchConditionBuilder();
        velocityAggsDatabaseService = new VelocityAggsDatabaseService(dataSource,jiraTestDbs.getJiraConditionsBuilder(),scmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        ciCdJobsDatabaseService = new CiCdJobsDatabaseService(dataSource);
        teamMembersDatabaseService = new TeamMembersDatabaseService(dataSource, m);
        ciCdInstancesDatabaseService = new CiCdInstancesDatabaseService(dataSource);
        jobRunsDatabaseService =  new CiCdJobRunsDatabaseService(DefaultObjectMapper.get(), dataSource);
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
                workItemsResponseTimeReportService, null, null, null, null, null,
                workItemsFirstAssigneeReportService, workItemFieldsMetaService, null);

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

        velocityAggsDatabaseService.ensureTableExistence(company);
        velocityConfigsDatabaseService.ensureTableExistence(company);
        repositoryService.ensureTableExistence(company);
        jiraFieldService.ensureTableExistence(company);
        teamMembersDatabaseService.ensureTableExistence(company);
        integId = integrationService.insert(company, Integration.builder()
                .application("github")
                .name("github test")
                .status("enabled")
                .build());
        integId1 = integrationService.insert(company, Integration.builder()
                .application("jira")
                .name("jira test")
                .status("enabled")
                .build());
        currentTime = DateUtils.truncate(new Date(Instant.parse("2021-05-05T15:00:00-08:00").getEpochSecond()), Calendar.DATE);

        String input = ResourceUtils.getResourceAsString("json/databases/jirausers_aug12.json");//account_id, displayname, in bean: name server.
        PaginatedResponse<JiraUser> jiraUsers = m.readValue(input,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, JiraUser.class));
        jiraUsers.getResponse().getRecords().forEach(user -> {
            DbJiraUser tmp = DbJiraUser.fromJiraUser(user, "1");
            jiraIssueService.insertJiraUser(company, tmp);
            if (user.getDisplayName() != null) {
                try {
                    userIdentityService.batchUpsert(company,
                            List.of(DbScmUser.builder()
                                    .integrationId("1")
                                    .cloudId(user.getAccountId())
                                    .displayName(user.getDisplayName())
                                    .originalDisplayName(user.getDisplayName())
                                    .build()));
                } catch (SQLException throwables) {
                    System.out.println("Failed to insert into integration_users with display name: " + user.getDisplayName() + " , company: " + company + ", integration id:" + "1");
                }
            }
        });

        String jiraIn = ResourceUtils.getResourceAsString("json/databases/jiraissues_jun62020.json");
        PaginatedResponse<JiraIssue> jissues = m.readValue(jiraIn,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, JiraIssue.class));
        jissues.getResponse().getRecords().forEach(issue -> {
            try {
                DbJiraIssue tmp = JiraIssueParser.parseJiraIssue(issue, integId, currentTime,JiraIssueParser.JiraParserConfig.builder()
                        .epicLinkField("customfield_10014")
                        .storyPointsField("customfield_10030")
                        .build());
                tmp = DatabaseTestUtils.populateDbJiraIssueUserIds(dataSource, company, "1", issue, tmp);
                if (jiraIssueService.get(company, tmp.getKey(), tmp.getIntegrationId(), tmp.getIngestedAt()).isPresent())
                    throw new RuntimeException("This issue shouldnt exist.");

                jiraIssueService.insert(company, tmp);
                if (jiraIssueService.get(company, tmp.getKey(), tmp.getIntegrationId(), tmp.getIngestedAt()).isEmpty())
                    throw new RuntimeException("This issue should exist.");
                if (jiraIssueService.get(company, tmp.getKey(), tmp.getIntegrationId(),
                        tmp.getIngestedAt() - 86400L).isPresent())
                    throw new RuntimeException("This issue shouldnt exist.");
            } catch (SQLException throwable) {
                throwable.printStackTrace();
            }
        });
        integrationTrackingService.upsert(company, IntegrationTracker.builder().integrationId(integId).latestIngestedAt(1584000L).build());

        input = ResourceUtils.getResourceAsString("json/databases/githubprs.json");
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
                            if(tmp.getMergeSha().equals("6ca5a1062809e836a0e8ac9dbc1b54eb7648eae6")) {
                                DbScmPullRequest.DbScmPullRequestBuilder tmp1 = tmp.toBuilder();
                                DbScmPullRequest tmp2 = tmp1.mergeSha("decf002f4fddc9ea62bfffabdbea74ac84b19467").build();
                                scmAggService.insert(company, tmp2);
                            }
                            else
                                scmAggService.insert(company, tmp);
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
                                tmp.getIntegrationId()).isEmpty())
                            scmAggService.insertIssue(company, tmp);
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
                                CICDJob.CICDJobBuilder bldr = CICDJob.builder().jobName("job"+tmp.getId()).projectName("project-").jobFullName("branches" )
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

        scmCommitPRMappingService.persistScmCommitPRMapping(company, null);
    }

    @Test
    public void test() throws BadRequestException {
        DbListResponse<List<DbAggregationResult>> result = velocityAggsDatabaseService.calculateVelocityWithoutStacks(company,
                VelocityConfigDTO.builder().build(),
                VelocityFilter.builder().calculation(VelocityFilter.CALCULATION.ticket_velocity).across(VelocityFilter.DISTINCT.velocity).page(0).pageSize(100).build(),
                WORK_ITEM_TYPE, JiraIssuesFilter.builder().integrationIds(List.of(integId,integId1)).projects(List.of("LEV")).excludeLabels(List.of("label1")).build(),
                null, WORKITEMS_MILESTONE_FILTER, ScmPrFilter.builder().integrationIds(List.of(integId,integId1)).build(),
                ScmCommitFilter.builder().integrationIds(List.of(integId,integId1)).build(), CiCdJobRunsFilter.builder().build(), WORKITEM_CUSTOM_FIELDS, null, false);
        Assert.assertNotNull(result);
        Assert.assertEquals(11, result.getCount().intValue());

        DbListResponse<List<DbAggregationResult>> result1 = velocityAggsDatabaseService.calculateVelocityWithoutStacks(company,
                VelocityConfigDTO.builder().build(),
                VelocityFilter.builder().calculation(VelocityFilter.CALCULATION.ticket_velocity).across(VelocityFilter.DISTINCT.velocity).page(0).pageSize(100).build(),
                WORK_ITEM_TYPE, JiraIssuesFilter.builder().partialMatch(Map.of("labels", Map.of("$contains", "Result"))).integrationIds(List.of(integId)).build(),
                null, WORKITEMS_MILESTONE_FILTER, ScmPrFilter.builder().integrationIds(List.of(integId1)).build(),
                ScmCommitFilter.builder().integrationIds(List.of(integId1)).build(),CiCdJobRunsFilter.builder().build(), WORKITEM_CUSTOM_FIELDS, null, false);
        Assert.assertNotNull(result1);
        Assert.assertEquals(0, result1.getCount().intValue());

        DbListResponse<List<DbAggregationResult>> result2 = velocityAggsDatabaseService.calculateVelocityWithoutStacks(company,
                VelocityConfigDTO.builder().build(),
                VelocityFilter.builder().calculation(VelocityFilter.CALCULATION.ticket_velocity).across(VelocityFilter.DISTINCT.velocity).page(0).pageSize(100).build(),
                WORK_ITEM_TYPE, JiraIssuesFilter.builder().integrationIds(List.of(integId)).build(),
                null, WORKITEMS_MILESTONE_FILTER, ScmPrFilter.builder().integrationIds(List.of(integId)).partialMatch(Map.of("message", Map.of("$contains", "Fix"))).build(),
                ScmCommitFilter.builder().integrationIds(List.of(integId)).build(), CiCdJobRunsFilter.builder().build(), WORKITEM_CUSTOM_FIELDS, null, false);
        Assert.assertNotNull(result2);
        Assert.assertEquals(20, result2.getCount().intValue());

        DbListResponse<List<DbAggregationResult>> result3 = velocityAggsDatabaseService.calculateVelocityWithoutStacks(company,
                VelocityConfigDTO.builder().build(),
                VelocityFilter.builder().calculation(VelocityFilter.CALCULATION.ticket_velocity).across(VelocityFilter.DISTINCT.velocity).page(0).pageSize(100).build(),
                WORK_ITEM_TYPE, JiraIssuesFilter.builder().partialMatch(Map.of("summary", Map.of("$contains", "Check"))).integrationIds(List.of(integId,integId1)).build(),
                null, WORKITEMS_MILESTONE_FILTER, ScmPrFilter.builder().integrationIds(List.of(integId1,integId)).build(),
                ScmCommitFilter.builder().integrationIds(List.of(integId1,integId)).build(), CiCdJobRunsFilter.builder().build(), WORKITEM_CUSTOM_FIELDS, null, false);
        Assert.assertNotNull(result3);
        Assert.assertEquals(1, result3.getCount().intValue());

        DbListResponse<List<DbAggregationResult>> result4 = velocityAggsDatabaseService.calculateVelocityWithoutStacks(company,
                VelocityConfigDTO.builder().build(),
                VelocityFilter.builder().calculation(VelocityFilter.CALCULATION.ticket_velocity).across(VelocityFilter.DISTINCT.velocity).page(0).pageSize(100).build(),
                WORK_ITEM_TYPE, JiraIssuesFilter.builder().integrationIds(List.of(integId,integId1))
                        .assignees(List.of(userIdOf("Meghana Dwarakanath").isPresent() ? userIdOf("Meghana Dwarakanath").get() : ""))
                        .build(),
                null, WORKITEMS_MILESTONE_FILTER, ScmPrFilter.builder().integrationIds(List.of(integId1,integId)).build(),
                ScmCommitFilter.builder().integrationIds(List.of(integId1,integId)).build(), CiCdJobRunsFilter.builder().build(), WORKITEM_CUSTOM_FIELDS, null, false);
        Assert.assertNotNull(result4);
        Assert.assertEquals(8, result4.getCount().intValue());

        DbListResponse<List<DbAggregationResult>> result5 = velocityAggsDatabaseService.calculateVelocityWithoutStacks(company,
                VelocityConfigDTO.builder().build(),
                VelocityFilter.builder().calculation(VelocityFilter.CALCULATION.ticket_velocity).across(VelocityFilter.DISTINCT.velocity).page(0).pageSize(100).build(),
                WORK_ITEM_TYPE, JiraIssuesFilter.builder().integrationIds(List.of(integId,integId1)).excludeFixVersions(List.of("v1")).excludeProjects(List.of("TS")).build(),
                null, WORKITEMS_MILESTONE_FILTER, ScmPrFilter.builder().integrationIds(List.of(integId1,integId)).build(),
                ScmCommitFilter.builder().integrationIds(List.of(integId1,integId)).build(), CiCdJobRunsFilter.builder().build(), WORKITEM_CUSTOM_FIELDS, null, false);
        Assert.assertNotNull(result5);
        Assert.assertEquals(19, result5.getCount().intValue());

        DbListResponse<List<DbAggregationResult>> result6 = velocityAggsDatabaseService.calculateVelocityWithoutStacks(company,
                VelocityConfigDTO.builder().build(),
                VelocityFilter.builder().calculation(VelocityFilter.CALCULATION.ticket_velocity).across(VelocityFilter.DISTINCT.velocity).page(0).pageSize(100).build(),
                WORK_ITEM_TYPE, JiraIssuesFilter.builder().integrationIds(List.of(integId,integId1)).issueCreatedRange(ImmutablePair.of(1590354205L,1590354209L))
                        .age(ImmutablePair.of(1590354205L,1590354209L)).build(),
                null, WORKITEMS_MILESTONE_FILTER, ScmPrFilter.builder().integrationIds(List.of(integId1,integId)).build(),
                ScmCommitFilter.builder().integrationIds(List.of(integId1,integId)).build(), CiCdJobRunsFilter.builder().build(), WORKITEM_CUSTOM_FIELDS, null, false);
        Assert.assertNotNull(result6);
        Assert.assertEquals(0, result6.getCount().intValue());

        DbListResponse<List<DbAggregationResult>> result7 = velocityAggsDatabaseService.calculateVelocityWithoutStacks(company,
                VelocityConfigDTO.builder().build(),
                VelocityFilter.builder().calculation(VelocityFilter.CALCULATION.ticket_velocity).across(VelocityFilter.DISTINCT.velocity).page(0).pageSize(100).build(),
                WORK_ITEM_TYPE, JiraIssuesFilter.builder().integrationIds(List.of(integId,integId1))
                        .missingFields(Map.of("priority",true,"version",true,"project",false,"customfield_10001",true)).build(),
                null, WORKITEMS_MILESTONE_FILTER, ScmPrFilter.builder().integrationIds(List.of(integId1,integId)).build(),
                ScmCommitFilter.builder().integrationIds(List.of(integId1,integId)).build(), CiCdJobRunsFilter.builder().build(), WORKITEM_CUSTOM_FIELDS, null, false);
        Assert.assertNotNull(result7);
        Assert.assertEquals(0, result7.getCount().intValue());

        DbListResponse<List<DbAggregationResult>> result8 = velocityAggsDatabaseService.calculateVelocityWithoutStacks(company,
                VelocityConfigDTO.builder().build(),
                VelocityFilter.builder().calculation(VelocityFilter.CALCULATION.ticket_velocity).across(VelocityFilter.DISTINCT.velocity).page(0).pageSize(100).build(),
                WORK_ITEM_TYPE, JiraIssuesFilter.builder().integrationIds(List.of(integId,integId1)).customFields(Map.of("customfield_10001",List.of("magic1","magic2")))
                        .extraCriteria(List.of(JiraIssuesFilter.EXTRA_CRITERIA.no_assignee)).build(),
                null, WORKITEMS_MILESTONE_FILTER, ScmPrFilter.builder().integrationIds(List.of(integId1,integId)).build(),
                ScmCommitFilter.builder().integrationIds(List.of(integId1,integId)).build(), CiCdJobRunsFilter.builder().build(), WORKITEM_CUSTOM_FIELDS, null, false);
        Assert.assertNotNull(result8);
        Assert.assertEquals(0, result8.getCount().intValue());

        DbListResponse<List<DbAggregationResult>> result9 = velocityAggsDatabaseService.calculateVelocityWithoutStacks(company,
                VelocityConfigDTO.builder().build(),
                VelocityFilter.builder().calculation(VelocityFilter.CALCULATION.ticket_velocity).across(VelocityFilter.DISTINCT.velocity).page(0).pageSize(100).build(),
                WORK_ITEM_TYPE, JiraIssuesFilter.builder().integrationIds(List.of(integId,integId1)).build(),
                null, WORKITEMS_MILESTONE_FILTER, ScmPrFilter.builder().integrationIds(List.of(integId1,integId)).sourceBranches(List.of("jen")).build(),
                ScmCommitFilter.builder().integrationIds(List.of(integId1,integId)).build(), CiCdJobRunsFilter.builder().build(), WORKITEM_CUSTOM_FIELDS, null, false);
        Assert.assertNotNull(result9);
        Assert.assertEquals(0, result9.getCount().intValue());

        DbListResponse<List<DbAggregationResult>> result10 = velocityAggsDatabaseService.calculateVelocityWithoutStacks(company,
                VelocityConfigDTO.builder().build(),
                VelocityFilter.builder().calculation(VelocityFilter.CALCULATION.ticket_velocity).across(VelocityFilter.DISTINCT.velocity).page(0).pageSize(100).build(),
                WORK_ITEM_TYPE, JiraIssuesFilter.builder().integrationIds(List.of(integId,integId1)).build(),
                null, WORKITEMS_MILESTONE_FILTER, ScmPrFilter.builder().integrationIds(List.of(integId1,integId)).prMergedRange(ImmutablePair.of(1590354205L,1590354209L)).build(),
                ScmCommitFilter.builder().integrationIds(List.of(integId1,integId)).build(), CiCdJobRunsFilter.builder().build(), WORKITEM_CUSTOM_FIELDS, null, false);
        Assert.assertNotNull(result10);
        Assert.assertEquals(0, result10.getCount().intValue());

        DbListResponse<List<DbAggregationResult>> result11 = velocityAggsDatabaseService.calculateVelocityWithoutStacks(company,
                VelocityConfigDTO.builder().build(),
                VelocityFilter.builder().calculation(VelocityFilter.CALCULATION.ticket_velocity).across(VelocityFilter.DISTINCT.velocity).page(0).pageSize(100).build(),
                WORK_ITEM_TYPE, JiraIssuesFilter.builder().integrationIds(List.of(integId,integId1)).build(),
                null, WORKITEMS_MILESTONE_FILTER, ScmPrFilter.builder().integrationIds(List.of(integId1,integId)).partialMatch(Map.of("project",Map.of("$begins","lev"))).build(),
                ScmCommitFilter.builder().integrationIds(List.of(integId1,integId)).build(), CiCdJobRunsFilter.builder().build(), WORKITEM_CUSTOM_FIELDS, null, false);
        Assert.assertNotNull(result11);
        Assert.assertEquals(0, result11.getCount().intValue());

        DbListResponse<List<DbAggregationResult>> result12 = velocityAggsDatabaseService.calculateVelocityWithoutStacks(company,
                VelocityConfigDTO.builder().build(),
                VelocityFilter.builder().calculation(VelocityFilter.CALCULATION.pr_velocity).across(VelocityFilter.DISTINCT.velocity).page(0).pageSize(100).build(),
                WORK_ITEM_TYPE, JiraIssuesFilter.builder().integrationIds(List.of(integId, integId1)).build(),
                null, WORKITEMS_MILESTONE_FILTER, ScmPrFilter.builder().integrationIds(List.of(integId, integId1))
                        .reviewers(List.of(
                                userIdentityService.getUser(company, integId, "ivan-levelops"),
                                userIdentityService.getUser(company, integId, "viraj-levelops"))).isApplyOuOnVelocityReport(false).build(),
                ScmCommitFilter.builder().build(), CiCdJobRunsFilter.builder().build(), WORKITEM_CUSTOM_FIELDS, null, false);
        Assert.assertNotNull(result12);
        Assert.assertEquals(0, result12.getCount().intValue());

        DbListResponse<List<DbAggregationResult>> result13 = velocityAggsDatabaseService.calculateVelocityWithoutStacks(company,
                VelocityConfigDTO.builder().build(),
                VelocityFilter.builder().calculation(VelocityFilter.CALCULATION.pr_velocity).across(VelocityFilter.DISTINCT.velocity).page(0).pageSize(100).build(),
                WORK_ITEM_TYPE, JiraIssuesFilter.builder().integrationIds(List.of(integId, integId1)).build(),
                null, WORKITEMS_MILESTONE_FILTER,
                ScmPrFilter.builder()
                        .integrationIds(List.of(integId, integId1))
                        .excludeReviewers(List.of(userIdentityService.getUser(company, integId, "viraj-levelops")))
                        .build(),
                ScmCommitFilter.builder().build(), CiCdJobRunsFilter.builder().build(), WORKITEM_CUSTOM_FIELDS, null, false);
        Assert.assertNotNull(result13);
        Assert.assertEquals(0, result13.getCount().intValue());

        DbListResponse<List<DbAggregationResult>> result14 = velocityAggsDatabaseService.calculateVelocityWithoutStacks(company,
                VelocityConfigDTO.builder().build(),
                VelocityFilter.builder().calculation(VelocityFilter.CALCULATION.pr_velocity).across(VelocityFilter.DISTINCT.velocity).page(0).pageSize(100).build(),
                WORK_ITEM_TYPE, JiraIssuesFilter.builder().integrationIds(List.of(integId, integId1)).build(),
                null, WORKITEMS_MILESTONE_FILTER, ScmPrFilter.builder().integrationIds(List.of(integId, integId1))
                        .approvers(List.of(userIdentityService.getUser(company, integId, "ivan-levelops"),
                                userIdentityService.getUser(company, integId, "viraj-levelops"))).isApplyOuOnVelocityReport(false).build(),
                ScmCommitFilter.builder().build(), CiCdJobRunsFilter.builder().build(), WORKITEM_CUSTOM_FIELDS, null, false);
        Assert.assertNotNull(result14);
        Assert.assertEquals(0, result14.getCount().intValue());

        DbListResponse<List<DbAggregationResult>> result15 = velocityAggsDatabaseService.calculateVelocityWithoutStacks(company,
                VelocityConfigDTO.builder().build(),
                VelocityFilter.builder().calculation(VelocityFilter.CALCULATION.pr_velocity).across(VelocityFilter.DISTINCT.velocity).page(0).pageSize(100).build(),
                WORK_ITEM_TYPE, JiraIssuesFilter.builder().integrationIds(List.of(integId, integId1)).build(),
                null, WORKITEMS_MILESTONE_FILTER, ScmPrFilter.builder().integrationIds(List.of(integId, integId1))
                        .excludeApprovers(List.of(
                                userIdentityService.getUser(company, integId, "viraj-levelops"))).build(),
                ScmCommitFilter.builder().build(), CiCdJobRunsFilter.builder().build(), WORKITEM_CUSTOM_FIELDS, null, false);
        Assert.assertNotNull(result15);
        Assert.assertEquals(0, result15.getCount().intValue());

        DbListResponse<List<DbAggregationResult>> result16 = velocityAggsDatabaseService.calculateVelocityWithoutStacks(company,
                VelocityConfigDTO.builder().build(),
                VelocityFilter.builder().calculation(VelocityFilter.CALCULATION.pr_velocity).across(VelocityFilter.DISTINCT.velocity).page(0).pageSize(100).build(),
                WORK_ITEM_TYPE, JiraIssuesFilter.builder().integrationIds(List.of(integId, integId1)).build(),
                null, WORKITEMS_MILESTONE_FILTER, ScmPrFilter.builder().integrationIds(List.of(integId, integId1))
                        .assignees(List.of(
                                userIdentityService.getUser(company, integId, "ivan-levelops"),
                                userIdentityService.getUser(company, integId, "viraj-levelops"))).build(),
                ScmCommitFilter.builder().build(), CiCdJobRunsFilter.builder().build(), WORKITEM_CUSTOM_FIELDS, null, false);
        Assert.assertNotNull(result16);
        Assert.assertEquals(2, result16.getCount().intValue());

        DbListResponse<List<DbAggregationResult>> result17 = velocityAggsDatabaseService.calculateVelocityWithoutStacks(company,
                VelocityConfigDTO.builder().build(),
                VelocityFilter.builder().calculation(VelocityFilter.CALCULATION.pr_velocity).across(VelocityFilter.DISTINCT.velocity).page(0).pageSize(100).build(),
                WORK_ITEM_TYPE, JiraIssuesFilter.builder().integrationIds(List.of(integId, integId1)).build(),
                null, WORKITEMS_MILESTONE_FILTER, ScmPrFilter.builder().integrationIds(List.of(integId, integId1))
                        .excludeAssignees(List.of(
                                userIdentityService.getUser(company, integId, "ivan-levelops"),
                                userIdentityService.getUser(company, integId, "viraj-levelops"))).build(),
                ScmCommitFilter.builder().build(), CiCdJobRunsFilter.builder().build(), WORKITEM_CUSTOM_FIELDS, null, false);
        Assert.assertNotNull(result17);
        Assert.assertEquals(8, result17.getCount().intValue());

        DbListResponse<List<DbAggregationResult>> result18 = velocityAggsDatabaseService.calculateVelocityWithoutStacks(company,
                VelocityConfigDTO.builder().build(),
                VelocityFilter.builder().calculation(VelocityFilter.CALCULATION.pr_velocity).across(VelocityFilter.DISTINCT.velocity).page(0).pageSize(100).build(),
                WORK_ITEM_TYPE, JiraIssuesFilter.builder().integrationIds(List.of(integId,integId1)).build(),
                null, WORKITEMS_MILESTONE_FILTER, ScmPrFilter.builder().integrationIds(List.of(integId,integId1)).build(),
                ScmCommitFilter.builder().integrationIds(List.of(integId,integId1)).committedAtRange(ImmutablePair.of(0L, 1651820345000L)).build(),
                CiCdJobRunsFilter.builder().build(), WORKITEM_CUSTOM_FIELDS, null, false);
        Assert.assertNotNull(result18);
        Assert.assertEquals(0, result18.getCount().intValue());

        result18 = velocityAggsDatabaseService.calculateVelocityWithoutStacks(company,
                VelocityConfigDTO.builder().build(),
                VelocityFilter.builder().calculation(VelocityFilter.CALCULATION.pr_velocity).across(VelocityFilter.DISTINCT.velocity).page(0).pageSize(100).build(),
                WORK_ITEM_TYPE, JiraIssuesFilter.builder().integrationIds(List.of(integId,integId1)).build(),
                null, WORKITEMS_MILESTONE_FILTER, ScmPrFilter.builder().integrationIds(List.of(integId,integId1)).build(),
                ScmCommitFilter.builder().committers(List.of(UUID.randomUUID().toString(), UUID.randomUUID().toString())).isApplyOuOnVelocityReport(false)
                        .integrationIds(List.of(integId,integId1)).committedAtRange(ImmutablePair.of(0L, 1651820345000L)).build(),
                CiCdJobRunsFilter.builder().build(), WORKITEM_CUSTOM_FIELDS, null, false);
        Assert.assertNotNull(result18);
        Assert.assertEquals(0, result18.getCount().intValue());

        result18 = velocityAggsDatabaseService.calculateVelocityWithoutStacks(company,
                VelocityConfigDTO.builder().build(),
                VelocityFilter.builder().calculation(VelocityFilter.CALCULATION.pr_velocity).across(VelocityFilter.DISTINCT.velocity).page(0).pageSize(100).build(),
                WORK_ITEM_TYPE, JiraIssuesFilter.builder().integrationIds(List.of(integId,integId1)).build(),
                null, WORKITEMS_MILESTONE_FILTER, ScmPrFilter.builder().integrationIds(List.of(integId,integId1)).build(),
                ScmCommitFilter.builder().authors(List.of(UUID.randomUUID().toString(), UUID.randomUUID().toString())).isApplyOuOnVelocityReport(false)
                        .integrationIds(List.of(integId,integId1)).committedAtRange(ImmutablePair.of(0L, 1651820345000L)).build(),
                CiCdJobRunsFilter.builder().build(), WORKITEM_CUSTOM_FIELDS, null, false);
        Assert.assertNotNull(result18);
        Assert.assertEquals(0, result18.getCount().intValue());
        DbListResponse<List<DbAggregationResult>> result19 = velocityAggsDatabaseService.calculateVelocityWithoutStacks(company,
                VelocityConfigDTO.builder().build(),
                VelocityFilter.builder().calculation(VelocityFilter.CALCULATION.pr_velocity).across(VelocityFilter.DISTINCT.velocity).page(0).pageSize(100).build(),
                WORK_ITEM_TYPE, JiraIssuesFilter.builder().integrationIds(List.of(integId, integId1)).build(),
                null, WORKITEMS_MILESTONE_FILTER, ScmPrFilter.builder().integrationIds(List.of(integId, integId1))
                        .approvers(List.of(userIdentityService.getUser(company, integId, "ivan-levelops"),
                                userIdentityService.getUser(company, integId, "viraj-levelops"))).isApplyOuOnVelocityReport(true).build(),
                ScmCommitFilter.builder().build(), CiCdJobRunsFilter.builder().build(), WORKITEM_CUSTOM_FIELDS, null, false);
        Assert.assertNotNull(result19);
        Assert.assertNotEquals(0, result19.getCount().intValue());
    }

    @Test
    public void testWithJiraAndWorkItemFilter() throws BadRequestException {
        DbListResponse<List<DbAggregationResult>> result = velocityAggsDatabaseService.calculateVelocityWithoutStacks(company,
                VelocityConfigDTO.builder().build(),
                VelocityFilter.builder().calculation(VelocityFilter.CALCULATION.ticket_velocity).across(VelocityFilter.DISTINCT.trend).page(0).pageSize(100).build(),
                WorkItemsType.WORK_ITEM, JiraIssuesFilter.builder().projects(List.of("test")).integrationIds(List.of(integId)).build(),
                WorkItemsFilter.builder().projects(List.of("test")).build(), WORKITEMS_MILESTONE_FILTER, ScmPrFilter.builder().integrationIds(List.of(integId1)).build(),
                ScmCommitFilter.builder().integrationIds(List.of(integId1)).build(), CiCdJobRunsFilter.builder().build(), WORKITEM_CUSTOM_FIELDS, null, false);

        Assert.assertNotNull(result);
        Assert.assertEquals(0, result.getCount().intValue());
    }
}
