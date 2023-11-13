package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.CICDInstance;
import io.levelops.commons.databases.models.database.CICDJob;
import io.levelops.commons.databases.models.database.CICDJobRun;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.QueryFilter;
import io.levelops.commons.databases.models.database.azuredevops.DbAzureDevopsProject;
import io.levelops.commons.databases.models.database.cicd.JobRunStage;
import io.levelops.commons.databases.models.database.cicd.JobRunStageStep;
import io.levelops.commons.databases.models.filters.CICD_TYPE;
import io.levelops.commons.databases.services.organization.ProductsDatabaseService;
import io.levelops.commons.databases.services.organization.TeamMembersDatabaseService;
import io.levelops.commons.databases.services.organization.TeamsDatabaseService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.azureDevops.models.EnrichedProjectData;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.lang3.StringUtils;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class AzureDevopsReleaseServiceTest {
    private static final String COMPANY = "test";
    private static final String INTEGRATION_ID = "1";
    private static CiCdPipelinesAggsService ciCdPipelinesAggsService;
    private static AzureDevopsReleaseService releaseService;
    private static CiCdJobsDatabaseService ciCdJobsDatabaseService;
    private static CiCdJobRunsDatabaseService ciCdJobRunsDatabaseService;
    private static CiCdJobRunStageDatabaseService ciCdJobRunStageDatabaseService;
    private static TriageRuleHitsService triageRuleHitsService;
    private static TriageRulesService triageRulesService;
    private static CiCdJobRunStageStepsDatabaseService ciCdJobRunStageStepsDatabaseService;
    private static final ObjectMapper OBJECT_MAPPER = DefaultObjectMapper.get();
    private static UUID instanceId;
    private static final List<DbAzureDevopsProject> dbReleases = new ArrayList<>();
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
        releaseService = new AzureDevopsReleaseService(ciCdJobsDatabaseService, ciCdJobRunsDatabaseService,
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
        String resourcePath = "json/databases/azure_devops_release.json";
        String projectsInput = ResourceUtils.getResourceAsString(resourcePath);
        PaginatedResponse<EnrichedProjectData> releases = OBJECT_MAPPER.readValue(projectsInput, OBJECT_MAPPER.getTypeFactory()
                .constructParametricType(PaginatedResponse.class, EnrichedProjectData.class));
        dbReleases.addAll(releases.getResponse().getRecords().stream()
                .map(enrichedProjectData -> DbAzureDevopsProject.fromReleases(enrichedProjectData, INTEGRATION_ID, "1", currentTime))
                .collect(Collectors.toList()));
        for (DbAzureDevopsProject dbPipelineRun : dbReleases) {
            releaseService.insert(COMPANY, instanceId, dbPipelineRun);
        }
    }

    @Test
    public void test() throws SQLException {
        DbListResponse<CICDJob> cicdJobDbListResponse = ciCdJobsDatabaseService.list(COMPANY, 0,10);
        Assert.assertNotNull(cicdJobDbListResponse);
        Assert.assertEquals(3, cicdJobDbListResponse.getRecords().size());
        DbListResponse<CICDJobRun> cicdJobRunDbListResponse = ciCdJobRunsDatabaseService.list(COMPANY, 0, 10);
        Assert.assertNotNull(cicdJobRunDbListResponse);
        List<CICDJobRun> cicdJobRuns = ciCdJobRunsDatabaseService.listByFilter(COMPANY, 0, 10, null, null,null).getRecords();
        Assert.assertEquals(5, cicdJobRuns.size());
        Assert.assertNotEquals(Optional.ofNullable(cicdJobRuns.get(0).getDuration()), 0L);

        // Test job run params
        Map<UUID, List<CICDJobRun.JobRunParam>> jobRunParams = ciCdJobRunsDatabaseService.getJobRunParams(COMPANY, cicdJobRuns.stream().map(CICDJobRun::getId).collect(Collectors.toList()));
        List<CICDJobRun.JobRunParam> allJobRunParams = jobRunParams.values().stream().flatMap(Collection::stream).collect(Collectors.toList());
        Assert.assertEquals(10, allJobRunParams.size());

        // Test job run stages and steps
        CICDJob job = cicdJobDbListResponse.getRecords().stream().filter(j -> StringUtils.isNotEmpty(j.getJobName()) && j.getJobName().endsWith("New release pipeline")).findFirst().get();
        CICDJobRun jobRun = cicdJobRuns.stream().filter(r -> r.getCicdJobId().equals(job.getId())).findFirst().get();
        DbListResponse<JobRunStage> stageDbListResponse = ciCdJobRunStageDatabaseService.list(COMPANY, 0, 3, QueryFilter.builder().strictMatches(Map.of("cicd_job_run_id", List.of(jobRun.getId().toString()))).build());
        Assert.assertNotNull(stageDbListResponse);
        Assert.assertNotNull(stageDbListResponse.getRecords());
        Assert.assertTrue(stageDbListResponse.getRecords().size() > 0);
        Assert.assertEquals(1, stageDbListResponse.getRecords().size());

        DbListResponse<JobRunStageStep> stepDbListResponse = ciCdJobRunStageStepsDatabaseService.getBatch(COMPANY, 0, 27, null, List.of(stageDbListResponse.getRecords().get(0).getId()), null);
        Assert.assertNotNull(stepDbListResponse);
        Assert.assertNotNull(stepDbListResponse.getRecords());
        Assert.assertTrue(stepDbListResponse.getRecords().size() > 0);
        Assert.assertEquals(4, stepDbListResponse.getRecords().size());
    }
}
