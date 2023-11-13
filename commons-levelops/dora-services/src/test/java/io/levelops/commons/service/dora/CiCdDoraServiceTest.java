package io.levelops.commons.service.dora;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.CICDInstance;
import io.levelops.commons.databases.models.database.CICDJob;
import io.levelops.commons.databases.models.database.CICDJobRun;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.TriageRule;
import io.levelops.commons.databases.models.database.TriageRuleHit;
import io.levelops.commons.databases.models.database.organization.DBOrgContentSection;
import io.levelops.commons.databases.models.database.organization.DBOrgUnit;
import io.levelops.commons.databases.models.database.organization.OrgUnitCategory;
import io.levelops.commons.databases.models.database.velocity.VelocityConfigDTO;
import io.levelops.commons.databases.models.dora.DoraResponseDTO;
import io.levelops.commons.databases.models.dora.DoraSingleStateDTO;
import io.levelops.commons.databases.models.filters.CiCdJobRunsFilter;
import io.levelops.commons.databases.models.organization.OUConfiguration;
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
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.ingestion.models.IntegrationType;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.mockito.Mockito;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.mockito.Mockito.when;

public class CiCdDoraServiceTest {
    private static final String company = "test";
    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static CiCdInstancesDatabaseService ciCdInstancesDatabaseService;
    private static CiCdJobsDatabaseService ciCdJobsDatabaseService;
    private static CiCdJobRunsDatabaseService ciCdJobRunsDatabaseService;
    private static CiCdDoraService ciCdDoraService;

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
    private final List<CICDJobRun> ciCdJobRuns = new ArrayList<>();
    private final List<CICDJob> ciCdJobs = new ArrayList<>();
    private final List<String> ciCdJobIds = new ArrayList<>();
    private final List<Map<String, Object>> ciCdJobRunParams = new ArrayList<>();
    private static OrgUsersDatabaseService usersService;
    private static DBOrgUnit unit1;
    private static OrgUnitCategory orgUnitCategory;
    private static OrgUnitCategory orgGroup1, orgGroup2, orgGroup3, orgGroup4, orgGroup5;
    private static String orgGroupId1, orgGroupId2, orgGroupId3, orgGroupId4, orgGroupId5;

