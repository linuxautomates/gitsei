package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.CICDInstance;
import io.levelops.commons.databases.models.database.CICDJobRun;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.QueryFilter;
import io.levelops.commons.databases.models.database.azuredevops.DbAzureDevopsBuild;
import io.levelops.commons.databases.models.database.azuredevops.DbAzureDevopsProject;
import io.levelops.commons.databases.models.database.cicd.JobRunStage;
import io.levelops.commons.databases.models.database.cicd.JobRunStageStep;
import io.levelops.commons.databases.models.filters.CICD_TYPE;
import io.levelops.commons.databases.models.filters.CiCdJobRunParameter;
import io.levelops.commons.databases.models.filters.CiCdPipelineJobRunsFilter;
import io.levelops.commons.databases.services.organization.ProductsDatabaseService;
import io.levelops.commons.databases.services.organization.TeamMembersDatabaseService;
import io.levelops.commons.databases.services.organization.TeamsDatabaseService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.azureDevops.models.Configuration;
import io.levelops.integrations.azureDevops.models.EnrichedProjectData;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.MapUtils;
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
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.models.filters.CiCdPipelineJobRunsFilter.DISTINCT.job_name;

public class AzureDevopsProjectServiceTest {

    private static final String COMPANY = "test";
    private static final String INTEGRATION_ID = "1";
    private static CiCdPipelinesAggsService ciCdPipelinesAggsService;
    private static AzureDevopsProjectService projectService;
    private static CiCdJobsDatabaseService ciCdJobsDatabaseService;
    private static CiCdJobRunsDatabaseService ciCdJobRunsDatabaseService;
    private static CiCdJobRunStageDatabaseService ciCdJobRunStageDatabaseService;
    private static TriageRuleHitsService triageRuleHitsService;
    private static TriageRulesService triageRulesService;
    private static CiCdJobRunStageStepsDatabaseService ciCdJobRunStageStepsDatabaseService;
    private static final ObjectMapper OBJECT_MAPPER = DefaultObjectMapper.get();
    private static UUID instanceId;
    private static final List<String> cicdJobRuns = new ArrayList<>();
    private static final List<DbAzureDevopsProject> dbPipelineRuns = new ArrayList<>();
    private static ProductsDatabaseService productsDatabaseService;
    private static UserIdentityService userIdentityService;
    private static TeamMembersDatabaseService teamMembersDatabaseService;
    private static TeamsDatabaseService teamsDatabaseService;


    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;
    private static Date currentTime;

    @BeforeClass
    public static void setup() throws SQLException, IOException {
        if (dataSource != null)
            return;
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        ciCdJobsDatabaseService = new CiCdJobsDatabaseService(dataSource);
        triageRulesService = new TriageRulesService(dataSource);
        triageRuleHitsService = new TriageRuleHitsService(dataSource, OBJECT_MAPPER);
        ciCdJobRunsDatabaseService = new CiCdJobRunsDatabaseService(OBJECT_MAPPER, dataSource);
        ciCdJobRunStageDatabaseService = new CiCdJobRunStageDatabaseService(dataSource, OBJECT_MAPPER);
        ciCdJobRunStageStepsDatabaseService = new CiCdJobRunStageStepsDatabaseService(dataSource);
        ciCdPipelinesAggsService = new CiCdPipelinesAggsService(dataSource, ciCdJobRunsDatabaseService);
        CiCdInstancesDatabaseService ciCdInstancesDatabaseService = new CiCdInstancesDatabaseService(dataSource);
        userIdentityService = new UserIdentityService(dataSource);
        projectService = new AzureDevopsProjectService(dataSource, ciCdJobsDatabaseService, ciCdJobRunsDatabaseService,
                ciCdJobRunStageDatabaseService, ciCdJobRunStageStepsDatabaseService, userIdentityService);
        teamMembersDatabaseService = new TeamMembersDatabaseService(dataSource, OBJECT_MAPPER);
        teamsDatabaseService = new TeamsDatabaseService(dataSource, OBJECT_MAPPER);
        UserService userService = new UserService(dataSource, OBJECT_MAPPER);
        IntegrationService integrationService = new IntegrationService(dataSource);
        new DatabaseSchemaService(dataSource).ensureSchemaExistence(COMPANY);
        integrationService.ensureTableExistence(COMPANY);
        integrationService.insert(COMPANY, Integration.builder()
                .id("1")
                .application("azureDevops")
                .name("azure_devops")
                .status("enabled")
                .build());
        productsDatabaseService = new ProductsDatabaseService(dataSource, OBJECT_MAPPER);
        productsDatabaseService.ensureTableExistence(COMPANY);
        userIdentityService.ensureTableExistence(COMPANY);
        teamMembersDatabaseService.ensureTableExistence(COMPANY);
        teamsDatabaseService.ensureTableExistence(COMPANY);
        currentTime = DateUtils.truncate(new Date(), Calendar.DATE);
        userService.ensureTableExistence(COMPANY);
        ciCdPipelinesAggsService.ensureTableExistence(COMPANY);
        ciCdInstancesDatabaseService.ensureTableExistence(COMPANY);
        ciCdJobsDatabaseService.ensureTableExistence(COMPANY);
        ciCdJobRunsDatabaseService.ensureTableExistence(COMPANY);
        ciCdJobRunStageDatabaseService.ensureTableExistence(COMPANY);
        ciCdJobRunStageStepsDatabaseService.ensureTableExistence(COMPANY);
        triageRulesService.ensureTableExistence(COMPANY);
        triageRuleHitsService.ensureTableExistence(COMPANY);
        instanceId = UUID.randomUUID();
        ciCdInstancesDatabaseService.insert(COMPANY, CICDInstance.builder()
                .id(instanceId)
                .integrationId("1")
                .name("azure-integration")
                .type(CICD_TYPE.azure_devops.toString())
                .build());
        String resourcePath = "json/databases/azure_devops_pipeline.json";
        String projectsInput = ResourceUtils.getResourceAsString(resourcePath);
        PaginatedResponse<EnrichedProjectData> pipelines = OBJECT_MAPPER.readValue(projectsInput, OBJECT_MAPPER.getTypeFactory()
                .constructParametricType(PaginatedResponse.class, EnrichedProjectData.class));
        dbPipelineRuns.addAll(pipelines.getResponse().getRecords().stream()
                .map(enrichedProjectData -> DbAzureDevopsProject.fromPipelineRuns(enrichedProjectData, INTEGRATION_ID, "1", currentTime))
                .collect(Collectors.toList()));
        for (DbAzureDevopsProject dbPipelineRun : dbPipelineRuns) {
            cicdJobRuns.add(projectService.insert(COMPANY, instanceId, dbPipelineRun));
        }
    }

