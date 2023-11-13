package io.levelops.aggregations.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.CICDInstance;
import io.levelops.commons.databases.models.database.CICDJob;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.cicd.JobRunSegment;
import io.levelops.commons.databases.models.database.cicd.JobRunStage;
import io.levelops.commons.databases.models.filters.CICD_TYPE;
import io.levelops.commons.databases.models.filters.CiCdJobRunsFilter;
import io.levelops.commons.databases.models.filters.CiCdPipelineJobRunsFilter;
import io.levelops.commons.databases.services.CiCdAggsService;
import io.levelops.commons.databases.services.CiCdInstancesDatabaseService;
import io.levelops.commons.databases.services.CiCdJobRunStageDatabaseService;
import io.levelops.commons.databases.services.CiCdJobRunStageStepsDatabaseService;
import io.levelops.commons.databases.services.CiCdJobRunTestDatabaseService;
import io.levelops.commons.databases.services.CiCdJobRunsDatabaseService;
import io.levelops.commons.databases.services.CiCdJobsDatabaseService;
import io.levelops.commons.databases.services.CiCdPipelinesAggsService;
import io.levelops.commons.databases.services.DatabaseSchemaService;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.TriageRuleHitsService;
import io.levelops.commons.databases.services.TriageRulesService;
import io.levelops.commons.databases.services.UserIdentityService;
import io.levelops.commons.databases.services.UserService;
import io.levelops.commons.databases.services.organization.ProductsDatabaseService;
import io.levelops.commons.databases.services.organization.TeamMembersDatabaseService;
import io.levelops.commons.databases.services.organization.TeamsDatabaseService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.gitlab.models.GitlabPipeline;
import io.levelops.integrations.gitlab.models.GitlabProject;
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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.models.filters.CiCdPipelineJobRunsFilter.DISTINCT.job_name;

public class GitlabPipelineServiceTest {

    private static final String COMPANY = "gitlabTest";
    private static final String INTEGRATION_ID = "1";
    private static CiCdPipelinesAggsService ciCdPipelinesAggsService;
    private static GitlabPipelineService gitlabPipelineService;
    private static final ObjectMapper OBJECT_MAPPER = DefaultObjectMapper.get();
    private final static Map<String, SortingOrder> DEFAULT_SORT_BY = Collections.emptyMap();
    private static UUID instanceId;
    private static ProductsDatabaseService productsDatabaseService;
    private static UserIdentityService userIdentityService;
    private static TeamMembersDatabaseService teamMembersDatabaseService;
    private static TeamsDatabaseService teamsDatabaseService;
    private static CiCdJobsDatabaseService ciCdJobsDatabaseService;
    private static CiCdJobRunStageDatabaseService ciCdJobRunStageDatabaseService;
    private static TriageRulesService rulesService;
    private static TriageRuleHitsService ruleHitsService;
    private static CiCdAggsService ciCdAggsService;

    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;
    private static Date currentTime;

