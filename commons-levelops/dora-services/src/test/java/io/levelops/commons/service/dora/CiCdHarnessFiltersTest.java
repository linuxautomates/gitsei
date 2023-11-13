package io.levelops.commons.service.dora;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.CICDInstance;
import io.levelops.commons.databases.models.database.CICDJob;
import io.levelops.commons.databases.models.database.CICDJobRun;
import io.levelops.commons.databases.models.database.CICDJobRunDTO;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.TriageRule;
import io.levelops.commons.databases.models.database.TriageRuleHit;
import io.levelops.commons.databases.models.database.organization.DBOrgUnit;
import io.levelops.commons.databases.models.database.organization.OrgUnitCategory;
import io.levelops.commons.databases.models.database.velocity.VelocityConfigDTO;
import io.levelops.commons.databases.models.dora.DoraResponseDTO;
import io.levelops.commons.databases.models.dora.DoraSingleStateDTO;
import io.levelops.commons.databases.models.filters.CiCdJobRunsFilter;
import io.levelops.commons.databases.services.CiCdAggsService;
import io.levelops.commons.databases.services.CiCdAggsServiceTest;
import io.levelops.commons.databases.services.CiCdInstanceUtils;
import io.levelops.commons.databases.services.CiCdInstancesDatabaseService;
import io.levelops.commons.databases.services.CiCdJobConfigChangesDatabaseService;
import io.levelops.commons.databases.services.CiCdJobRunStageDatabaseService;
import io.levelops.commons.databases.services.CiCdJobRunStageStepsDatabaseService;
import io.levelops.commons.databases.services.CiCdJobRunsDatabaseService;
import io.levelops.commons.databases.services.CiCdJobsDatabaseService;
import io.levelops.commons.databases.services.DashboardWidgetService;
import io.levelops.commons.databases.services.DatabaseSchemaService;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.TagItemDBService;
import io.levelops.commons.databases.services.TagsService;
import io.levelops.commons.databases.services.TriageRuleHitsService;
import io.levelops.commons.databases.services.TriageRulesService;
import io.levelops.commons.databases.services.UserIdentityService;
import io.levelops.commons.databases.services.UserService;
import io.levelops.commons.databases.services.organization.OrgUsersDatabaseService;
import io.levelops.commons.databases.services.organization.OrgVersionsDatabaseService;
import io.levelops.commons.databases.services.organization.ProductsDatabaseService;
import io.levelops.commons.databases.services.organization.WorkspaceDatabaseService;
import io.levelops.commons.databases.services.velocity.VelocityConfigsDatabaseService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class CiCdHarnessFiltersTest {
    private static final String company = "test";
    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static CiCdInstancesDatabaseService ciCdInstancesDatabaseService;
    private static CiCdJobsDatabaseService ciCdJobsDatabaseService;
    private static CiCdJobRunsDatabaseService ciCdJobRunsDatabaseService;
    private static CiCdDoraService ciCdDoraService;
    private static CiCdAggsService ciCdAggsService;

    private static DataSource dataSource;
    private static final ObjectMapper mapper = DefaultObjectMapper.get();
    private static UserService userService;
    private static CiCdJobConfigChangesDatabaseService ciCdJobConfigChangesDatabaseService;
    private static IntegrationService integrationService;
    private static ProductsDatabaseService productsDatabaseService;
    private static CiCdJobRunStageDatabaseService ciCdJobRunStageDatabaseService;
    private static CiCdJobRunStageStepsDatabaseService ciCdJobRunStageStepsDatabaseService;
    private static TriageRulesService triageRulesService;
    private static VelocityConfigsDatabaseService velocityConfigsDatabaseService;
    private static TriageRuleHitsService triageRuleHitsService;
    private static String rule1;
    private static String rule2;
    private static String rule3;

    private static Integration integration;
    private static Integration integration2;
    private static Integration integration3;
    private static String integrationId2;
    private static String integrationId3;
    private final static List<CICDJobRun> ciCdJobRuns = new ArrayList<>();
    private final static List<CICDJob> ciCdJobs = new ArrayList<>();
    private final static List<String> ciCdJobIds = new ArrayList<>();
    private final static List<Map<String, Object>> ciCdJobRunParams = new ArrayList<>();
    private static OrgUsersDatabaseService usersService;
    private static DBOrgUnit unit1;
    private static OrgUnitCategory orgUnitCategory;
    private static OrgUnitCategory orgGroup1, orgGroup2, orgGroup3, orgGroup4, orgGroup5;
    private static String orgGroupId1, orgGroupId2, orgGroupId3, orgGroupId4, orgGroupId5;

    @BeforeClass
    public static void setup() throws SQLException, IOException {
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();

        userService = new UserService(dataSource, MAPPER);
        ciCdInstancesDatabaseService = new CiCdInstancesDatabaseService(dataSource);
        ciCdJobsDatabaseService = new CiCdJobsDatabaseService(dataSource);
        ciCdJobRunsDatabaseService = new CiCdJobRunsDatabaseService(DefaultObjectMapper.get(), dataSource);
        ciCdJobConfigChangesDatabaseService = new CiCdJobConfigChangesDatabaseService(dataSource);
        productsDatabaseService = new ProductsDatabaseService(dataSource, MAPPER);
        ciCdDoraService = new CiCdDoraService(dataSource);
        integrationService = new IntegrationService(dataSource);
        ciCdJobRunStageDatabaseService = new CiCdJobRunStageDatabaseService(dataSource, DefaultObjectMapper.get());
        ciCdJobRunStageStepsDatabaseService = new CiCdJobRunStageStepsDatabaseService(dataSource);
        ciCdAggsService = new CiCdAggsService(dataSource);
        triageRulesService = new TriageRulesService(dataSource);
        triageRuleHitsService = new TriageRuleHitsService(dataSource, DefaultObjectMapper.get());
        velocityConfigsDatabaseService = new VelocityConfigsDatabaseService(dataSource, MAPPER, null);

        new DatabaseSchemaService(dataSource).ensureSchemaExistence(company);
        integrationService.ensureTableExistence(company);
        userService.ensureTableExistence(company);
        productsDatabaseService.ensureTableExistence(company);
        ciCdInstancesDatabaseService.ensureTableExistence(company);
        ciCdJobsDatabaseService.ensureTableExistence(company);
        ciCdJobRunsDatabaseService.ensureTableExistence(company);
        velocityConfigsDatabaseService.ensureTableExistence(company);
        ciCdAggsService.ensureTableExistence(company);

        integration = Integration.builder()
                .id("1")
                .name("name")
                .url("http")
                .status("good")
                .application("jira")
                .description("desc")
                .satellite(true)
                .build();
        integrationService.insert(company, integration);

        TagsService tagsService = new TagsService(dataSource);
        tagsService.ensureTableExistence(company);
        TagItemDBService tagItemService = new TagItemDBService(dataSource);
        tagItemService.ensureTableExistence(company);
        OrgVersionsDatabaseService versionsService = new OrgVersionsDatabaseService(dataSource);
        versionsService.ensureTableExistence(company);
        UserIdentityService userIdentityService = new UserIdentityService(dataSource);
        userIdentityService.ensureTableExistence(company);
        usersService = new OrgUsersDatabaseService(dataSource, MAPPER, versionsService, userIdentityService);
        usersService.ensureTableExistence(company);
        DashboardWidgetService dashboardWidgetService = new DashboardWidgetService(dataSource, mapper);
        dashboardWidgetService.ensureTableExistence(company);
        new WorkspaceDatabaseService(dataSource).ensureTableExistence(company);
        orgGroup1 = OrgUnitCategory.builder()
                .name("TEAM A")
                .description("Sample team")
                .isPredefined(true)
                .build();

        orgGroup2 = OrgUnitCategory.builder()
                .name("TEAM B")
                .description("Sample team")
                .isPredefined(true)
                .build();
        TriageRule tRuel1 = TriageRule.builder()
                .application("all")
                .name("test-rule1")
                .description("test1")
                .owner("ashish@levelops.io")
                .regexes(List.of(""))
                .build();

        TriageRule tRuel2 = TriageRule.builder()
                .application("all")
                .name("test-rule2")
                .description("test2")
                .owner("ashish@levelops.io")
                .regexes(List.of(""))
                .build();

        TriageRule tRuel3 = TriageRule.builder()
                .application("all")
                .name("test-rule3")
                .description("test3")
                .owner("ashish@levelops.io")
                .regexes(List.of(""))
                .build();

        ciCdJobRunStageDatabaseService.ensureTableExistence(company);
        ciCdJobRunStageStepsDatabaseService.ensureTableExistence(company);
        triageRulesService.ensureTableExistence(company);
        triageRuleHitsService.ensureTableExistence(company);
        rule1 = triageRulesService.insert(company, tRuel1);
        rule2 = triageRulesService.insert(company, tRuel2);
        rule3 = triageRulesService.insert(company, tRuel3);

        integration2 = Integration.builder()
                .name("integration-name-" + 2)
                .status("status-" + 0).application("azure_devops").url("http://www.dummy.com")
                .satellite(false).build();
        integrationId2 = integrationService.insert(company, integration2);

        integration3 = Integration.builder()
                .name("integration-name-" + 3)
                .status("status-" + 0).application("jenkins").url("http://www.dummy.com")
                .satellite(false).build();
        integrationId3 = integrationService.insert(company, integration3);
    }

    @Before
    public void cleanup() throws SQLException {
        dataSource.getConnection().prepareStatement("DELETE FROM test.cicd_jobs;").execute();
    }

    // region Setup Jobs and Job Runs
    private static void setupJobsAndJobRuns(UUID cicdInstanceId, List<CiCdAggsServiceTest.JobDetails> allJobDetails) throws SQLException {
        for (CiCdAggsServiceTest.JobDetails currentJobAllRuns : allJobDetails) {
            CICDJob cicdJob = CICDJob.builder()
                    .cicdInstanceId(cicdInstanceId)
                    .jobName(currentJobAllRuns.getJobName())
                    .jobFullName(currentJobAllRuns.getJobFullName())
                    .jobNormalizedFullName(currentJobAllRuns.getJobFullName())
                    .branchName(currentJobAllRuns.getBranchName())
                    .projectName("project-1")
                    .moduleName(currentJobAllRuns.getModuleName())
                    .scmUrl(currentJobAllRuns.getScmUrl())
                    .scmUserId(currentJobAllRuns.getScmUserId())
                    .build();
            String cicdJobIdString = ciCdJobsDatabaseService.insert(company, cicdJob);
            ciCdJobIds.add(cicdJobIdString);
            Assert.assertNotNull(cicdJobIdString);
            ciCdJobs.add(cicdJob);
            UUID cicdJobId = UUID.fromString(cicdJobIdString);
            int count = 0;
            for (CiCdAggsServiceTest.JobRunDetails currentJobRun : currentJobAllRuns.getRuns()) {
                CICDJobRun.CICDJobRunBuilder bldr = CICDJobRun.builder()
                        .cicdJobId(cicdJobId)
                        .jobRunNumber(currentJobRun.getNumber())
                        .status(currentJobRun.getStatus())
                        .metadata(currentJobRun.getMetadata())
                        .startTime(Instant.ofEpochSecond(currentJobRun.getStartTime()))
                        .endTime(Instant.ofEpochSecond(currentJobRun.getEndTime()))
                        .duration(currentJobRun.getDuration().intValue())
                        .cicdUserId(currentJobRun.getUserId())
                        .source(CICDJobRun.Source.ANALYTICS_PERIODIC_PUSH)
                        .referenceId(UUID.randomUUID().toString())
                        .scmCommitIds(currentJobRun.getCommitIds());
                if (CollectionUtils.isNotEmpty(currentJobRun.getParams())) {
                    List<CICDJobRun.JobRunParam> params = new ArrayList<>();
                    for (CiCdAggsServiceTest.JobRunParam currentParam : currentJobRun.getParams()) {
                        CICDJobRun.JobRunParam sanitized = CICDJobRun.JobRunParam.builder()
                                .type(currentParam.getType())
                                .name(currentParam.getName())
                                .value(currentParam.getValue())
                                .build();
                        params.add(sanitized);
                        ciCdJobRunParams.add(
                                Map.of("name", currentParam.getName(), "value", currentParam.getValue())
                        );
                    }
                    bldr.params(params);
                }

                CICDJobRun cicdJobRun = bldr.build();
                String cicdJobRunId = ciCdJobRunsDatabaseService.insert(company, cicdJobRun);
                insertTriageRuleHits(cicdJobRunId, count++ % 2 == 0 ? rule1 : rule2);
                ciCdJobRuns.add(cicdJobRun);
            }
        }
    }
    // endregion

    public static void insertTriageRuleHits(String cicdJobRunId, String rule) throws SQLException {

        TriageRuleHit ruleHit = TriageRuleHit.builder()
                .jobRunId(cicdJobRunId)
                .ruleId(rule)
                .hitContent("")
                .count(1)
                .build();
        triageRuleHitsService.insert(company, ruleHit);
    }

    @Test
    public void testMetaDataFilterStrictMatches() throws IOException, SQLException {
        String workFlowProfile = ResourceUtils.getResourceAsString("json/dora/cicd_filters_velocity.json");
        VelocityConfigDTO velocityConfigDTO = mapper.readValue(workFlowProfile, VelocityConfigDTO.class);

        String data = ResourceUtils.getResourceAsString("json/dora/cicd_job_runs_with_metadata.json");
        List<CiCdAggsServiceTest.JobDetails> allJobDetails
                = MAPPER.readValue(data, MAPPER.getTypeFactory().constructCollectionType(
                List.class, CiCdAggsServiceTest.JobDetails.class
        ));
        CICDInstance cicdInstance = CiCdInstanceUtils.createCiCdInstance(ciCdInstancesDatabaseService, company, 0);
        setupJobsAndJobRuns(null, allJobDetails);
        setupJobsAndJobRuns(cicdInstance.getId(), allJobDetails);
        Long begin = Long.valueOf(159261332);
        Long end = Instant.now().getEpochSecond();

        DbListResponse<CICDJob> jobs =  ciCdJobsDatabaseService.listByFilter(company, 0 , 100, null,
                Arrays.asList("Build-commons-levelops", "Build-Aggregations-Service", "Build-Server-API", "Deploy-Internal-Api-to-kubernetes-cluster"),
                null,
                null,
                List.of(cicdInstance.getId()));

        List<String> jobIdsInString = CollectionUtils.emptyIfNull(jobs.getRecords()).stream().map(m->m.getId().toString()).collect(Collectors.toList());

        velocityConfigDTO = velocityConfigDTO.toBuilder().deploymentFrequency(
                        velocityConfigDTO.getDeploymentFrequency().toBuilder().integrationIds(List.of(velocityConfigDTO.getDeploymentFrequency().getIntegrationId()))
                                .velocityConfigFilters(velocityConfigDTO.getDeploymentFrequency().getVelocityConfigFilters().toBuilder()
                                        .deploymentFrequency(velocityConfigDTO.getDeploymentFrequency().getVelocityConfigFilters().getDeploymentFrequency().toBuilder()
                                                .calculationField(velocityConfigDTO.getDeploymentFrequency().getVelocityConfigFilters().getDeploymentFrequency().getCalculationField())
                                                .event(velocityConfigDTO.getDeploymentFrequency().getVelocityConfigFilters().getDeploymentFrequency().getEvent().toBuilder().values(jobIdsInString).build())
                                                .build())
                                        .build())
                                .integrationIds(List.of(velocityConfigDTO.getDeploymentFrequency().getIntegrationId()))
                                .build())
                .build();

        DoraResponseDTO doraResponseDTO = ciCdDoraService.calculateNewDeploymentFrequency(
                company,
                CiCdJobRunsFilter.builder()
                        .across(CiCdJobRunsFilter.DISTINCT.trend)
                        .endTimeRange(ImmutablePair.of(begin, end))
                        .jobNormalizedFullNames(List.of("Build-commons-levelops", "Build-Aggregations-Service", "Build-Server-API", "Deploy-Internal-Api-to-kubernetes-cluster"))
                        .calculation(CiCdJobRunsFilter.CALCULATION.count)
                        .rollback(true)
                        .services(List.of("Deploy_to_GCP"))
                        .deploymentTypes(List.of("Kubernetes"))
                        .infrastructures(List.of("CD_only_Docker_Registry_Infrastructure"))
                        .environments(List.of("CD_only_Docker_Registry_Env"))
                        .branches(List.of("master"))
                        .repositories(List.of("https://github.com/meetrajsinh-crest/private-repo"))
                        .build(),
                null,
                velocityConfigDTO,
                null
        );

        Assert.assertEquals(doraResponseDTO.getStats().getTotalDeployment(), (Integer) 4);
        Assert.assertEquals(doraResponseDTO.getStats().getBand(), DoraSingleStateDTO.Band.LOW);
    }

    @Test
    public void testMetaDataExcludeFilter() throws IOException, SQLException {
        String workFlowProfile = ResourceUtils.getResourceAsString("json/dora/cicd_filters_velocity.json");
        VelocityConfigDTO velocityConfigDTO = mapper.readValue(workFlowProfile, VelocityConfigDTO.class);

        String data = ResourceUtils.getResourceAsString("json/dora/cicd_job_runs_with_metadata.json");
        List<CiCdAggsServiceTest.JobDetails> allJobDetails
                = MAPPER.readValue(data, MAPPER.getTypeFactory().constructCollectionType(
                List.class, CiCdAggsServiceTest.JobDetails.class
        ));
        CICDInstance cicdInstance = CiCdInstanceUtils.createCiCdInstance(ciCdInstancesDatabaseService, company, 0);
        setupJobsAndJobRuns(null, allJobDetails);
        setupJobsAndJobRuns(cicdInstance.getId(), allJobDetails);
        Long begin = Long.valueOf(0);
        Long end = Instant.now().getEpochSecond();

        DbListResponse<CICDJob> jobs =  ciCdJobsDatabaseService.listByFilter(company, 0 , 100, null,
                Arrays.asList("Build-commons-levelops", "Build-Aggregations-Service", "Build-Server-API", "Deploy-Internal-Api-to-kubernetes-cluster"),
                null,
                null,
                List.of(cicdInstance.getId()));

        List<String> jobIdsInString = CollectionUtils.emptyIfNull(jobs.getRecords()).stream().map(m->m.getId().toString()).collect(Collectors.toList());

        velocityConfigDTO = velocityConfigDTO.toBuilder().deploymentFrequency(
                        velocityConfigDTO.getDeploymentFrequency().toBuilder().integrationIds(List.of(velocityConfigDTO.getDeploymentFrequency().getIntegrationId()))
                                .velocityConfigFilters(velocityConfigDTO.getDeploymentFrequency().getVelocityConfigFilters().toBuilder()
                                        .deploymentFrequency(velocityConfigDTO.getDeploymentFrequency().getVelocityConfigFilters().getDeploymentFrequency().toBuilder()
                                                .calculationField(velocityConfigDTO.getDeploymentFrequency().getVelocityConfigFilters().getDeploymentFrequency().getCalculationField())
                                                .event(velocityConfigDTO.getDeploymentFrequency().getVelocityConfigFilters().getDeploymentFrequency().getEvent().toBuilder().values(jobIdsInString).build())
                                                .build())
                                        .build())
                                .integrationIds(List.of(velocityConfigDTO.getDeploymentFrequency().getIntegrationId()))
                                .build())
                .build();
        DoraResponseDTO doraResponseDTO = ciCdDoraService.calculateNewDeploymentFrequency(
                company,
                CiCdJobRunsFilter.builder()
                        .across(CiCdJobRunsFilter.DISTINCT.trend)
                        .endTimeRange(ImmutablePair.of(begin, end))
                        .jobNormalizedFullNames(List.of("Build-commons-levelops", "Build-Aggregations-Service", "Build-Server-API", "Deploy-Internal-Api-to-kubernetes-cluster"))
                        .calculation(CiCdJobRunsFilter.CALCULATION.count)
                        .excludeBranches(List.of("main"))
                        .excludeEnvironments(List.of("env1"))
                        .excludeInfrastructures(List.of("inf1"))
                        .excludeRollback(false)
                        .excludeServices(List.of("service1"))
                        .excludeJobNormalizedFullNames(List.of("Build-commons-levelops"))
                        .build(),
                null,
                velocityConfigDTO,
                null
        );

        Assert.assertEquals(doraResponseDTO.getStats().getTotalDeployment(), (Integer)3);
        Assert.assertEquals(doraResponseDTO.getStats().getBand(), DoraSingleStateDTO.Band.LOW);
    }

    @Test
    public void testMetaDataPartialMatchFilter() throws IOException, SQLException {
        String workFlowProfile = ResourceUtils.getResourceAsString("json/dora/cicd_filters_velocity.json");
        VelocityConfigDTO velocityConfigDTO = mapper.readValue(workFlowProfile, VelocityConfigDTO.class);

        String data = ResourceUtils.getResourceAsString("json/dora/cicd_job_runs_with_metadata.json");
        List<CiCdAggsServiceTest.JobDetails> allJobDetails
                = MAPPER.readValue(data, MAPPER.getTypeFactory().constructCollectionType(
                List.class, CiCdAggsServiceTest.JobDetails.class
        ));
        CICDInstance cicdInstance = CiCdInstanceUtils.createCiCdInstance(ciCdInstancesDatabaseService, company, 0);
        setupJobsAndJobRuns(null, allJobDetails);
        setupJobsAndJobRuns(cicdInstance.getId(), allJobDetails);
        Long begin = Long.valueOf(0);
        Long end = Instant.now().getEpochSecond();
        DbListResponse<CICDJob> jobs =  ciCdJobsDatabaseService.listByFilter(company, 0 , 100, null,
                Arrays.asList("Build-commons-levelops", "Build-Aggregations-Service", "Build-Server-API", "Deploy-Internal-Api-to-kubernetes-cluster"),
                null,
                null,
                List.of(cicdInstance.getId()));

        List<String> jobIdsInString = CollectionUtils.emptyIfNull(jobs.getRecords()).stream().map(m->m.getId().toString()).collect(Collectors.toList());

        velocityConfigDTO = velocityConfigDTO.toBuilder().deploymentFrequency(
                        velocityConfigDTO.getDeploymentFrequency().toBuilder().integrationIds(List.of(velocityConfigDTO.getDeploymentFrequency().getIntegrationId()))
                                .velocityConfigFilters(velocityConfigDTO.getDeploymentFrequency().getVelocityConfigFilters().toBuilder()
                                        .deploymentFrequency(velocityConfigDTO.getDeploymentFrequency().getVelocityConfigFilters().getDeploymentFrequency().toBuilder()
                                                .calculationField(velocityConfigDTO.getDeploymentFrequency().getVelocityConfigFilters().getDeploymentFrequency().getCalculationField())
                                                .event(velocityConfigDTO.getDeploymentFrequency().getVelocityConfigFilters().getDeploymentFrequency().getEvent().toBuilder().values(jobIdsInString).build())
                                                .build())
                                        .build())
                                .integrationIds(List.of(velocityConfigDTO.getDeploymentFrequency().getIntegrationId()))
                                .build())
                .build();
        DoraResponseDTO doraResponseDTO = ciCdDoraService.calculateNewDeploymentFrequency(
                company,
                CiCdJobRunsFilter.builder()
                        .across(CiCdJobRunsFilter.DISTINCT.trend)
                        .endTimeRange(ImmutablePair.of(begin, end))
                        .jobNormalizedFullNames(List.of("Build-commons-levelops", "Build-Aggregations-Service", "Build-Server-API", "Deploy-Internal-Api-to-kubernetes-cluster"))
                        .calculation(CiCdJobRunsFilter.CALCULATION.count)
                        .partialMatch(new HashMap<>(Map.of("tags", Map.of("$begins", "del"), "branches", Map.of("$begins", "mas"))))
                        .build(),
                null,
                velocityConfigDTO,
                null
        );

        Assert.assertEquals(doraResponseDTO.getStats().getTotalDeployment(), (Integer) 3);
        Assert.assertEquals(doraResponseDTO.getStats().getBand(), DoraSingleStateDTO.Band.LOW);
    }

    @Test
    public void testDrillDownFieldsForStrictMatchFilter() throws SQLException, IOException {
        String workFlowProfile = ResourceUtils.getResourceAsString("json/dora/cicd_filters_velocity.json");
        VelocityConfigDTO velocityConfigDTO = mapper.readValue(workFlowProfile, VelocityConfigDTO.class);

        String data = ResourceUtils.getResourceAsString("json/dora/cicd_job_runs_with_metadata.json");
        List<CiCdAggsServiceTest.JobDetails> allJobDetails
                = MAPPER.readValue(data, MAPPER.getTypeFactory().constructCollectionType(
                List.class, CiCdAggsServiceTest.JobDetails.class
        ));
        CICDInstance cicdInstance = CiCdInstanceUtils.createCiCdInstance(ciCdInstancesDatabaseService, company, 0);
        setupJobsAndJobRuns(null, allJobDetails);
        setupJobsAndJobRuns(cicdInstance.getId(), allJobDetails);
        Long begin = Long.valueOf(0);
        Long end = Instant.now().getEpochSecond();

        DbListResponse<CICDJob> cicdJobRuns = ciCdJobsDatabaseService.list(company, 0, 100);
        List<UUID> jobUUID = cicdJobRuns.getRecords().stream().map(CICDJob::getId).collect(Collectors.toList());
        List<String> jobIds = new ArrayList<>();
        for (UUID id: jobUUID) {
            jobIds.add(id.toString());
        }
            DbListResponse<CICDJobRunDTO> response = ciCdAggsService.listCiCdJobRunsForDora(
                company,
                CiCdJobRunsFilter.builder()
                        .across(CiCdJobRunsFilter.DISTINCT.trend)
                        .endTimeRange(ImmutablePair.of(begin, end))
                        .jobNormalizedFullNames(List.of("Build-commons-levelops", "Build-Aggregations-Service", "Build-Server-API", "Deploy-Internal-Api-to-kubernetes-cluster"))
                        .calculation(CiCdJobRunsFilter.CALCULATION.count)
                        .rollback(true)
                        .services(List.of("Deploy_to_GCP"))
                        .deploymentTypes(List.of("Kubernetes"))
                        .infrastructures(List.of("CD_only_Docker_Registry_Infrastructure"))
                        .environments(List.of("CD_only_Docker_Registry_Env"))
                        .branches(List.of("master"))
                        .repositories(List.of("https://github.com/meetrajsinh-crest/private-repo"))
                        .build(),
                    0,
                    10,
                    null,
                    jobIds
            );

        Assert.assertTrue(true);
        Assert.assertEquals(response.getCount(), (Integer) 8);
        Assert.assertNotNull(response.getRecords().get(0).getEnvironmentIds());
        Assert.assertNotNull(response.getRecords().get(0).getServiceTypes());
        Assert.assertNotNull(response.getRecords().get(0).getServiceIds());
        Assert.assertNotNull(response.getRecords().get(0).getInfraIds());
        Assert.assertNotNull(response.getRecords().get(0).getCicdBranch());
        Assert.assertNotNull(response.getRecords().get(0).getRepoUrl());
    }

    @Test
    public void testDrillDownFieldsForExcludeMatchFilter() throws SQLException, IOException {
        String workFlowProfile = ResourceUtils.getResourceAsString("json/dora/cicd_filters_velocity.json");
        VelocityConfigDTO velocityConfigDTO = mapper.readValue(workFlowProfile, VelocityConfigDTO.class);

        String data = ResourceUtils.getResourceAsString("json/dora/cicd_job_runs_with_metadata.json");
        List<CiCdAggsServiceTest.JobDetails> allJobDetails
                = MAPPER.readValue(data, MAPPER.getTypeFactory().constructCollectionType(
                List.class, CiCdAggsServiceTest.JobDetails.class
        ));
        CICDInstance cicdInstance = CiCdInstanceUtils.createCiCdInstance(ciCdInstancesDatabaseService, company, 0);
        setupJobsAndJobRuns(null, allJobDetails);
        setupJobsAndJobRuns(cicdInstance.getId(), allJobDetails);
        Long begin = Long.valueOf(0);
        Long end = Instant.now().getEpochSecond();

        DbListResponse<CICDJob> cicdJobRuns = ciCdJobsDatabaseService.list(company, 0, 100);
        List<UUID> jobUUID = cicdJobRuns.getRecords().stream().map(CICDJob::getId).collect(Collectors.toList());
        List<String> jobIds = new ArrayList<>();
        for (UUID id: jobUUID) {
            jobIds.add(id.toString());
        }
        DbListResponse<CICDJobRunDTO> response = ciCdAggsService.listCiCdJobRunsForDora(
                company,
                CiCdJobRunsFilter.builder()
                        .across(CiCdJobRunsFilter.DISTINCT.trend)
                        .endTimeRange(ImmutablePair.of(begin, end))
                        .jobNormalizedFullNames(List.of("Build-commons-levelops", "Build-Aggregations-Service", "Build-Server-API", "Deploy-Internal-Api-to-kubernetes-cluster"))
                        .calculation(CiCdJobRunsFilter.CALCULATION.count)
                        .excludeBranches(List.of("main"))
                        .excludeEnvironments(List.of("env1"))
                        .excludeInfrastructures(List.of("inf1"))
                        .excludeRollback(false)
                        .excludeServices(List.of("service1"))
                        .excludeJobNormalizedFullNames(List.of("Build-commons-levelops"))
                        .build(),
                0,
                10,
                null,
                jobIds
        );

        Assert.assertTrue(true);
        Assert.assertEquals(response.getCount(), (Integer) 6);
        Assert.assertNotNull(response.getRecords().get(0).getEnvironmentIds());
        Assert.assertNotNull(response.getRecords().get(0).getServiceTypes());
        Assert.assertNotNull(response.getRecords().get(0).getServiceIds());
        Assert.assertNotNull(response.getRecords().get(0).getInfraIds());
        Assert.assertNotNull(response.getRecords().get(0).getCicdBranch());
        Assert.assertNotNull(response.getRecords().get(0).getRepoUrl());
    }

    @Test
    public void testDrillDownFieldsForPartialMatchFilter() throws SQLException, IOException {
        String workFlowProfile = ResourceUtils.getResourceAsString("json/dora/cicd_filters_velocity.json");
        VelocityConfigDTO velocityConfigDTO = mapper.readValue(workFlowProfile, VelocityConfigDTO.class);

        String data = ResourceUtils.getResourceAsString("json/dora/cicd_job_runs_with_metadata.json");
        List<CiCdAggsServiceTest.JobDetails> allJobDetails
                = MAPPER.readValue(data, MAPPER.getTypeFactory().constructCollectionType(
                List.class, CiCdAggsServiceTest.JobDetails.class
        ));
        CICDInstance cicdInstance = CiCdInstanceUtils.createCiCdInstance(ciCdInstancesDatabaseService, company, 0);
        setupJobsAndJobRuns(null, allJobDetails);
        setupJobsAndJobRuns(cicdInstance.getId(), allJobDetails);
        Long begin = Long.valueOf(0);
        Long end = Instant.now().getEpochSecond();

        DbListResponse<CICDJob> cicdJobRuns = ciCdJobsDatabaseService.list(company, 0, 100);
        List<UUID> jobUUID = cicdJobRuns.getRecords().stream().map(CICDJob::getId).collect(Collectors.toList());
        List<String> jobIds = new ArrayList<>();
        for (UUID id: jobUUID) {
            jobIds.add(id.toString());
        }
        DbListResponse<CICDJobRunDTO> response = ciCdAggsService.listCiCdJobRunsForDora(
                company,
                CiCdJobRunsFilter.builder()
                        .across(CiCdJobRunsFilter.DISTINCT.trend)
                        .endTimeRange(ImmutablePair.of(begin, end))
                        .jobNormalizedFullNames(List.of("Build-commons-levelops", "Build-Aggregations-Service", "Build-Server-API", "Deploy-Internal-Api-to-kubernetes-cluster"))
                        .calculation(CiCdJobRunsFilter.CALCULATION.count)
                        .partialMatch(new HashMap<>(Map.of("tags", Map.of("$begins", "del"), "branches", Map.of("$begins", "mas"))))
                        .build(),
                0,
                10,
                null,
                jobIds
        );

        Assert.assertTrue(true);
        Assert.assertEquals(response.getCount(), (Integer) 6);
        Assert.assertNotNull(response.getRecords().get(0).getEnvironmentIds());
        Assert.assertNotNull(response.getRecords().get(0).getServiceTypes());
        Assert.assertNotNull(response.getRecords().get(0).getServiceIds());
        Assert.assertNotNull(response.getRecords().get(0).getInfraIds());
        Assert.assertNotNull(response.getRecords().get(0).getCicdBranch());
        Assert.assertNotNull(response.getRecords().get(0).getRepoUrl());
    }
}