    @BeforeClass
    public static void setup() throws SQLException {
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
    private void setupJobsAndJobRuns(UUID cicdInstanceId, List<CiCdAggsServiceTest.JobDetails> allJobDetails) throws SQLException {
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

    public void insertTriageRuleHits(String cicdJobRunId, String rule) throws SQLException {

        TriageRuleHit ruleHit = TriageRuleHit.builder()
                .jobRunId(cicdJobRunId)
                .ruleId(rule)
                .hitContent("")
                .count(1)
                .build();
        triageRuleHitsService.insert(company, ruleHit);
    }

    private long calculateOffset() {
        long diff = Instant.now().getEpochSecond() - 1593062362;
        long days = diff / (TimeUnit.DAYS.toSeconds(1));
        long offset = days * (TimeUnit.DAYS.toSeconds(1));
        return offset;
    }

    private List<CiCdAggsServiceTest.JobDetails> fixJobRunTimestamps(List<CiCdAggsServiceTest.JobDetails> allJobDetails, Long offset) {
        if (CollectionUtils.isEmpty(allJobDetails)) {
            return allJobDetails;
        }
        return allJobDetails.stream()
                .map(jobDetails -> {
                    if (CollectionUtils.isEmpty(jobDetails.getRuns())) {
                        return jobDetails;
                    }
                    jobDetails.setRuns(
                            jobDetails.getRuns().stream()
                                    .map(run -> {
                                        run.setStartTime(run.getStartTime() + offset);
                                        run.setEndTime(run.getEndTime() != null ? run.getEndTime() + offset : null);
                                        return run;
                                    })
                                    .collect(Collectors.toList())
                    );
                    return jobDetails;
                })
                .collect(Collectors.toList());
    }

    @Test
    public void testCalculateNewDeploymentFrequency() throws IOException, SQLException {
        // configure
        String workFlowProfile = ResourceUtils.getResourceAsString("velocity/new_workflow_profile_cicd.json");
        VelocityConfigDTO velocityConfigDTO = mapper.readValue(workFlowProfile, VelocityConfigDTO.class);
        Long begin = Long.valueOf(1592613323);
        Long end = Long.valueOf(1593477323);

        String data = ResourceUtils.getResourceAsString("json/dora/cicd_plugin_result.json");
        List<CiCdAggsServiceTest.JobDetails> allJobDetails
                = MAPPER.readValue(data, MAPPER.getTypeFactory().constructCollectionType(
                List.class, CiCdAggsServiceTest.JobDetails.class
        ));
        allJobDetails = fixJobRunTimestamps(allJobDetails, calculateOffset());
        CICDInstance cicdInstance = CiCdInstanceUtils.createCiCdInstance(ciCdInstancesDatabaseService, company, 0);
        setupJobsAndJobRuns(null, allJobDetails);
        setupJobsAndJobRuns(cicdInstance.getId(), allJobDetails);
        // execute
        DoraResponseDTO doraResponseDTO = ciCdDoraService.calculateNewDeploymentFrequency(
                company,
                CiCdJobRunsFilter.builder()
                        .across(CiCdJobRunsFilter.DISTINCT.trend)
                        .endTimeRange(ImmutablePair.of(begin, end))
                        .calculation(CiCdJobRunsFilter.CALCULATION.count)
                        .jobNormalizedFullNames(new ArrayList<String>())
                        .build(),
                null,
                velocityConfigDTO,
                null
        );

        // assert
        Assert.assertTrue(doraResponseDTO.getTimeSeries().getDay().size() > 0);
        Assert.assertTrue(doraResponseDTO.getTimeSeries().getWeek().size() > 0);
        Assert.assertTrue(doraResponseDTO.getTimeSeries().getMonth().size() > 0);
        Assert.assertEquals(doraResponseDTO.getStats().getBand(), DoraSingleStateDTO.Band.LOW);
    }

    @Test
    public void calculateNewChangeFailureRate() throws IOException, SQLException {
        // configure
        String workFlowProfile = ResourceUtils.getResourceAsString("velocity/new_workflow_profile_cicd.json");
        VelocityConfigDTO velocityConfigDTO = mapper.readValue(workFlowProfile, VelocityConfigDTO.class);
        Long begin = Long.valueOf(1592613323);
        Long end = Long.valueOf(1593477323);

        String data = ResourceUtils.getResourceAsString("json/dora/cicd_plugin_result.json");
        List<CiCdAggsServiceTest.JobDetails> allJobDetails
                = MAPPER.readValue(data, MAPPER.getTypeFactory().constructCollectionType(
                List.class, CiCdAggsServiceTest.JobDetails.class
        ));
        allJobDetails = fixJobRunTimestamps(allJobDetails, calculateOffset());
        CICDInstance cicdInstance = CiCdInstanceUtils.createCiCdInstance(ciCdInstancesDatabaseService, company, 0);
        setupJobsAndJobRuns(null, allJobDetails);
        setupJobsAndJobRuns(cicdInstance.getId(), allJobDetails);

        // execute
        DoraResponseDTO doraResponseDTO = ciCdDoraService.calculateNewChangeFailureRate(
                company,
                CiCdJobRunsFilter.builder()
                        .across(CiCdJobRunsFilter.DISTINCT.trend)
                        .endTimeRange(ImmutablePair.of(begin, end))
                        .calculation(CiCdJobRunsFilter.CALCULATION.count)
                        .jobNormalizedFullNames(new ArrayList<String>())
                        .build(),
                CiCdJobRunsFilter.builder()
                        .across(CiCdJobRunsFilter.DISTINCT.trend)
                        .endTimeRange(ImmutablePair.of(begin, end))
                        .calculation(CiCdJobRunsFilter.CALCULATION.count)
                        .jobNormalizedFullNames(new ArrayList<String>())
                        .build(),
                null,
                velocityConfigDTO,
                null
        );

        // assert
        Assert.assertTrue(doraResponseDTO.getTimeSeries().getDay().size() > 0);
        Assert.assertTrue(doraResponseDTO.getTimeSeries().getWeek().size() > 0);
        Assert.assertTrue(doraResponseDTO.getTimeSeries().getMonth().size() > 0);
        Assert.assertEquals(doraResponseDTO.getStats().getBand(), DoraSingleStateDTO.Band.ELITE);
        Assert.assertFalse(doraResponseDTO.getStats().getIsAbsolute());
    }

    @Test
    public void testGetCiCdJobParams() throws SQLException, IOException {
        // configure
        String data = ResourceUtils.getResourceAsString("json/dora/cicd_plugin_result.json");
        List<CiCdAggsServiceTest.JobDetails> allJobDetails
                = MAPPER.readValue(data, MAPPER.getTypeFactory().constructCollectionType(
                List.class, CiCdAggsServiceTest.JobDetails.class
        ));
        allJobDetails = fixJobRunTimestamps(allJobDetails, calculateOffset());
        CICDInstance cicdInstance = CiCdInstanceUtils.createCiCdInstance(ciCdInstancesDatabaseService, company, 0);
        setupJobsAndJobRuns(cicdInstance.getId(), allJobDetails);

        // execute
        List<Map<String, Object>> result = ciCdDoraService.getCicdJobParams(company, ciCdJobIds);

        // assert
        Assert.assertTrue(ciCdJobRunParams.containsAll(result));
        for (CICDJobRun cicdJobRun : ciCdJobRuns) {
            Assert.assertTrue(ciCdJobIds.contains(cicdJobRun.getCicdJobId().toString()));
        }
    }

    @Test
    public void testCreateWhereClauseAndUpdateParamsJobRunsForCicdFilters() {

        Long begin = Long.valueOf(1592613323);
        Long end = Long.valueOf(1593477323);
        OUConfiguration ouConfig = OUConfiguration.builder()
                .ouId(UUID.fromString("c08c83ba-459d-4283-8eb6-55e1c3aa3995"))
                .ouRefId(32835)
                .sections(Set.of(
                        DBOrgContentSection.builder()
                                .id(UUID.fromString("c79b07d5-96ae-4c88-9f79-17a6a827ec8c"))
                                .integrationFilters(Map.of(
                                        "job_normalized_full_names", List.of("default/Propelo_Project/CD only Docker Registry", "default/Propelo_Project/CI only Docker Registry")))
                                .integrationId(4236)
                                .integrationType(IntegrationType.HARNESSNG)
                                .integrationName("QA HArness Satellite")
                                .defaultSection(false)
                                .build(),
                        DBOrgContentSection.builder()
                                .id(UUID.fromString("85052678-fb23-46f7-89aa-508e8f8cad4d"))
                                .defaultSection(true)
                                .build()))
                .request(DefaultListRequest.builder()
                        .filter(Map.of(
                                "job_normalized_full_names", List.of("default/Propelo_Project/CD only Docker Registry", "default/Propelo_Project/CI only Docker Registry",
                                        "integration_ids",  List.of(4236))))
                        .ouIds(Set.of(32835))
                        .build())
                .staticUsers(false)
                .dynamicUsers(false)
                .integrationIds(Set.of(4236))
                .build();
        CiCdJobRunsFilter ciCdJobRunsFilter = CiCdJobRunsFilter.builder()
                .across(CiCdJobRunsFilter.DISTINCT.trend)
                .endTimeRange(ImmutablePair.of(begin, end))
                .calculation(CiCdJobRunsFilter.CALCULATION.count)
                .jobNormalizedFullNames(new ArrayList<String>())
                .cicdUserIds(List.of("krina.vadgama"))
                .build();
        CiCdDoraService spyCiCdDoraService = Mockito.spy(ciCdDoraService);
        Map<String, List<String>> conditions = spyCiCdDoraService.
                createWhereClauseAndUpdateParamsJobRuns(company, ciCdJobRunsFilter, new HashMap<>(), "", false, ouConfig);

        Assert.assertTrue(conditions.get("cicd_conditions").contains("r.cicd_user_id IN (:r.cicd_user_id__)"));
    }

    @Test
    public void testCreateWhereClauseAndUpdateParamsJobRunsForBothCiCdFlag() {

        OUConfiguration ouConfig = OUConfiguration.builder()
                .ouId(UUID.fromString("c08c83ba-459d-4283-8eb6-55e1c3aa3995"))
                .ouRefId(32835)
                .sections(Set.of())
                .build();
        CiCdJobRunsFilter ciCdJobRunsFilter = CiCdJobRunsFilter.builder()
                .isCiJob(true)
                .isCdJob(true)
                .build();
        CiCdDoraService spyCiCdDoraService = Mockito.spy(ciCdDoraService);
        Map<String, List<String>> conditions = spyCiCdDoraService.
                createWhereClauseAndUpdateParamsJobRuns(company, ciCdJobRunsFilter, new HashMap<>(), "", false, ouConfig);

        Assert.assertTrue(conditions.get("cicd_conditions").contains("(r.cd = :is_cd_ OR r.ci = :is_ci_)"));
    }

    @Test
    public void testCreateWhereClauseAndUpdateParamsJobRunsForOneCiFlag() {

        OUConfiguration ouConfig = OUConfiguration.builder()
                .ouId(UUID.fromString("c08c83ba-459d-4283-8eb6-55e1c3aa3995"))
                .ouRefId(32835)
                .sections(Set.of())
                .build();
        CiCdJobRunsFilter ciCdJobRunsFilter = CiCdJobRunsFilter.builder()
                .isCiJob(true)
                .build();
        CiCdDoraService spyCiCdDoraService = Mockito.spy(ciCdDoraService);
        Map<String, List<String>> conditions = spyCiCdDoraService.
                createWhereClauseAndUpdateParamsJobRuns(company, ciCdJobRunsFilter, new HashMap<>(), "", false, ouConfig);

        Assert.assertTrue(conditions.get("cicd_conditions").contains("r.ci = :is_ci_"));
        Assert.assertFalse(conditions.get("cicd_conditions").contains("r.cd = :is_cd_"));
    }

    @Test
    public void testCreateWhereClauseAndUpdateParamsJobRunsForOneCdFlag() {

        OUConfiguration ouConfig = OUConfiguration.builder()
                .ouId(UUID.fromString("c08c83ba-459d-4283-8eb6-55e1c3aa3995"))
                .ouRefId(32835)
                .sections(Set.of())
                .build();
        CiCdJobRunsFilter ciCdJobRunsFilter = CiCdJobRunsFilter.builder()
                .isCdJob(true)
                .build();
        CiCdDoraService spyCiCdDoraService = Mockito.spy(ciCdDoraService);
        Map<String, List<String>> conditions = spyCiCdDoraService.
                createWhereClauseAndUpdateParamsJobRuns(company, ciCdJobRunsFilter, new HashMap<>(), "", false, ouConfig);

        Assert.assertFalse(conditions.get("cicd_conditions").contains("r.ci = :is_ci_"));
        Assert.assertTrue(conditions.get("cicd_conditions").contains("r.cd = :is_cd_"));
    }

    @Test
    public void testCreateWhereClauseAndUpdateParamsJobRunsForNoCiCdFlag() {

        OUConfiguration ouConfig = OUConfiguration.builder()
                .ouId(UUID.fromString("c08c83ba-459d-4283-8eb6-55e1c3aa3995"))
                .ouRefId(32835)
                .sections(Set.of())
                .build();
        CiCdJobRunsFilter ciCdJobRunsFilter = CiCdJobRunsFilter.builder().build();
        CiCdDoraService spyCiCdDoraService = Mockito.spy(ciCdDoraService);
        Map<String, List<String>> conditions = spyCiCdDoraService.
                createWhereClauseAndUpdateParamsJobRuns(company, ciCdJobRunsFilter, new HashMap<>(), "", false, ouConfig);

        Assert.assertNull(ciCdJobRunsFilter.getIsCiJob());
        Assert.assertNull(ciCdJobRunsFilter.getIsCdJob());
        Assert.assertFalse(conditions.get("cicd_conditions").contains("r.ci = :is_ci_"));
        Assert.assertFalse(conditions.get("cicd_conditions").contains("r.cd = :is_cd_"));
    }

    @Test
    public void testStageStepFilterCondition() {
        OUConfiguration ouConfig = OUConfiguration.builder()
                .ouId(UUID.fromString("c08c83ba-459d-4283-8eb6-55e1c3aa3995"))
                .ouRefId(32835)
                .sections(Set.of())
                .build();
        CiCdJobRunsFilter ciCdJobRunsFilter = CiCdJobRunsFilter.builder()
                .stageNames(List.of("build", "deploy"))
                .stageStatuses(List.of("success"))
                .stepNames(List.of("lint-test-job"))
                .stepStatuses(List.of("canceled"))
                .excludeStageNames(List.of("lint"))
                .excludeStepNames(List.of("build-job"))
                .build();
        CiCdDoraService spyCiCdDoraService = Mockito.spy(ciCdDoraService);
        Map<String, List<String>> conditions = spyCiCdDoraService.
                createWhereClauseAndUpdateParamsJobRuns(company, ciCdJobRunsFilter, new HashMap<>(), "", false, ouConfig);

        Assert.assertTrue(conditions.get("cicd_conditions").contains("cicd_job_run_stage.name IN (:stage_names_)"));
        Assert.assertTrue(conditions.get("cicd_conditions").contains("cicd_job_run_stage_steps.display_name IN (:step_names_)"));
        Assert.assertTrue(conditions.get("cicd_conditions").contains(" LOWER(cicd_job_run_stage.result)  LIKE ANY(LOWER(ARRAY[:stage_statuses_ ]::text)::text[])"));
        Assert.assertTrue(conditions.get("cicd_conditions").contains(" LOWER(cicd_job_run_stage_steps.result)  LIKE ANY(LOWER(ARRAY[:step_statuses_ ]::text)::text[])"));
        Assert.assertTrue(conditions.get("cicd_conditions").contains("r.id not in (Select cicd_job_run_id from test.cicd_job_run_stages where name IN (:exclude_stage_names_))"));
        Assert.assertTrue(conditions.get("cicd_conditions").contains("r.id not in ( select cicd_job_run_id from test.cicd_job_run_stages where id in (select cicd_job_run_stage_id from test.cicd_job_run_stage_steps where display_name in (:exclude_step_names_)))"));

        ciCdJobRunsFilter = CiCdJobRunsFilter.builder()
                .stageNames(List.of("build", "deploy"))
                .stepNames(List.of("build-job"))
                .excludeStageStatuses(List.of("passed", "passed_with_warnings"))
                .excludeStepStatuses(List.of("running", "skipped"))
                .build();

        conditions = spyCiCdDoraService.
                createWhereClauseAndUpdateParamsJobRuns(company, ciCdJobRunsFilter, new HashMap<>(), "", false, ouConfig);
        Assert.assertTrue(conditions.get("cicd_conditions").contains("r.id not in (Select cicd_job_run_id from test.cicd_job_run_stages where name IN (:exclude_stage_names_) and  LOWER(result)  LIKE ANY(LOWER(ARRAY[:exclude_stage_statuses_ ]::text)::text[]))"));
        Assert.assertTrue(conditions.get("cicd_conditions").contains("r.id not in ( select cicd_job_run_id from test.cicd_job_run_stages where id in (select cicd_job_run_stage_id from test.cicd_job_run_stage_steps where display_name in (:exclude_step_names_) and  LOWER(result)  LIKE ANY(LOWER(ARRAY[:exclude_step_statuses_ ]::text)::text[])))"));

    }

    @Test
    public void testStackingForPipelineOnDF() throws IOException, SQLException {
        String workFlowProfile = ResourceUtils.getResourceAsString("velocity/new_workflow_profile_cicd.json");
        VelocityConfigDTO velocityConfigDTO = mapper.readValue(workFlowProfile, VelocityConfigDTO.class);
        Long begin = Long.valueOf(1592613323);
        Long end = Long.valueOf(1593477323);

        String data = ResourceUtils.getResourceAsString("json/dora/cicd_plugin_result.json");
        List<CiCdAggsServiceTest.JobDetails> allJobDetails
                = MAPPER.readValue(data, MAPPER.getTypeFactory().constructCollectionType(
                List.class, CiCdAggsServiceTest.JobDetails.class
        ));
        CICDInstance cicdInstance = CiCdInstanceUtils.createCiCdInstance(ciCdInstancesDatabaseService, company, 0);
        setupJobsAndJobRuns(null, allJobDetails);
        setupJobsAndJobRuns(cicdInstance.getId(), allJobDetails);

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

        DoraResponseDTO response = ciCdDoraService.calculateNewDeploymentFrequency(company,
                CiCdJobRunsFilter.builder()
                        .across(CiCdJobRunsFilter.DISTINCT.trend)
                        .endTimeRange(ImmutablePair.of(begin, end))
                        .calculation(CiCdJobRunsFilter.CALCULATION.count)
                        .integrationIds(List.of("1"))
                        .build(),
                null,
                velocityConfigDTO,
                "job_name");

        DefaultObjectMapper.prettyPrint(response);
        Assert.assertEquals(response.getStats().getTotalDeployment(), (Integer) 4);
        Assert.assertEquals(response.getStats().getBand(), DoraSingleStateDTO.Band.HIGH);
        Assert.assertTrue(response.getTimeSeries().getDay().stream().filter(m -> m.getStacks() != null).count() > 0);
        Assert.assertTrue(response.getTimeSeries().getWeek().stream().filter(m -> m.getStacks() != null).count() > 0);
        Assert.assertTrue(response.getTimeSeries().getMonth().stream().filter(m -> m.getStacks() != null).count() > 0);
    }

    @Test
    public void testStackingForPipelineOnCFR() throws IOException, SQLException {
        String workFlowProfile = ResourceUtils.getResourceAsString("velocity/new_workflow_profile_cicd.json");
        VelocityConfigDTO velocityConfigDTO = mapper.readValue(workFlowProfile, VelocityConfigDTO.class);
        Long begin = Long.valueOf(1592613323);
        Long end = Long.valueOf(1593477323);

        String data = ResourceUtils.getResourceAsString("json/dora/cicd_plugin_result.json");
        List<CiCdAggsServiceTest.JobDetails> allJobDetails
                = MAPPER.readValue(data, MAPPER.getTypeFactory().constructCollectionType(
                List.class, CiCdAggsServiceTest.JobDetails.class
        ));
        CICDInstance cicdInstance = CiCdInstanceUtils.createCiCdInstance(ciCdInstancesDatabaseService, company, 0);
        setupJobsAndJobRuns(null, allJobDetails);
        setupJobsAndJobRuns(cicdInstance.getId(), allJobDetails);

        DbListResponse<CICDJob> jobs =  ciCdJobsDatabaseService.listByFilter(company, 0 , 100, null,
                Arrays.asList("Build-commons-levelops", "Build-Aggregations-Service", "Build-Server-API", "Deploy-Internal-Api-to-kubernetes-cluster"),
                null,
                null,
                List.of(cicdInstance.getId()));

        List<String> jobIdsInString = CollectionUtils.emptyIfNull(jobs.getRecords()).stream().map(m->m.getId().toString()).collect(Collectors.toList());

        velocityConfigDTO = velocityConfigDTO.toBuilder().changeFailureRate(
                        velocityConfigDTO.getChangeFailureRate().toBuilder().integrationIds(List.of(velocityConfigDTO.getChangeFailureRate().getIntegrationId()))
                                .velocityConfigFilters(velocityConfigDTO.getChangeFailureRate().getVelocityConfigFilters().toBuilder()
                                        .failedDeployment(velocityConfigDTO.getChangeFailureRate().getVelocityConfigFilters().getFailedDeployment().toBuilder()
                                                .calculationField(velocityConfigDTO.getChangeFailureRate().getVelocityConfigFilters().getFailedDeployment().getCalculationField())
                                                .event(velocityConfigDTO.getChangeFailureRate().getVelocityConfigFilters().getFailedDeployment().getEvent().toBuilder().values(jobIdsInString.subList(3,4)).build())
                                                .build())
                                        .totalDeployment(velocityConfigDTO.getChangeFailureRate().getVelocityConfigFilters().getTotalDeployment().toBuilder()
                                                .calculationField(velocityConfigDTO.getChangeFailureRate().getVelocityConfigFilters().getTotalDeployment().getCalculationField())
                                                .event(velocityConfigDTO.getChangeFailureRate().getVelocityConfigFilters().getTotalDeployment().getEvent().toBuilder().values(jobIdsInString.subList(0,3)).build())
                                                .build())
                                        .build())
                                .integrationIds(List.of(velocityConfigDTO.getChangeFailureRate().getIntegrationId()))
                                .build())
                .build();
        DoraResponseDTO response = ciCdDoraService.calculateNewChangeFailureRate(company,
                CiCdJobRunsFilter.builder()
                        .across(CiCdJobRunsFilter.DISTINCT.trend)
                        .endTimeRange(ImmutablePair.of(begin, end))
                        .calculation(CiCdJobRunsFilter.CALCULATION.count)
                        .jobNormalizedFullNames(List.of("Build-commons-levelops", "Build-Server-API", "Build-Aggregations-Service"))
                        .integrationIds(List.of("1"))
                        .build(),
                CiCdJobRunsFilter.builder()
                        .across(CiCdJobRunsFilter.DISTINCT.trend)
                        .endTimeRange(ImmutablePair.of(begin, end))
                        .calculation(CiCdJobRunsFilter.CALCULATION.count)
                        .jobNormalizedFullNames(List.of("Deploy-Internal-Api-to-kubernetes-cluster"))
                        .integrationIds(List.of("1"))
                        .build(),
                null,
                velocityConfigDTO,
                "job_name");

        DefaultObjectMapper.prettyPrint(response);
        Assert.assertEquals(response.getStats().getTotalDeployment(), (Integer) 1);
        Assert.assertEquals(response.getStats().getBand(), DoraSingleStateDTO.Band.LOW);
        Assert.assertTrue(response.getTimeSeries().getDay().stream().filter(m -> m.getStacks() != null).count() > 0);
        Assert.assertTrue(response.getTimeSeries().getWeek().stream().filter(m -> m.getStacks() != null).count() > 0);
        Assert.assertTrue(response.getTimeSeries().getMonth().stream().filter(m -> m.getStacks() != null).count() > 0);
    }

    @Test
    public void testStackingForPipelineOnDFForHarnessData() throws IOException, SQLException {
        String workFlowProfile = ResourceUtils.getResourceAsString("velocity/new_workflow_profile_cicd.json");
        VelocityConfigDTO velocityConfigDTO = mapper.readValue(workFlowProfile, VelocityConfigDTO.class);
        Long begin = Long.valueOf(1592613323);
        Long end = Long.valueOf(1593477323);

        String data = ResourceUtils.getResourceAsString("json/dora/cicd_job_runs_with_metadata.json");
        List<CiCdAggsServiceTest.JobDetails> allJobDetails
                = MAPPER.readValue(data, MAPPER.getTypeFactory().constructCollectionType(
                List.class, CiCdAggsServiceTest.JobDetails.class
        ));
        CICDInstance cicdInstance = CiCdInstanceUtils.createCiCdInstance(ciCdInstancesDatabaseService, company, 0);
        setupJobsAndJobRuns(null, allJobDetails);
        setupJobsAndJobRuns(cicdInstance.getId(), allJobDetails);

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

        DoraResponseDTO response = ciCdDoraService.calculateNewDeploymentFrequency(company,
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
                "job_name");

        DefaultObjectMapper.prettyPrint(response);
        Assert.assertEquals(response.getStats().getTotalDeployment(), (Integer) 3);
        Assert.assertEquals(response.getStats().getBand(), DoraSingleStateDTO.Band.HIGH);
        Assert.assertTrue(response.getTimeSeries().getDay().stream().filter(m -> m.getStacks() != null).count() > 0);
        Assert.assertTrue(response.getTimeSeries().getWeek().stream().filter(m -> m.getStacks() != null).count() > 0);
        Assert.assertTrue(response.getTimeSeries().getMonth().stream().filter(m -> m.getStacks() != null).count() > 0);
    }

    @Test
    public void testStackingForPipelineOnCFRForHarnessData() throws IOException, SQLException {
        String workFlowProfile = ResourceUtils.getResourceAsString("velocity/new_workflow_profile_cicd.json");
        VelocityConfigDTO velocityConfigDTO = mapper.readValue(workFlowProfile, VelocityConfigDTO.class);
        Long begin = Long.valueOf(1592613323);
        Long end = Long.valueOf(1593477323);

        String data = ResourceUtils.getResourceAsString("json/dora/cicd_job_runs_with_metadata.json");
        List<CiCdAggsServiceTest.JobDetails> allJobDetails
                = MAPPER.readValue(data, MAPPER.getTypeFactory().constructCollectionType(
                List.class, CiCdAggsServiceTest.JobDetails.class
        ));
        CICDInstance cicdInstance = CiCdInstanceUtils.createCiCdInstance(ciCdInstancesDatabaseService, company, 0);
        setupJobsAndJobRuns(null, allJobDetails);
        setupJobsAndJobRuns(cicdInstance.getId(), allJobDetails);

        DbListResponse<CICDJob> jobs =  ciCdJobsDatabaseService.listByFilter(company, 0 , 100, null,
                Arrays.asList("Build-commons-levelops", "Build-Aggregations-Service", "Build-Server-API", "Deploy-Internal-Api-to-kubernetes-cluster"),
                null,
                null,
                List.of(cicdInstance.getId()));

        List<String> jobIdsInString = CollectionUtils.emptyIfNull(jobs.getRecords()).stream().map(m->m.getId().toString()).collect(Collectors.toList());

        velocityConfigDTO = velocityConfigDTO.toBuilder().changeFailureRate(
                        velocityConfigDTO.getChangeFailureRate().toBuilder().integrationIds(List.of(velocityConfigDTO.getChangeFailureRate().getIntegrationId()))
                                .velocityConfigFilters(velocityConfigDTO.getChangeFailureRate().getVelocityConfigFilters().toBuilder()
                                        .failedDeployment(velocityConfigDTO.getChangeFailureRate().getVelocityConfigFilters().getFailedDeployment().toBuilder()
                                                .calculationField(velocityConfigDTO.getChangeFailureRate().getVelocityConfigFilters().getFailedDeployment().getCalculationField())
                                                .event(velocityConfigDTO.getChangeFailureRate().getVelocityConfigFilters().getFailedDeployment().getEvent().toBuilder().values(jobIdsInString.subList(3,4)).build())
                                                .build())
                                        .totalDeployment(velocityConfigDTO.getChangeFailureRate().getVelocityConfigFilters().getTotalDeployment().toBuilder()
                                                .calculationField(velocityConfigDTO.getChangeFailureRate().getVelocityConfigFilters().getTotalDeployment().getCalculationField())
                                                .event(velocityConfigDTO.getChangeFailureRate().getVelocityConfigFilters().getTotalDeployment().getEvent().toBuilder().values(jobIdsInString.subList(0,3)).build())
                                                .build())
                                        .build())
                                .integrationIds(List.of(velocityConfigDTO.getChangeFailureRate().getIntegrationId()))
                                .build())
                .build();
        DoraResponseDTO response = ciCdDoraService.calculateNewChangeFailureRate(company,
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
                        .build(),
                CiCdJobRunsFilter.builder()
                        .across(CiCdJobRunsFilter.DISTINCT.trend)
                        .endTimeRange(ImmutablePair.of(begin, end))
                        .jobNormalizedFullNames(List.of("Build-commons-levelops", "Build-Aggregations-Service", "Build-Server-API", "Deploy-Internal-Api-to-kubernetes-cluster"))
                        .calculation(CiCdJobRunsFilter.CALCULATION.count)
                        .build(),
                null,
                velocityConfigDTO,
                "job_name");

        DefaultObjectMapper.prettyPrint(response);
        Assert.assertEquals(response.getStats().getTotalDeployment(), (Integer) 3);
        Assert.assertEquals(response.getStats().getBand(), DoraSingleStateDTO.Band.MEDIUM);
        Assert.assertTrue(response.getTimeSeries().getDay().stream().filter(m -> m.getStacks() != null).count() > 0);
        Assert.assertTrue(response.getTimeSeries().getWeek().stream().filter(m -> m.getStacks() != null).count() > 0);
        Assert.assertTrue(response.getTimeSeries().getMonth().stream().filter(m -> m.getStacks() != null).count() > 0);
    }
}
