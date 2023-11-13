package io.levelops.commons.databases.services;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.*;
import io.levelops.commons.databases.models.database.cicd.JobRunStage;
import io.levelops.commons.databases.models.database.cicd.JobRunStageStep;
import io.levelops.commons.databases.models.database.organization.*;
import io.levelops.commons.databases.models.filters.CICD_AGG_INTERVAL;
import io.levelops.commons.databases.models.filters.CICD_TYPE;
import io.levelops.commons.databases.models.filters.CiCdJobConfigChangesFilter;
import io.levelops.commons.databases.models.filters.CiCdJobQualifiedName;
import io.levelops.commons.databases.models.filters.CiCdJobRunParameter;
import io.levelops.commons.databases.models.filters.CiCdJobRunsFilter;
import io.levelops.commons.databases.models.response.AcrossUniqueKey;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.organization.*;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.gitlab.models.GitlabJob;
import io.levelops.integrations.gitlab.models.GitlabPipeline;
import io.levelops.integrations.gitlab.models.GitlabProject;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.dao.DuplicateKeyException;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.levelops.commons.databases.services.CiCdDateUtils.extractDataComponentForDbResults;
import static org.assertj.core.api.Assertions.assertThat;

@Log4j2
@SuppressWarnings("unused")
public class CiCdAggsServiceTest {
    private static final Integer PAGE_NUMBER = 0;
    private static final Integer PAGE_SIZE = 300;
    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();
    private static final String company = "test";
    private static final String company1 = "gitlab";
    private static final boolean VALUES_ONLY = false;


    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;
    private static final ObjectMapper mapper = DefaultObjectMapper.get();
    private static UserService userService;
    private static CiCdInstancesDatabaseService ciCdInstancesDatabaseService;
    private static CiCdJobsDatabaseService ciCdJobsDatabaseService;
    private static CiCdJobRunsDatabaseService ciCdJobRunsDatabaseService;
    private static CiCdJobConfigChangesDatabaseService ciCdJobConfigChangesDatabaseService;
    private static CiCdAggsService ciCdAggsService;
    private static IntegrationService integrationService;
    private static ProductsDatabaseService productsDatabaseService;
    private static ProductService productService;
    private static CiCdJobRunStageDatabaseService ciCdJobRunStageDatabaseService;
    private static CiCdJobRunStageStepsDatabaseService ciCdJobRunStageStepsDatabaseService;
    private static TriageRulesService triageRulesService;
    private static TriageRuleHitsService triageRuleHitsService;
    private static String rule1;
    private static String rule2;
    private static String rule3;

    private static Integration integration;
    private static Integration integration2;
    private static Integration integration3;
    private static Integration integration4;
    private static String integrationId2;
    private static String integrationId3;
    private static String integrationId4;
    private final List<CICDJobRun> ciCdJobRunsWithMetadata = new ArrayList<>();
    private final List<CICDJobRun> ciCdJobRuns = new ArrayList<>();
    private final List<CICDJob> ciCdJobs = new ArrayList<>();
    private final List<CICDJob> ciCdJobsWithMetadata = new ArrayList<>();

    private static OrgUnitHelper unitsHelper;
    private static OrgUsersDatabaseService usersService;
    private static OrgUnitsDatabaseService unitsService;
    private static DBOrgUnit unit1;
    private static OrgUnitCategoryDatabaseService orgUnitCategoryDatabaseService;
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
        ciCdAggsService = new CiCdAggsService(dataSource);
        new DatabaseSchemaService(dataSource)
                .ensureSchemaExistence(company);
        integrationService = new IntegrationService(dataSource);
        integrationService.ensureTableExistence(company);
        ciCdJobRunStageDatabaseService = new CiCdJobRunStageDatabaseService(dataSource, DefaultObjectMapper.get());
        ciCdJobRunStageStepsDatabaseService = new CiCdJobRunStageStepsDatabaseService(dataSource);
        triageRulesService = new TriageRulesService(dataSource);
        triageRuleHitsService = new TriageRuleHitsService(dataSource, DefaultObjectMapper.get());

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
        userService.ensureTableExistence(company);
        productsDatabaseService = new ProductsDatabaseService(dataSource, MAPPER);
        productsDatabaseService.ensureTableExistence(company);
        productService = new ProductService(dataSource);
        productService.ensureTableExistence(company);
        ciCdInstancesDatabaseService.ensureTableExistence(company);
        ciCdJobsDatabaseService.ensureTableExistence(company);
        ciCdJobRunsDatabaseService.ensureTableExistence(company);
        ciCdJobConfigChangesDatabaseService.ensureTableExistence(company);
        ciCdAggsService.ensureTableExistence(company);

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
        orgUnitCategoryDatabaseService = new OrgUnitCategoryDatabaseService(dataSource, unitsHelper, mapper);
        orgUnitCategoryDatabaseService.ensureTableExistence(company);
        unitsService = new OrgUnitsDatabaseService(dataSource, MAPPER, tagItemService, usersService, versionsService, dashboardWidgetService);
        unitsService.ensureTableExistence(company);
        unitsHelper = new OrgUnitHelper(unitsService, integrationService);

        new DatabaseSchemaService(dataSource)
                .ensureSchemaExistence(company1);
        integrationService.ensureTableExistence(company1);
        integrationService.insert(company1, integration);
        userService.ensureTableExistence(company1);
        productsDatabaseService.ensureTableExistence(company1);
        productService.ensureTableExistence(company1);
        ciCdInstancesDatabaseService.ensureTableExistence(company1);
        ciCdJobsDatabaseService.ensureTableExistence(company1);
        ciCdJobRunsDatabaseService.ensureTableExistence(company1);
        ciCdJobRunStageDatabaseService.ensureTableExistence(company1);
        ciCdJobRunStageStepsDatabaseService.ensureTableExistence(company1);
        ciCdJobConfigChangesDatabaseService.ensureTableExistence(company1);
        ciCdAggsService.ensureTableExistence(company1);
        triageRulesService.ensureTableExistence(company1);
        triageRuleHitsService.ensureTableExistence(company1);

        orgGroup1 = OrgUnitCategory.builder()
                .name("TEAM A")
                .description("Sample team")
                .isPredefined(true)
                .build();
        orgGroupId1 = orgUnitCategoryDatabaseService.insert(company, orgGroup1);

        orgGroup2 = OrgUnitCategory.builder()
                .name("TEAM B")
                .description("Sample team")
                .isPredefined(true)
                .build();
        orgGroupId2 = orgUnitCategoryDatabaseService.insert(company, orgGroup2);
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

        integration4 = Integration.builder()
                .name("gitlab_integration")
                .status("status-" + 0).application("jenkins").url("http://www.dummy.com")
                .satellite(false).build();
        integrationId4 = integrationService.insert(company, integration4);

        var orgUser1 = DBOrgUser.builder()
                .email("email1")
                .fullName("fullName1")
                .customFields(Map.of("test_name", "test1"))
                .ids(Set.of(DBOrgUser.LoginId.builder().cloudId("testread").username("cloudId").integrationType(integration2.getApplication())
                        .integrationId(Integer.parseInt(integrationId2)).build()))
                .versions(Set.of(1))
                .build();
        var userId1 = usersService.upsert(company, orgUser1);