    @BeforeClass
    public static void setup() throws IOException, SQLException {
        if (dataSource != null)
            return;
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        ciCdJobsDatabaseService = new CiCdJobsDatabaseService(dataSource);
        CiCdJobRunsDatabaseService ciCdJobRunsDatabaseService = new CiCdJobRunsDatabaseService(OBJECT_MAPPER, dataSource);
        ciCdPipelinesAggsService = new CiCdPipelinesAggsService(dataSource, ciCdJobRunsDatabaseService);
        CiCdInstancesDatabaseService ciCdInstancesDatabaseService = new CiCdInstancesDatabaseService(dataSource);
        userIdentityService = new UserIdentityService(dataSource);
        teamMembersDatabaseService = new TeamMembersDatabaseService(dataSource, OBJECT_MAPPER);
        teamsDatabaseService = new TeamsDatabaseService(dataSource, OBJECT_MAPPER);
        ciCdJobRunStageDatabaseService = new CiCdJobRunStageDatabaseService(dataSource, OBJECT_MAPPER);
        CiCdJobRunStageStepsDatabaseService ciCdJobRunStageStepsDatabaseService = new CiCdJobRunStageStepsDatabaseService(dataSource);
        CiCdJobRunTestDatabaseService ciCdJobRunTestDatabaseService = new CiCdJobRunTestDatabaseService(dataSource);
        rulesService = new TriageRulesService(dataSource);
        ruleHitsService = new TriageRuleHitsService(dataSource, OBJECT_MAPPER);
        gitlabPipelineService = new GitlabPipelineService(dataSource, ciCdJobsDatabaseService,
                ciCdJobRunsDatabaseService, ciCdJobRunStageDatabaseService,
                ciCdJobRunStageStepsDatabaseService, ciCdJobRunTestDatabaseService);
        UserService userService = new UserService(dataSource, OBJECT_MAPPER);
        IntegrationService integrationService = new IntegrationService(dataSource);
        ciCdAggsService = new CiCdAggsService(dataSource);
        new DatabaseSchemaService(dataSource).ensureSchemaExistence(COMPANY);
        integrationService.ensureTableExistence(COMPANY);
        integrationService.insert(COMPANY, Integration.builder()
                .id("1")
                .application("gitlab")
                .name("gitlab-integ")
                .status("enabled")
                .build());
        userIdentityService.ensureTableExistence(COMPANY);
        teamMembersDatabaseService.ensureTableExistence(COMPANY);
        teamsDatabaseService.ensureTableExistence(COMPANY);
        productsDatabaseService = new ProductsDatabaseService(dataSource, OBJECT_MAPPER);
        productsDatabaseService.ensureTableExistence(COMPANY);
        currentTime = DateUtils.truncate(new Date(), Calendar.DATE);
        userService.ensureTableExistence(COMPANY);
        ciCdPipelinesAggsService.ensureTableExistence(COMPANY);
        ciCdInstancesDatabaseService.ensureTableExistence(COMPANY);
        ciCdJobsDatabaseService.ensureTableExistence(COMPANY);
        ciCdJobRunsDatabaseService.ensureTableExistence(COMPANY);
        ciCdJobRunStageDatabaseService.ensureTableExistence(COMPANY);
        ciCdJobRunStageStepsDatabaseService.ensureTableExistence(COMPANY);
        ciCdJobRunTestDatabaseService.ensureTableExistence(COMPANY);
        rulesService.ensureTableExistence(COMPANY);
        ruleHitsService.ensureTableExistence(COMPANY);
        instanceId = UUID.randomUUID();
        ciCdInstancesDatabaseService.insert(COMPANY, CICDInstance.builder()
                .id(instanceId)
                .integrationId("1")
                .name("gitlab-integration")
                .type(CICD_TYPE.gitlab.toString())
                .build());
        String resourcePath = "gitlab/gitlab_pipelines.json";
        String projectsInput = ResourceUtils.getResourceAsString(resourcePath);
        PaginatedResponse<GitlabProject> projects = OBJECT_MAPPER.readValue(projectsInput, OBJECT_MAPPER.getTypeFactory()
                .constructParametricType(PaginatedResponse.class, GitlabProject.class));
        GitlabProject gitlabProject = projects.getResponse().getRecords().get(0);
        GitlabPipeline gitlabPipeline = gitlabProject.getPipelines().get(0);
        String id = gitlabPipelineService.insert(COMPANY, INTEGRATION_ID, instanceId, gitlabPipeline);
        if (id == null) {
            throw new RuntimeException("Failed to insert pipeline with id" + gitlabPipeline.getPipelineId());
        }
    }

    @Test
    public void test() throws SQLException {
        var result = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(COMPANY,
                CiCdPipelineJobRunsFilter.builder()
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.duration)
                        .across(job_name)
                        .build(), false, null);
        Assert.assertEquals(result.getRecords().size(), 1);