    @Test
    public void test() throws SQLException {
        DbListResponse<CICDJobRun> cicdJobRunDbListResponse = ciCdJobRunsDatabaseService.listByFilter(COMPANY, 0, cicdJobRuns.size(), cicdJobRuns.stream().map(UUID::fromString).collect(Collectors.toList()), null,null);
        List<CICDJobRun> jobRuns = cicdJobRunDbListResponse.getRecords();
        Assert.assertNotNull(cicdJobRunDbListResponse);
        Assert.assertEquals(cicdJobRuns.size(), jobRuns.size());

        // Test job run params
        Map<UUID, List<CICDJobRun.JobRunParam>> jobRunParams = ciCdJobRunsDatabaseService.getJobRunParams(COMPANY, cicdJobRuns.stream().map(UUID::fromString).collect(Collectors.toList()));
        List<CICDJobRun.JobRunParam> allJobRunParams = jobRunParams.values().stream().flatMap(Collection::stream).collect(Collectors.toList());
        Assert.assertEquals(6, allJobRunParams.stream().map(CICDJobRun.JobRunParam::getName).filter(Objects::nonNull).count());
        Assert.assertEquals(6, allJobRunParams.stream().map(CICDJobRun.JobRunParam::getValue).filter(Objects::nonNull).count());
        Optional<CICDJobRun> cicdJobRun = jobRuns.stream().filter(run -> run.getJobRunNumber() == 140).findFirst();
        Assert.assertFalse(cicdJobRun.isEmpty());
        UUID cicdJobRunId = cicdJobRun.get().getId();
        List<CICDJobRun.JobRunParam> pipelineVariables = jobRunParams.get(cicdJobRunId);
        Assert.assertNotNull(pipelineVariables);
        Assert.assertTrue(pipelineVariables.size() > 0);
        Assert.assertTrue(pipelineVariables.stream().anyMatch(param -> "demo1/pom1.xml".equals(param.getValue()) && "mavenPOMFile".equals(param.getName())));
        Assert.assertTrue(pipelineVariables.stream().anyMatch(param -> "true".equals(param.getValue()) && "system.debug".equals(param.getName())));

        // Test job run stages and steps
        DbListResponse<JobRunStage> stageDbListResponse = ciCdJobRunStageDatabaseService.list(COMPANY, 0, 3, QueryFilter.builder().strictMatches(Map.of("cicd_job_run_id", List.of(cicdJobRunId.toString()))).build());
        Assert.assertNotNull(stageDbListResponse);
        Assert.assertNotNull(stageDbListResponse.getRecords());
        Assert.assertTrue(stageDbListResponse.getRecords().size() > 0);
        Assert.assertEquals(3, stageDbListResponse.getRecords().size());

        DbListResponse<JobRunStageStep> stepDbListResponse = ciCdJobRunStageStepsDatabaseService.getBatch(COMPANY, 0, 27, null, stageDbListResponse.getRecords().stream().map(JobRunStage::getId).collect(Collectors.toList()), null);
        Assert.assertNotNull(stepDbListResponse);
        Assert.assertNotNull(stepDbListResponse.getRecords());
        Assert.assertTrue(stepDbListResponse.getRecords().size() > 0);
        Assert.assertEquals(27, stepDbListResponse.getRecords().size());

        // Test update logic
        var pipelineRun = dbPipelineRuns.get(0).getPipelineRuns().get(0);
        var variables = pipelineRun.getVariables() != null ? pipelineRun.getVariables() : new HashMap<String, Configuration.Variable>();
        variables.put("CUSTOM_PARAM", Configuration.Variable.builder().value("CUSTOM_VALUE").build());
        DbAzureDevopsProject project = dbPipelineRuns.get(0).toBuilder().pipelineRuns(List.of(pipelineRun.toBuilder().result("CUSTOM_RESULT").variables(variables).build())).build();
        CICDJobRun jobRun = jobRuns.stream().filter(run -> (long)project.getPipelineRuns().get(0).getRunId() == run.getJobRunNumber()).findFirst().get();
        String newInserted = projectService.insert(COMPANY, instanceId, project);
        Assert.assertNotNull(newInserted);
        Assert.assertEquals(jobRun.getId().toString(), newInserted);
        var insertedJobRun = ciCdJobRunsDatabaseService.listByFilter(COMPANY, 0,1, List.of(UUID.fromString(newInserted)), null, null).getRecords().get(0);
        Assert.assertNotNull(insertedJobRun);
        Assert.assertEquals("CUSTOM_RESULT", insertedJobRun.getStatus());
        Assert.assertEquals(ListUtils.emptyIfNull(jobRun.getParams()).size() + 1, insertedJobRun.getParams().size());

        var pipelineRun1 = dbPipelineRuns.get(1).getPipelineRuns().get(0);
        DbAzureDevopsProject project1 = dbPipelineRuns.get(1).toBuilder().pipelineRuns(List.of(pipelineRun1.toBuilder().result("CUSTOM_RESULT").finishedDate(pipelineRun1.getFinishedDate().plusSeconds(86400)).build())).build();
        CICDJobRun jobRun1 = jobRuns.stream().filter(run -> (long)project1.getPipelineRuns().get(0).getRunId() == run.getJobRunNumber()).findFirst().get();
        String newInserted1 = projectService.insert(COMPANY, instanceId, project1);
        Assert.assertNotNull(newInserted1);
        Assert.assertEquals(jobRun1.getId().toString(), newInserted1);
        var insertedJobRun1 = ciCdJobRunsDatabaseService.listByFilter(COMPANY, 0,1, List.of(UUID.fromString(newInserted1)), null, null).getRecords().get(0);
        Assert.assertNotNull(insertedJobRun1);
        Assert.assertEquals("CUSTOM_RESULT", insertedJobRun1.getStatus());

        // Test not update
        var pipelineRun2 = dbPipelineRuns.get(2).getPipelineRuns().get(0);
        DbAzureDevopsProject project2 = dbPipelineRuns.get(2).toBuilder().pipelineRuns(List.of(pipelineRun2.toBuilder().result("CUSTOM_RESULT").build())).build();
        CICDJobRun jobRun2 = jobRuns.stream().filter(run -> (long)project2.getPipelineRuns().get(0).getRunId() == run.getJobRunNumber()).findFirst().get();
        String newInserted2 = projectService.insert(COMPANY, instanceId, project2);
        Assert.assertNotNull(newInserted2);
        Assert.assertEquals(jobRun2.getId().toString(), newInserted2);
        var insertedJobRun2 = ciCdJobRunsDatabaseService.listByFilter(COMPANY, 0,1, List.of(UUID.fromString(newInserted2)), null, null).getRecords().get(0);
        Assert.assertNotNull(insertedJobRun2);
        Assert.assertNotEquals("CUSTOM_RESULT", insertedJobRun2.getStatus());
    }