        var orgUser2 = DBOrgUser.builder()
                .email("email2")
                .fullName("fullName2")
                .customFields(Map.of("sample_name", "sample"))
                .ids(Set.of(DBOrgUser.LoginId.builder().cloudId("SCMTrigger").integrationId(Integer.parseInt(integrationId2)).build()))
                .versions(Set.of(2, 3))
                .build();
        var userId2 = usersService.upsert(company, orgUser2);
        var orgUser3 = DBOrgUser.builder()
                .email("email3")
                .fullName("fullName3")
                .customFields(Map.of("test_name", "test1"))
                .ids(Set.of(DBOrgUser.LoginId.builder().cloudId("SYSTEM").username("cloudId").integrationType(integration2.getApplication())
                        .integrationId(Integer.parseInt(integrationId2)).build()))
                .versions(Set.of(1))
                .build();
        var userId3 = usersService.upsert(company, orgUser3);
        var manager1 = OrgUserId.builder().id(userId1.getId()).refId(userId1.getRefId()).fullName(orgUser1.getFullName()).email(orgUser1.getEmail()).build();
        var manager2 = OrgUserId.builder().id(userId2.getId()).refId(userId2.getRefId()).fullName(orgUser2.getFullName()).email(orgUser2.getEmail()).build();
        var managers = Set.of(
                manager1,
                manager2
        );
        unit1 = DBOrgUnit.builder()
                .name("unit4")
                .description("My unit4")
                .active(true)
                .versions(Set.of(1))
                .managers(managers)
                .ouCategoryId(UUID.fromString(orgGroupId1))
                .sections(Set.of(DBOrgContentSection.builder()
                        .integrationFilters(Map.of("cicd_user_ids", List.of(userId1.getId(), userId2.getId(), userId3.getId())))
                        .defaultSection(false)
                        .users(Set.of(1, 2, 3))
                        .build()))
                .refId(1)
                .build();
        unitsService.insertForId(company, unit1);
    }

    @Before
    public void cleanup() throws SQLException {
        dataSource.getConnection().prepareStatement("DELETE FROM test.cicd_job_run_stage_steps;").execute();
        dataSource.getConnection().prepareStatement("DELETE FROM test.cicd_job_run_stages;").execute();
        dataSource.getConnection().prepareStatement("DELETE FROM test.cicd_jobs;").execute();
    }

    // region Setup Jobs and Job Runs
    private void setupJobsAndJobRuns(UUID cicdInstanceId, List<JobDetails> allJobDetails) throws SQLException {
        for (JobDetails currentJobAllRuns : allJobDetails) {
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
            Assert.assertNotNull(cicdJobIdString);
            ciCdJobs.add(cicdJob);
            UUID cicdJobId = UUID.fromString(cicdJobIdString);
            int count = 0;
            for (JobRunDetails currentJobRun : currentJobAllRuns.getRuns()) {
                CICDJobRun.CICDJobRunBuilder bldr = CICDJobRun.builder()
                        .cicdJobId(cicdJobId)
                        .jobRunNumber(currentJobRun.getNumber())
                        .status(currentJobRun.getStatus())
                        .startTime(Instant.ofEpochSecond(currentJobRun.getStartTime()))
                        .duration(currentJobRun.getDuration().intValue())
                        .cicdUserId(currentJobRun.getUserId())
                        .source(CICDJobRun.Source.ANALYTICS_PERIODIC_PUSH)
                        .referenceId(UUID.randomUUID().toString())
                        .scmCommitIds(currentJobRun.getCommitIds());
                if (CollectionUtils.isNotEmpty(currentJobRun.getParams())) {
                    List<CICDJobRun.JobRunParam> params = new ArrayList<>();
                    for (JobRunParam currentParam : currentJobRun.getParams()) {
                        CICDJobRun.JobRunParam sanitized = CICDJobRun.JobRunParam.builder()
                                .type(currentParam.getType())
                                .name(currentParam.getName())
                                .value(currentParam.getValue())
                                .build();
                        params.add(sanitized);
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

    private void setupJobsAndJobRunsWithMetadata(UUID cicdInstanceId, List<JobDetails> allJobDetails) throws SQLException {
        for (JobDetails currentJobAllRuns : allJobDetails) {
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
            Assert.assertNotNull(cicdJobIdString);
            ciCdJobsWithMetadata.add(cicdJob);
            UUID cicdJobId = UUID.fromString(cicdJobIdString);
            int count = 0;
            for (JobRunDetails currentJobRun : currentJobAllRuns.getRuns()) {
                CICDJobRun.CICDJobRunBuilder bldr = CICDJobRun.builder()
                        .cicdJobId(cicdJobId)
                        .jobRunNumber(currentJobRun.getNumber())
                        .status(currentJobRun.getStatus())
                        .startTime(Instant.ofEpochSecond(currentJobRun.getStartTime()))
                        .duration(currentJobRun.getDuration().intValue())
                        .cicdUserId(currentJobRun.getUserId())
                        .metadata(currentJobRun.getMetadata())
                        .scmCommitIds(currentJobRun.getCommitIds());

                CICDJobRun cicdJobRun = bldr.build();
                String cicdJobRunId = ciCdJobRunsDatabaseService.insert(company, cicdJobRun);
                ciCdJobRunsWithMetadata.add(cicdJobRun);
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

    private List<JobDetails> fixJobRunTimestamps(List<JobDetails> allJobDetails, Long offset) {
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

    // region Job Runs
    @Test
    public void testGroupByAndCalculateCiCdJobs() throws SQLException, IOException {
        // region Setup
        String data = ResourceUtils.getResourceAsString("json/databases/jenkins_plugin_result.json");
        List<JobDetails> allJobDetails = MAPPER.readValue(data, MAPPER.getTypeFactory().constructCollectionType(List.class, JobDetails.class));
        allJobDetails = fixJobRunTimestamps(allJobDetails, calculateOffset());
        CICDInstance cicdInstance = CiCdInstanceUtils.createCiCdInstance(ciCdInstancesDatabaseService, company, 0);
        setupJobsAndJobRuns(null, allJobDetails);
        setupJobsAndJobRuns(cicdInstance.getId(), allJobDetails);
        // endregion

        // region trend - count
        List<Long> listOfInputDates = ciCdJobRuns.stream().map(CICDJobRun::getStartTime)
                .map(Instant::getEpochSecond)
                .collect(Collectors.toList());
        List<String> actualList;
        List<String> expectedList;

        DbListResponse<DbAggregationResult> dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder().across(CiCdJobRunsFilter.DISTINCT.triage_rule)
                        .calculation(CiCdJobRunsFilter.CALCULATION.count).build(), VALUES_ONLY);

        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(3, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(3, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(3, dbAggsResponse.getRecords().size());
        List<DbAggregationResult> expected = List.of(
                DbAggregationResult.builder().key("test-rule1").additionalKey(rule1).count(74L).build(),
                DbAggregationResult.builder().key("test-rule2").additionalKey(rule2).count(58L).build(),
                DbAggregationResult.builder().key("test-rule3").additionalKey(rule3).count(0l).build()
        );
        verifyRecords(dbAggsResponse.getRecords(), expected, false);

        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder().across(CiCdJobRunsFilter.DISTINCT.trend)
                        .aggInterval(CICD_AGG_INTERVAL.month)
                        .calculation(CiCdJobRunsFilter.CALCULATION.count).build(),
                VALUES_ONLY
        );
        expectedList = listOfInputDates.stream()
                .filter(Objects::nonNull)
                .map(instant -> extractDataComponentForDbResults(Date.from(Instant.ofEpochSecond(instant)),
                        Calendar.MONTH, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        Assert.assertNotNull(dbAggsResponse);
        Assertions.assertThat(dbAggsResponse.getCount().intValue()).isIn(1, 2); // depends on the day of the months...
        Assertions.assertThat(dbAggsResponse.getTotalCount().intValue()).isIn(1, 2); // depends on the day of the months...
        Assertions.assertThat(dbAggsResponse.getRecords().size()).isIn(1, 2); // depends on the day of the months...
        actualList = dbAggsResponse.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .map(key -> new Date(TimeUnit.MILLISECONDS.toMicros(Long.parseLong(key))))
                .map(date -> extractDataComponentForDbResults(date, Calendar.MONTH, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        Assert.assertNotNull(dbAggsResponse);
        assertThat(expectedList.containsAll(actualList)).isEqualTo(true);
        assertThat(dbAggsResponse.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()))
                .isNotNull();
        // endregion

        // region trend - duration
        expectedList = listOfInputDates.stream()
                .filter(Objects::nonNull)
                .map(instant -> extractDataComponentForDbResults(new Date(TimeUnit.MILLISECONDS.toMicros(instant)),
                        Calendar.YEAR, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder().across(CiCdJobRunsFilter.DISTINCT.trend)
                        .aggInterval(CICD_AGG_INTERVAL.year)
                        .calculation(CiCdJobRunsFilter.CALCULATION.duration).build(),
                VALUES_ONLY
        );
        int count = new HashSet<>(expectedList).size();
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(count, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(count, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(count, dbAggsResponse.getRecords().size());
        actualList = dbAggsResponse.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .map(key -> new Date(TimeUnit.MILLISECONDS.toMicros(Long.parseLong(key))))
                .map(date -> extractDataComponentForDbResults(date, Calendar.YEAR, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        Assert.assertNotNull(dbAggsResponse);
        assertThat(expectedList.containsAll(actualList)).isEqualTo(true);
        assertThat(dbAggsResponse.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()))
                .isNotNull();
        // endregion

        //region cicd_job_id - count
        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder().across(CiCdJobRunsFilter.DISTINCT.cicd_job_id).calculation(CiCdJobRunsFilter.CALCULATION.count).build(),
                VALUES_ONLY
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(22, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(22, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(22, dbAggsResponse.getRecords().size());
        Assert.assertEquals(42, dbAggsResponse.getRecords().get(0).getCount().longValue());
        Assert.assertEquals("Pipe2", dbAggsResponse.getRecords().get(0).getKey());
        //endregion


        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder()
                        .across(CiCdJobRunsFilter.DISTINCT.cicd_user_id)
                        .stacks(List.of(CiCdJobRunsFilter.DISTINCT.cicd_user_id))
                        .calculation(CiCdJobRunsFilter.CALCULATION.count).build(),
                VALUES_ONLY
        );

        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(3, dbAggsResponse.getRecords().size());

        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder()
                        .across(CiCdJobRunsFilter.DISTINCT.qualified_job_name)
                        .stacks(List.of(CiCdJobRunsFilter.DISTINCT.job_name))
                        .calculation(CiCdJobRunsFilter.CALCULATION.count).build(),
                VALUES_ONLY
        );

        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(22, dbAggsResponse.getRecords().size());

        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder()
                        .across(CiCdJobRunsFilter.DISTINCT.job_name)
                        .stacks(List.of(CiCdJobRunsFilter.DISTINCT.job_normalized_full_name))
                        .calculation(CiCdJobRunsFilter.CALCULATION.count).build(),
                VALUES_ONLY
        );

        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(11, dbAggsResponse.getRecords().size());

        // region job_name - count
        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder().across(CiCdJobRunsFilter.DISTINCT.job_name).calculation(CiCdJobRunsFilter.CALCULATION.count).build(),
                VALUES_ONLY
        );

        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(11, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(11, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(11, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("Pipe2").count(84L).build(),
                DbAggregationResult.builder().key("Pipe3").count(18L).build(),
                DbAggregationResult.builder().key("openapi-generator").count(10L).build(),
                DbAggregationResult.builder().key("Deploy-Server_API-to-Kubernetes-Cluster").count(4L).build(),
                DbAggregationResult.builder().key("com.wordnik$swagger-codegen_2.9.1").count(4L).build(),
                DbAggregationResult.builder().key("Build-Internal-API").count(2L).build(),
                DbAggregationResult.builder().key("Build-commons-levelops").count(2L).build(),
                DbAggregationResult.builder().key("Build-Server-API").count(2L).build(),
                DbAggregationResult.builder().key("Deploy-Aggregations-Service").count(2L).build(),
                DbAggregationResult.builder().key("Build-Aggregations-Service").count(2L).build(),
                DbAggregationResult.builder().key("Deploy-Internal-Api-to-kubernetes-cluster").count(2L).build()
        );
        Assert.assertEquals(expected, dbAggsResponse.getRecords());
        // endregion


        // region job normalized full name - count

        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder()
                        .across(CiCdJobRunsFilter.DISTINCT.job_normalized_full_name)
                        .calculation(CiCdJobRunsFilter.CALCULATION.count)
                        .build(),
                VALUES_ONLY
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(11, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(11, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(11, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("Pipe2").count(84L).build(),
                DbAggregationResult.builder().key("Pipe3").count(18L).build(),
                DbAggregationResult.builder().key("openapi-generator").count(10L).build(),
                DbAggregationResult.builder().key("Deploy-Server_API-to-Kubernetes-Cluster").count(4L).build(),
                DbAggregationResult.builder().key("com.wordnik$swagger-codegen_2.9.1").count(4L).build(),
                DbAggregationResult.builder().key("Build-Internal-API").count(2L).build(),
                DbAggregationResult.builder().key("Build-commons-levelops").count(2L).build(),
                DbAggregationResult.builder().key("Build-Server-API").count(2L).build(),
                DbAggregationResult.builder().key("Deploy-Aggregations-Service").count(2L).build(),
                DbAggregationResult.builder().key("Build-Aggregations-Service").count(2L).build(),
                DbAggregationResult.builder().key("Deploy-Internal-Api-to-kubernetes-cluster").count(2L).build()
        );
        verifyRecords(dbAggsResponse.getRecords(), expected, false);
        // endregion

        // region job normalized full name - count - stacks cicd user id

        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder()
                        .across(CiCdJobRunsFilter.DISTINCT.job_normalized_full_name)
                        .stacks(List.of(CiCdJobRunsFilter.DISTINCT.cicd_user_id))
                        .calculation(CiCdJobRunsFilter.CALCULATION.count)
                        .build(),
                VALUES_ONLY
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(11, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(11, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(11, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("Pipe2").count(84L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("testread").count(84L).build()
                        )).build(),
                DbAggregationResult.builder().key("Pipe3").count(18L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("testread").count(18L).build()
                        )).build(),
                DbAggregationResult.builder().key("openapi-generator").count(10L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("testread").count(10L).build()
                        )).build(),
                DbAggregationResult.builder().key("Deploy-Server_API-to-Kubernetes-Cluster").count(4L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("SCMTrigger").count(4L).build()
                        )).build(),
                DbAggregationResult.builder().key("com.wordnik$swagger-codegen_2.9.1").count(4L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("SYSTEM").count(4L).build()
                        )).build(),
                DbAggregationResult.builder().key("Build-Internal-API").count(2L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("SCMTrigger").count(2L).build()
                        )).build(),
                DbAggregationResult.builder().key("Build-commons-levelops").count(2L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("SCMTrigger").count(2L).build()
                        )).build(),
                DbAggregationResult.builder().key("Build-Server-API").count(2L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("SCMTrigger").count(2L).build()
                        )).build(),
                DbAggregationResult.builder().key("Deploy-Aggregations-Service").count(2L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("SCMTrigger").count(2L).build()
                        )).build(),
                DbAggregationResult.builder().key("Build-Aggregations-Service").count(2L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("SCMTrigger").count(2L).build()
                        )).build(),
                DbAggregationResult.builder().key("Deploy-Internal-Api-to-kubernetes-cluster").count(2L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("SCMTrigger").count(2L).build()
                        )).build()
        );
        verifyRecords(dbAggsResponse.getRecords(), expected, false);
        // endregion

        // region job normalized full name - count - with job normalized full name filter
        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder().jobNormalizedFullNames(List.of("Build-Internal-API", "Build-Server-API"))
                        .across(CiCdJobRunsFilter.DISTINCT.job_normalized_full_name)
                        .calculation(CiCdJobRunsFilter.CALCULATION.count)
                        .build(),
                VALUES_ONLY
        );

        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(2, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("Build-Internal-API").count(2L).build(),
                DbAggregationResult.builder().key("Build-Server-API").count(2L).build()
        );
        verifyRecords(dbAggsResponse.getRecords(), expected, false);
        // endregion

        // region job normalized full name - count - with invalid job normalized full name filter

        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder().jobNormalizedFullNames(List.of("invalid"))
                        .across(CiCdJobRunsFilter.DISTINCT.job_normalized_full_name)
                        .calculation(CiCdJobRunsFilter.CALCULATION.count)
                        .build(),
                VALUES_ONLY
        );

        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(0, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(0, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(0, dbAggsResponse.getRecords().size());
        // endregion


        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder().across(CiCdJobRunsFilter.DISTINCT.project_name)
                        .stacks(List.of(CiCdJobRunsFilter.DISTINCT.job_name)).calculation(CiCdJobRunsFilter.CALCULATION.count)
                        .build(),
                VALUES_ONLY
        );
        DefaultObjectMapper.prettyPrint(dbAggsResponse);
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(1, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getTotalCount().intValue());

        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder().across(CiCdJobRunsFilter.DISTINCT.job_name)
                        .stacks(List.of(CiCdJobRunsFilter.DISTINCT.project_name)).calculation(CiCdJobRunsFilter.CALCULATION.count)
                        .build(),
                VALUES_ONLY
        );
        DefaultObjectMapper.prettyPrint(dbAggsResponse);
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(11, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(11, dbAggsResponse.getTotalCount().intValue());

        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder().across(CiCdJobRunsFilter.DISTINCT.project_name)
                        .projects(List.of("project-1")).calculation(CiCdJobRunsFilter.CALCULATION.count)
                        .build(),
                VALUES_ONLY
        );
        DefaultObjectMapper.prettyPrint(dbAggsResponse);
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(1, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getTotalCount().intValue());

        // region job normalized full name - count - with partial job normalized full name filter
        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder()
                        .partialMatch(Map.of("job_normalized_full_name", Map.of("$contains", "Build")))
                        .across(CiCdJobRunsFilter.DISTINCT.job_normalized_full_name)
                        .calculation(CiCdJobRunsFilter.CALCULATION.count)
                        .build(),
                VALUES_ONLY
        );

        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(4, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(4, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(4, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("Build-commons-levelops").count(2L).build(),
                DbAggregationResult.builder().key("Build-Internal-API").count(2L).build(),
                DbAggregationResult.builder().key("Build-Aggregations-Service").count(2L).build(),
                DbAggregationResult.builder().key("Build-Server-API").count(2L).build()
        );
        verifyRecords(dbAggsResponse.getRecords(), expected, false);
        // endregion

        // region job normalized full name - count - with invalid partial job normalized full name filter
        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder()
                        .partialMatch(Map.of("job_normalized_full_name", Map.of("$contains", "BB1New")))
                        .across(CiCdJobRunsFilter.DISTINCT.job_normalized_full_name)
                        .stacks(List.of(CiCdJobRunsFilter.DISTINCT.cicd_user_id))
                        .calculation(CiCdJobRunsFilter.CALCULATION.count)
                        .build(),
                VALUES_ONLY
        );

        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(0, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(0, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(0, dbAggsResponse.getRecords().size());
        // endregion

        // region job_name - count
        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder().across(CiCdJobRunsFilter.DISTINCT.instance_name).calculation(CiCdJobRunsFilter.CALCULATION.count).build(),
                VALUES_ONLY
        );

        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(1, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("instance-name-0").count(66L).build()
        );
        Assert.assertEquals(expected, dbAggsResponse.getRecords());

        // endregion

        // region instance name - count - with partial type filter
        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder()
                        .partialMatch(Map.of("type", Map.of("$contains", "jenkins")))
                        .across(CiCdJobRunsFilter.DISTINCT.instance_name)
                        .calculation(CiCdJobRunsFilter.CALCULATION.count)
                        .build(),
                VALUES_ONLY
        );

        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(1, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("instance-name-0").count(66L).build()
        );
        verifyRecords(dbAggsResponse.getRecords(), expected, false);
        // endregion

        // region job_name - duration
        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder().across(CiCdJobRunsFilter.DISTINCT.job_name).calculation(CiCdJobRunsFilter.CALCULATION.duration).build(),
                VALUES_ONLY
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(11, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(11, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(11, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("Build-commons-levelops").count(2L).sum(398L).min(199L).max(199L).median(199L).build(),
                DbAggregationResult.builder().key("Build-Internal-API").count(2L).sum(208L).min(104L).max(104L).median(104L).build(),
                DbAggregationResult.builder().key("Build-Aggregations-Service").count(2L).sum(184L).min(92L).max(92L).median(92L).build(),
                DbAggregationResult.builder().key("Build-Server-API").count(2L).sum(158L).min(79L).max(79L).median(79L).build(),
                DbAggregationResult.builder().key("com.wordnik$swagger-codegen_2.9.1").count(4L).sum(464L).min(69L).max(163L).median(69L).build(),
                DbAggregationResult.builder().key("Pipe2").count(84L).sum(516702L).min(1L).max(121500L).median(69L).build(),
                DbAggregationResult.builder().key("Deploy-Aggregations-Service").count(2L).sum(104L).min(52L).max(52L).median(52L).build(),
                DbAggregationResult.builder().key("Deploy-Server_API-to-Kubernetes-Cluster").count(4L).sum(228L).min(51L).max(63L).median(51L).build(),
                DbAggregationResult.builder().key("Deploy-Internal-Api-to-kubernetes-cluster").count(2L).sum(92L).min(46L).max(46L).median(46L).build(),
                DbAggregationResult.builder().key("openapi-generator").count(10L).sum(1040L).min(35L).max(254L).median(43L).build(),
                DbAggregationResult.builder().key("Pipe3").count(18L).sum(320L).min(12L).max(48L).median(14L).build()
        );
        Assertions.assertThat(dbAggsResponse.getRecords()).containsAll(expected);
        // endregion

        // region qualified job_name - count
        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder().across(CiCdJobRunsFilter.DISTINCT.qualified_job_name).calculation(CiCdJobRunsFilter.CALCULATION.count).build(),
                VALUES_ONLY
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(22, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(22, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(22, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("Pipe2").additionalKey("instance-name-0").count(42L).build(),
                DbAggregationResult.builder().key("Pipe2").additionalKey(null).count(42L).build(),
                DbAggregationResult.builder().key("Pipe3").additionalKey("instance-name-0").count(9L).build(),
                DbAggregationResult.builder().key("Pipe3").additionalKey(null).count(9L).build(),
                DbAggregationResult.builder().key("openapi-generator").additionalKey("instance-name-0").count(5L).build(),
                DbAggregationResult.builder().key("openapi-generator").additionalKey(null).count(5L).build(),
                DbAggregationResult.builder().key("Deploy-Server_API-to-Kubernetes-Cluster").additionalKey("instance-name-0").count(2L).build(),
                DbAggregationResult.builder().key("Deploy-Server_API-to-Kubernetes-Cluster").additionalKey(null).count(2L).build(),
                DbAggregationResult.builder().key("com.wordnik$swagger-codegen_2.9.1").additionalKey("instance-name-0").count(2L).build(),
                DbAggregationResult.builder().key("com.wordnik$swagger-codegen_2.9.1").additionalKey(null).count(2L).build(),
                DbAggregationResult.builder().key("Build-Internal-API").additionalKey("instance-name-0").count(1L).build(),
                DbAggregationResult.builder().key("Build-Internal-API").additionalKey(null).count(1L).build(),
                DbAggregationResult.builder().key("Build-commons-levelops").additionalKey("instance-name-0").count(1L).build(),
                DbAggregationResult.builder().key("Build-commons-levelops").additionalKey(null).count(1L).build(),
                DbAggregationResult.builder().key("Build-Server-API").additionalKey("instance-name-0").count(1L).build(),
                DbAggregationResult.builder().key("Build-Server-API").additionalKey(null).count(1L).build(),
                DbAggregationResult.builder().key("Deploy-Aggregations-Service").additionalKey("instance-name-0").count(1L).build(),
                DbAggregationResult.builder().key("Deploy-Aggregations-Service").additionalKey(null).count(1L).build(),
                DbAggregationResult.builder().key("Build-Aggregations-Service").additionalKey("instance-name-0").count(1L).build(),
                DbAggregationResult.builder().key("Build-Aggregations-Service").additionalKey(null).count(1L).build(),
                DbAggregationResult.builder().key("Deploy-Internal-Api-to-kubernetes-cluster").additionalKey("instance-name-0").count(1L).build(),
                DbAggregationResult.builder().key("Deploy-Internal-Api-to-kubernetes-cluster").additionalKey(null).count(1L).build()
        );
        //Assert.assertEquals(expected.stream().collect(Collectors.toSet()), dbAggsResponse.getRecords().stream().collect(Collectors.toSet()));
        verifyRecords(dbAggsResponse.getRecords(), expected, false);
        // endregion

        // region qualified job_name - duration
        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder().across(CiCdJobRunsFilter.DISTINCT.qualified_job_name).calculation(CiCdJobRunsFilter.CALCULATION.duration).build(),
                VALUES_ONLY
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(22, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(22, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(22, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("Pipe2").additionalKey("instance-name-0").count(42L).sum(258351L).min(1L).max(121500L).median(69L).build(),
                DbAggregationResult.builder().key("Pipe2").count(42L).sum(258351L).min(1L).max(121500L).median(69L).build(),
                DbAggregationResult.builder().key("openapi-generator").additionalKey("instance-name-0").count(5L).sum(520L).min(35L).max(254L).median(43L).build(),
                DbAggregationResult.builder().key("openapi-generator").count(5L).sum(520L).min(35L).max(254L).median(43L).build(),
                DbAggregationResult.builder().key("Build-commons-levelops").additionalKey("instance-name-0").count(1L).sum(199L).min(199L).max(199L).median(199L).build(),
                DbAggregationResult.builder().key("Build-commons-levelops").count(1L).sum(199L).min(199L).max(199L).median(199L).build(),
                DbAggregationResult.builder().key("com.wordnik$swagger-codegen_2.9.1").additionalKey("instance-name-0").count(2L).sum(232L).min(69L).max(163L).median(69L).build(),
                DbAggregationResult.builder().key("com.wordnik$swagger-codegen_2.9.1").count(2L).sum(232L).min(69L).max(163L).median(69L).build(),
                DbAggregationResult.builder().key("Build-Internal-API").additionalKey("instance-name-0").count(1L).sum(104L).min(104L).max(104L).median(104L).build(),
                DbAggregationResult.builder().key("Build-Internal-API").count(1L).sum(104L).min(104L).max(104L).median(104L).build(),
                DbAggregationResult.builder().key("Build-Aggregations-Service").additionalKey("instance-name-0").count(1L).sum(92L).min(92L).max(92L).median(92L).build(),
                DbAggregationResult.builder().key("Build-Aggregations-Service").count(1L).sum(92L).min(92L).max(92L).median(92L).build(),
                DbAggregationResult.builder().key("Build-Server-API").additionalKey("instance-name-0").count(1L).sum(79L).min(79L).max(79L).median(79L).build(),
                DbAggregationResult.builder().key("Build-Server-API").count(1L).sum(79L).min(79L).max(79L).median(79L).build(),
                DbAggregationResult.builder().key("Deploy-Server_API-to-Kubernetes-Cluster").additionalKey("instance-name-0").count(2L).sum(114L).min(51L).max(63L).median(51L).build(),
                DbAggregationResult.builder().key("Deploy-Server_API-to-Kubernetes-Cluster").count(2L).sum(114L).min(51L).max(63L).median(51L).build(),
                DbAggregationResult.builder().key("Deploy-Aggregations-Service").additionalKey("instance-name-0").count(1L).sum(52L).min(52L).max(52L).median(52L).build(),
                DbAggregationResult.builder().key("Deploy-Aggregations-Service").count(1L).sum(52L).min(52L).max(52L).median(52L).build(),
                DbAggregationResult.builder().key("Pipe3").additionalKey("instance-name-0").count(9L).sum(160L).min(12L).max(48L).median(14L).build(),
                DbAggregationResult.builder().key("Pipe3").count(9L).sum(160L).min(12L).max(48L).median(14L).build(),
                DbAggregationResult.builder().key("Deploy-Internal-Api-to-kubernetes-cluster").additionalKey("instance-name-0").count(1L).sum(46L).min(46L).max(46L).median(46L).build(),
                DbAggregationResult.builder().key("Deploy-Internal-Api-to-kubernetes-cluster").count(1L).sum(46L).min(46L).max(46L).median(46L).build()
        );
        //Assert.assertEquals(expected, dbAggsResponse.getRecords());
        Assert.assertEquals(expected.stream().collect(Collectors.toSet()), dbAggsResponse.getRecords().stream().collect(Collectors.toSet()));
        // endregion

        // region cicd_user_id - count
        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder().across(CiCdJobRunsFilter.DISTINCT.cicd_user_id).calculation(CiCdJobRunsFilter.CALCULATION.count).build(),
                VALUES_ONLY
        );

        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(3, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(3, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(3, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("testread").count(112L).build(),
                DbAggregationResult.builder().key("SCMTrigger").count(16L).build(),
                DbAggregationResult.builder().key("SYSTEM").count(4L).build()
        );
        Assert.assertEquals(expected, dbAggsResponse.getRecords());
        // endregion

        // region cicd_user_id - duration
        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder().across(CiCdJobRunsFilter.DISTINCT.cicd_user_id).calculation(CiCdJobRunsFilter.CALCULATION.duration).build(),
                VALUES_ONLY
        );

        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(3, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(3, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(3, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("SYSTEM").count(4L).sum(464L).min(69L).max(163L).median(69L).build(),
                DbAggregationResult.builder().key("SCMTrigger").count(16L).sum(1372L).min(46L).max(199L).median(63L).build(),
                DbAggregationResult.builder().key("testread").count(112L).sum(518062L).min(1L).max(121500L).median(43L).build()
        );
        Assert.assertEquals(expected, dbAggsResponse.getRecords());
        // endregion

        // region job_status - count
        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder().across(CiCdJobRunsFilter.DISTINCT.job_status).calculation(CiCdJobRunsFilter.CALCULATION.count).build(),
                VALUES_ONLY
        );

        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(3, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(3, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(3, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("SUCCESS").count(88L).build(),
                DbAggregationResult.builder().key("FAILURE").count(38L).build(),
                DbAggregationResult.builder().key("ABORTED").count(6L).build()

        );
        Assert.assertEquals(expected, dbAggsResponse.getRecords());
        // endregion

        // region Job Name + Correct Params
        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder().across(CiCdJobRunsFilter.DISTINCT.trend).calculation(CiCdJobRunsFilter.CALCULATION.count)
                        .jobNames(List.of("Build-commons-levelops"))
                        .parameters(List.of(CiCdJobRunParameter.builder().name("BRANCH_NAME").values(List.of("master", "dev")).build()))
                        .build(),
                VALUES_ONLY
        );

        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(1, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getRecords().size());
        // endregion

        // region Job Name + InCorrect Params
        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder().across(CiCdJobRunsFilter.DISTINCT.trend).calculation(CiCdJobRunsFilter.CALCULATION.count)
                        .jobNames(List.of("Build-commons-levelops"))
                        .parameters(List.of(CiCdJobRunParameter.builder().name("BRANCH_NAME").values(List.of("master1", "dev")).build()))
                        .build(),
                VALUES_ONLY
        );

        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(0, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(0, dbAggsResponse.getTotalCount().intValue());
        Assert.assertTrue(CollectionUtils.isEmpty(dbAggsResponse.getRecords()));
        // endregion

        // region Job Name + InValid Params
        expectedList = listOfInputDates.stream()
                .filter(Objects::nonNull)
                .map(instant -> extractDataComponentForDbResults(new Date(TimeUnit.MILLISECONDS.toMicros(instant)),
                        Calendar.YEAR, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder().across(CiCdJobRunsFilter.DISTINCT.trend).calculation(CiCdJobRunsFilter.CALCULATION.count)
                        .jobNames(List.of("Build-commons-levelops"))
                        .parameters(List.of(CiCdJobRunParameter.builder().name("BRANCH_NAME").values(List.of()).build()))
                        .build(),
                VALUES_ONLY
        );

        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(1, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getRecords().size());
        actualList = dbAggsResponse.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .map(key -> new Date(TimeUnit.MILLISECONDS.toMicros(Long.parseLong(key))))
                .map(date -> extractDataComponentForDbResults(date, Calendar.YEAR, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        Assert.assertNotNull(dbAggsResponse);
        assertThat(expectedList.containsAll(actualList)).isEqualTo(true);
        assertThat(dbAggsResponse.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()))
                .isNotNull();
        // endregion

        // region Qualified Job Name + Correct Params
        expectedList = listOfInputDates.stream()
                .filter(Objects::nonNull)
                .map(instant -> extractDataComponentForDbResults(new Date(TimeUnit.MILLISECONDS.toMicros(instant)),
                        Calendar.YEAR, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder().across(CiCdJobRunsFilter.DISTINCT.trend).calculation(CiCdJobRunsFilter.CALCULATION.count)
                        .qualifiedJobNames(List.of(CiCdJobQualifiedName.builder().instanceName("instance-name-0").jobName("Build-commons-levelops").build()))
                        .parameters(List.of(CiCdJobRunParameter.builder().name("BRANCH_NAME").values(List.of("master", "dev")).build()))
                        .build(),
                VALUES_ONLY
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(1, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getRecords().size());
        actualList = dbAggsResponse.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .map(key -> new Date(TimeUnit.MILLISECONDS.toMicros(Long.parseLong(key))))
                .map(date -> extractDataComponentForDbResults(date, Calendar.YEAR, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        Assert.assertNotNull(dbAggsResponse);
        assertThat(expectedList.containsAll(actualList)).isEqualTo(true);
        assertThat(dbAggsResponse.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()))
                .isNotNull();
        // endregion

        // region Qualified Job Name + InCorrect Params
        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder().across(CiCdJobRunsFilter.DISTINCT.trend).calculation(CiCdJobRunsFilter.CALCULATION.count)
                        .qualifiedJobNames(List.of(CiCdJobQualifiedName.builder().instanceName("instance-name-0").jobName("Build-commons-levelops").build()))
                        .parameters(List.of(CiCdJobRunParameter.builder().name("BRANCH_NAME").values(List.of("master1", "dev")).build()))
                        .build(),
                VALUES_ONLY
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(0, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(0, dbAggsResponse.getTotalCount().intValue());
        Assert.assertTrue(CollectionUtils.isEmpty(dbAggsResponse.getRecords()));
        // endregion

        // region Qualified Job Name + InValid Params
        expectedList = listOfInputDates.stream()
                .filter(Objects::nonNull)
                .map(instant -> extractDataComponentForDbResults(new Date(TimeUnit.MILLISECONDS.toMicros(instant)),
                        Calendar.YEAR, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder().across(CiCdJobRunsFilter.DISTINCT.trend).calculation(CiCdJobRunsFilter.CALCULATION.count)
                        .qualifiedJobNames(
                                List.of(
                                        CiCdJobQualifiedName.builder().instanceName("instance-name-0").jobName("Build-commons-levelops").build(),
                                        CiCdJobQualifiedName.builder().instanceName(null).jobName("Build-commons-levelops").build()
                                )
                        )
                        .parameters(List.of(CiCdJobRunParameter.builder().name("BRANCH_NAME").values(List.of()).build()))
                        .build(),
                VALUES_ONLY
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(1, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getRecords().size());
        actualList = dbAggsResponse.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .map(key -> new Date(TimeUnit.MILLISECONDS.toMicros(Long.parseLong(key))))
                .map(date -> extractDataComponentForDbResults(date, Calendar.YEAR, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        Assert.assertNotNull(dbAggsResponse);
        assertThat(expectedList.containsAll(actualList)).isEqualTo(true);
        assertThat(dbAggsResponse.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()))
                .isNotNull();
        //end region

        // region instance name
        expectedList = listOfInputDates.stream()
                .filter(Objects::nonNull)
                .map(instant -> extractDataComponentForDbResults(new Date(TimeUnit.MILLISECONDS.toMicros(instant)),
                        Calendar.YEAR, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder().across(CiCdJobRunsFilter.DISTINCT.trend).calculation(CiCdJobRunsFilter.CALCULATION.count)
                        .instanceNames(List.of("instance-name-0"))
                        .parameters(List.of(CiCdJobRunParameter.builder().name("BRANCH_NAME").values(List.of("master", "dev")).build()))
                        .build(),
                VALUES_ONLY
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(1, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getRecords().size());

        actualList = dbAggsResponse.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .map(key -> new Date(TimeUnit.MILLISECONDS.toMicros(Long.parseLong(key))))
                .map(date -> extractDataComponentForDbResults(date, Calendar.YEAR, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        Assert.assertNotNull(dbAggsResponse);
        assertThat(expectedList.containsAll(actualList)).isEqualTo(true);
        assertThat(dbAggsResponse.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()))
                .isNotNull();
        // endregion

        // region instance names + InCorrect Params
        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder().across(CiCdJobRunsFilter.DISTINCT.trend).calculation(CiCdJobRunsFilter.CALCULATION.count)
                        .instanceNames(List.of("instance-name-0"))
                        .parameters(List.of(CiCdJobRunParameter.builder().name("BRANCH_NAME").values(List.of("master1", "dev1")).build()))
                        .build(),
                VALUES_ONLY
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(0, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(0, dbAggsResponse.getTotalCount().intValue());
        Assert.assertTrue(CollectionUtils.isEmpty(dbAggsResponse.getRecords()));
        // endregion

        // region trend - count - stack job name
        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder().across(CiCdJobRunsFilter.DISTINCT.trend)
                        .calculation(CiCdJobRunsFilter.CALCULATION.count)
                        .stacks(List.of(CiCdJobRunsFilter.DISTINCT.job_status))
                        .build(),
                VALUES_ONLY
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(6, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(6, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(6, dbAggsResponse.getRecords().size());
        // endregion

        // region job_name - count - stack job status
        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder().across(CiCdJobRunsFilter.DISTINCT.job_name).calculation(CiCdJobRunsFilter.CALCULATION.count)
                        .stacks(List.of(CiCdJobRunsFilter.DISTINCT.job_status))
                        .build(),
                VALUES_ONLY
        );

        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(11, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(11, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(11, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("Pipe2").count(84L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("SUCCESS").count(74L).build(),
                                DbAggregationResult.builder().key("FAILURE").count(6L).build(),
                                DbAggregationResult.builder().key("ABORTED").count(4L).build()
                        )).build(),
                DbAggregationResult.builder().key("Pipe3").count(18L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("FAILURE").count(18L).build()
                        )).build(),
                DbAggregationResult.builder().key("openapi-generator").count(10L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("FAILURE").count(8L).build(),
                                DbAggregationResult.builder().key("ABORTED").count(2L).build()
                        )).build(),
                DbAggregationResult.builder().key("Deploy-Server_API-to-Kubernetes-Cluster").count(4L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("FAILURE").count(2L).build(),
                                DbAggregationResult.builder().key("SUCCESS").count(2L).build()
                        )).build(),
                DbAggregationResult.builder().key("com.wordnik$swagger-codegen_2.9.1").count(4L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("FAILURE").count(4L).build()
                        )).build(),
                DbAggregationResult.builder().key("Build-Internal-API").count(2L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("SUCCESS").count(2L).build()
                        )).build(),
                DbAggregationResult.builder().key("Build-commons-levelops").count(2L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("SUCCESS").count(2L).build()
                        )).build(),
                DbAggregationResult.builder().key("Build-Server-API").count(2L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("SUCCESS").count(2L).build()
                        )).build(),
                DbAggregationResult.builder().key("Deploy-Aggregations-Service").count(2L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("SUCCESS").count(2L).build()
                        )).build(),
                DbAggregationResult.builder().key("Build-Aggregations-Service").count(2L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("SUCCESS").count(2L).build()
                        )).build(),
                DbAggregationResult.builder().key("Deploy-Internal-Api-to-kubernetes-cluster").count(2L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("SUCCESS").count(2L).build()
                        )).build()
        );
        verifyRecords(dbAggsResponse.getRecords(), expected, false);
        // endregion

        // region job_name - count - stack cicd_user_id
        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder().across(CiCdJobRunsFilter.DISTINCT.job_name).calculation(CiCdJobRunsFilter.CALCULATION.count)
                        .stacks(List.of(CiCdJobRunsFilter.DISTINCT.cicd_user_id))
                        .build(),
                VALUES_ONLY
        );

        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(11, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(11, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(11, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("Pipe2").count(84L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("testread").count(84L).build()
                        )).build(),
                DbAggregationResult.builder().key("Pipe3").count(18L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("testread").count(18L).build()
                        )).build(),
                DbAggregationResult.builder().key("openapi-generator").count(10L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("testread").count(10L).build()
                        )).build(),
                DbAggregationResult.builder().key("Deploy-Server_API-to-Kubernetes-Cluster").count(4L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("SCMTrigger").count(4L).build()
                        )).build(),
                DbAggregationResult.builder().key("com.wordnik$swagger-codegen_2.9.1").count(4L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("SYSTEM").count(4L).build()
                        )).build(),
                DbAggregationResult.builder().key("Build-Internal-API").count(2L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("SCMTrigger").count(2L).build()
                        )).build(),
                DbAggregationResult.builder().key("Build-commons-levelops").count(2L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("SCMTrigger").count(2L).build()
                        )).build(),
                DbAggregationResult.builder().key("Build-Server-API").count(2L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("SCMTrigger").count(2L).build()
                        )).build(),
                DbAggregationResult.builder().key("Deploy-Aggregations-Service").count(2L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("SCMTrigger").count(2L).build()
                        )).build(),
                DbAggregationResult.builder().key("Build-Aggregations-Service").count(2L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("SCMTrigger").count(2L).build()
                        )).build(),
                DbAggregationResult.builder().key("Deploy-Internal-Api-to-kubernetes-cluster").count(2L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("SCMTrigger").count(2L).build()
                        )).build()
        );
        verifyRecords(dbAggsResponse.getRecords(), expected, false);
        // endregion

        // region qualified job_name - count - stack job status
        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder().across(CiCdJobRunsFilter.DISTINCT.qualified_job_name).calculation(CiCdJobRunsFilter.CALCULATION.count)
                        .stacks(List.of(CiCdJobRunsFilter.DISTINCT.job_status))
                        .build(),
                VALUES_ONLY
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(22, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(22, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(22, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("Pipe2").additionalKey("instance-name-0").count(42L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("SUCCESS").count(37L).build(),
                                DbAggregationResult.builder().key("FAILURE").count(3L).build(),
                                DbAggregationResult.builder().key("ABORTED").count(2L).build()
                        )).build(),
                DbAggregationResult.builder().key("Pipe2").additionalKey(null).count(42L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("SUCCESS").count(37L).build(),
                                DbAggregationResult.builder().key("FAILURE").count(3L).build(),
                                DbAggregationResult.builder().key("ABORTED").count(2L).build()
                        )).build(),
                DbAggregationResult.builder().key("Pipe3").additionalKey("instance-name-0").count(9L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("FAILURE").count(9L).build()
                        )).build(),
                DbAggregationResult.builder().key("Pipe3").additionalKey(null).count(9L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("FAILURE").count(9L).build()
                        )).build(),
                DbAggregationResult.builder().key("openapi-generator").additionalKey("instance-name-0").count(5L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("FAILURE").count(4L).build(),
                                DbAggregationResult.builder().key("ABORTED").count(1L).build()
                        )).build(),
                DbAggregationResult.builder().key("openapi-generator").additionalKey(null).count(5L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("FAILURE").count(4L).build(),
                                DbAggregationResult.builder().key("ABORTED").count(1L).build()
                        )).build(),
                DbAggregationResult.builder().key("Deploy-Server_API-to-Kubernetes-Cluster").additionalKey("instance-name-0").count(2L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("SUCCESS").count(1L).build(),
                                DbAggregationResult.builder().key("FAILURE").count(1L).build()
                        )).build(),
                DbAggregationResult.builder().key("Deploy-Server_API-to-Kubernetes-Cluster").additionalKey(null).count(2L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("SUCCESS").count(1L).build(),
                                DbAggregationResult.builder().key("FAILURE").count(1L).build()
                        )).build(),
                DbAggregationResult.builder().key("com.wordnik$swagger-codegen_2.9.1").additionalKey("instance-name-0").count(2L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("FAILURE").count(2L).build()
                        )).build(),
                DbAggregationResult.builder().key("com.wordnik$swagger-codegen_2.9.1").additionalKey(null).count(2L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("FAILURE").count(2L).build()
                        )).build(),
                DbAggregationResult.builder().key("Build-Internal-API").additionalKey("instance-name-0").count(1L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("SUCCESS").count(1L).build()
                        )).build(),
                DbAggregationResult.builder().key("Build-Internal-API").additionalKey(null).count(1L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("SUCCESS").count(1L).build()
                        )).build(),
                DbAggregationResult.builder().key("Build-commons-levelops").additionalKey("instance-name-0").count(1L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("SUCCESS").count(1L).build()
                        )).build(),
                DbAggregationResult.builder().key("Build-commons-levelops").additionalKey(null).count(1L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("SUCCESS").count(1L).build()
                        )).build(),
                DbAggregationResult.builder().key("Build-Server-API").additionalKey("instance-name-0").count(1L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("SUCCESS").count(1L).build()
                        )).build(),
                DbAggregationResult.builder().key("Build-Server-API").additionalKey(null).count(1L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("SUCCESS").count(1L).build()
                        )).build(),
                DbAggregationResult.builder().key("Deploy-Aggregations-Service").additionalKey("instance-name-0").count(1L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("SUCCESS").count(1L).build()
                        )).build(),
                DbAggregationResult.builder().key("Deploy-Aggregations-Service").additionalKey(null).count(1L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("SUCCESS").count(1L).build()
                        )).build(),
                DbAggregationResult.builder().key("Build-Aggregations-Service").additionalKey("instance-name-0").count(1L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("SUCCESS").count(1L).build()
                        )).build(),
                DbAggregationResult.builder().key("Build-Aggregations-Service").additionalKey(null).count(1L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("SUCCESS").count(1L).build()
                        )).build(),
                DbAggregationResult.builder().key("Deploy-Internal-Api-to-kubernetes-cluster").additionalKey("instance-name-0").count(1L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("SUCCESS").count(1L).build()
                        )).build(),
                DbAggregationResult.builder().key("Deploy-Internal-Api-to-kubernetes-cluster").additionalKey(null).count(1L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("SUCCESS").count(1L).build()
                        )).build()
        );
        verifyRecords(dbAggsResponse.getRecords(), expected, false);
        // endregion

        // region cicd_user_id - count - stack job status
        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder().across(CiCdJobRunsFilter.DISTINCT.cicd_user_id).calculation(CiCdJobRunsFilter.CALCULATION.count)
                        .stacks(List.of(CiCdJobRunsFilter.DISTINCT.job_status))
                        .build(),
                VALUES_ONLY
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(3, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(3, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(3, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("testread").count(112L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("SUCCESS").count(74L).build(),
                                DbAggregationResult.builder().key("FAILURE").count(32L).build(),
                                DbAggregationResult.builder().key("ABORTED").count(6L).build()
                        )).build(),
                DbAggregationResult.builder().key("SCMTrigger").count(16L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("SUCCESS").count(14L).build(),
                                DbAggregationResult.builder().key("FAILURE").count(2L).build()
                        )).build(),
                DbAggregationResult.builder().key("SYSTEM").count(4L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("FAILURE").count(4L).build()
                        )).build()
        );
        verifyRecords(dbAggsResponse.getRecords(), expected, false);
        // endregion

        // region cicd_user_id - count - stack qualified job name
        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder().across(CiCdJobRunsFilter.DISTINCT.cicd_user_id).calculation(CiCdJobRunsFilter.CALCULATION.count)
                        .stacks(List.of(CiCdJobRunsFilter.DISTINCT.qualified_job_name))
                        .build(),
                VALUES_ONLY
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(3, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(3, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(3, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("testread").count(112L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("Pipe2").additionalKey(null).count(42L).build(),
                                DbAggregationResult.builder().key("Pipe2").additionalKey("instance-name-0").count(42L).build(),
                                DbAggregationResult.builder().key("Pipe3").additionalKey(null).count(9L).build(),
                                DbAggregationResult.builder().key("Pipe3").additionalKey("instance-name-0").count(9L).build(),
                                DbAggregationResult.builder().key("openapi-generator").additionalKey(null).count(5L).build(),
                                DbAggregationResult.builder().key("openapi-generator").additionalKey("instance-name-0").count(5L).build()
                        )).build(),
                DbAggregationResult.builder().key("SCMTrigger").count(16L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("Deploy-Server_API-to-Kubernetes-Cluster").additionalKey(null).count(2L).build(),
                                DbAggregationResult.builder().key("Deploy-Server_API-to-Kubernetes-Cluster").additionalKey("instance-name-0").count(2L).build(),
                                DbAggregationResult.builder().key("Build-Internal-API").additionalKey(null).count(1L).build(),
                                DbAggregationResult.builder().key("Build-Internal-API").additionalKey("instance-name-0").count(1L).build(),
                                DbAggregationResult.builder().key("Deploy-Internal-Api-to-kubernetes-cluster").additionalKey(null).count(1L).build(),
                                DbAggregationResult.builder().key("Deploy-Internal-Api-to-kubernetes-cluster").additionalKey("instance-name-0").count(1L).build(),
                                DbAggregationResult.builder().key("Build-Server-API").additionalKey(null).count(1L).build(),
                                DbAggregationResult.builder().key("Build-Server-API").additionalKey("instance-name-0").count(1L).build(),
                                DbAggregationResult.builder().key("Build-commons-levelops").additionalKey(null).count(1L).build(),
                                DbAggregationResult.builder().key("Build-commons-levelops").additionalKey("instance-name-0").count(1L).build(),
                                DbAggregationResult.builder().key("Build-Aggregations-Service").additionalKey(null).count(1L).build(),
                                DbAggregationResult.builder().key("Build-Aggregations-Service").additionalKey("instance-name-0").count(1L).build(),
                                DbAggregationResult.builder().key("Deploy-Aggregations-Service").additionalKey(null).count(1L).build(),
                                DbAggregationResult.builder().key("Deploy-Aggregations-Service").additionalKey("instance-name-0").count(1L).build()
                        )).build(),
                DbAggregationResult.builder().key("SYSTEM").count(4L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("com.wordnik$swagger-codegen_2.9.1").additionalKey(null).count(2L).build(),
                                DbAggregationResult.builder().key("com.wordnik$swagger-codegen_2.9.1").additionalKey("instance-name-0").count(2L).build()
                        )).build()


        );
        verifyRecords(dbAggsResponse.getRecords(), expected, false);
        // endregion

        // region job_status - count - stack job name
        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder().across(CiCdJobRunsFilter.DISTINCT.job_status).calculation(CiCdJobRunsFilter.CALCULATION.count)
                        .stacks(List.of(CiCdJobRunsFilter.DISTINCT.job_name))
                        .build(),
                VALUES_ONLY
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(3, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(3, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(3, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("SUCCESS").count(88L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("Pipe2").count(74L).build(),
                                DbAggregationResult.builder().key("Build-Server-API").count(2L).build(),
                                DbAggregationResult.builder().key("Build-Internal-API").count(2L).build(),
                                DbAggregationResult.builder().key("Build-commons-levelops").count(2L).build(),
                                DbAggregationResult.builder().key("Deploy-Server_API-to-Kubernetes-Cluster").count(2L).build(),
                                DbAggregationResult.builder().key("Build-Aggregations-Service").count(2L).build(),
                                DbAggregationResult.builder().key("Deploy-Internal-Api-to-kubernetes-cluster").count(2L).build(),
                                DbAggregationResult.builder().key("Deploy-Aggregations-Service").count(2L).build()
                        )).build(),
                DbAggregationResult.builder().key("FAILURE").count(38L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("Pipe3").count(18L).build(),
                                DbAggregationResult.builder().key("openapi-generator").count(8L).build(),
                                DbAggregationResult.builder().key("Pipe2").count(6L).build(),
                                DbAggregationResult.builder().key("com.wordnik$swagger-codegen_2.9.1").count(4L).build(),
                                DbAggregationResult.builder().key("Deploy-Server_API-to-Kubernetes-Cluster").count(2L).build()
                        )).build(),
                DbAggregationResult.builder().key("ABORTED").count(6L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("Pipe2").count(4L).build(),
                                DbAggregationResult.builder().key("openapi-generator").count(2L).build()

                        )).build()

        );
        verifyRecords(dbAggsResponse.getRecords(), expected, false);
        // endregion

        // region List - no filters - page size
        DbListResponse<CICDJobRunDTO> dbListResponse = ciCdAggsService.listCiCdJobRuns(company,
                CiCdJobRunsFilter.builder().build(), PAGE_NUMBER, 10);
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(10, dbListResponse.getCount().intValue());
        Assert.assertEquals(132, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(10, dbListResponse.getRecords().size());
        // endregion

        // region List - no filters
        dbListResponse = ciCdAggsService.listCiCdJobRuns(company,
                CiCdJobRunsFilter.builder().build(), null, PAGE_NUMBER, PAGE_SIZE);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(132, dbListResponse.getCount().intValue());
        Assert.assertEquals(132, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(132, dbListResponse.getRecords().size());
        // endregion

        // region List - job start time
        dbListResponse = ciCdAggsService.listCiCdJobRuns(company,
                CiCdJobRunsFilter.builder()
                        .startTimeRange(ImmutablePair.of(Instant.now().getEpochSecond(), null))
                        .build(), null, PAGE_NUMBER, PAGE_SIZE);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(0, dbListResponse.getCount().intValue());
        Assert.assertEquals(0, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(0, dbListResponse.getRecords().size());

        // endregion

        // region List - job end time
        dbListResponse = ciCdAggsService.listCiCdJobRuns(company,
                CiCdJobRunsFilter.builder()
                        .startTimeRange(ImmutablePair.of(null, Instant.now().getEpochSecond()))
                        .build(), null, PAGE_NUMBER, PAGE_SIZE);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(132, dbListResponse.getCount().intValue());
        Assert.assertEquals(132, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(132, dbListResponse.getRecords().size());

        // endregion

        // region List - All filters
        dbListResponse = ciCdAggsService.listCiCdJobRuns(company,
                CiCdJobRunsFilter.builder().jobNames(List.of("Pipe2"))
                        .qualifiedJobNames(List.of(CiCdJobQualifiedName.builder().instanceName("instance-name-0").jobName("Pipe2").build()))
                        .build(), null, PAGE_NUMBER, PAGE_SIZE);
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(42, dbListResponse.getCount().intValue());
        Assert.assertEquals(42, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(42, dbListResponse.getRecords().size());
        // endregion

        // region List - instance name filter
        dbListResponse = ciCdAggsService.listCiCdJobRuns(company,
                CiCdJobRunsFilter.builder().instanceNames(List.of("instance-name-0"))
                        .build(), null, PAGE_NUMBER, PAGE_SIZE);
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(66, dbListResponse.getCount().intValue());
        Assert.assertEquals(66, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(66, dbListResponse.getRecords().size());
        // endregion

        //region cicd job runs exclude filters
        //region List - instance name exclude filter
        dbListResponse = ciCdAggsService.listCiCdJobRuns(company,
                CiCdJobRunsFilter.builder()
                        .excludeInstanceNames(List.of("instance-name-1"))
                        .build(), null, PAGE_NUMBER, PAGE_SIZE);
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(66, dbListResponse.getCount().intValue());
        Assert.assertEquals(66, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(66, dbListResponse.getRecords().size());

        dbListResponse = ciCdAggsService.listCiCdJobRuns(company,
                CiCdJobRunsFilter.builder()
                        .excludeInstanceNames(List.of("instance-name-1"))
                        .instanceNames(List.of("instance-name-1"))
                        .build(), null, PAGE_NUMBER, PAGE_SIZE);
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(0, dbListResponse.getCount().intValue());
        Assert.assertEquals(0, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(0, dbListResponse.getRecords().size());
        //endregion

        //region List - job names exclude filter
        dbListResponse = ciCdAggsService.listCiCdJobRuns(company,
                CiCdJobRunsFilter.builder()
                        .excludeJobNames(List.of("Pipe2"))
                        .build(), null, PAGE_NUMBER, PAGE_SIZE);
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(48, dbListResponse.getCount().intValue());
        Assert.assertEquals(48, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(48, dbListResponse.getRecords().size());

        dbListResponse = ciCdAggsService.listCiCdJobRuns(company,
                CiCdJobRunsFilter.builder()
                        .excludeJobNames(List.of("Pipe2"))
                        .jobNames(List.of("Pipe2"))
                        .build(), null, PAGE_NUMBER, PAGE_SIZE);
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(0, dbListResponse.getCount().intValue());
        Assert.assertEquals(0, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(0, dbListResponse.getRecords().size());
        //endregion

        //region List - projects exclude filter
        dbListResponse = ciCdAggsService.listCiCdJobRuns(company,
                CiCdJobRunsFilter.builder()
                        .excludeProjects(List.of("project-0"))
                        .build(), null, PAGE_NUMBER, PAGE_SIZE);
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(132, dbListResponse.getCount().intValue());
        Assert.assertEquals(132, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(132, dbListResponse.getRecords().size());

        dbListResponse = ciCdAggsService.listCiCdJobRuns(company,
                CiCdJobRunsFilter.builder()
                        .excludeProjects(List.of("project-0"))
                        .projects(List.of("project-0"))
                        .build(), null, PAGE_NUMBER, PAGE_SIZE);
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(0, dbListResponse.getCount().intValue());
        Assert.assertEquals(0, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(0, dbListResponse.getRecords().size());
        //endregion

        //region List - job normalized full names exclude filter
        dbListResponse = ciCdAggsService.listCiCdJobRuns(company,
                CiCdJobRunsFilter.builder().excludeJobNormalizedFullNames(List.of("jobname-0/branch-name-0"))
                        .build(), null, PAGE_NUMBER, PAGE_SIZE);
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(132, dbListResponse.getCount().intValue());
        Assert.assertEquals(132, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(132, dbListResponse.getRecords().size());

        dbListResponse = ciCdAggsService.listCiCdJobRuns(company,
                CiCdJobRunsFilter.builder()
                        .excludeJobNormalizedFullNames(List.of("jobname-0/branch-name-0"))
                        .jobNormalizedFullNames(List.of("jobname-0/branch-name-0"))
                        .build(), null, PAGE_NUMBER, PAGE_SIZE);
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(0, dbListResponse.getCount().intValue());
        Assert.assertEquals(0, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(0, dbListResponse.getRecords().size());
        //endregion

        //region List - job statuses exclude filter
        dbListResponse = ciCdAggsService.listCiCdJobRuns(company,
                CiCdJobRunsFilter.builder()
                        .excludeJobStatuses(List.of("SUCCESS"))
                        .build(), null, PAGE_NUMBER, PAGE_SIZE);
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(44, dbListResponse.getCount().intValue());
        Assert.assertEquals(44, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(44, dbListResponse.getRecords().size());

        dbListResponse = ciCdAggsService.listCiCdJobRuns(company,
                CiCdJobRunsFilter.builder()
                        .excludeJobStatuses(List.of("SUCCESS"))
                        .jobStatuses(List.of("SUCCESS"))
                        .build(), null, PAGE_NUMBER, PAGE_SIZE);
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(0, dbListResponse.getCount().intValue());
        Assert.assertEquals(0, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(0, dbListResponse.getRecords().size());
        //endregion

        //region List - job types exclude filter
        dbListResponse = ciCdAggsService.listCiCdJobRuns(company,
                CiCdJobRunsFilter.builder()
                        .excludeTypes(List.of(CICD_TYPE.azure_devops))
                        .build(), null, PAGE_NUMBER, PAGE_SIZE);
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(66, dbListResponse.getCount().intValue());
        Assert.assertEquals(66, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(66, dbListResponse.getRecords().size());

        dbListResponse = ciCdAggsService.listCiCdJobRuns(company,
                CiCdJobRunsFilter.builder()
                        .excludeTypes(List.of(CICD_TYPE.azure_devops))
                        .types(List.of(CICD_TYPE.azure_devops))
                        .build(), null, PAGE_NUMBER, PAGE_SIZE);
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(0, dbListResponse.getCount().intValue());
        Assert.assertEquals(0, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(0, dbListResponse.getRecords().size());
        //endregion

        //region List - job ciCdUserIds exclude filter
        dbListResponse = ciCdAggsService.listCiCdJobRuns(company,
                CiCdJobRunsFilter.builder()
                        .excludeCiCdUserIds(List.of("user-jenkins-0"))
                        .build(), null, PAGE_NUMBER, PAGE_SIZE);
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(132, dbListResponse.getCount().intValue());
        Assert.assertEquals(132, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(132, dbListResponse.getRecords().size());

        dbListResponse = ciCdAggsService.listCiCdJobRuns(company,
                CiCdJobRunsFilter.builder()
                        .excludeCiCdUserIds(List.of("user-jenkins-0"))
                        .cicdUserIds(List.of("user-jenkins-0"))
                        .build(), null, PAGE_NUMBER, PAGE_SIZE);
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(0, dbListResponse.getCount().intValue());
        Assert.assertEquals(0, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(0, dbListResponse.getRecords().size());
        //endregion

        //region List - job qualified jobNames exclude filter
        CiCdJobQualifiedName ciCdJobQualifiedName = CiCdJobQualifiedName.builder().instanceName("instance-name-1").jobName("jobname-0").build();
        dbListResponse = ciCdAggsService.listCiCdJobRuns(company,
                CiCdJobRunsFilter.builder()
                        .excludeQualifiedJobNames(List.of(ciCdJobQualifiedName))
                        .build(), PAGE_NUMBER, PAGE_SIZE);
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(66, dbListResponse.getCount().intValue());
        Assert.assertEquals(66, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(66, dbListResponse.getRecords().size());

        dbListResponse = ciCdAggsService.listCiCdJobRuns(company,
                CiCdJobRunsFilter.builder()
                        .excludeQualifiedJobNames(List.of(ciCdJobQualifiedName))
                        .qualifiedJobNames(List.of(ciCdJobQualifiedName))
                        .build(), PAGE_NUMBER, PAGE_SIZE);
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(0, dbListResponse.getCount().intValue());
        Assert.assertEquals(0, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(0, dbListResponse.getRecords().size());
        //endregion
        //endregion
    }
    // endregion

    // region Job Runs for stage and steps filters
    @Test
    public void testGroupByAndCalculateForCiCdJobRunStageSteps() throws IOException, SQLException {
        setupGitlabPipelineData();
        DbListResponse<DbAggregationResult> dbAggregationResults = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company1,
                CiCdJobRunsFilter.builder()
                        .across(CiCdJobRunsFilter.DISTINCT.stage_name)
                        .calculation(CiCdJobRunsFilter.CALCULATION.count)
                        .build(),
                true,
                null);

        Assert.assertEquals(3, dbAggregationResults.getTotalCount().intValue());
        Assert.assertTrue(dbAggregationResults.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()).containsAll(List.of("test", "build", "deploy")));

        dbAggregationResults = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company1,
                CiCdJobRunsFilter.builder()
                        .across(CiCdJobRunsFilter.DISTINCT.step_name)
                        .calculation(CiCdJobRunsFilter.CALCULATION.count)
                        .build(),
                true,
                null);

        Assert.assertEquals(5, dbAggregationResults.getTotalCount().intValue());
        Assert.assertTrue(dbAggregationResults.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()).containsAll(List.of("build-job", "lint-test-job", "deploy-job", "unit-test-job")));

        dbAggregationResults = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company1,
                CiCdJobRunsFilter.builder()
                        .across(CiCdJobRunsFilter.DISTINCT.stage_status)
                        .calculation(CiCdJobRunsFilter.CALCULATION.count)
                        .build(),
                true,
                null);

        Assert.assertEquals(4, dbAggregationResults.getTotalCount().intValue());
        Assert.assertEquals(dbAggregationResults.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()), List.of("PASSED", "FAILED", "PASSED WITH WARNINGS", "CANCELED"));

        dbAggregationResults = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company1,
                CiCdJobRunsFilter.builder()
                        .across(CiCdJobRunsFilter.DISTINCT.step_status)
                        .calculation(CiCdJobRunsFilter.CALCULATION.count)
                        .build(),
                true,
                null);

        Assert.assertEquals(3, dbAggregationResults.getTotalCount().intValue());
        Assert.assertTrue(dbAggregationResults.getRecords().stream().map(DbAggregationResult::getKey).filter(x -> x!=null).collect(Collectors.toList()).containsAll(List.of("SUCCESS", "FAILED")));

        dbAggregationResults = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company1,
                CiCdJobRunsFilter.builder()
                        .across(CiCdJobRunsFilter.DISTINCT.step_name)
                        .calculation(CiCdJobRunsFilter.CALCULATION.count)
                        .jobNormalizedFullNames(List.of("null/main"))
                        .stageNames(List.of("build", "test"))
                        .build(),
                true,
                null);

        Assert.assertEquals(3, dbAggregationResults.getTotalCount().intValue());
        Assert.assertTrue(dbAggregationResults.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()).containsAll(List.of("build-job", "lint-test-job", "unit-test-job")));

        dbAggregationResults = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company1,
                CiCdJobRunsFilter.builder()
                        .across(CiCdJobRunsFilter.DISTINCT.step_name)
                        .calculation(CiCdJobRunsFilter.CALCULATION.count)
                        .jobNormalizedFullNames(List.of("null/main"))
                        .instanceNames(List.of("instance-name-0"))
                        .excludeStageNames(List.of("build", "test"))
                        .build(),
                true,
                null);
        Assert.assertEquals(0, dbAggregationResults.getTotalCount().intValue());
        Assert.assertTrue(dbAggregationResults.getRecords().stream().map(DbAggregationResult::getKey).filter(x -> x!=null).collect(Collectors.toList()).isEmpty());

        dbAggregationResults = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company1,
                CiCdJobRunsFilter.builder()
                        .across(CiCdJobRunsFilter.DISTINCT.step_status)
                        .calculation(CiCdJobRunsFilter.CALCULATION.count)
                        .stepNames(List.of("lint-test-job", "build-job"))
                        .build(),
                true,
                null);
        Assert.assertEquals(1, dbAggregationResults.getTotalCount().intValue());
        Assert.assertTrue(dbAggregationResults.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()).containsAll(List.of("SUCCESS")));

        dbAggregationResults = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company1,
                CiCdJobRunsFilter.builder()
                        .across(CiCdJobRunsFilter.DISTINCT.step_status)
                        .calculation(CiCdJobRunsFilter.CALCULATION.count)
                        .stepNames(List.of("unit-test-job", "deploy-job"))
                        .build(),
                true,
                null);
        Assert.assertEquals(2, dbAggregationResults.getTotalCount().intValue());
        Assert.assertTrue(dbAggregationResults.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()).containsAll(List.of("FAILED", "SUCCESS")));

        dbAggregationResults = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company1,
                CiCdJobRunsFilter.builder()
                        .across(CiCdJobRunsFilter.DISTINCT.step_status)
                        .calculation(CiCdJobRunsFilter.CALCULATION.count)
                        .stepStatuses(List.of("FAILED"))
                        .build(),
                true,
                null);
        Assert.assertEquals(1, dbAggregationResults.getTotalCount().intValue());
        Assert.assertTrue(dbAggregationResults.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()).containsAll(List.of("FAILED")));
    }

    @Test
    public void testGroupByAndCalculateForCiCdJobRunStageStepsFiltersForJobCounts() throws IOException, SQLException {
        setupGitlabPipelineData();
        DbListResponse<DbAggregationResult> dbAggregationResults = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company1,
                CiCdJobRunsFilter.builder()
                        .across(CiCdJobRunsFilter.DISTINCT.job_name)
                        .calculation(CiCdJobRunsFilter.CALCULATION.count)
                        .jobNormalizedFullNames(List.of("null/main"))
                        .stageNames(List.of("build", "test"))
                        .stepStatuses(List.of("SUCCESS"))
                        .excludeStageStatuses(List.of("FAILED"))
                        .build(),
                true,
                null);
        Assert.assertEquals(1, dbAggregationResults.getTotalCount().intValue());
        Assert.assertEquals("main", dbAggregationResults.getRecords().get(0).getKey());
        Assert.assertEquals(6, dbAggregationResults.getRecords().get(0).getCount().intValue());

        DbListResponse<CICDJobRunDTO> dbListResponse = ciCdAggsService.listCiCdJobRuns(company1,
                CiCdJobRunsFilter.builder()
                        .across(CiCdJobRunsFilter.DISTINCT.job_name)
                        .calculation(CiCdJobRunsFilter.CALCULATION.count)
                        .jobNormalizedFullNames(List.of("null/main"))
                        .stageNames(List.of("build", "test"))
                        .stepStatuses(List.of("SUCCESS"))
                        .excludeStageStatuses(List.of("FAILED"))
                        .build(), 0, 10, null);

        Assert.assertEquals(6, dbListResponse.getTotalCount().intValue());
        Assert.assertTrue(dbListResponse.getRecords().stream().map(x -> x.getJobName()).collect(Collectors.toSet()).equals(Set.of("main")));

    }

    private void setupGitlabPipelineData() throws IOException, SQLException {
        String projectsInput = ResourceUtils.getResourceAsString("json/databases/gitlab_pipelines.json");
        PaginatedResponse<GitlabProject> projects = MAPPER.readValue(projectsInput, MAPPER.getTypeFactory()
                .constructParametricType(PaginatedResponse.class, GitlabProject.class));
        GitlabProject gitlabProject = projects.getResponse().getRecords().get(0);
        List<GitlabPipeline> pipelines = gitlabProject.getPipelines();
        CICDInstance cicdInstance = CiCdInstanceUtils.createCiCdInstance(ciCdInstancesDatabaseService, company1, 0);
        pipelines.forEach((pipeline) -> {
            String ciCdJobId = null;
            CICDJobRun cicdJobRun = null;
            try {
                ciCdJobId = insertGitlabJobs(company1, cicdInstance.getId(), pipeline);
                cicdJobRun = insertGitlabJobRuns(company1, pipeline, ciCdJobId);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            insertStageAndStep(company1, pipeline, cicdJobRun);
        });
    }

    private String insertGitlabJobs(String company1, UUID ciCdInstanceId, GitlabPipeline pipeline) throws SQLException {
        CICDJob cicdJob = CICDJob.builder()
                .projectName(pipeline.getPathWithNamespace())
                .jobName(pipeline.getRef())
                .jobFullName(pipeline.getPathWithNamespace() + "/" + pipeline.getRef())
                .jobNormalizedFullName(pipeline.getPathWithNamespace() + "/" + pipeline.getRef())
                .branchName(pipeline.getRef())
                .scmUrl(pipeline.getHttpUrlToRepo())
                .cicdInstanceId(ciCdInstanceId)
                .build();
        return ciCdJobsDatabaseService.insert(company1, cicdJob);
    }

    private CICDJobRun insertGitlabJobRuns(String company1, GitlabPipeline pipeline, String ciCdJobId) throws SQLException {
        CICDJobRun cicdJobRun = CICDJobRun.builder()
                .cicdJobId(UUID.fromString(ciCdJobId))
                .jobRunNumber(Long.valueOf(pipeline.getPipelineId()))
                .status(pipeline.getStatus())
                .startTime(pipeline.getStartedAt() != null ? pipeline.getStartedAt().toInstant() : null)
                .endTime(pipeline.getFinishedAt() != null ? pipeline.getFinishedAt().toInstant() : null)
                .duration(pipeline.getDuration())
                .cicdUserId(pipeline.getUser() != null ? pipeline.getUser().getName() : "UNKNOWN")
                .scmCommitIds(List.of(pipeline.getSha()))
                .build();
        String cicdJobRunId = ciCdJobRunsDatabaseService.insert(company1, cicdJobRun);
        cicdJobRun = cicdJobRun.toBuilder().id(UUID.fromString(cicdJobRunId)).build();

        return cicdJobRun;
    }

    private List<String> insertStageAndStep(String company1, GitlabPipeline pipeline, CICDJobRun ciCdJobRun) {
        Map<String, List<GitlabJob>> jobsAcrossStage = CollectionUtils.emptyIfNull(pipeline.getJobs()).stream()
                .collect(Collectors.groupingBy(GitlabJob::getStage, Collectors.toList()));
        return jobsAcrossStage.entrySet().stream()
                .flatMap(es -> {
                    try {
                        Optional<String> optStageId = insertJobRunStage(company1, ciCdJobRun, es);
                        if (optStageId.isEmpty()) {
                            return Stream.empty();
                        }
                        List<String> stepId = insertJobRunStageStep(company1, es, optStageId.get());
                        return stepId.stream();
                    } catch (SQLException throwables) {
                        throw new RuntimeException("Failed to insert job run stage for cicdJobRunId " + ciCdJobRun.getId(), throwables);
                    }
                }).collect(Collectors.toList());
    }

    private String calculateStageStatusFromJobStatuses(Map.Entry<String, List<GitlabJob>> es) {
        List<String> states = new ArrayList<>();

        es.getValue().forEach(job -> {
            if("failed".equalsIgnoreCase(job.getStatus()) && job.isAllowFailure())
                states.add("passed with warnings");
            else if ("failed".equalsIgnoreCase(job.getStatus()))
                states.add("failed");
            else if("canceled".equalsIgnoreCase(job.getStatus()))
                states.add("canceled");
            else if("success".equalsIgnoreCase(job.getStatus()))
                states.add("passed");
            else
                states.add(job.getStatus());
        });

        return states.contains("canceled") ? "canceled" :
                states.contains("failed") ?  "failed" :
                        states.contains("passed with warnings") ? "passed with warnings" :
                                !states.contains("passed") ? "undefined in SEI" : "passed";
    }

    private Optional<String> insertJobRunStage(String company1, CICDJobRun ciCdJobRun, Map.Entry<String, List<GitlabJob>> es) throws SQLException {
        List<String> states = new ArrayList<>();
        long duration = 0;
        String stageStatus = calculateStageStatusFromJobStatuses(es);
        Instant instant = es.getValue().stream()
                .filter(job -> job.getStartedAt() != null)
                .map(job -> job.getStartedAt().toInstant())
                .min(Instant::compareTo)
                .orElse(ciCdJobRun.getStartTime());

        Instant endInstant = es.getValue().stream()
                .filter(job -> job.getFinishedAt() != null)
                .map(job -> job.getFinishedAt().toInstant())
                .max(Instant::compareTo)
                .orElse(ciCdJobRun.getEndTime());

        if(!(instant == null || endInstant == null ))
            duration = Duration.between(instant, endInstant).toSeconds();
        else
            duration = 0;

        JobRunStage jobRunStage = JobRunStage.builder()
                .ciCdJobRunId(ciCdJobRun.getId())
                .name(es.getKey())
                .stageId(es.getKey())
                .state(stageStatus)
                .duration(duration < 0 ? 0 : (int) duration)
                .startTime(instant)
                .result(stageStatus)
                .description(StringUtils.EMPTY)
                .logs(StringUtils.EMPTY)
                .url(StringUtils.EMPTY)
                .fullPath(Set.of())
                .childJobRuns(Set.of())
                .build();
        return Optional.ofNullable(ciCdJobRunStageDatabaseService.insert(company1, jobRunStage));
    }

    private List<String> insertJobRunStageStep(String company1, Map.Entry<String, List<GitlabJob>> es, String finalStageId) {
        return es.getValue().stream()
                .filter(job -> job.getStartedAt() != null)
                .map(job -> {
                    try {
                        JobRunStageStep jobRunStageStep = JobRunStageStep.builder()
                                .cicdJobRunStageId(UUID.fromString(finalStageId))
                                .stepId(job.getId())
                                .displayName(job.getName())
                                .startTime((job.getStartedAt() != null) ? job.getStartedAt().toInstant() : null)
                                .state(job.getStatus().toUpperCase())
                                .result(job.getStatus().toUpperCase())
                                .duration((int) job.getDuration())
                                .build();
                        return ciCdJobRunStageStepsDatabaseService.insert(company1, jobRunStageStep);
                    } catch (SQLException throwables) {
                        throw new RuntimeException("Failed to insert job run stage step for stage id " + finalStageId, throwables);
                    }
                }).collect(Collectors.toList());
    }
    // endregion

    // region Job runs for metadata
    @Test
    public void testGroupByAndCalculateCiCdJobsForMetadata() throws SQLException, IOException {
        String data = ResourceUtils.getResourceAsString("json/databases/harness_jobs_job_runs.json");
        List<JobDetails> allJobDetails = MAPPER.readValue(data, MAPPER.getTypeFactory().constructCollectionType(List.class, JobDetails.class));
        allJobDetails = fixJobRunTimestamps(allJobDetails, calculateOffset());
        CICDInstance cicdInstance = CiCdInstanceUtils.createCiCdInstance(ciCdInstancesDatabaseService, company, 0);
        setupJobsAndJobRunsWithMetadata(cicdInstance.getId(), allJobDetails);

        DbListResponse<DbAggregationResult> dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder()
                        .across(CiCdJobRunsFilter.DISTINCT.service)
                        .calculation(CiCdJobRunsFilter.CALCULATION.count)
                        .build(),
                VALUES_ONLY
        );

        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(4, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(4, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(4, dbAggsResponse.getRecords().size());

        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder()
                        .across(CiCdJobRunsFilter.DISTINCT.branch)
                        .calculation(CiCdJobRunsFilter.CALCULATION.count)
                        .build(),
                VALUES_ONLY
        );

        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(3, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(3, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(3, dbAggsResponse.getRecords().size());

        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder()
                        .across(CiCdJobRunsFilter.DISTINCT.repository)
                        .calculation(CiCdJobRunsFilter.CALCULATION.count)
                        .build(),
                VALUES_ONLY
        );

        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(1, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getRecords().size());

        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder()
                        .across(CiCdJobRunsFilter.DISTINCT.environment)
                        .calculation(CiCdJobRunsFilter.CALCULATION.count)
                        .build(),
                VALUES_ONLY
        );

        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(3, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(3, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(3, dbAggsResponse.getRecords().size());

        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder()
                        .across(CiCdJobRunsFilter.DISTINCT.infrastructure)
                        .calculation(CiCdJobRunsFilter.CALCULATION.count)
                        .build(),
                VALUES_ONLY
        );

        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(5, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(5, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(5, dbAggsResponse.getRecords().size());

        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder()
                        .across(CiCdJobRunsFilter.DISTINCT.deployment_type)
                        .calculation(CiCdJobRunsFilter.CALCULATION.count)
                        .build(),
                VALUES_ONLY
        );

        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(2, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getRecords().size());
    }
    // endregion

    // region Verify Records
    private void verifyRecord(DbAggregationResult a, DbAggregationResult e, boolean ignoreKey) {
        Assert.assertEquals((e == null), (a == null));
        if (e == null) {
            return;
        }
        if (!ignoreKey) {
            Assert.assertEquals(a.getKey(), e.getKey());
        }
        Assert.assertEquals(a.getAdditionalKey(), e.getAdditionalKey());
        Assert.assertEquals(a.getMedian(), e.getMedian());
        Assert.assertEquals(a.getMin(), e.getMin());
        Assert.assertEquals(a.getMax(), e.getMax());
        Assert.assertEquals(a.getCount(), e.getCount());
        Assert.assertEquals(a.getSum(), e.getSum());
        Assert.assertEquals(a.getTotalTickets(), e.getTotalTickets());
        Assert.assertEquals(a.getLinesAddedCount(), e.getLinesAddedCount());
        Assert.assertEquals(a.getLinesRemovedCount(), e.getLinesRemovedCount());
        Assert.assertEquals(a.getFilesChangedCount(), e.getFilesChangedCount());
        verifyRecords(a.getStacks(), e.getStacks(), false);
    }

    private Map<Object, DbAggregationResult> convertListToMap(List<DbAggregationResult> lst, boolean ignoreKey) {
        Map<Object, DbAggregationResult> map = new HashMap<>();
        for (int i = 0; i < lst.size(); i++) {
            if (ignoreKey) {
                map.put(i, lst.get(i));
            } else {
                map.put(AcrossUniqueKey.fromDbAggregationResult(lst.get(i)), lst.get(i));
            }
        }
        return map;
    }

    private void verifyRecords(List<DbAggregationResult> a, List<DbAggregationResult> e, boolean ignoreKey) {
        Assert.assertEquals(CollectionUtils.isEmpty(a), CollectionUtils.isEmpty(e));
        if (CollectionUtils.isEmpty(a)) {
            return;
        }
        Assert.assertEquals(a.size(), e.size());
        Map<Object, DbAggregationResult> actualMap = convertListToMap(a, ignoreKey);
        Map<Object, DbAggregationResult> expectedMap = convertListToMap(e, ignoreKey);
        for (Object key : actualMap.keySet()) {
            verifyRecord(actualMap.get(key), expectedMap.get(key), ignoreKey);
        }
    }
    // endregion

    // region Job Config Changes
    @Test
    public void testGroupByAndCalculateCiCdJobConfigChanges() throws SQLException {
        // region Setup
        List<CICDInstance> cicdInstances = CiCdInstanceUtils.createCiCdInstances(ciCdInstancesDatabaseService, company, integration, 2);
        List<CICDJob> cicdJobs = new ArrayList<>();
        List<CICDJob> cicdJobsWithoutInstances = CiCdJobUtils.createCICDJobs(ciCdJobsDatabaseService, company, null, 3);
        cicdJobs.addAll(cicdJobsWithoutInstances);
        for (int i = 0; i < cicdInstances.size(); i++) {
            List<CICDJob> cicdJobsWithInstances = CiCdJobUtils.createCICDJobs(ciCdJobsDatabaseService, company, cicdInstances.get(i), 3);
            cicdJobs.addAll(cicdJobsWithInstances);
        }

        List<CICDJobConfigChange> cicdJobConfigChanges = new ArrayList<>();
        for (int i = 0; i < cicdJobs.size(); i++) {
            cicdJobConfigChanges.addAll(CiCdJobConfigChangesUtils.createCICDJobConfigChanges(ciCdJobConfigChangesDatabaseService, company, cicdJobs.get(i), i, i + 1));
        }
        // endregion

        // region trend - no filters
        DbListResponse<DbAggregationResult> dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobConfigChanges(company,
                CiCdJobConfigChangesFilter.builder().across(CiCdJobConfigChangesFilter.DISTINCT.trend).calculation(CiCdJobConfigChangesFilter.CALCULATION.count).build(),
                VALUES_ONLY,
                null
        );

        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(9, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(9, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(9, dbAggsResponse.getRecords().size());
        List<DbAggregationResult> expected = List.of(
                DbAggregationResult.builder().key("1593500400").count(1L).build(),
                DbAggregationResult.builder().key("1593586800").count(2L).build(),
                DbAggregationResult.builder().key("1593673200").count(3L).build(),
                DbAggregationResult.builder().key("1593759600").count(4L).build(),
                DbAggregationResult.builder().key("1593846000").count(5L).build(),
                DbAggregationResult.builder().key("1594450800").count(6L).build(),
                DbAggregationResult.builder().key("1594537200").count(7L).build(),
                DbAggregationResult.builder().key("1594623600").count(8L).build(),
                DbAggregationResult.builder().key("1594710000").count(9L).build()
        );
        Assert.assertEquals(expected.stream().map(DbAggregationResult::getCount).collect(Collectors.toSet()), dbAggsResponse.getRecords().stream().map(DbAggregationResult::getCount).collect(Collectors.toSet()));
        // endregion

        // region trend - job name
        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobConfigChanges(company,
                CiCdJobConfigChangesFilter.builder().across(CiCdJobConfigChangesFilter.DISTINCT.trend).calculation(CiCdJobConfigChangesFilter.CALCULATION.count)
                        .jobNames(List.of("jobname-0")).build(),
                VALUES_ONLY,
                null
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(7, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(7, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(7, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("1593673200").count(1L).build(),
                DbAggregationResult.builder().key("1593759600").count(1L).build(),
                DbAggregationResult.builder().key("1593846000").count(1L).build(),
                DbAggregationResult.builder().key("1594450800").count(2L).build(),
                DbAggregationResult.builder().key("1594537200").count(2L).build(),
                DbAggregationResult.builder().key("1594623600").count(2L).build(),
                DbAggregationResult.builder().key("1594710000").count(3L).build()
        );
        Assert.assertEquals(expected.stream().map(DbAggregationResult::getCount).collect(Collectors.toSet()), dbAggsResponse.getRecords().stream().map(DbAggregationResult::getCount).collect(Collectors.toSet()));
        // endregion
        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobConfigChanges(company,
                CiCdJobConfigChangesFilter.builder().across(CiCdJobConfigChangesFilter.DISTINCT.project_name)
                        .stacks(List.of(CiCdJobConfigChangesFilter.DISTINCT.job_name)).calculation(CiCdJobConfigChangesFilter.CALCULATION.count)
                        .build(),
                VALUES_ONLY,
                null
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(3, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(3, dbAggsResponse.getTotalCount().intValue());

        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobConfigChanges(company,
                CiCdJobConfigChangesFilter.builder().across(CiCdJobConfigChangesFilter.DISTINCT.job_name)
                        .stacks(List.of(CiCdJobConfigChangesFilter.DISTINCT.project_name)).calculation(CiCdJobConfigChangesFilter.CALCULATION.count)
                        .build(),
                VALUES_ONLY,
                null
        );

        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(3, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(3, dbAggsResponse.getTotalCount().intValue());

        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobConfigChanges(company,
                CiCdJobConfigChangesFilter.builder().across(CiCdJobConfigChangesFilter.DISTINCT.project_name).projects(List.of("project-1")).calculation(CiCdJobConfigChangesFilter.CALCULATION.count)
                        .build(),
                VALUES_ONLY,
                null
        );
        DefaultObjectMapper.prettyPrint(dbAggsResponse);
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(1, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getTotalCount().intValue());

        // region trend - qualified job name
        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobConfigChanges(company,
                CiCdJobConfigChangesFilter.builder().across(CiCdJobConfigChangesFilter.DISTINCT.trend).calculation(CiCdJobConfigChangesFilter.CALCULATION.count)
                        .qualifiedJobNames(List.of(CiCdJobQualifiedName.builder().instanceName("instance-name-0").jobName("jobname-0").build())).build(),
                VALUES_ONLY,
                null
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(4, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(4, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(4, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("1593673200").count(1L).build(),
                DbAggregationResult.builder().key("1593759600").count(1L).build(),
                DbAggregationResult.builder().key("1593846000").count(1L).build(),
                DbAggregationResult.builder().key("1594450800").count(1L).build()
        );
        Assert.assertEquals(expected.stream().map(DbAggregationResult::getCount).collect(Collectors.toSet()), dbAggsResponse.getRecords().stream().map(DbAggregationResult::getCount).collect(Collectors.toSet()));
        // endregion

        // region trend - cicd user id
        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobConfigChanges(company,
                CiCdJobConfigChangesFilter.builder().across(CiCdJobConfigChangesFilter.DISTINCT.trend).calculation(CiCdJobConfigChangesFilter.CALCULATION.count)
                        .cicdUserIds(List.of("user-jenkins-8")).build(),
                VALUES_ONLY,
                null
        );

        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(9, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(9, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(9, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("1593500400").count(1L).build(),
                DbAggregationResult.builder().key("1593586800").count(1L).build(),
                DbAggregationResult.builder().key("1593673200").count(1L).build(),
                DbAggregationResult.builder().key("1593759600").count(1L).build(),
                DbAggregationResult.builder().key("1593846000").count(1L).build(),
                DbAggregationResult.builder().key("1594450800").count(1L).build(),
                DbAggregationResult.builder().key("1594537200").count(1L).build(),
                DbAggregationResult.builder().key("1594623600").count(1L).build(),
                DbAggregationResult.builder().key("1594710000").count(1L).build()
        );
        Assert.assertEquals(expected.stream().map(DbAggregationResult::getCount).collect(Collectors.toSet()), dbAggsResponse.getRecords().stream().map(DbAggregationResult::getCount).collect(Collectors.toSet()));
        // endregion

        // region job name - no filters
        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobConfigChanges(company,
                CiCdJobConfigChangesFilter.builder().across(CiCdJobConfigChangesFilter.DISTINCT.job_name).calculation(CiCdJobConfigChangesFilter.CALCULATION.count).build(),
                VALUES_ONLY,
                null
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(3, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(3, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(3, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("jobname-2").count(18L).build(),
                DbAggregationResult.builder().key("jobname-1").count(15L).build(),
                DbAggregationResult.builder().key("jobname-0").count(12L).build()
        );
        Assert.assertEquals(expected, dbAggsResponse.getRecords());
        // endregion

        // region instance name - no filters
        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobConfigChanges(company,
                CiCdJobConfigChangesFilter.builder().across(CiCdJobConfigChangesFilter.DISTINCT.instance_name).calculation(CiCdJobConfigChangesFilter.CALCULATION.count).build(),
                VALUES_ONLY,
                null
        );
        Assert.assertEquals(2, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("instance-name-1").count(24L).build(),
                DbAggregationResult.builder().key("instance-name-0").count(15L).build()
        );
        Assert.assertEquals(expected, dbAggsResponse.getRecords());
        // endregion

        // region instance name - type filter
        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobConfigChanges(company,
                CiCdJobConfigChangesFilter.builder().across(CiCdJobConfigChangesFilter.DISTINCT.instance_name)
                        .types(List.of(CICD_TYPE.jenkins))
                        .calculation(CiCdJobConfigChangesFilter.CALCULATION.count).build(),
                VALUES_ONLY,
                null
        );
        Assert.assertEquals(2, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("instance-name-1").count(24L).build(),
                DbAggregationResult.builder().key("instance-name-0").count(15L).build()
        );
        Assert.assertEquals(expected, dbAggsResponse.getRecords());
        // endregion

        // region job name - job name
        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobConfigChanges(company,
                CiCdJobConfigChangesFilter.builder().across(CiCdJobConfigChangesFilter.DISTINCT.job_name).calculation(CiCdJobConfigChangesFilter.CALCULATION.count)
                        .jobNames(List.of("jobname-0")).build(),
                VALUES_ONLY,
                null
        );

        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(1, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("jobname-0").count(12L).build()
        );
        Assert.assertEquals(expected, dbAggsResponse.getRecords());
        // endregion

        // region job name - qualified job name
        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobConfigChanges(company,
                CiCdJobConfigChangesFilter.builder().across(CiCdJobConfigChangesFilter.DISTINCT.job_name).calculation(CiCdJobConfigChangesFilter.CALCULATION.count)
                        .qualifiedJobNames(List.of(CiCdJobQualifiedName.builder().jobName("jobname-2").instanceName("instance-name-0").build())).build(),
                VALUES_ONLY,
                null
        );

        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(1, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("jobname-2").count(6L).build()
        );
        Assert.assertEquals(expected, dbAggsResponse.getRecords());
        // endregion

        // region job name - cicd user id
        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobConfigChanges(company,
                CiCdJobConfigChangesFilter.builder().across(CiCdJobConfigChangesFilter.DISTINCT.job_name).calculation(CiCdJobConfigChangesFilter.CALCULATION.count)
                        .cicdUserIds(List.of("user-jenkins-0")).build(),
                VALUES_ONLY,
                null
        );

        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(1, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("jobname-0").count(1L).build()
        );
        Assert.assertEquals(expected, dbAggsResponse.getRecords());
        // endregion

        // region qualified job name - no filters
        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobConfigChanges(company,
                CiCdJobConfigChangesFilter.builder().across(CiCdJobConfigChangesFilter.DISTINCT.qualified_job_name).calculation(CiCdJobConfigChangesFilter.CALCULATION.count).build(),
                VALUES_ONLY,
                null
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(9, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(9, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(9, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("jobname-2").additionalKey("instance-name-1").count(9L).build(),
                DbAggregationResult.builder().key("jobname-1").additionalKey("instance-name-1").count(8L).build(),
                DbAggregationResult.builder().key("jobname-0").additionalKey("instance-name-1").count(7L).build(),
                DbAggregationResult.builder().key("jobname-2").additionalKey("instance-name-0").count(6L).build(),
                DbAggregationResult.builder().key("jobname-1").additionalKey("instance-name-0").count(5L).build(),
                DbAggregationResult.builder().key("jobname-0").additionalKey("instance-name-0").count(4L).build(),
                DbAggregationResult.builder().key("jobname-2").additionalKey(null).count(3L).build(),
                DbAggregationResult.builder().key("jobname-1").additionalKey(null).count(2L).build(),
                DbAggregationResult.builder().key("jobname-0").additionalKey(null).count(1L).build()
        );
        Assert.assertEquals(expected, dbAggsResponse.getRecords());
        // endregion

        // region qualified  job name - job name
        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobConfigChanges(company,
                CiCdJobConfigChangesFilter.builder().across(CiCdJobConfigChangesFilter.DISTINCT.qualified_job_name).calculation(CiCdJobConfigChangesFilter.CALCULATION.count)
                        .jobNames(List.of("jobname-0")).build(),
                VALUES_ONLY,
                null
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(3, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(3, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(3, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("jobname-0").additionalKey("instance-name-1").count(7L).build(),
                DbAggregationResult.builder().key("jobname-0").additionalKey("instance-name-0").count(4L).build(),
                DbAggregationResult.builder().key("jobname-0").additionalKey(null).count(1L).build()
        );
        Assert.assertEquals(expected, dbAggsResponse.getRecords());
        // endregion

        // region qualified job name - qualified job name
        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobConfigChanges(company,
                CiCdJobConfigChangesFilter.builder().across(CiCdJobConfigChangesFilter.DISTINCT.qualified_job_name).calculation(CiCdJobConfigChangesFilter.CALCULATION.count)
                        .qualifiedJobNames(List.of(
                                CiCdJobQualifiedName.builder().instanceName("instance-name-1").jobName("jobname-1").build(),
                                CiCdJobQualifiedName.builder().instanceName(null).jobName("jobname-2").build()
                        )).build(),
                VALUES_ONLY,
                null
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(2, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("jobname-1").additionalKey("instance-name-1").count(8L).build(),
                DbAggregationResult.builder().key("jobname-2").additionalKey(null).count(3L).build()
        );
        Assert.assertEquals(expected, dbAggsResponse.getRecords());
        // endregion

        // region qualified job name - cicd user id
        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobConfigChanges(company,
                CiCdJobConfigChangesFilter.builder().across(CiCdJobConfigChangesFilter.DISTINCT.qualified_job_name).calculation(CiCdJobConfigChangesFilter.CALCULATION.count)
                        .cicdUserIds(List.of("user-jenkins-3")).build(),
                VALUES_ONLY,
                null
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(1, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("jobname-0").additionalKey("instance-name-0").count(4L).build()
        );
        Assert.assertEquals(expected, dbAggsResponse.getRecords());
        // endregion

        // region cicd user id - no filters
        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobConfigChanges(company,
                CiCdJobConfigChangesFilter.builder().across(CiCdJobConfigChangesFilter.DISTINCT.cicd_user_id).calculation(CiCdJobConfigChangesFilter.CALCULATION.count).build(),
                VALUES_ONLY,
                null
        );

        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(9, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(9, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(9, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("user-jenkins-8").count(9L).build(),
                DbAggregationResult.builder().key("user-jenkins-7").count(8L).build(),
                DbAggregationResult.builder().key("user-jenkins-6").count(7L).build(),
                DbAggregationResult.builder().key("user-jenkins-5").count(6L).build(),
                DbAggregationResult.builder().key("user-jenkins-4").count(5L).build(),
                DbAggregationResult.builder().key("user-jenkins-3").count(4L).build(),
                DbAggregationResult.builder().key("user-jenkins-2").count(3L).build(),
                DbAggregationResult.builder().key("user-jenkins-1").count(2L).build(),
                DbAggregationResult.builder().key("user-jenkins-0").count(1L).build()
        );
        Assert.assertEquals(expected, dbAggsResponse.getRecords());
        // endregion

        // region cicd user id - job name
        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobConfigChanges(company,
                CiCdJobConfigChangesFilter.builder().across(CiCdJobConfigChangesFilter.DISTINCT.cicd_user_id).calculation(CiCdJobConfigChangesFilter.CALCULATION.count)
                        .jobNames(List.of("jobname-0")).build(),
                VALUES_ONLY,
                null
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(3, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(3, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(3, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("user-jenkins-6").count(7L).build(),
                DbAggregationResult.builder().key("user-jenkins-3").count(4L).build(),
                DbAggregationResult.builder().key("user-jenkins-0").count(1L).build()
        );
        Assert.assertEquals(expected, dbAggsResponse.getRecords());
        // endregion

        // region cicd user id - qualified job name
        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobConfigChanges(company,
                CiCdJobConfigChangesFilter.builder().across(CiCdJobConfigChangesFilter.DISTINCT.cicd_user_id).calculation(CiCdJobConfigChangesFilter.CALCULATION.count)
                        .qualifiedJobNames(List.of(CiCdJobQualifiedName.builder().instanceName("instance-name-0").jobName("jobname-0").build())).build(),
                VALUES_ONLY,
                null
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(1, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("user-jenkins-3").count(4L).build()
        );
        Assert.assertEquals(expected, dbAggsResponse.getRecords());
        // endregion

        // region cicd user id - cicd user id
        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobConfigChanges(company,
                CiCdJobConfigChangesFilter.builder().across(CiCdJobConfigChangesFilter.DISTINCT.cicd_user_id).calculation(CiCdJobConfigChangesFilter.CALCULATION.count)
                        .cicdUserIds(List.of("user-jenkins-0")).build(),
                VALUES_ONLY,
                null
        );

        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(1, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("user-jenkins-0").count(1L).build()
        );
        Assert.assertEquals(expected, dbAggsResponse.getRecords());
        // endregion

        // region trend - no filters - stack job name
        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobConfigChanges(company,
                CiCdJobConfigChangesFilter.builder().across(CiCdJobConfigChangesFilter.DISTINCT.trend)
                        .calculation(CiCdJobConfigChangesFilter.CALCULATION.count)
                        .stacks(List.of(CiCdJobConfigChangesFilter.DISTINCT.job_name))
                        .build(), Map.of("trend", SortingOrder.ASC),
                VALUES_ONLY,
                null
        );

        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(9, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(9, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(9, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("1593500400").count(1L).stacks(
                        List.of(DbAggregationResult.builder().key("jobname-2").count(1L).build())
                ).build(),
                DbAggregationResult.builder().key("1593586800").count(2L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("jobname-2").count(1L).build(),
                                DbAggregationResult.builder().key("jobname-1").count(1L).build()
                        )).build(),
                DbAggregationResult.builder().key("1593673200").count(3L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("jobname-2").count(1L).build(),
                                DbAggregationResult.builder().key("jobname-1").count(1L).build(),
                                DbAggregationResult.builder().key("jobname-0").count(1L).build()
                        )).build(),
                DbAggregationResult.builder().key("1593759600").count(4L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("jobname-2").count(2L).build(),
                                DbAggregationResult.builder().key("jobname-1").count(1L).build(),
                                DbAggregationResult.builder().key("jobname-0").count(1L).build()
                        )).build(),
                DbAggregationResult.builder().key("1593846000").count(5L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("jobname-2").count(2L).build(),
                                DbAggregationResult.builder().key("jobname-1").count(2L).build(),
                                DbAggregationResult.builder().key("jobname-0").count(1L).build()
                        )).build(),
                DbAggregationResult.builder().key("1594450800").count(6L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("jobname-2").count(2L).build(),
                                DbAggregationResult.builder().key("jobname-1").count(2L).build(),
                                DbAggregationResult.builder().key("jobname-0").count(2L).build()
                        )).build(),
                DbAggregationResult.builder().key("1594537200").count(7L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("jobname-2").count(3L).build(),
                                DbAggregationResult.builder().key("jobname-1").count(2L).build(),
                                DbAggregationResult.builder().key("jobname-0").count(2L).build()
                        )).build(),
                DbAggregationResult.builder().key("1594623600").count(8L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("jobname-2").count(3L).build(),
                                DbAggregationResult.builder().key("jobname-1").count(3L).build(),
                                DbAggregationResult.builder().key("jobname-0").count(2L).build()
                        )).build(),
                DbAggregationResult.builder().key("1594710000").count(9L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("jobname-2").count(3L).build(),
                                DbAggregationResult.builder().key("jobname-1").count(3L).build(),
                                DbAggregationResult.builder().key("jobname-0").count(3L).build()
                        )).build()
        );
        Assert.assertEquals(expected.stream().map(DbAggregationResult::getCount).collect(Collectors.toSet()), dbAggsResponse.getRecords().stream().map(DbAggregationResult::getCount).collect(Collectors.toSet()));
        verifyRecords(dbAggsResponse.getRecords(), expected, true);
        // endregion

        // region trend - job name - stack job name
        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobConfigChanges(company,
                CiCdJobConfigChangesFilter.builder().across(CiCdJobConfigChangesFilter.DISTINCT.trend).calculation(CiCdJobConfigChangesFilter.CALCULATION.count)
                        .stacks(List.of(CiCdJobConfigChangesFilter.DISTINCT.job_name))
                        .jobNames(List.of("jobname-0")).build(), Map.of("trend", SortingOrder.ASC),
                VALUES_ONLY,
                null
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(7, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(7, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(7, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("1593673200").count(1L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("jobname-0").count(1L).build()
                        )).build(),
                DbAggregationResult.builder().key("1593759600").count(1L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("jobname-0").count(1L).build()
                        )).build(),
                DbAggregationResult.builder().key("1593846000").count(1L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("jobname-0").count(1L).build()
                        )).build(),
                DbAggregationResult.builder().key("1594450800").count(2L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("jobname-0").count(2L).build()
                        )).build(),
                DbAggregationResult.builder().key("1594537200").count(2L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("jobname-0").count(2L).build()
                        )).build(),
                DbAggregationResult.builder().key("1594623600").count(2L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("jobname-0").count(2L).build()
                        )).build(),
                DbAggregationResult.builder().key("1594710000").count(3L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("jobname-0").count(3L).build()
                        )).build()
        );
        Assert.assertEquals(expected.stream().map(DbAggregationResult::getCount).collect(Collectors.toSet()), dbAggsResponse.getRecords().stream().map(DbAggregationResult::getCount).collect(Collectors.toSet()));
        verifyRecords(dbAggsResponse.getRecords(), expected, true);
        // endregion

        // region trend - qualified job name - stack job name
        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobConfigChanges(company,
                CiCdJobConfigChangesFilter.builder().across(CiCdJobConfigChangesFilter.DISTINCT.trend).calculation(CiCdJobConfigChangesFilter.CALCULATION.count)
                        .stacks(List.of(CiCdJobConfigChangesFilter.DISTINCT.job_name))
                        .qualifiedJobNames(List.of(CiCdJobQualifiedName.builder().instanceName("instance-name-0").jobName("jobname-0").build())).build(),
                VALUES_ONLY,
                null
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(4, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(4, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(4, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("1594450800").count(1L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("jobname-0").count(1L).build()
                        )).build(),
                DbAggregationResult.builder().key("1594537200").count(1L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("jobname-0").count(1L).build()
                        )).build(),
                DbAggregationResult.builder().key("1594623600").count(1L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("jobname-0").count(1L).build()
                        )).build(),
                DbAggregationResult.builder().key("1594710000").count(1L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("jobname-0").count(1L).build()
                        )).build()
        );
        Assert.assertEquals(expected.stream().map(DbAggregationResult::getCount).collect(Collectors.toSet()), dbAggsResponse.getRecords().stream().map(DbAggregationResult::getCount).collect(Collectors.toSet()));
        verifyRecords(dbAggsResponse.getRecords(), expected, true);
        // endregion

        // region trend - cicd user id - stack job name
        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobConfigChanges(company,
                CiCdJobConfigChangesFilter.builder().across(CiCdJobConfigChangesFilter.DISTINCT.trend).calculation(CiCdJobConfigChangesFilter.CALCULATION.count)
                        .stacks(List.of(CiCdJobConfigChangesFilter.DISTINCT.job_name))
                        .cicdUserIds(List.of("user-jenkins-8")).build(),
                VALUES_ONLY,
                null
        );

        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(9, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(9, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(9, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("1593500400").count(1L).stacks(
                        List.of(DbAggregationResult.builder().key("jobname-2").count(1L).build())
                ).build(),
                DbAggregationResult.builder().key("1593586800").count(1L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("jobname-2").count(1L).build()
                        )).build(),
                DbAggregationResult.builder().key("1593673200").count(1L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("jobname-2").count(1L).build()
                        )).build(),
                DbAggregationResult.builder().key("1593759600").count(1L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("jobname-2").count(1L).build()
                        )).build(),
                DbAggregationResult.builder().key("1593846000").count(1L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("jobname-2").count(1L).build()
                        )).build(),
                DbAggregationResult.builder().key("1594450800").count(1L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("jobname-2").count(1L).build()
                        )).build(),
                DbAggregationResult.builder().key("1594537200").count(1L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("jobname-2").count(1L).build()
                        )).build(),
                DbAggregationResult.builder().key("1594623600").count(1L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("jobname-2").count(1L).build()
                        )).build(),
                DbAggregationResult.builder().key("1594710000").count(1L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("jobname-2").count(1L).build()
                        )).build()
        );
        verifyRecords(dbAggsResponse.getRecords(), expected, true);
        // endregion

        // region trend - no filters - stack qualified job name
        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobConfigChanges(company,
                CiCdJobConfigChangesFilter.builder().across(CiCdJobConfigChangesFilter.DISTINCT.trend)
                        .calculation(CiCdJobConfigChangesFilter.CALCULATION.count)
                        .stacks(List.of(CiCdJobConfigChangesFilter.DISTINCT.qualified_job_name))
                        .build(), Map.of("trend", SortingOrder.ASC),
                VALUES_ONLY,
                null
        );

        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(9, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(9, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(9, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("1593500400").count(1L).stacks(
                        List.of(DbAggregationResult.builder().key("jobname-2").additionalKey("instance-name-1").count(1L).build())
                ).build(),
                DbAggregationResult.builder().key("1593586800").count(2L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("jobname-2").additionalKey("instance-name-1").count(1L).build(),
                                DbAggregationResult.builder().key("jobname-1").additionalKey("instance-name-1").count(1L).build()
                        )).build(),
                DbAggregationResult.builder().key("1593673200").count(3L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("jobname-2").additionalKey("instance-name-1").count(1L).build(),
                                DbAggregationResult.builder().key("jobname-1").additionalKey("instance-name-1").count(1L).build(),
                                DbAggregationResult.builder().key("jobname-0").additionalKey("instance-name-1").count(1L).build()
                        )).build(),
                DbAggregationResult.builder().key("1593759600").count(4L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("jobname-2").additionalKey("instance-name-1").count(1L).build(),
                                DbAggregationResult.builder().key("jobname-1").additionalKey("instance-name-1").count(1L).build(),
                                DbAggregationResult.builder().key("jobname-0").additionalKey("instance-name-1").count(1L).build(),
                                DbAggregationResult.builder().key("jobname-2").additionalKey("instance-name-0").count(1L).build()
                        )).build(),
                DbAggregationResult.builder().key("1593846000").count(5L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("jobname-2").additionalKey("instance-name-1").count(1L).build(),
                                DbAggregationResult.builder().key("jobname-1").additionalKey("instance-name-1").count(1L).build(),
                                DbAggregationResult.builder().key("jobname-0").additionalKey("instance-name-1").count(1L).build(),
                                DbAggregationResult.builder().key("jobname-2").additionalKey("instance-name-0").count(1L).build(),
                                DbAggregationResult.builder().key("jobname-1").additionalKey("instance-name-0").count(1L).build()
                        )).build(),
                DbAggregationResult.builder().key("1594450800").count(6L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("jobname-2").additionalKey("instance-name-1").count(1L).build(),
                                DbAggregationResult.builder().key("jobname-1").additionalKey("instance-name-1").count(1L).build(),
                                DbAggregationResult.builder().key("jobname-0").additionalKey("instance-name-1").count(1L).build(),
                                DbAggregationResult.builder().key("jobname-2").additionalKey("instance-name-0").count(1L).build(),
                                DbAggregationResult.builder().key("jobname-1").additionalKey("instance-name-0").count(1L).build(),
                                DbAggregationResult.builder().key("jobname-0").additionalKey("instance-name-0").count(1L).build()
                        )).build(),
                DbAggregationResult.builder().key("1594537200").count(7L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("jobname-2").additionalKey("instance-name-1").count(1L).build(),
                                DbAggregationResult.builder().key("jobname-1").additionalKey("instance-name-1").count(1L).build(),
                                DbAggregationResult.builder().key("jobname-0").additionalKey("instance-name-1").count(1L).build(),
                                DbAggregationResult.builder().key("jobname-2").additionalKey("instance-name-0").count(1L).build(),
                                DbAggregationResult.builder().key("jobname-1").additionalKey("instance-name-0").count(1L).build(),
                                DbAggregationResult.builder().key("jobname-0").additionalKey("instance-name-0").count(1L).build(),
                                DbAggregationResult.builder().key("jobname-2").additionalKey(null).count(1L).build()
                        )).build(),
                DbAggregationResult.builder().key("1594623600").count(8L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("jobname-2").additionalKey("instance-name-1").count(1L).build(),
                                DbAggregationResult.builder().key("jobname-1").additionalKey("instance-name-1").count(1L).build(),
                                DbAggregationResult.builder().key("jobname-0").additionalKey("instance-name-1").count(1L).build(),
                                DbAggregationResult.builder().key("jobname-2").additionalKey("instance-name-0").count(1L).build(),
                                DbAggregationResult.builder().key("jobname-1").additionalKey("instance-name-0").count(1L).build(),
                                DbAggregationResult.builder().key("jobname-0").additionalKey("instance-name-0").count(1L).build(),
                                DbAggregationResult.builder().key("jobname-2").additionalKey(null).count(1L).build(),
                                DbAggregationResult.builder().key("jobname-1").additionalKey(null).count(1L).build()
                        )).build(),
                DbAggregationResult.builder().key("1594710000").count(9L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("jobname-2").additionalKey("instance-name-1").count(1L).build(),
                                DbAggregationResult.builder().key("jobname-1").additionalKey("instance-name-1").count(1L).build(),
                                DbAggregationResult.builder().key("jobname-0").additionalKey("instance-name-1").count(1L).build(),
                                DbAggregationResult.builder().key("jobname-2").additionalKey("instance-name-0").count(1L).build(),
                                DbAggregationResult.builder().key("jobname-1").additionalKey("instance-name-0").count(1L).build(),
                                DbAggregationResult.builder().key("jobname-0").additionalKey("instance-name-0").count(1L).build(),
                                DbAggregationResult.builder().key("jobname-2").additionalKey(null).count(1L).build(),
                                DbAggregationResult.builder().key("jobname-1").additionalKey(null).count(1L).build(),
                                DbAggregationResult.builder().key("jobname-0").additionalKey(null).count(1L).build()
                        )).build()
        );
        Assert.assertEquals(expected.stream().map(DbAggregationResult::getCount).collect(Collectors.toSet()), dbAggsResponse.getRecords().stream().map(DbAggregationResult::getCount).collect(Collectors.toSet()));
        verifyRecords(dbAggsResponse.getRecords(), expected, true);
        // endregion

        // region trend - no filters - stack cicd user id
        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobConfigChanges(company,
                CiCdJobConfigChangesFilter.builder().across(CiCdJobConfigChangesFilter.DISTINCT.trend)
                        .calculation(CiCdJobConfigChangesFilter.CALCULATION.count)
                        .stacks(List.of(CiCdJobConfigChangesFilter.DISTINCT.cicd_user_id))
                        .build(), Map.of("trend", SortingOrder.ASC),
                VALUES_ONLY,
                null
        );

        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(9, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(9, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(9, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("1593500400").count(1L).stacks(
                        List.of(DbAggregationResult.builder().key("user-jenkins-8").count(1L).build())
                ).build(),
                DbAggregationResult.builder().key("1593586800").count(2L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("user-jenkins-8").count(1L).build(),
                                DbAggregationResult.builder().key("user-jenkins-7").count(1L).build()
                        )).build(),
                DbAggregationResult.builder().key("1593673200").count(3L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("user-jenkins-8").count(1L).build(),
                                DbAggregationResult.builder().key("user-jenkins-7").count(1L).build(),
                                DbAggregationResult.builder().key("user-jenkins-6").count(1L).build()
                        )).build(),
                DbAggregationResult.builder().key("1593759600").count(4L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("user-jenkins-8").count(1L).build(),
                                DbAggregationResult.builder().key("user-jenkins-7").count(1L).build(),
                                DbAggregationResult.builder().key("user-jenkins-6").count(1L).build(),
                                DbAggregationResult.builder().key("user-jenkins-5").count(1L).build()
                        )).build(),
                DbAggregationResult.builder().key("1593846000").count(5L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("user-jenkins-8").count(1L).build(),
                                DbAggregationResult.builder().key("user-jenkins-7").count(1L).build(),
                                DbAggregationResult.builder().key("user-jenkins-6").count(1L).build(),
                                DbAggregationResult.builder().key("user-jenkins-5").count(1L).build(),
                                DbAggregationResult.builder().key("user-jenkins-4").count(1L).build()
                        )).build(),
                DbAggregationResult.builder().key("1594450800").count(6L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("user-jenkins-8").count(1L).build(),
                                DbAggregationResult.builder().key("user-jenkins-7").count(1L).build(),
                                DbAggregationResult.builder().key("user-jenkins-6").count(1L).build(),
                                DbAggregationResult.builder().key("user-jenkins-5").count(1L).build(),
                                DbAggregationResult.builder().key("user-jenkins-4").count(1L).build(),
                                DbAggregationResult.builder().key("user-jenkins-3").count(1L).build()
                        )).build(),
                DbAggregationResult.builder().key("1594537200").count(7L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("user-jenkins-8").count(1L).build(),
                                DbAggregationResult.builder().key("user-jenkins-7").count(1L).build(),
                                DbAggregationResult.builder().key("user-jenkins-6").count(1L).build(),
                                DbAggregationResult.builder().key("user-jenkins-5").count(1L).build(),
                                DbAggregationResult.builder().key("user-jenkins-4").count(1L).build(),
                                DbAggregationResult.builder().key("user-jenkins-3").count(1L).build(),
                                DbAggregationResult.builder().key("user-jenkins-2").count(1L).build()
                        )).build(),
                DbAggregationResult.builder().key("1594623600").count(8L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("user-jenkins-8").count(1L).build(),
                                DbAggregationResult.builder().key("user-jenkins-7").count(1L).build(),
                                DbAggregationResult.builder().key("user-jenkins-6").count(1L).build(),
                                DbAggregationResult.builder().key("user-jenkins-5").count(1L).build(),
                                DbAggregationResult.builder().key("user-jenkins-4").count(1L).build(),
                                DbAggregationResult.builder().key("user-jenkins-3").count(1L).build(),
                                DbAggregationResult.builder().key("user-jenkins-2").count(1L).build(),
                                DbAggregationResult.builder().key("user-jenkins-1").count(1L).build()
                        )).build(),
                DbAggregationResult.builder().key("1594710000").count(9L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("user-jenkins-8").count(1L).build(),
                                DbAggregationResult.builder().key("user-jenkins-7").count(1L).build(),
                                DbAggregationResult.builder().key("user-jenkins-6").count(1L).build(),
                                DbAggregationResult.builder().key("user-jenkins-5").count(1L).build(),
                                DbAggregationResult.builder().key("user-jenkins-4").count(1L).build(),
                                DbAggregationResult.builder().key("user-jenkins-3").count(1L).build(),
                                DbAggregationResult.builder().key("user-jenkins-2").count(1L).build(),
                                DbAggregationResult.builder().key("user-jenkins-1").count(1L).build(),
                                DbAggregationResult.builder().key("user-jenkins-0").count(1L).build()
                        )).build()
        );
        Assert.assertEquals(expected.stream().map(DbAggregationResult::getCount).collect(Collectors.toSet()), dbAggsResponse.getRecords().stream().map(DbAggregationResult::getCount).collect(Collectors.toSet()));
        verifyRecords(dbAggsResponse.getRecords(), expected, true);
        // endregion

        // region job name - no filters - stack cicd user id
        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobConfigChanges(company,
                CiCdJobConfigChangesFilter.builder().across(CiCdJobConfigChangesFilter.DISTINCT.job_name)
                        .calculation(CiCdJobConfigChangesFilter.CALCULATION.count)
                        .stacks(List.of(CiCdJobConfigChangesFilter.DISTINCT.cicd_user_id))
                        .build(),
                VALUES_ONLY,
                null
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(3, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(3, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(3, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("jobname-2").count(18L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("user-jenkins-8").count(9L).build(),
                                DbAggregationResult.builder().key("user-jenkins-5").count(6L).build(),
                                DbAggregationResult.builder().key("user-jenkins-2").count(3L).build()
                        )).build(),
                DbAggregationResult.builder().key("jobname-1").count(15L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("user-jenkins-7").count(8L).build(),
                                DbAggregationResult.builder().key("user-jenkins-4").count(5L).build(),
                                DbAggregationResult.builder().key("user-jenkins-1").count(2L).build()
                        )).build(),
                DbAggregationResult.builder().key("jobname-0").count(12L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("user-jenkins-6").count(7L).build(),
                                DbAggregationResult.builder().key("user-jenkins-3").count(4L).build(),
                                DbAggregationResult.builder().key("user-jenkins-0").count(1L).build()
                        )).build()
        );
        Assert.assertEquals(expected, dbAggsResponse.getRecords());
        // endregion

        // region qualified job name - no filters - stack cicd user id
        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobConfigChanges(company,
                CiCdJobConfigChangesFilter.builder().across(CiCdJobConfigChangesFilter.DISTINCT.qualified_job_name)
                        .calculation(CiCdJobConfigChangesFilter.CALCULATION.count)
                        .stacks(List.of(CiCdJobConfigChangesFilter.DISTINCT.cicd_user_id))
                        .build(),
                VALUES_ONLY,
                null
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(9, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(9, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(9, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("jobname-2").additionalKey("instance-name-1").count(9L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("user-jenkins-8").count(9l).build()
                        )).build(),
                DbAggregationResult.builder().key("jobname-1").additionalKey("instance-name-1").count(8L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("user-jenkins-7").count(8l).build()
                        )).build(),
                DbAggregationResult.builder().key("jobname-0").additionalKey("instance-name-1").count(7L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("user-jenkins-6").count(7l).build()
                        )).build(),
                DbAggregationResult.builder().key("jobname-2").additionalKey("instance-name-0").count(6L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("user-jenkins-5").count(6l).build()
                        )).build(),
                DbAggregationResult.builder().key("jobname-1").additionalKey("instance-name-0").count(5L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("user-jenkins-4").count(5l).build()
                        )).build(),
                DbAggregationResult.builder().key("jobname-0").additionalKey("instance-name-0").count(4L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("user-jenkins-3").count(4l).build()
                        )).build(),
                DbAggregationResult.builder().key("jobname-2").additionalKey(null).count(3L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("user-jenkins-2").count(3l).build()
                        )).build(),
                DbAggregationResult.builder().key("jobname-1").additionalKey(null).count(2L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("user-jenkins-1").count(2l).build()
                        )).build(),
                DbAggregationResult.builder().key("jobname-0").additionalKey(null).count(1L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("user-jenkins-0").count(1l).build()
                        )).build()
        );
        Assert.assertEquals(expected, dbAggsResponse.getRecords());
        // endregion

        // region cicd user id - no filters - stack job name
        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobConfigChanges(company,
                CiCdJobConfigChangesFilter.builder().across(CiCdJobConfigChangesFilter.DISTINCT.cicd_user_id)
                        .calculation(CiCdJobConfigChangesFilter.CALCULATION.count)
                        .stacks(List.of(CiCdJobConfigChangesFilter.DISTINCT.job_name))
                        .build(),
                VALUES_ONLY,
                null
        );

        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(9, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(9, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(9, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("user-jenkins-8").count(9L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("jobname-2").count(9L).build()
                        )).build(),
                DbAggregationResult.builder().key("user-jenkins-7").count(8L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("jobname-1").count(8L).build()
                        )).build(),
                DbAggregationResult.builder().key("user-jenkins-6").count(7L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("jobname-0").count(7L).build()
                        )).build(),
                DbAggregationResult.builder().key("user-jenkins-5").count(6L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("jobname-2").count(6L).build()
                        )).build(),
                DbAggregationResult.builder().key("user-jenkins-4").count(5L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("jobname-1").count(5L).build()
                        )).build(),
                DbAggregationResult.builder().key("user-jenkins-3").count(4L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("jobname-0").count(4L).build()
                        )).build(),
                DbAggregationResult.builder().key("user-jenkins-2").count(3L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("jobname-2").count(3L).build()
                        )).build(),
                DbAggregationResult.builder().key("user-jenkins-1").count(2L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("jobname-1").count(2L).build()
                        )).build(),
                DbAggregationResult.builder().key("user-jenkins-0").count(1L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("jobname-0").count(1L).build()
                        )).build()
        );
        Assert.assertEquals(expected, dbAggsResponse.getRecords());
        // endregion
        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobConfigChanges(company,
                CiCdJobConfigChangesFilter.builder().across(CiCdJobConfigChangesFilter.DISTINCT.instance_name)
                        .calculation(CiCdJobConfigChangesFilter.CALCULATION.count)
                        .stacks(List.of(CiCdJobConfigChangesFilter.DISTINCT.job_name))
                        .build(),
                VALUES_ONLY,
                null);
        assertThat(dbAggsResponse.getCount()).isEqualTo(2);

        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder().across(CiCdJobRunsFilter.DISTINCT.instance_name)
                        .calculation(CiCdJobRunsFilter.CALCULATION.count)
                        .stacks(List.of(CiCdJobRunsFilter.DISTINCT.job_status))
                        .build(),
                VALUES_ONLY);

        assertThat(dbAggsResponse.getCount()).isEqualTo(0);

        // region list - no filters - page size
        DbListResponse<CICDJobConfigChangeDTO> dbListResponse = ciCdAggsService.listCiCdJobConfigChanges(company,
                CiCdJobConfigChangesFilter.builder().build(), null, PAGE_NUMBER, 5);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(5, dbListResponse.getCount().intValue());
        Assert.assertEquals(45, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(5, dbListResponse.getRecords().size());
        // endregion

        // region list - no filters
        dbListResponse = ciCdAggsService.listCiCdJobConfigChanges(company,
                CiCdJobConfigChangesFilter.builder().build(), null, PAGE_NUMBER, PAGE_SIZE);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(45, dbListResponse.getCount().intValue());
        Assert.assertEquals(45, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(45, dbListResponse.getRecords().size());

        CICDJobConfigChangeDTO jobConfigChangeDTO = dbListResponse.getRecords().get(0);
        Assert.assertNotNull(jobConfigChangeDTO.getId());
        //Assert.assertEquals(jobConfigChangeDTO.getCicdJobId(), cicdJobs.get(0).getId());
        Assert.assertEquals(jobConfigChangeDTO.getJobName(), "jobname-2");
        Assert.assertEquals(jobConfigChangeDTO.getChangeType(), "changed");
        Assert.assertEquals(jobConfigChangeDTO.getCicdUserId(), "user-jenkins-8");
        // endregion

        // region list - job name
        dbListResponse = ciCdAggsService.listCiCdJobConfigChanges(company,
                CiCdJobConfigChangesFilter.builder().jobNames(List.of("jobname-0")).build(), null, PAGE_NUMBER, PAGE_SIZE);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(12, dbListResponse.getCount().intValue());
        Assert.assertEquals(12, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(12, dbListResponse.getRecords().size());
        // endregion

        // region list - qualified job name
        dbListResponse = ciCdAggsService.listCiCdJobConfigChanges(company,
                CiCdJobConfigChangesFilter.builder().qualifiedJobNames(
                        List.of(CiCdJobQualifiedName.builder().instanceName("instance-name-1").jobName("jobname-1").build(),
                                CiCdJobQualifiedName.builder().instanceName(null).jobName("jobname-2").build())
                ).build(), null, PAGE_NUMBER, PAGE_SIZE);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(11, dbListResponse.getCount().intValue());
        Assert.assertEquals(11, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(11, dbListResponse.getRecords().size());
        // endregion

        // region list - instance names
        dbListResponse = ciCdAggsService.listCiCdJobConfigChanges(company,
                CiCdJobConfigChangesFilter.builder().instanceNames(List.of("instance-name-1")).build(), null, PAGE_NUMBER, PAGE_SIZE);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(24, dbListResponse.getCount().intValue());
        Assert.assertEquals(24, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(24, dbListResponse.getRecords().size());
        // endregion

        // region list - cicd user id
        dbListResponse = ciCdAggsService.listCiCdJobConfigChanges(company,
                CiCdJobConfigChangesFilter.builder().cicdUserIds(List.of("user-jenkins-0")).build(), null, PAGE_NUMBER, PAGE_SIZE);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(1, dbListResponse.getCount().intValue());
        Assert.assertEquals(1, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(1, dbListResponse.getRecords().size());
        // endregion

        // region list - job name + cicd user id
        dbListResponse = ciCdAggsService.listCiCdJobConfigChanges(company,
                CiCdJobConfigChangesFilter.builder().jobNames(List.of("jobname-0"))
                        .qualifiedJobNames(List.of(CiCdJobQualifiedName.builder().instanceName("instance-name-1").jobName("jobname-0").build()))
                        .cicdUserIds(List.of("user-jenkins-6")).build(), null, PAGE_NUMBER, PAGE_SIZE);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(7, dbListResponse.getCount().intValue());
        Assert.assertEquals(7, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(7, dbListResponse.getRecords().size());
        // endregion
    }

    @Test
    public void testListCICDJobRunsForDora() throws SQLException, IOException {
        setupGitlabPipelineData();
        DbListResponse<CICDJob> jobs = ciCdJobsDatabaseService.listByFilter(company1, 0, 20, null, null, null, null, null);
        List<String> jobIds = CollectionUtils.emptyIfNull(jobs.getRecords()).stream().map(job -> job.getId().toString()).collect(Collectors.toList());

        DbListResponse<CICDJobRunDTO> jobRunsWithFilter = ciCdAggsService.listCiCdJobRunsForDora(company1,
                CiCdJobRunsFilter.builder()
                        .calculation(CiCdJobRunsFilter.CALCULATION.count)
                        .build(),
                0,
                20,
                null,
                jobIds);

        DbListResponse<CICDJobRunDTO> jobRunsWithoutFilter = ciCdAggsService.listCiCdJobRunsForDora(company1,
                CiCdJobRunsFilter.builder()
                        .calculation(CiCdJobRunsFilter.CALCULATION.count)
                        .build(),
                0,
                20,
                null,
                null);

        Assert.assertEquals(jobRunsWithFilter.getTotalCount(), jobRunsWithoutFilter.getTotalCount());
        Assert.assertEquals(jobRunsWithFilter.getRecords(), jobRunsWithoutFilter.getRecords());
    }

    @Test
    public void aggIntervalTest() throws SQLException {
        String jobid = ciCdJobsDatabaseService.insert(company, CICDJob.builder()
                .jobName("test")
                .jobNormalizedFullName("test-job")
                .jobFullName("test")
                .scmUrl("test/url")
                .build());
        List<CICDJobRun> jobRuns = List.of(CICDJobRun.builder()
                        .scmCommitIds(List.of("commit-1"))
                        .jobRunNumber(1L)
                        .duration(10)
                        .cicdJobId(UUID.fromString(jobid))
                        .startTime(Instant.ofEpochSecond(1611266461))
                        .endTime(Instant.ofEpochSecond(1615266461))
                        .build(),
                CICDJobRun.builder()
                        .scmCommitIds(List.of("commit-2"))
                        .jobRunNumber(2L)
                        .duration(10)
                        .cicdJobId(UUID.fromString(jobid))
                        .startTime(Instant.ofEpochSecond(1611066461))
                        .endTime(Instant.ofEpochSecond(1612165008))
                        .build());
        for (var runs : jobRuns) {
            ciCdJobRunsDatabaseService.insert(company, runs);
        }
        DbListResponse<DbAggregationResult> dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder().across(CiCdJobRunsFilter.DISTINCT.job_end).calculation(CiCdJobRunsFilter.CALCULATION.duration).build(),
                VALUES_ONLY
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(2, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getRecords().size());
        List<DbAggregationResult> expected = List.of(
                DbAggregationResult.builder().key("1612080000").additionalKey("31-1-2021").count(1L).median(10L).min(10L).max(10L).sum(10L).build(),
                DbAggregationResult.builder().key("1615190400").additionalKey("8-3-2021").count(1L).median(10L).min(10L).max(10L).sum(10L).build()
        );
        Assert.assertEquals(expected, dbAggsResponse.getRecords());

        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder().across(CiCdJobRunsFilter.DISTINCT.job_end).aggInterval(CICD_AGG_INTERVAL.week).calculation(CiCdJobRunsFilter.CALCULATION.count).build(),
                VALUES_ONLY
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(2, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("1611561600").additionalKey("4-2021").count(1L).build(),
                DbAggregationResult.builder().key("1615190400").additionalKey("10-2021").count(1L).build()
        );
        Assert.assertEquals(expected, dbAggsResponse.getRecords());

        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder().across(CiCdJobRunsFilter.DISTINCT.job_end).aggInterval(CICD_AGG_INTERVAL.month).calculation(CiCdJobRunsFilter.CALCULATION.count).build(),
                VALUES_ONLY
        );
        DefaultObjectMapper.prettyPrint(dbAggsResponse);
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(2, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("1614585600").additionalKey("3-2021").count(1L).build(),
                DbAggregationResult.builder().key("1609488000").additionalKey("1-2021").count(1L).build()
        );
        Assert.assertEquals(expected, dbAggsResponse.getRecords());

        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder().across(CiCdJobRunsFilter.DISTINCT.job_end).aggInterval(CICD_AGG_INTERVAL.quarter).calculation(CiCdJobRunsFilter.CALCULATION.count).build(),
                VALUES_ONLY
        );
        DefaultObjectMapper.prettyPrint(dbAggsResponse);
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(1, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("1609488000").additionalKey("Q1-2021").count(2L).build()
        );
        Assert.assertEquals(expected, dbAggsResponse.getRecords());

        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder().across(CiCdJobRunsFilter.DISTINCT.job_end).aggInterval(CICD_AGG_INTERVAL.year).calculation(CiCdJobRunsFilter.CALCULATION.count).build(),
                VALUES_ONLY
        );
        DefaultObjectMapper.prettyPrint(dbAggsResponse);
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(1, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("1609488000").additionalKey("2021").count(2L).build()
        );
        Assert.assertEquals(expected, dbAggsResponse.getRecords());

        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder().across(CiCdJobRunsFilter.DISTINCT.job_end)
                        .jobNormalizedFullNames(List.of("test-job")).calculation(CiCdJobRunsFilter.CALCULATION.duration).build(),
                VALUES_ONLY
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(2, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getRecords().size());

        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder().across(CiCdJobRunsFilter.DISTINCT.job_end)
                        .jobNormalizedFullNames(List.of("test-job")).calculation(CiCdJobRunsFilter.CALCULATION.count).build(),
                VALUES_ONLY
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(2, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getRecords().size());

    }

    @Test
    public void testOptimizedTableCalls() throws SQLException {
        Integration integration1 = Integration.builder()
                .id("2")
                .name("name2")
                .url("http")
                .status("good")
                .application("jira")
                .description("desc")
                .satellite(true)
                .build();
        try {
            integrationService.insert(company, integration1);
        } catch (DuplicateKeyException e) {
            // nothing
        }
        CICDInstance.CICDInstanceBuilder bldr = CICDInstance.builder()
                .id(UUID.randomUUID())
                .name("instance-name-1")
                .url("https://jenkins.dev.levelops.io/")
                .integrationId("1")
                .type(CICD_TYPE.azure_devops.toString());
        CICDInstance cicdInstance = bldr.build();
        String id = ciCdInstancesDatabaseService.insert(company, cicdInstance);
        String jobid = ciCdJobsDatabaseService.insert(company, CICDJob.builder()
                .jobName("test")
                .jobNormalizedFullName("test-job")
                .jobFullName("test")
                .scmUrl("test/url")
                .projectName("proj1")
                .cicdInstanceId(UUID.fromString(id))
                .build());
        String jobid1 = ciCdJobsDatabaseService.insert(company, CICDJob.builder()
                .jobName("test1")
                .jobNormalizedFullName("test-job1")
                .jobFullName("test1")
                .scmUrl("test1/url")
                .projectName("proj2")
                .cicdInstanceId(UUID.fromString(id))
                .build());
        List<CICDJobRun> jobRuns =
                List.of(
                        CICDJobRun.builder()
                                .scmCommitIds(List.of("commit-1"))
                                .jobRunNumber(1L)
                                .duration(10)
                                .cicdUserId("0")
                                .cicdJobId(UUID.fromString(jobid))
                                .status("PENDING")
                                .startTime(Instant.ofEpochSecond(1611266461))
                                .endTime(Instant.ofEpochSecond(1615266461))
                                .build(),
                        CICDJobRun.builder()
                                .scmCommitIds(List.of("commit-2"))
                                .jobRunNumber(2L)
                                .duration(10)
                                .cicdUserId("1")
                                .status("DONE")
                                .cicdJobId(UUID.fromString(jobid))
                                .startTime(Instant.ofEpochSecond(1611066461))
                                .endTime(Instant.ofEpochSecond(1612165008))
                                .build(),
                        CICDJobRun.builder()
                                .scmCommitIds(List.of("commit-3"))
                                .jobRunNumber(3L)
                                .duration(10)
                                .status("DONE")
                                .cicdUserId("1")
                                .cicdJobId(UUID.fromString(jobid1))
                                .startTime(Instant.ofEpochSecond(1611066461))
                                .endTime(Instant.ofEpochSecond(1612165008))
                                .params(List.of(CICDJobRun.JobRunParam.builder().type("StringParameterValue")
                                        .name("BRANCH_NAME").value("master").build()))
                                .build());
        for (var runs : jobRuns) {
            ciCdJobRunsDatabaseService.insert(company, runs);
        }
        DbListResponse<DbAggregationResult> dbAggregationResultDbListResponse = ciCdAggsService
                .groupByAndCalculateCiCdJobRuns(company, CiCdJobRunsFilter.builder()
                        .calculation(CiCdJobRunsFilter.CALCULATION.count)
                        .across(CiCdJobRunsFilter.DISTINCT.job_status)
                        .cicdUserIds(List.of("1")).build(), VALUES_ONLY);
        Assert.assertEquals(2, dbAggregationResultDbListResponse.getRecords().get(0).getCount().intValue());
        dbAggregationResultDbListResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company, CiCdJobRunsFilter.builder()
                .calculation(CiCdJobRunsFilter.CALCULATION.count)
                .across(CiCdJobRunsFilter.DISTINCT.job_status)
                .integrationIds(List.of("1")).build(), VALUES_ONLY);
        Assert.assertEquals(2, dbAggregationResultDbListResponse.getRecords().get(0).getCount().intValue());
        dbAggregationResultDbListResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company, CiCdJobRunsFilter.builder()
                .calculation(CiCdJobRunsFilter.CALCULATION.count)
                .across(CiCdJobRunsFilter.DISTINCT.job_status)
                .jobNames(List.of("test1")).build(), VALUES_ONLY);
        Assert.assertEquals(1, dbAggregationResultDbListResponse.getRecords().get(0).getCount().intValue());
        dbAggregationResultDbListResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company, CiCdJobRunsFilter.builder()
                .calculation(CiCdJobRunsFilter.CALCULATION.count)
                .across(CiCdJobRunsFilter.DISTINCT.job_status)
                .jobNormalizedFullNames(List.of("test-job1")).build(), VALUES_ONLY);
        Assert.assertEquals(1, dbAggregationResultDbListResponse.getRecords().get(0).getCount().intValue());
        dbAggregationResultDbListResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company, CiCdJobRunsFilter.builder()
                .calculation(CiCdJobRunsFilter.CALCULATION.count)
                .across(CiCdJobRunsFilter.DISTINCT.job_status)
                .jobStatuses(List.of("DONE")).build(), VALUES_ONLY);
        Assert.assertEquals(2, dbAggregationResultDbListResponse.getRecords().get(0).getCount().intValue());
        dbAggregationResultDbListResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company, CiCdJobRunsFilter.builder()
                .calculation(CiCdJobRunsFilter.CALCULATION.count)
                .across(CiCdJobRunsFilter.DISTINCT.job_status)
                .types(List.of(CICD_TYPE.azure_devops)).build(), VALUES_ONLY);
        Assert.assertEquals(2, dbAggregationResultDbListResponse.getRecords().get(0).getCount().intValue());
        dbAggregationResultDbListResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company, CiCdJobRunsFilter.builder()
                .calculation(CiCdJobRunsFilter.CALCULATION.count)
                .across(CiCdJobRunsFilter.DISTINCT.job_status)
                .parameters(List.of(CiCdJobRunParameter.builder().name("BRANCH_NAME")
                        .values(List.of("master")).build())).build(), VALUES_ONLY);
        Assert.assertEquals(1, dbAggregationResultDbListResponse.getRecords().get(0).getCount().intValue());
        dbAggregationResultDbListResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company, CiCdJobRunsFilter.builder()
                .calculation(CiCdJobRunsFilter.CALCULATION.count)
                .across(CiCdJobRunsFilter.DISTINCT.job_status)
                .partialMatch(Map.of("job_normalized_full_name", Map.of("$begins", "test"))).build(), VALUES_ONLY);
        Assert.assertEquals(2, dbAggregationResultDbListResponse.getRecords().get(0).getCount().intValue());
        dbAggregationResultDbListResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company, CiCdJobRunsFilter.builder()
                .calculation(CiCdJobRunsFilter.CALCULATION.count)
                .across(CiCdJobRunsFilter.DISTINCT.job_status)
                .partialMatch(Map.of("job_normalized_full_name", Map.of("$ends", "job1"))).build(), VALUES_ONLY);
        Assert.assertEquals(1, dbAggregationResultDbListResponse.getRecords().get(0).getCount().intValue());
        dbAggregationResultDbListResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company, CiCdJobRunsFilter.builder()
                .calculation(CiCdJobRunsFilter.CALCULATION.count)
                .across(CiCdJobRunsFilter.DISTINCT.job_status)
                .jobStatuses(List.of("DONE"))
                .partialMatch(Map.of("job_normalized_full_name", Map.of("$ends", "job1' OR 'A'='A';--"))).build(), VALUES_ONLY);
        DefaultObjectMapper.prettyPrint(dbAggregationResultDbListResponse);
        Assert.assertEquals(0, dbAggregationResultDbListResponse.getRecords().size());
        dbAggregationResultDbListResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company, CiCdJobRunsFilter.builder()
                .calculation(CiCdJobRunsFilter.CALCULATION.count)
                .across(CiCdJobRunsFilter.DISTINCT.job_status)
                .projects(List.of("proj1")).build(), VALUES_ONLY);
        Assert.assertEquals(1, dbAggregationResultDbListResponse.getRecords().get(0).getCount().intValue());
    }

    @Test
    public void testJoinTableUsingFilterAndAcross() throws SQLException {
        Integration integration1 = Integration.builder()
                .id("2")
                .name("name2")
                .url("http")
                .status("good")
                .application("jira")
                .description("desc")
                .satellite(true)
                .build();
        integrationService.insert(company, integration1);

        CICDInstance.CICDInstanceBuilder bldr = CICDInstance.builder()
                .id(UUID.randomUUID())
                .name("instance-name-1")
                .url("https://jenkins.dev.levelops.io/")
                .integrationId("1")
                .type(CICD_TYPE.azure_devops.toString());
        CICDInstance cicdInstance = bldr.build();
        String id = ciCdInstancesDatabaseService.insert(company, cicdInstance);
        String jobid = ciCdJobsDatabaseService.insert(company, CICDJob.builder()
                .jobName("test")
                .jobNormalizedFullName("test-job")
                .jobFullName("test")
                .scmUrl("test/url")
                .projectName("proj1")
                .cicdInstanceId(UUID.fromString(id))
                .build());
        String jobid1 = ciCdJobsDatabaseService.insert(company, CICDJob.builder()
                .jobName("test1")
                .jobNormalizedFullName("test-job1")
                .jobFullName("test1")
                .scmUrl("test1/url")
                .projectName("proj2")
                .cicdInstanceId(UUID.fromString(id))
                .build());
        List<CICDJobRun> jobRuns =
                List.of(
                        CICDJobRun.builder()
                                .scmCommitIds(List.of("commit-1"))
                                .jobRunNumber(1L)
                                .duration(10)
                                .cicdUserId("0")
                                .cicdJobId(UUID.fromString(jobid))
                                .status("PENDING")
                                .startTime(Instant.ofEpochSecond(1611266461))
                                .endTime(Instant.ofEpochSecond(1615266461))
                                .build(),
                        CICDJobRun.builder()
                                .scmCommitIds(List.of("commit-2"))
                                .jobRunNumber(2L)
                                .duration(10)
                                .cicdUserId("1")
                                .status("DONE")
                                .cicdJobId(UUID.fromString(jobid))
                                .startTime(Instant.ofEpochSecond(1611066461))
                                .endTime(Instant.ofEpochSecond(1612165008))
                                .build(),
                        CICDJobRun.builder()
                                .scmCommitIds(List.of("commit-3"))
                                .jobRunNumber(3L)
                                .duration(10)
                                .status("DONE")
                                .cicdUserId("1")
                                .cicdJobId(UUID.fromString(jobid1))
                                .startTime(Instant.ofEpochSecond(1611066461))
                                .endTime(Instant.ofEpochSecond(1612165008))
                                .params(List.of(CICDJobRun.JobRunParam.builder().type("StringParameterValue")
                                        .name("BRANCH_NAME").value("master").build()))
                                .build());
        for (var runs : jobRuns) {
            ciCdJobRunsDatabaseService.insert(company, runs);
        }
        DbListResponse<DbAggregationResult> dbAggregationResultDbListResponse = ciCdAggsService
                .groupByAndCalculateCiCdJobRuns(company, CiCdJobRunsFilter.builder()
                        .calculation(CiCdJobRunsFilter.CALCULATION.count)
                        .across(CiCdJobRunsFilter.DISTINCT.job_status)
                        .cicdUserIds(List.of("1")).build(), VALUES_ONLY);
        Assert.assertEquals(2, dbAggregationResultDbListResponse.getRecords().get(0).getCount().intValue());
        dbAggregationResultDbListResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company, CiCdJobRunsFilter.builder()
                .calculation(CiCdJobRunsFilter.CALCULATION.count)
                .across(CiCdJobRunsFilter.DISTINCT.job_status)
                .integrationIds(List.of("1")).build(), VALUES_ONLY);
        Assert.assertEquals(2, dbAggregationResultDbListResponse.getRecords().get(0).getCount().intValue());
        dbAggregationResultDbListResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company, CiCdJobRunsFilter.builder()
                .calculation(CiCdJobRunsFilter.CALCULATION.count)
                .across(CiCdJobRunsFilter.DISTINCT.job_status)
                .jobNames(List.of("test1")).build(), VALUES_ONLY);
        Assert.assertEquals(1, dbAggregationResultDbListResponse.getRecords().get(0).getCount().intValue());
        dbAggregationResultDbListResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company, CiCdJobRunsFilter.builder()
                .calculation(CiCdJobRunsFilter.CALCULATION.count)
                .across(CiCdJobRunsFilter.DISTINCT.job_status)
                .jobNormalizedFullNames(List.of("test-job1")).build(), VALUES_ONLY);
        Assert.assertEquals(1, dbAggregationResultDbListResponse.getRecords().get(0).getCount().intValue());
        dbAggregationResultDbListResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company, CiCdJobRunsFilter.builder()
                .calculation(CiCdJobRunsFilter.CALCULATION.count)
                .across(CiCdJobRunsFilter.DISTINCT.job_status)
                .jobStatuses(List.of("DONE")).build(), VALUES_ONLY);
        Assert.assertEquals(2, dbAggregationResultDbListResponse.getRecords().get(0).getCount().intValue());
        dbAggregationResultDbListResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company, CiCdJobRunsFilter.builder()
                .calculation(CiCdJobRunsFilter.CALCULATION.count)
                .across(CiCdJobRunsFilter.DISTINCT.job_status)
                .types(List.of(CICD_TYPE.azure_devops)).build(), VALUES_ONLY);
        Assert.assertEquals(2, dbAggregationResultDbListResponse.getRecords().get(0).getCount().intValue());
        dbAggregationResultDbListResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company, CiCdJobRunsFilter.builder()
                .calculation(CiCdJobRunsFilter.CALCULATION.count)
                .across(CiCdJobRunsFilter.DISTINCT.job_status)
                .parameters(List.of(CiCdJobRunParameter.builder().name("BRANCH_NAME")
                        .values(List.of("master")).build())).build(), VALUES_ONLY);
        Assert.assertEquals(1, dbAggregationResultDbListResponse.getRecords().get(0).getCount().intValue());
        dbAggregationResultDbListResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company, CiCdJobRunsFilter.builder()
                .calculation(CiCdJobRunsFilter.CALCULATION.count)
                .across(CiCdJobRunsFilter.DISTINCT.job_status)
                .partialMatch(Map.of("job_normalized_full_name", Map.of("$begins", "test"))).build(), VALUES_ONLY);
        Assert.assertEquals(2, dbAggregationResultDbListResponse.getRecords().get(0).getCount().intValue());
        dbAggregationResultDbListResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company, CiCdJobRunsFilter.builder()
                .calculation(CiCdJobRunsFilter.CALCULATION.count)
                .across(CiCdJobRunsFilter.DISTINCT.job_status)
                .partialMatch(Map.of("random_text", Map.of("$begins", "test"))).build(), VALUES_ONLY);
        Assert.assertEquals(2, dbAggregationResultDbListResponse.getRecords().get(0).getCount().intValue());
        dbAggregationResultDbListResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company, CiCdJobRunsFilter.builder()
                .calculation(CiCdJobRunsFilter.CALCULATION.count)
                .across(CiCdJobRunsFilter.DISTINCT.job_status)
                .partialMatch(Map.of("job_normalized_full_name", Map.of("$ends", "job1"))).build(), VALUES_ONLY);
        Assert.assertEquals(1, dbAggregationResultDbListResponse.getRecords().get(0).getCount().intValue());
        dbAggregationResultDbListResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company, CiCdJobRunsFilter.builder()
                .calculation(CiCdJobRunsFilter.CALCULATION.count)
                .across(CiCdJobRunsFilter.DISTINCT.job_status)
                .projects(List.of("proj1")).build(), VALUES_ONLY);
        Assert.assertEquals(1, dbAggregationResultDbListResponse.getRecords().get(0).getCount().intValue());
        dbAggregationResultDbListResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company, CiCdJobRunsFilter.builder()
                .calculation(CiCdJobRunsFilter.CALCULATION.count)
                .across(CiCdJobRunsFilter.DISTINCT.job_status)
                .instanceNames(List.of("instance-name-1")).build(), VALUES_ONLY);
        Assert.assertEquals(2, dbAggregationResultDbListResponse.getRecords().get(0).getCount().intValue());
        dbAggregationResultDbListResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company, CiCdJobRunsFilter.builder()
                .calculation(CiCdJobRunsFilter.CALCULATION.count)
                .across(CiCdJobRunsFilter.DISTINCT.job_status)
                .qualifiedJobNames(List.of(CiCdJobQualifiedName.builder().jobName("test1")
                        .instanceName("instance-name-1").build()))
                .build(), VALUES_ONLY);
        Assert.assertEquals(1, dbAggregationResultDbListResponse.getRecords().get(0).getCount().intValue());
        dbAggregationResultDbListResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company, CiCdJobRunsFilter.builder()
                .calculation(CiCdJobRunsFilter.CALCULATION.count)
                .across(CiCdJobRunsFilter.DISTINCT.instance_name)
                .types(List.of(CICD_TYPE.azure_devops))
                .qualifiedJobNames(List.of(CiCdJobQualifiedName.builder().jobName("test1")
                        .instanceName("instance-name-1").build()))
                .build(), VALUES_ONLY);
        Assert.assertEquals(1, dbAggregationResultDbListResponse.getRecords().get(0).getCount().intValue());
        dbAggregationResultDbListResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company, CiCdJobRunsFilter.builder()
                .calculation(CiCdJobRunsFilter.CALCULATION.count)
                .across(CiCdJobRunsFilter.DISTINCT.job_name)
                .build(), VALUES_ONLY);
        Assert.assertEquals(2, dbAggregationResultDbListResponse.getTotalCount().intValue());
        dbAggregationResultDbListResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company, CiCdJobRunsFilter.builder()
                .calculation(CiCdJobRunsFilter.CALCULATION.count)
                .across(CiCdJobRunsFilter.DISTINCT.cicd_user_id)
                .build(), VALUES_ONLY);
        Assert.assertEquals(2, dbAggregationResultDbListResponse.getTotalCount().intValue());
        dbAggregationResultDbListResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company, CiCdJobRunsFilter.builder()
                .calculation(CiCdJobRunsFilter.CALCULATION.count)
                .across(CiCdJobRunsFilter.DISTINCT.instance_name)
                .build(), VALUES_ONLY);
        Assert.assertEquals(1, dbAggregationResultDbListResponse.getTotalCount().intValue());
        dbAggregationResultDbListResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company, CiCdJobRunsFilter.builder()
                .calculation(CiCdJobRunsFilter.CALCULATION.count)
                .across(CiCdJobRunsFilter.DISTINCT.job_normalized_full_name)
                .build(), VALUES_ONLY);
        Assert.assertEquals(2, dbAggregationResultDbListResponse.getTotalCount().intValue());
        // TODO this test is time sensitive - need to fix