        var listResponse = ciCdPipelinesAggsService.listCiCdJobRuns(COMPANY, CiCdPipelineJobRunsFilter.builder()
                .calculation(CiCdPipelineJobRunsFilter.CALCULATION.duration)
                .across(job_name)
                .build(), DEFAULT_SORT_BY, null, 0, 10);
        Assert.assertEquals("lev-branch", listResponse.getRecords().get(0).getJobName());
        Assert.assertEquals("success", listResponse.getRecords().get(0).getStatus());
        Assert.assertEquals("gitlab-integration", listResponse.getRecords().get(0).getCicdInstanceName());
    }

    @Test
    public void testStageStatusCanceledAndDuration() throws SQLException {
        DbListResponse<JobRunStage> stageList = ciCdJobRunStageDatabaseService.list(COMPANY, 0, 20);

        Assert.assertEquals("PASSED WITH WARNINGS", stageList.getRecords().stream().filter(stage -> "test".equals(stage.getStageId())).map(JobRunSegment::getResult).findFirst().get());
        Assert.assertEquals("CANCELED", stageList.getRecords().stream().filter(stage -> "build".equals(stage.getStageId())).map(JobRunSegment::getResult).findFirst().get());
        Assert.assertEquals("FAILED", stageList.getRecords().stream().filter(stage -> "deploy".equals(stage.getStageId())).map(JobRunSegment::getResult).findFirst().get());
        Assert.assertEquals("UNDEFINED IN SEI", stageList.getRecords().stream().filter(stage -> "lint".equals(stage.getStageId())).map(JobRunSegment::getResult).findFirst().get());
        Assert.assertEquals("PASSED", stageList.getRecords().stream().filter(stage -> "trigger".equals(stage.getStageId())).map(JobRunSegment::getResult).findFirst().get());
        stageList.getRecords().forEach(stage -> {
            Assert.assertTrue(stage.getDuration() > 0);
        });
    }

    @Test
    public void testDrillDownWithStageNameStatusFilter() throws SQLException {
        DbListResponse<CICDJob> cicdJobRuns = ciCdJobsDatabaseService.list(COMPANY, 0, 100);
        List<UUID> jobUUID = cicdJobRuns.getRecords().stream().map(CICDJob::getId).collect(Collectors.toList());
        List<String> jobIds = new ArrayList<>();
        for (UUID id: jobUUID) {
            jobIds.add(id.toString());
        }

        var result = ciCdAggsService.listCiCdJobRunsForDora(COMPANY,
                CiCdJobRunsFilter.builder()
                        .stageNames(List.of("build"))
                        .stageStatuses(List.of("canceled"))
                        .build(), 0, 20, null, jobIds);

        Assert.assertEquals(1, result.getTotalCount().intValue());

        result = ciCdAggsService.listCiCdJobRunsForDora(COMPANY,
                CiCdJobRunsFilter.builder()
                        .stageNames(List.of("test"))
                        .stageStatuses(List.of("PASSED WITH WARNINGS".toLowerCase()))
                        .build(), 0, 20, null, jobIds);

        Assert.assertEquals(1, result.getTotalCount().intValue());

        result = ciCdAggsService.listCiCdJobRunsForDora(COMPANY,
                CiCdJobRunsFilter.builder()
                        .stageNames(List.of("deploy"))
                        .stageStatuses(List.of("FAILED".toLowerCase()))
                        .build(), 0, 20, null, jobIds);

        Assert.assertEquals(1, result.getTotalCount().intValue());

        result = ciCdAggsService.listCiCdJobRunsForDora(COMPANY,
                CiCdJobRunsFilter.builder()
                        .stageNames(List.of("lint"))
                        .stageStatuses(List.of("undefined in SEI"))
                        .build(), 0, 20, null, jobIds);

        Assert.assertEquals(1, result.getTotalCount().intValue());

        result = ciCdAggsService.listCiCdJobRunsForDora(COMPANY,
                CiCdJobRunsFilter.builder()
                        .stageNames(List.of("trigger"))
                        .stageStatuses(List.of("PASSED".toLowerCase()))
                        .build(), 0, 20, null, jobIds);

        Assert.assertEquals(1, result.getTotalCount().intValue());

        result = ciCdAggsService.listCiCdJobRunsForDora(COMPANY,
                CiCdJobRunsFilter.builder()
                        .stageNames(List.of("trigger"))
                        .stageStatuses(List.of("FAILED".toLowerCase()))
                        .build(), 0, 20, null, jobIds);

        Assert.assertEquals(0, result.getTotalCount().intValue());
    }

}