    @Test
    public void testUpdateCicdJobs() throws IOException, SQLException {
        String buildsResourcePath = "json/databases/azure_devops_builds.json";
        String buildsInput = ResourceUtils.getResourceAsString(buildsResourcePath);
        PaginatedResponse<EnrichedProjectData> projectData = OBJECT_MAPPER.readValue(buildsInput, OBJECT_MAPPER.getTypeFactory()
                .constructParametricType(PaginatedResponse.class, EnrichedProjectData.class));
        List<DbAzureDevopsBuild> dbAzureDevopsBuilds = projectData.getResponse().getRecords().stream()
                .flatMap(enrichedProjectData -> enrichedProjectData.getBuilds().stream())
                .map(build -> DbAzureDevopsBuild.fromBuild(build, INTEGRATION_ID, currentTime))
                .collect(Collectors.toList());
        DbAzureDevopsBuild updatedBuild = dbAzureDevopsBuilds.get(0).toBuilder().buildId(127).build();
        var result = projectService.processBuilds(COMPANY, instanceId,updatedBuild);
        Assert.assertEquals(2, result);
        DbListResponse<CICDJobRun> cicdJobRun = ciCdJobRunsDatabaseService.listByFilter(COMPANY, 0, 10, null, null, List.of(Long.valueOf(updatedBuild.getBuildId())));
        Assert.assertNotNull(cicdJobRun.getRecords().get(0).getCicdUserId());
        Assert.assertEquals(dbAzureDevopsBuilds.get(0).getRequestedByUniqueName(), cicdJobRun.getRecords().get(0).getCicdUserId());
    }
}