//        dbAggregationResultDbListResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company, CiCdJobRunsFilter.builder()
//                .calculation(CiCdJobRunsFilter.CALCULATION.count)
//                .across(CiCdJobRunsFilter.DISTINCT.trend)
//                .build(), VALUES_ONLY);
//        Assert.assertEquals(1, dbAggregationResultDbListResponse.getTotalCount().intValue());
        dbAggregationResultDbListResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company, CiCdJobRunsFilter.builder()
                .calculation(CiCdJobRunsFilter.CALCULATION.count)
                .across(CiCdJobRunsFilter.DISTINCT.job_end)
                .build(), VALUES_ONLY);
        Assert.assertEquals(2, dbAggregationResultDbListResponse.getTotalCount().intValue());
        dbAggregationResultDbListResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company, CiCdJobRunsFilter.builder()
                .calculation(CiCdJobRunsFilter.CALCULATION.count)
                .across(CiCdJobRunsFilter.DISTINCT.project_name)
                .build(), VALUES_ONLY);
        Assert.assertEquals(2, dbAggregationResultDbListResponse.getTotalCount().intValue());
        dbAggregationResultDbListResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company, CiCdJobRunsFilter.builder()
                .calculation(CiCdJobRunsFilter.CALCULATION.count)
                .across(CiCdJobRunsFilter.DISTINCT.qualified_job_name)
                .build(), VALUES_ONLY);
        Assert.assertEquals(2, dbAggregationResultDbListResponse.getTotalCount().intValue());
        dbAggregationResultDbListResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company, CiCdJobRunsFilter.builder()
                .calculation(CiCdJobRunsFilter.CALCULATION.count)
                .across(CiCdJobRunsFilter.DISTINCT.qualified_job_name)
                .stacks(List.of(CiCdJobRunsFilter.DISTINCT.job_status, CiCdJobRunsFilter.DISTINCT.qualified_job_name,
                        CiCdJobRunsFilter.DISTINCT.instance_name, CiCdJobRunsFilter.DISTINCT.project_name,
                        CiCdJobRunsFilter.DISTINCT.job_name, CiCdJobRunsFilter.DISTINCT.cicd_user_id,
                        CiCdJobRunsFilter.DISTINCT.job_normalized_full_name, CiCdJobRunsFilter.DISTINCT.trend,
                        CiCdJobRunsFilter.DISTINCT.job_end))
                .build(), VALUES_ONLY);
        Assert.assertEquals(2, dbAggregationResultDbListResponse.getTotalCount().intValue());

        DbListResponse<DbAggregationResult> dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobConfigChanges(company,
                CiCdJobConfigChangesFilter.builder().across(CiCdJobConfigChangesFilter.DISTINCT.trend)
                        .calculation(CiCdJobConfigChangesFilter.CALCULATION.count)
                        .changeStartTime(0L)
                        .changeEndTime(222222222L)
                        .build(),
                VALUES_ONLY,
                null
        );
        Assert.assertEquals(2, dbAggregationResultDbListResponse.getTotalCount().intValue());
    }

    @Test
    public void testJobRunProductFilters() throws SQLException, IOException {
        Instant now = Instant.now();
        Instant before2Days = now.minus(2, ChronoUnit.DAYS);
        Instant before3Days = now.minus(3, ChronoUnit.DAYS);
        Instant before4Days = now.minus(4, ChronoUnit.DAYS);

        CICDInstance cicdInstance1 = CiCdInstanceUtils.createCiCdInstance(ciCdInstancesDatabaseService, company, integration2.toBuilder().id(integrationId2).build(), 0);
        CICDJob cicdJob1 = CiCdJobUtils.createCICDJob(ciCdJobsDatabaseService, company, 0, cicdInstance1);
        CICDJobRun cicdJobRun1 = CICDJobRunUtils.createCICDJobRun(ciCdJobRunsDatabaseService, cicdJob1, company, 0,
                before3Days, null, null, null);
        CiCdJobConfigChangesUtils.createCICDJobConfigChanges(ciCdJobConfigChangesDatabaseService, company, cicdJob1, 0, 1);

        CICDInstance cicdInstance2 = CiCdInstanceUtils.createCiCdInstance(ciCdInstancesDatabaseService, company, integration3.toBuilder().id(integrationId3).build(), 0);
        CICDJob cicdJob2 = CiCdJobUtils.createCICDJob(ciCdJobsDatabaseService, company, 0, cicdInstance2);
        CICDJobRun cicdJobRun2 = CICDJobRunUtils.createCICDJobRun(ciCdJobRunsDatabaseService, cicdJob2, company, 0,
                before3Days, null, null, null);
        CiCdJobConfigChangesUtils.createCICDJobConfigChanges(ciCdJobConfigChangesDatabaseService, company, cicdJob2, 0, 1);
        insertTriageRuleHits(cicdJobRun1.getId().toString(), rule1);
        insertTriageRuleHits(cicdJobRun2.getId().toString(), rule2);
        Assert.assertNotNull(integrationId2);
        DBOrgProduct orgProduct1 = DBOrgProduct.builder()
                .name("product-1")
                .description("prod-1")
                .integrations(Set.of(DBOrgProduct.Integ.builder()
                        .integrationId(Integer.parseInt(integrationId2))
                        .name(integration2.getName())
                        .type("azure_devops")
                        .filters(Map.of("job_names", List.of("jobname-0"),
                                "job_statuses", List.of("SUCCESS")))
                        .build())).build();

        DBOrgProduct orgProduct2 = DBOrgProduct.builder()
                .name("product-2")
                .description("prod-2")
                .integrations(Set.of(DBOrgProduct.Integ.builder()
                        .integrationId(Integer.parseInt(integrationId2))
                        .name(integration2.getName())
                        .type("azure_devops")
                        .filters(Map.of("job_names", List.of("jobname-12"),
                                "projects", List.of("project-123"),
                                "job_statuses", List.of("SUCCESS")))
                        .build())).build();
        DBOrgProduct orgProduct3 = DBOrgProduct.builder()
                .name("product-3")
                .description("prod-3")
                .integrations(Set.of(DBOrgProduct.Integ.builder()
                        .integrationId(Integer.parseInt(integrationId2))
                        .name(integration2.getName())
                        .type("azure_devops")
                        .filters(Map.of("job_statuses", List.of("FAILURE")))
                        .build()))
                .build();
        DBOrgProduct orgProduct4 = DBOrgProduct.builder()
                .name("product-4")
                .description("prod-4")
                .integrations(Set.of(DBOrgProduct.Integ.builder()
                        .integrationId(Integer.parseInt(integrationId3))
                        .name(integration3.getName())
                        .type("jenkins")
                        .filters(Map.of("job_statuses", List.of(cicdJobRun2.getStatus()),
                                "cicd_user_ids", List.of(cicdJobRun2.getCicdUserId()),
                                "job_names", List.of(cicdJob2.getJobName()),
                                "job_normalized_full_names", List.of(cicdJob2.getJobNormalizedFullName()),
                                "instance_names", List.of(cicdInstance2.getName()),
                                "integration_ids", List.of(cicdInstance2.getIntegrationId()),
                                "projects", List.of(cicdJob2.getProjectName())))
                        .build()))
                .build();
        DBOrgProduct orgProduct5 = DBOrgProduct.builder()
                .name("product-5")
                .description("prod-5")
                .integrations(Set.of(DBOrgProduct.Integ.builder()
                        .integrationId(Integer.parseInt(integrationId2))
                        .name(integration2.getName())
                        .type("azure_devops")
                        .filters(Map.of("partial_match", Map.of("job_normalized_full_name", Map.of("$begins", "jobname"))))
                        .build()))
                .build();
        DBOrgProduct orgProduct6 = DBOrgProduct.builder()
                .name("product-6")
                .description("prod-6")
                .integrations(Set.of(DBOrgProduct.Integ.builder()
                        .integrationId(Integer.parseInt(integrationId2))
                        .name(integration2.getName())
                        .type("azure_devops")
                        .filters(Map.of("partial_match", Map.of("job_normalized_full_name", Map.of("$begins", "jobname"))))
                        .build()))
                .build();
        String orgProductId1 = productsDatabaseService.insert(company, orgProduct1);
        String orgProductId2 = productsDatabaseService.insert(company, orgProduct2);
        String orgProductId3 = productsDatabaseService.insert(company, orgProduct3);
        String orgProductId4 = productsDatabaseService.insert(company, orgProduct4);
        String orgProductId5 = productsDatabaseService.insert(company, orgProduct5);
        integration2 = integration2.toBuilder().id(integrationId2).build();


        // endregion
        //Without stacks
        //positive test
        var dbAggregationResultDbListResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company, CiCdJobRunsFilter.builder()
                .calculation(CiCdJobRunsFilter.CALCULATION.count)
                .across(CiCdJobRunsFilter.DISTINCT.qualified_job_name)
                .orgProductsIds(Set.of(UUID.fromString(orgProductId1)))
                .build(), VALUES_ONLY);
        DefaultObjectMapper.prettyPrint(dbAggregationResultDbListResponse);
        Assert.assertEquals(1, dbAggregationResultDbListResponse.getTotalCount().intValue());

        dbAggregationResultDbListResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company, CiCdJobRunsFilter.builder()
                .calculation(CiCdJobRunsFilter.CALCULATION.count)
                .across(CiCdJobRunsFilter.DISTINCT.triage_rule)
                .orgProductsIds(Set.of(UUID.fromString(orgProductId1)))
                .build(), VALUES_ONLY);
        DefaultObjectMapper.prettyPrint(dbAggregationResultDbListResponse);
        Assert.assertEquals(1, dbAggregationResultDbListResponse.getTotalCount().intValue());

        dbAggregationResultDbListResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company, CiCdJobRunsFilter.builder()
                .calculation(CiCdJobRunsFilter.CALCULATION.count)
                .across(CiCdJobRunsFilter.DISTINCT.triage_rule)
                .orgProductsIds(Set.of(UUID.fromString(orgProductId4)))
                .build(), VALUES_ONLY);
        DefaultObjectMapper.prettyPrint(dbAggregationResultDbListResponse);
        Assert.assertEquals(1, dbAggregationResultDbListResponse.getTotalCount().intValue());


        dbAggregationResultDbListResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company, CiCdJobRunsFilter.builder()
                .calculation(CiCdJobRunsFilter.CALCULATION.count)
                .across(CiCdJobRunsFilter.DISTINCT.job_end)
                .aggInterval(CICD_AGG_INTERVAL.week)
                .orgProductsIds(Set.of(UUID.fromString(orgProductId5)))
                .build(), VALUES_ONLY);
        DefaultObjectMapper.prettyPrint(dbAggregationResultDbListResponse);
        Assert.assertEquals(1, dbAggregationResultDbListResponse.getTotalCount().intValue());

        dbAggregationResultDbListResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company, CiCdJobRunsFilter.builder()
                .calculation(CiCdJobRunsFilter.CALCULATION.count)
                .across(CiCdJobRunsFilter.DISTINCT.job_name)
                .orgProductsIds(Set.of(UUID.fromString(orgProductId1), UUID.fromString(orgProductId4)))
                .build(), VALUES_ONLY);
        DefaultObjectMapper.prettyPrint(dbAggregationResultDbListResponse);
        Assert.assertEquals(1, dbAggregationResultDbListResponse.getTotalCount().intValue());

        dbAggregationResultDbListResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company, CiCdJobRunsFilter.builder()
                .calculation(CiCdJobRunsFilter.CALCULATION.duration)
                .across(CiCdJobRunsFilter.DISTINCT.trend)
                .orgProductsIds(Set.of(UUID.fromString(orgProductId1)))
                .build(), VALUES_ONLY);
        DefaultObjectMapper.prettyPrint(dbAggregationResultDbListResponse);
        Assert.assertEquals(1, dbAggregationResultDbListResponse.getTotalCount().intValue());

        dbAggregationResultDbListResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company, CiCdJobRunsFilter.builder()
                .calculation(CiCdJobRunsFilter.CALCULATION.duration)
                .across(CiCdJobRunsFilter.DISTINCT.cicd_user_id)
                .orgProductsIds(Set.of(UUID.fromString(orgProductId4)))
                .build(), VALUES_ONLY);
        DefaultObjectMapper.prettyPrint(dbAggregationResultDbListResponse);
        Assert.assertEquals(1, dbAggregationResultDbListResponse.getTotalCount().intValue());

        //negative
        dbAggregationResultDbListResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company, CiCdJobRunsFilter.builder()
                .calculation(CiCdJobRunsFilter.CALCULATION.count)
                .across(CiCdJobRunsFilter.DISTINCT.project_name)
                .orgProductsIds(Set.of(UUID.fromString(orgProductId2)))
                .build(), VALUES_ONLY);
        DefaultObjectMapper.prettyPrint(dbAggregationResultDbListResponse);
        Assert.assertEquals(0, dbAggregationResultDbListResponse.getTotalCount().intValue());

        dbAggregationResultDbListResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company, CiCdJobRunsFilter.builder()
                .calculation(CiCdJobRunsFilter.CALCULATION.duration)
                .across(CiCdJobRunsFilter.DISTINCT.job_status)
                .orgProductsIds(Set.of(UUID.fromString(orgProductId2), UUID.fromString(orgProductId3)))
                .build(), VALUES_ONLY);
        DefaultObjectMapper.prettyPrint(dbAggregationResultDbListResponse);
        Assert.assertEquals(0, dbAggregationResultDbListResponse.getTotalCount().intValue());

        //With stacks
        //positive test
        dbAggregationResultDbListResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company, CiCdJobRunsFilter.builder()
                .calculation(CiCdJobRunsFilter.CALCULATION.count)
                .across(CiCdJobRunsFilter.DISTINCT.job_status)
                .orgProductsIds(Set.of(UUID.fromString(orgProductId1)))
                .stacks(List.of(CiCdJobRunsFilter.DISTINCT.triage_rule))
                .build(), VALUES_ONLY);
        DefaultObjectMapper.prettyPrint(dbAggregationResultDbListResponse);
        Assert.assertEquals(1, dbAggregationResultDbListResponse.getTotalCount().intValue());

        dbAggregationResultDbListResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company, CiCdJobRunsFilter.builder()
                .calculation(CiCdJobRunsFilter.CALCULATION.count)
                .across(CiCdJobRunsFilter.DISTINCT.qualified_job_name)
                .orgProductsIds(Set.of(UUID.fromString(orgProductId1)))
                .stacks(List.of(CiCdJobRunsFilter.DISTINCT.job_status))
                .build(), VALUES_ONLY);
        DefaultObjectMapper.prettyPrint(dbAggregationResultDbListResponse);
        Assert.assertEquals(1, dbAggregationResultDbListResponse.getTotalCount().intValue());

        dbAggregationResultDbListResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company, CiCdJobRunsFilter.builder()
                .calculation(CiCdJobRunsFilter.CALCULATION.count)
                .across(CiCdJobRunsFilter.DISTINCT.job_name)
                .orgProductsIds(Set.of(UUID.fromString(orgProductId1), UUID.fromString(orgProductId4)))
                .stacks(List.of(CiCdJobRunsFilter.DISTINCT.trend))
                .build(), VALUES_ONLY);
        DefaultObjectMapper.prettyPrint(dbAggregationResultDbListResponse);
        Assert.assertEquals(1, dbAggregationResultDbListResponse.getTotalCount().intValue());

        dbAggregationResultDbListResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company, CiCdJobRunsFilter.builder()
                .calculation(CiCdJobRunsFilter.CALCULATION.duration)
                .across(CiCdJobRunsFilter.DISTINCT.trend)
                .orgProductsIds(Set.of(UUID.fromString(orgProductId1)))
                .stacks(List.of(CiCdJobRunsFilter.DISTINCT.instance_name))
                .build(), VALUES_ONLY);
        DefaultObjectMapper.prettyPrint(dbAggregationResultDbListResponse);
        Assert.assertEquals(1, dbAggregationResultDbListResponse.getTotalCount().intValue());

        dbAggregationResultDbListResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company, CiCdJobRunsFilter.builder()
                .calculation(CiCdJobRunsFilter.CALCULATION.duration)
                .across(CiCdJobRunsFilter.DISTINCT.cicd_user_id)
                .orgProductsIds(Set.of(UUID.fromString(orgProductId4)))
                .stacks(List.of(CiCdJobRunsFilter.DISTINCT.job_normalized_full_name))
                .build(), VALUES_ONLY);
        DefaultObjectMapper.prettyPrint(dbAggregationResultDbListResponse);
        Assert.assertEquals(1, dbAggregationResultDbListResponse.getTotalCount().intValue());

        //negative
        dbAggregationResultDbListResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company, CiCdJobRunsFilter.builder()
                .calculation(CiCdJobRunsFilter.CALCULATION.count)
                .across(CiCdJobRunsFilter.DISTINCT.project_name)
                .orgProductsIds(Set.of(UUID.fromString(orgProductId2)))
                .stacks(List.of(CiCdJobRunsFilter.DISTINCT.project_name))
                .build(), VALUES_ONLY);
        DefaultObjectMapper.prettyPrint(dbAggregationResultDbListResponse);
        Assert.assertEquals(0, dbAggregationResultDbListResponse.getTotalCount().intValue());

        dbAggregationResultDbListResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company, CiCdJobRunsFilter.builder()
                .calculation(CiCdJobRunsFilter.CALCULATION.duration)
                .across(CiCdJobRunsFilter.DISTINCT.job_status)
                .orgProductsIds(Set.of(UUID.fromString(orgProductId2), UUID.fromString(orgProductId3)))
                .stacks(List.of(CiCdJobRunsFilter.DISTINCT.job_name))
                .build(), VALUES_ONLY);
        DefaultObjectMapper.prettyPrint(dbAggregationResultDbListResponse);
        Assert.assertEquals(0, dbAggregationResultDbListResponse.getTotalCount().intValue());

        //list
        var dbListResponse = ciCdAggsService.listCiCdJobRuns(company,
                CiCdJobRunsFilter.builder().jobNames(List.of("Pipe2"))
                        .orgProductsIds(Set.of(UUID.fromString(orgProductId1)))
                        .build(), null, PAGE_NUMBER, PAGE_SIZE);
        DefaultObjectMapper.prettyPrint(dbListResponse);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(1, dbListResponse.getCount().intValue());
        Assert.assertEquals(1, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(1, dbListResponse.getRecords().size());

        dbListResponse = ciCdAggsService.listCiCdJobRuns(company,
                CiCdJobRunsFilter.builder()
                        .orgProductsIds(Set.of(UUID.fromString(orgProductId4), UUID.fromString(orgProductId1)))
                        .build(), null, PAGE_NUMBER, PAGE_SIZE);
        DefaultObjectMapper.prettyPrint(dbListResponse);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(2, dbListResponse.getCount().intValue());
        Assert.assertEquals(2, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(2, dbListResponse.getRecords().size());

        dbListResponse = ciCdAggsService.listCiCdJobRuns(company,
                CiCdJobRunsFilter.builder()
                        .orgProductsIds(Set.of(UUID.fromString(orgProductId4)))
                        .build(), null, PAGE_NUMBER, PAGE_SIZE);
        DefaultObjectMapper.prettyPrint(dbListResponse);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(1, dbListResponse.getCount().intValue());
        Assert.assertEquals(1, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(1, dbListResponse.getRecords().size());

        dbListResponse = ciCdAggsService.listCiCdJobRuns(company,
                CiCdJobRunsFilter.builder()
                        .orgProductsIds(Set.of(UUID.fromString(orgProductId2), UUID.fromString(orgProductId3)))
                        .build(), null, PAGE_NUMBER, PAGE_SIZE);
        DefaultObjectMapper.prettyPrint(dbListResponse);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(0, dbListResponse.getCount().intValue());
        Assert.assertEquals(0, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(0, dbListResponse.getRecords().size());

        dbListResponse = ciCdAggsService.listCiCdJobRuns(company,
                CiCdJobRunsFilter.builder()
                        .orgProductsIds(Set.of(UUID.fromString(orgProductId3)))
                        .build(), null, PAGE_NUMBER, PAGE_SIZE);
        DefaultObjectMapper.prettyPrint(dbListResponse);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(0, dbListResponse.getCount().intValue());
        Assert.assertEquals(0, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(0, dbListResponse.getRecords().size());

    }

    @Test
    public void testJobConfigChangesProductFilters() throws SQLException, IOException {
        Instant now = Instant.now();
        Instant before2Days = now.minus(2, ChronoUnit.DAYS);
        Instant before3Days = now.minus(3, ChronoUnit.DAYS);
        Instant before4Days = now.minus(4, ChronoUnit.DAYS);

        CICDInstance cicdInstance1 = CiCdInstanceUtils.createCiCdInstance(ciCdInstancesDatabaseService, company, integration2.toBuilder().id(integrationId2).build(), 0);
        CICDJob cicdJob1 = CiCdJobUtils.createCICDJob(ciCdJobsDatabaseService, company, 0, cicdInstance1);
        CICDJobRun cicdJobRun1 = CICDJobRunUtils.createCICDJobRun(ciCdJobRunsDatabaseService, cicdJob1, company, 0,
                before3Days, null, null, null);
        CiCdJobConfigChangesUtils.createCICDJobConfigChanges(ciCdJobConfigChangesDatabaseService, company, cicdJob1, 0, 1);

        CICDInstance cicdInstance2 = CiCdInstanceUtils.createCiCdInstance(ciCdInstancesDatabaseService, company, integration3.toBuilder().id(integrationId3).build(), 0);
        CICDJob cicdJob2 = CiCdJobUtils.createCICDJob(ciCdJobsDatabaseService, company, 0, cicdInstance2);
        CICDJobRun cicdJobRun2 = CICDJobRunUtils.createCICDJobRun(ciCdJobRunsDatabaseService, cicdJob2, company, 0,
                before3Days, null, null, null);
        CiCdJobConfigChangesUtils.createCICDJobConfigChanges(ciCdJobConfigChangesDatabaseService, company, cicdJob2, 0, 1);

        Assert.assertNotNull(integrationId2);

        DBOrgProduct orgProduct1 = DBOrgProduct.builder()
                .name("product-1")
                .description("prod-1")
                .integrations(Set.of(DBOrgProduct.Integ.builder()
                        .integrationId(Integer.parseInt(integrationId2))
                        .name(integration2.getName())
                        .type("azure_devops")
                        .filters(Map.of("job_names", List.of("jobname-0"),
                                "projects", List.of(cicdJob1.getProjectName())))
                        .build())).build();

        DBOrgProduct orgProduct2 = DBOrgProduct.builder()
                .name("product-2")
                .description("prod-2")
                .integrations(Set.of(DBOrgProduct.Integ.builder()
                        .integrationId(Integer.parseInt(integrationId2))
                        .name(integration2.getName())
                        .type("azure_devops")
                        .filters(Map.of("job_names", List.of("jobname-12"),
                                "projects", List.of("project-123")))
                        .build())).build();
        DBOrgProduct orgProduct3 = DBOrgProduct.builder()
                .name("product-3")
                .description("prod-3")
                .integrations(Set.of(DBOrgProduct.Integ.builder()
                        .integrationId(Integer.parseInt(integrationId2))
                        .name(integration2.getName())
                        .type("azure_devops")
                        .filters(Map.of("instance_names", List.of("inst-name")))
                        .build()))
                .build();
        DBOrgProduct orgProduct4 = DBOrgProduct.builder()
                .name("product-4")
                .description("prod-4")
                .integrations(Set.of(DBOrgProduct.Integ.builder()
                        .integrationId(Integer.parseInt(integrationId3))
                        .type("jenkins")
                        .filters(Map.of("cicd_user_ids", List.of(cicdJobRun2.getCicdUserId()),
                                "job_names", List.of(cicdJob2.getJobName()),
                                "instance_names", List.of(cicdInstance2.getName()),
                                "projects", List.of(cicdJob2.getProjectName())))
                        .build()))
                .build();
        String orgProductId1 = productsDatabaseService.insert(company, orgProduct1);
        String orgProductId2 = productsDatabaseService.insert(company, orgProduct2);
        String orgProductId3 = productsDatabaseService.insert(company, orgProduct3);
        String orgProductId4 = productsDatabaseService.insert(company, orgProduct4);
        integration2 = integration2.toBuilder().id(integrationId2).build();


        // endregion
        //Without stacks
        //positive test
        var dbAggregationResultDbListResponse = ciCdAggsService.groupByAndCalculateCiCdJobConfigChanges(company, CiCdJobConfigChangesFilter.builder()
                .calculation(CiCdJobConfigChangesFilter.CALCULATION.count)
                .across(CiCdJobConfigChangesFilter.DISTINCT.qualified_job_name)
                .orgProductsIds(Set.of(UUID.fromString(orgProductId4)))
                .build(), VALUES_ONLY, null);
        DefaultObjectMapper.prettyPrint(dbAggregationResultDbListResponse);
        Assert.assertEquals(1, dbAggregationResultDbListResponse.getTotalCount().intValue());

        dbAggregationResultDbListResponse = ciCdAggsService.groupByAndCalculateCiCdJobConfigChanges(company, CiCdJobConfigChangesFilter.builder()
                .calculation(CiCdJobConfigChangesFilter.CALCULATION.count)
                .across(CiCdJobConfigChangesFilter.DISTINCT.job_name)
                .orgProductsIds(Set.of(UUID.fromString(orgProductId1), UUID.fromString(orgProductId4)))
                .build(), VALUES_ONLY, null);
        DefaultObjectMapper.prettyPrint(dbAggregationResultDbListResponse);
        Assert.assertEquals(1, dbAggregationResultDbListResponse.getTotalCount().intValue());

        dbAggregationResultDbListResponse = ciCdAggsService.groupByAndCalculateCiCdJobConfigChanges(company, CiCdJobConfigChangesFilter.builder()
                .calculation(CiCdJobConfigChangesFilter.CALCULATION.count)
                .across(CiCdJobConfigChangesFilter.DISTINCT.trend)
                .orgProductsIds(Set.of(UUID.fromString(orgProductId1)))
                .build(), VALUES_ONLY, null);
        DefaultObjectMapper.prettyPrint(dbAggregationResultDbListResponse);
        Assert.assertEquals(1, dbAggregationResultDbListResponse.getTotalCount().intValue());

        dbAggregationResultDbListResponse = ciCdAggsService.groupByAndCalculateCiCdJobConfigChanges(company, CiCdJobConfigChangesFilter.builder()
                .calculation(CiCdJobConfigChangesFilter.CALCULATION.count)
                .across(CiCdJobConfigChangesFilter.DISTINCT.cicd_user_id)
                .orgProductsIds(Set.of(UUID.fromString(orgProductId4)))
                .build(), VALUES_ONLY, null);
        DefaultObjectMapper.prettyPrint(dbAggregationResultDbListResponse);
        Assert.assertEquals(1, dbAggregationResultDbListResponse.getTotalCount().intValue());
        //negative
        dbAggregationResultDbListResponse = ciCdAggsService.groupByAndCalculateCiCdJobConfigChanges(company, CiCdJobConfigChangesFilter.builder()
                .calculation(CiCdJobConfigChangesFilter.CALCULATION.count)
                .across(CiCdJobConfigChangesFilter.DISTINCT.project_name)
                .orgProductsIds(Set.of(UUID.fromString(orgProductId2)))
                .build(), VALUES_ONLY, null);
        DefaultObjectMapper.prettyPrint(dbAggregationResultDbListResponse);
        Assert.assertEquals(0, dbAggregationResultDbListResponse.getTotalCount().intValue());

        dbAggregationResultDbListResponse = ciCdAggsService.groupByAndCalculateCiCdJobConfigChanges(company, CiCdJobConfigChangesFilter.builder()
                .calculation(CiCdJobConfigChangesFilter.CALCULATION.count)
                .across(CiCdJobConfigChangesFilter.DISTINCT.job_name)
                .orgProductsIds(Set.of(UUID.fromString(orgProductId2), UUID.fromString(orgProductId3)))
                .build(), VALUES_ONLY, null);
        DefaultObjectMapper.prettyPrint(dbAggregationResultDbListResponse);
        Assert.assertEquals(0, dbAggregationResultDbListResponse.getTotalCount().intValue());

        //With stacks
        //positive test
        dbAggregationResultDbListResponse = ciCdAggsService.groupByAndCalculateCiCdJobConfigChanges(company, CiCdJobConfigChangesFilter.builder()
                .calculation(CiCdJobConfigChangesFilter.CALCULATION.count)
                .across(CiCdJobConfigChangesFilter.DISTINCT.qualified_job_name)
                .orgProductsIds(Set.of(UUID.fromString(orgProductId1)))
                .stacks(List.of(CiCdJobConfigChangesFilter.DISTINCT.trend))
                .build(), VALUES_ONLY, null);
        DefaultObjectMapper.prettyPrint(dbAggregationResultDbListResponse);
        Assert.assertEquals(1, dbAggregationResultDbListResponse.getTotalCount().intValue());

        dbAggregationResultDbListResponse = ciCdAggsService.groupByAndCalculateCiCdJobConfigChanges(company, CiCdJobConfigChangesFilter.builder()
                .calculation(CiCdJobConfigChangesFilter.CALCULATION.count)
                .across(CiCdJobConfigChangesFilter.DISTINCT.job_name)
                .orgProductsIds(Set.of(UUID.fromString(orgProductId1), UUID.fromString(orgProductId4)))
                .stacks(List.of(CiCdJobConfigChangesFilter.DISTINCT.project_name))
                .build(), VALUES_ONLY, null);
        DefaultObjectMapper.prettyPrint(dbAggregationResultDbListResponse);
        Assert.assertEquals(1, dbAggregationResultDbListResponse.getTotalCount().intValue());

        dbAggregationResultDbListResponse = ciCdAggsService.groupByAndCalculateCiCdJobConfigChanges(company, CiCdJobConfigChangesFilter.builder()
                .calculation(CiCdJobConfigChangesFilter.CALCULATION.count)
                .across(CiCdJobConfigChangesFilter.DISTINCT.trend)
                .orgProductsIds(Set.of(UUID.fromString(orgProductId1)))
                .stacks(List.of(CiCdJobConfigChangesFilter.DISTINCT.instance_name))
                .build(), VALUES_ONLY, null);
        DefaultObjectMapper.prettyPrint(dbAggregationResultDbListResponse);
        Assert.assertEquals(1, dbAggregationResultDbListResponse.getTotalCount().intValue());

        dbAggregationResultDbListResponse = ciCdAggsService.groupByAndCalculateCiCdJobConfigChanges(company, CiCdJobConfigChangesFilter.builder()
                .calculation(CiCdJobConfigChangesFilter.CALCULATION.count)
                .across(CiCdJobConfigChangesFilter.DISTINCT.cicd_user_id)
                .orgProductsIds(Set.of(UUID.fromString(orgProductId4)))
                .stacks(List.of(CiCdJobConfigChangesFilter.DISTINCT.instance_name))
                .build(), VALUES_ONLY, null);
        DefaultObjectMapper.prettyPrint(dbAggregationResultDbListResponse);
        Assert.assertEquals(1, dbAggregationResultDbListResponse.getTotalCount().intValue());

        //negative
        dbAggregationResultDbListResponse = ciCdAggsService.groupByAndCalculateCiCdJobConfigChanges(company, CiCdJobConfigChangesFilter.builder()
                .calculation(CiCdJobConfigChangesFilter.CALCULATION.count)
                .across(CiCdJobConfigChangesFilter.DISTINCT.project_name)
                .orgProductsIds(Set.of(UUID.fromString(orgProductId2)))
                .stacks(List.of(CiCdJobConfigChangesFilter.DISTINCT.cicd_user_id))
                .build(), VALUES_ONLY, null);
        DefaultObjectMapper.prettyPrint(dbAggregationResultDbListResponse);
        Assert.assertEquals(0, dbAggregationResultDbListResponse.getTotalCount().intValue());

        dbAggregationResultDbListResponse = ciCdAggsService.groupByAndCalculateCiCdJobConfigChanges(company, CiCdJobConfigChangesFilter.builder()
                .calculation(CiCdJobConfigChangesFilter.CALCULATION.count)
                .across(CiCdJobConfigChangesFilter.DISTINCT.project_name)
                .orgProductsIds(Set.of(UUID.fromString(orgProductId2), UUID.fromString(orgProductId3)))
                .stacks(List.of(CiCdJobConfigChangesFilter.DISTINCT.job_name))
                .build(), VALUES_ONLY, null);
        DefaultObjectMapper.prettyPrint(dbAggregationResultDbListResponse);
        Assert.assertEquals(0, dbAggregationResultDbListResponse.getTotalCount().intValue());

        //list
        var dbListResponse = ciCdAggsService.listCiCdJobConfigChanges(company,
                CiCdJobConfigChangesFilter.builder()
                        .orgProductsIds(Set.of(UUID.fromString(orgProductId1)))
                        .build(), null, PAGE_NUMBER, PAGE_SIZE);
        DefaultObjectMapper.prettyPrint(dbListResponse);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(1, dbListResponse.getCount().intValue());
        Assert.assertEquals(1, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(1, dbListResponse.getRecords().size());

        dbListResponse = ciCdAggsService.listCiCdJobConfigChanges(company,
                CiCdJobConfigChangesFilter.builder()
                        .orgProductsIds(Set.of(UUID.fromString(orgProductId4), UUID.fromString(orgProductId1)))
                        .build(), null, PAGE_NUMBER, PAGE_SIZE);
        DefaultObjectMapper.prettyPrint(dbListResponse);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(2, dbListResponse.getCount().intValue());
        Assert.assertEquals(2, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(2, dbListResponse.getRecords().size());

        dbListResponse = ciCdAggsService.listCiCdJobConfigChanges(company,
                CiCdJobConfigChangesFilter.builder()
                        .orgProductsIds(Set.of(UUID.fromString(orgProductId4)))
                        .build(), null, PAGE_NUMBER, PAGE_SIZE);
        DefaultObjectMapper.prettyPrint(dbListResponse);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(1, dbListResponse.getCount().intValue());
        Assert.assertEquals(1, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(1, dbListResponse.getRecords().size());

        dbListResponse = ciCdAggsService.listCiCdJobConfigChanges(company,
                CiCdJobConfigChangesFilter.builder()
                        .orgProductsIds(Set.of(UUID.fromString(orgProductId2), UUID.fromString(orgProductId3)))
                        .build(), null, PAGE_NUMBER, PAGE_SIZE);
        DefaultObjectMapper.prettyPrint(dbListResponse);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(0, dbListResponse.getCount().intValue());
        Assert.assertEquals(0, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(0, dbListResponse.getRecords().size());

        dbListResponse = ciCdAggsService.listCiCdJobConfigChanges(company,
                CiCdJobConfigChangesFilter.builder()
                        .orgProductsIds(Set.of(UUID.fromString(orgProductId3)))
                        .build(), null, PAGE_NUMBER, PAGE_SIZE);
        DefaultObjectMapper.prettyPrint(dbListResponse);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(0, dbListResponse.getCount().intValue());
        Assert.assertEquals(0, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(0, dbListResponse.getRecords().size());

    }

    @Test
    public void testJobRunsSortBy() throws SQLException {
        Instant now = Instant.now();
        Instant before2Days = now.minus(2, ChronoUnit.DAYS);
        Instant before3Days = now.minus(3, ChronoUnit.DAYS);
        Instant before4Days = now.minus(4, ChronoUnit.DAYS);

        CICDInstance cicdInstance1 = CiCdInstanceUtils.createCiCdInstance(ciCdInstancesDatabaseService,
                company, integration2.toBuilder().id(integrationId2).build(), 0);
        CICDJob cicdJob1 = CiCdJobUtils.createCICDJob(ciCdJobsDatabaseService, company, 0, cicdInstance1);
        CICDJobRun cicdJobRun1 = CICDJobRun.builder()
                .cicdJobId(cicdJob1.getId())
                .jobRunNumber(10L)
                .status("FAILED")
                .cicdUserId("XYZ")
                .duration(10)
                .startTime(before3Days)
                .endTime(before2Days)
                .scmCommitIds(List.of())
                .build();
        ciCdJobRunsDatabaseService.insert(company, cicdJobRun1);

        CICDInstance cicdInstance2 = CiCdInstanceUtils.createCiCdInstance(ciCdInstancesDatabaseService,
                company, integration3.toBuilder().id(integrationId3).build(), 1);
        CICDJob cicdJob2 = CiCdJobUtils.createCICDJob(ciCdJobsDatabaseService, company, 1, cicdInstance2);
        CICDJobRun cicdJobRun2 = CICDJobRun.builder()
                .cicdJobId(cicdJob2.getId())
                .jobRunNumber(10L)
                .status("SUCCESS")
                .duration(20)
                .startTime(before4Days)
                .endTime(before3Days)
                .scmCommitIds(List.of())
                .build();
        ciCdJobRunsDatabaseService.insert(company, cicdJobRun2);

        var dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder().across(CiCdJobRunsFilter.DISTINCT.job_status)
                        .calculation(CiCdJobRunsFilter.CALCULATION.count)
                        .stacks(List.of(CiCdJobRunsFilter.DISTINCT.qualified_job_name))
                        .build(), Map.of("job_status", SortingOrder.ASC), VALUES_ONLY
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(2, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getRecords().size());
        assertThat(dbAggsResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(DbAggregationResult::getKey));

        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder().across(CiCdJobRunsFilter.DISTINCT.job_status)
                        .calculation(CiCdJobRunsFilter.CALCULATION.duration)
                        .build(), Map.of("job_status", SortingOrder.DESC), VALUES_ONLY
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(2, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getRecords().size());
        assertThat(dbAggsResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(DbAggregationResult::getKey).reversed());

        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder().across(CiCdJobRunsFilter.DISTINCT.job_status)
                        .calculation(CiCdJobRunsFilter.CALCULATION.duration)
                        .build(), Map.of("duration", SortingOrder.DESC), VALUES_ONLY
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(2, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getRecords().size());
        assertThat(dbAggsResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(DbAggregationResult::getMedian).reversed());

        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder().across(CiCdJobRunsFilter.DISTINCT.job_name)
                        .calculation(CiCdJobRunsFilter.CALCULATION.count)
                        .build(), Map.of("job_name", SortingOrder.ASC), VALUES_ONLY
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(2, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getRecords().size());
        assertThat(dbAggsResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(DbAggregationResult::getKey));

        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder().across(CiCdJobRunsFilter.DISTINCT.job_name)
                        .calculation(CiCdJobRunsFilter.CALCULATION.duration)
                        .build(), Map.of("job_name", SortingOrder.DESC), VALUES_ONLY
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(2, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getRecords().size());
        assertThat(dbAggsResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(DbAggregationResult::getKey).reversed());

        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder().across(CiCdJobRunsFilter.DISTINCT.job_name)
                        .calculation(CiCdJobRunsFilter.CALCULATION.duration)
                        .build(), Map.of("duration", SortingOrder.DESC), VALUES_ONLY
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(2, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getRecords().size());
        assertThat(dbAggsResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(DbAggregationResult::getMedian).reversed());

        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder().across(CiCdJobRunsFilter.DISTINCT.qualified_job_name)
                        .calculation(CiCdJobRunsFilter.CALCULATION.count)
                        .build(), Map.of("qualified_job_name", SortingOrder.ASC), VALUES_ONLY
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(2, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getRecords().size());
        assertThat(dbAggsResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(DbAggregationResult::getKey));

        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder().across(CiCdJobRunsFilter.DISTINCT.qualified_job_name)
                        .calculation(CiCdJobRunsFilter.CALCULATION.duration)
                        .build(), Map.of("qualified_job_name", SortingOrder.DESC), VALUES_ONLY
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(2, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getRecords().size());
        assertThat(dbAggsResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(DbAggregationResult::getKey).reversed());

        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder().across(CiCdJobRunsFilter.DISTINCT.qualified_job_name)
                        .calculation(CiCdJobRunsFilter.CALCULATION.count)
                        .build(), Map.of("count", SortingOrder.ASC), VALUES_ONLY
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(2, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getRecords().size());
        assertThat(dbAggsResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(DbAggregationResult::getCount));

        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder().across(CiCdJobRunsFilter.DISTINCT.job_normalized_full_name)
                        .calculation(CiCdJobRunsFilter.CALCULATION.count)
                        .build(), Map.of("job_normalized_full_name", SortingOrder.ASC), VALUES_ONLY
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(2, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getRecords().size());
        assertThat(dbAggsResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(DbAggregationResult::getKey));

        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder().across(CiCdJobRunsFilter.DISTINCT.job_normalized_full_name)
                        .calculation(CiCdJobRunsFilter.CALCULATION.duration)
                        .build(), Map.of("job_normalized_full_name", SortingOrder.DESC), VALUES_ONLY
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(2, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getRecords().size());
        assertThat(dbAggsResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(DbAggregationResult::getKey).reversed());

        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder().across(CiCdJobRunsFilter.DISTINCT.job_normalized_full_name)
                        .calculation(CiCdJobRunsFilter.CALCULATION.count)
                        .build(), Map.of("count", SortingOrder.ASC), VALUES_ONLY
        );
        DefaultObjectMapper.prettyPrint(dbAggsResponse);
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(2, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getRecords().size());
        assertThat(dbAggsResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(DbAggregationResult::getCount));


        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder().across(CiCdJobRunsFilter.DISTINCT.instance_name)
                        .calculation(CiCdJobRunsFilter.CALCULATION.count)
                        .build(), Map.of("instance_name", SortingOrder.ASC), VALUES_ONLY
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(2, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getRecords().size());
        assertThat(dbAggsResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(DbAggregationResult::getKey));

        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder().across(CiCdJobRunsFilter.DISTINCT.instance_name)
                        .calculation(CiCdJobRunsFilter.CALCULATION.duration)
                        .build(), Map.of("instance_name", SortingOrder.DESC), VALUES_ONLY
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(2, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getRecords().size());
        assertThat(dbAggsResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(DbAggregationResult::getKey).reversed());

        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder().across(CiCdJobRunsFilter.DISTINCT.cicd_user_id)
                        .calculation(CiCdJobRunsFilter.CALCULATION.count)
                        .build(), Map.of("cicd_user_id", SortingOrder.ASC), VALUES_ONLY
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(2, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getRecords().size());
        assertThat(getKeys(dbAggsResponse)).isSortedAccordingTo(Comparator.nullsFirst(Comparator.naturalOrder()));

        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder().across(CiCdJobRunsFilter.DISTINCT.cicd_user_id)
                        .calculation(CiCdJobRunsFilter.CALCULATION.duration)
                        .build(), Map.of("cicd_user_id", SortingOrder.DESC), VALUES_ONLY
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(2, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getRecords().size());
        assertThat(getKeys(dbAggsResponse)).isSortedAccordingTo(Comparator.nullsLast(Comparator.reverseOrder()));

        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder().across(CiCdJobRunsFilter.DISTINCT.cicd_job_id)
                        .calculation(CiCdJobRunsFilter.CALCULATION.count)
                        .build(), Map.of("cicd_job_id", SortingOrder.ASC), VALUES_ONLY
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(2, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getRecords().size());
        assertThat(dbAggsResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(DbAggregationResult::getKey));

        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder().across(CiCdJobRunsFilter.DISTINCT.cicd_job_id)
                        .calculation(CiCdJobRunsFilter.CALCULATION.duration)
                        .build(), Map.of("cicd_job_id", SortingOrder.DESC), VALUES_ONLY
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(2, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getRecords().size());
        assertThat(dbAggsResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(DbAggregationResult::getKey).reversed());

        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder().across(CiCdJobRunsFilter.DISTINCT.job_end)
                        .calculation(CiCdJobRunsFilter.CALCULATION.count)
                        .build(), Map.of("job_end", SortingOrder.ASC), VALUES_ONLY
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(2, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getRecords().size());
        assertThat(dbAggsResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(DbAggregationResult::getKey));

        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder().across(CiCdJobRunsFilter.DISTINCT.job_end)
                        .calculation(CiCdJobRunsFilter.CALCULATION.duration)
                        .build(), Map.of("job_end", SortingOrder.DESC), VALUES_ONLY
        );
        DefaultObjectMapper.prettyPrint(dbAggsResponse);
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(2, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getRecords().size());
        assertThat(dbAggsResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(DbAggregationResult::getKey).reversed());

        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder().across(CiCdJobRunsFilter.DISTINCT.project_name)
                        .calculation(CiCdJobRunsFilter.CALCULATION.count)
                        .build(), Map.of("project_name", SortingOrder.ASC), VALUES_ONLY
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(2, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getRecords().size());
        assertThat(dbAggsResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(DbAggregationResult::getKey));

        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder().across(CiCdJobRunsFilter.DISTINCT.project_name)
                        .calculation(CiCdJobRunsFilter.CALCULATION.duration)
                        .build(), Map.of("project_name", SortingOrder.DESC), VALUES_ONLY
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(2, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getRecords().size());
        assertThat(dbAggsResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(DbAggregationResult::getKey).reversed());

        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder().across(CiCdJobRunsFilter.DISTINCT.trend)
                        .calculation(CiCdJobRunsFilter.CALCULATION.count)
                        .build(), Map.of("trend", SortingOrder.ASC), VALUES_ONLY
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(2, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getRecords().size());
        assertThat(dbAggsResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(DbAggregationResult::getKey));

        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder().across(CiCdJobRunsFilter.DISTINCT.trend)
                        .calculation(CiCdJobRunsFilter.CALCULATION.duration)
                        .build(), Map.of("trend", SortingOrder.DESC), VALUES_ONLY
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(2, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getRecords().size());
        assertThat(dbAggsResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(DbAggregationResult::getKey).reversed());

        //drilldown

        DbListResponse<CICDJobRunDTO> dbListResponse = ciCdAggsService.listCiCdJobRuns(company,
                CiCdJobRunsFilter.builder().build(), Map.of("status", SortingOrder.ASC), PAGE_NUMBER, PAGE_SIZE);
        Assert.assertNotNull(dbListResponse);
        assertThat(dbListResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(CICDJobRunDTO::getStatus));

        dbListResponse = ciCdAggsService.listCiCdJobRuns(company,
                CiCdJobRunsFilter.builder().build(), Map.of("status", SortingOrder.DESC), PAGE_NUMBER, PAGE_SIZE);
        Assert.assertNotNull(dbListResponse);
        assertThat(dbListResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(CICDJobRunDTO::getStatus).reversed());

        dbListResponse = ciCdAggsService.listCiCdJobRuns(company,
                CiCdJobRunsFilter.builder().build(), Map.of("cicd_user_id", SortingOrder.ASC), PAGE_NUMBER, PAGE_SIZE);
        Assert.assertNotNull(dbListResponse);
        assertThat(getKeysList(dbListResponse)).isSortedAccordingTo(Comparator.nullsFirst(Comparator.naturalOrder()));

        dbListResponse = ciCdAggsService.listCiCdJobRuns(company,
                CiCdJobRunsFilter.builder().build(), Map.of("cicd_user_id", SortingOrder.DESC), PAGE_NUMBER, PAGE_SIZE);
        Assert.assertNotNull(dbListResponse);
        assertThat(getKeysList(dbListResponse)).isSortedAccordingTo(Comparator.nullsLast(Comparator.reverseOrder()));

        dbListResponse = ciCdAggsService.listCiCdJobRuns(company,
                CiCdJobRunsFilter.builder().build(), Map.of("duration", SortingOrder.ASC), PAGE_NUMBER, PAGE_SIZE);
        Assert.assertNotNull(dbListResponse);
        assertThat(dbListResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(CICDJobRunDTO::getDuration));

        dbListResponse = ciCdAggsService.listCiCdJobRuns(company,
                CiCdJobRunsFilter.builder().build(), Map.of("duration", SortingOrder.DESC), PAGE_NUMBER, PAGE_SIZE);
        Assert.assertNotNull(dbListResponse);
        assertThat(dbListResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(CICDJobRunDTO::getDuration).reversed());

        dbListResponse = ciCdAggsService.listCiCdJobRuns(company,
                CiCdJobRunsFilter.builder().build(), Map.of("job_run_number", SortingOrder.ASC), PAGE_NUMBER, PAGE_SIZE);
        Assert.assertNotNull(dbListResponse);
        assertThat(dbListResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(CICDJobRunDTO::getJobRunNumber));

        dbListResponse = ciCdAggsService.listCiCdJobRuns(company,
                CiCdJobRunsFilter.builder().build(), Map.of("job_run_number", SortingOrder.DESC), PAGE_NUMBER, PAGE_SIZE);
        Assert.assertNotNull(dbListResponse);
        assertThat(dbListResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(CICDJobRunDTO::getJobRunNumber).reversed());

        dbListResponse = ciCdAggsService.listCiCdJobRuns(company,
                CiCdJobRunsFilter.builder().build(), Map.of("instance_name", SortingOrder.ASC), PAGE_NUMBER, PAGE_SIZE);
        Assert.assertNotNull(dbListResponse);
        assertThat(dbListResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(CICDJobRunDTO::getCicdInstanceName));

        dbListResponse = ciCdAggsService.listCiCdJobRuns(company,
                CiCdJobRunsFilter.builder().build(), Map.of("instance_name", SortingOrder.DESC), PAGE_NUMBER, PAGE_SIZE);
        Assert.assertNotNull(dbListResponse);
        assertThat(dbListResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(CICDJobRunDTO::getCicdInstanceName).reversed());

        dbListResponse = ciCdAggsService.listCiCdJobRuns(company,
                CiCdJobRunsFilter.builder().build(), Map.of("job_normalized_full_name", SortingOrder.ASC), PAGE_NUMBER, PAGE_SIZE);
        Assert.assertNotNull(dbListResponse);
        assertThat(dbListResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(CICDJobRunDTO::getJobNormalizedFullName));

        dbListResponse = ciCdAggsService.listCiCdJobRuns(company,
                CiCdJobRunsFilter.builder().build(), Map.of("job_normalized_full_name", SortingOrder.DESC), PAGE_NUMBER, PAGE_SIZE);
        Assert.assertNotNull(dbListResponse);
        assertThat(dbListResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(CICDJobRunDTO::getJobNormalizedFullName).reversed());

    }

    private List<String> getKeys(DbListResponse<DbAggregationResult> dbAggsResponse) {
        return dbAggsResponse.getRecords().stream()
                .map(result -> (StringUtils.isEmpty(result.getKey()) || "null".equals(result.getKey())) ? null : result.getKey())
                .collect(Collectors.toList());
    }

    private List<String> getKeysList(DbListResponse<CICDJobRunDTO> dbListResponse) {
        return dbListResponse.getRecords().stream().map(CICDJobRunDTO::getCicdUserId).collect(Collectors.toList());
    }

    // endregion

    // region Heper Classes
    public static class JobDetails {
        @JsonProperty("job_name")
        public String jobName;
        @JsonProperty("job_full_name")
        public String jobFullName;
        @JsonProperty("job_normalized_full_name")
        public String jobNormalizedFullName;
        @JsonProperty("branch_name")
        public String branchName;
        @JsonProperty("module_name")
        public String moduleName;
        @JsonProperty("scm_url")
        public String scmUrl;
        @JsonProperty("scm_user_id")
        public String scmUserId;
        @JsonProperty("runs")
        public List<JobRunDetails> runs;

        public String getJobName() {
            return jobName;
        }

        public void setJobName(String jobName) {
            this.jobName = jobName;
        }

        public String getJobFullName() {
            return jobFullName;
        }

        public void setJobFullName(String jobFullName) {
            this.jobFullName = jobFullName;
        }

        public String getJobNormalizedFullName() {
            return jobNormalizedFullName;
        }

        public void setJobNormalizedFullName(String jobNormalizedFullName) {
            this.jobNormalizedFullName = jobNormalizedFullName;
        }

        public String getBranchName() {
            return branchName;
        }

        public void setBranchName(String branchName) {
            this.branchName = branchName;
        }

        public String getModuleName() {
            return moduleName;
        }

        public void setModuleName(String moduleName) {
            this.moduleName = moduleName;
        }

        public String getScmUrl() {
            return scmUrl;
        }

        public void setScmUrl(String scmUrl) {
            this.scmUrl = scmUrl;
        }

        public String getScmUserId() {
            return scmUserId;
        }

        public void setScmUserId(String scmUserId) {
            this.scmUserId = scmUserId;
        }

        public List<JobRunDetails> getRuns() {
            return runs;
        }

        public void setRuns(List<JobRunDetails> runs) {
            this.runs = runs;
        }
    }

    public static class JobRunDetails {
        @JsonProperty("number")
        private Long number;
        @JsonProperty("status")
        private String status;
        @JsonProperty("start_time")
        private Long startTime;
        @JsonProperty("end_time")
        private Long endTime;
        @JsonProperty("duration")
        private Long duration;
        @JsonProperty("user_id")
        private String userId;
        @JsonProperty("commit_ids")
        private List<String> commitIds;
        @JsonProperty("metadata")
        private Map<String, Object> metadata;
        @JsonProperty("params")
        private List<JobRunParam> params;

        public Long getNumber() {
            return number;
        }

        public void setNumber(Long number) {
            this.number = number;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Long getStartTime() {
            return startTime;
        }

        public Long getEndTime() {
            return endTime;
        }

        public void setStartTime(Long startTime) {
            this.startTime = startTime;
        }

        public void setEndTime(Long endTime) {
            this.endTime = endTime;
        }

        public Long getDuration() {
            return duration;
        }

        public void setDuration(Long duration) {
            this.duration = duration;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public List<String> getCommitIds() {
            return commitIds;
        }

        public void setCommitIds(List<String> commitIds) {
            this.commitIds = commitIds;
        }

        public Map<String, Object> getMetadata() {
            return  metadata;
        }

        public void setMetadata(Map<String, Object> metadata) {
            this.metadata = metadata;
        }

        public List<JobRunParam> getParams() {
            return params;
        }

        public void setParams(List<JobRunParam> params) {
            this.params = params;
        }
    }

    public static class JobRunParam {
        @JsonProperty("type")
        private String type;
        @JsonProperty("name")
        private String name;
        @JsonProperty("value")
        private String value;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}