package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.CICDInstance;
import io.levelops.commons.databases.models.database.CICDJob;
import io.levelops.commons.databases.models.database.CICDJobRun;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.organization.*;
import io.levelops.commons.databases.models.filters.CICD_AGG_INTERVAL;
import io.levelops.commons.databases.models.filters.CiCdJobRunsFilter;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.dev_productivity.DevProductivityProfileDatabaseService;
import io.levelops.commons.databases.services.organization.*;
import io.levelops.commons.databases.services.velocity.OrgProfileDatabaseService;
import io.levelops.commons.databases.services.velocity.VelocityConfigsDatabaseService;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.web.exceptions.BadRequestException;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.assertj.core.api.Assertions;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.services.CICDJobRunUtils.createCiCdJobRunsFilter;
import static io.levelops.commons.databases.services.ScmCommitUtils.arrayUniq;

public class CiCdAggServiceOuTest {
    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();
    private static final String company = "test";
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
    private static CiCdScmCombinedAggsService ciCdScmCombinedAggsService;
    private static CiCdAggsService ciCdAggsService;
    private static IntegrationService integrationService;
    private static ProductsDatabaseService productsDatabaseService;
    private static ScmAggService scmAggService;
    private static CiCdScmMappingService ciCdScmMappingService;

    private static Integration integration;
    private static Integration integration2;
    private static Integration integration3;
    private static String integrationId2;
    private static String integrationId3;
    private final List<CICDJobRun> ciCdJobRuns = new ArrayList<>();
    private final List<CICDJob> ciCdJobs = new ArrayList<>();

    private static OrgUnitHelper unitsHelper;
    private static OrgUsersDatabaseService usersService;
    private static OrgUnitsDatabaseService unitsService;
    private static DBOrgUnit unit1, unit2, unit3;
    private static ProductService workspaceDatabaseService;
    private static OrgUnitCategoryDatabaseService orgUnitCategoryDatabaseService;
    private static OrgUnitCategory orgGroup1;
    private static String orgGroupId1;
    private static Pair<UUID, Integer> ids, ids2, ids3;
    private static OrgProfileDatabaseService ouProfileDbService;
    private static VelocityConfigsDatabaseService velocityConfigDbService;
    private static DevProductivityProfileDatabaseService devProductivityProfileDbService;
    private static TicketCategorizationSchemeDatabaseService ticketCategorizationSchemeDatabaseService;

    @BeforeClass
    public static void setup() throws SQLException {
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";" +
                "CREATE SCHEMA test;").execute();
        dataSource.getConnection().prepareStatement(arrayUniq)
                .execute();
        userService = new UserService(dataSource, MAPPER);
        ciCdInstancesDatabaseService = new CiCdInstancesDatabaseService(dataSource);
        workspaceDatabaseService = new ProductService(dataSource);
        TagsService tagsService = new TagsService(dataSource);
        UserIdentityService userIdentityService = new UserIdentityService(dataSource);
        TagItemDBService tagItemService = new TagItemDBService(dataSource);
        ciCdJobsDatabaseService = new CiCdJobsDatabaseService(dataSource);
        OrgVersionsDatabaseService versionsService = new OrgVersionsDatabaseService(dataSource);
        ciCdJobRunsDatabaseService = new CiCdJobRunsDatabaseService(DefaultObjectMapper.get(), dataSource);
        ciCdJobConfigChangesDatabaseService = new CiCdJobConfigChangesDatabaseService(dataSource);
        usersService = new OrgUsersDatabaseService(dataSource, MAPPER, versionsService, userIdentityService);
        new UserService(dataSource, mapper).ensureTableExistence(company);
        DashboardWidgetService dashboardWidgetService = new DashboardWidgetService(dataSource, mapper);
        dashboardWidgetService.ensureTableExistence(company);
        unitsService = new OrgUnitsDatabaseService(dataSource, MAPPER, tagItemService, usersService, versionsService, dashboardWidgetService);
        integrationService = new IntegrationService(dataSource);
        unitsHelper = new OrgUnitHelper(unitsService, integrationService);
        dashboardWidgetService.ensureTableExistence(company);
        ciCdScmCombinedAggsService = new CiCdScmCombinedAggsService(dataSource, ciCdJobRunsDatabaseService);
        ciCdAggsService = new CiCdAggsService(dataSource);
        scmAggService = new ScmAggService(dataSource, userIdentityService);
        ciCdScmMappingService = new CiCdScmMappingService(dataSource);

        integrationService.ensureTableExistence(company);
        ciCdInstancesDatabaseService.ensureTableExistence(company);
        ciCdJobsDatabaseService.ensureTableExistence(company);
        ouProfileDbService = new OrgProfileDatabaseService(dataSource,mapper);
        ouProfileDbService.ensureTableExistence(company);
        velocityConfigDbService = new VelocityConfigsDatabaseService(dataSource,mapper,ouProfileDbService);
        velocityConfigDbService.ensureTableExistence(company);
        ticketCategorizationSchemeDatabaseService = new TicketCategorizationSchemeDatabaseService(dataSource, mapper);
        ticketCategorizationSchemeDatabaseService.ensureTableExistence(company);
        devProductivityProfileDbService = new DevProductivityProfileDatabaseService(dataSource,mapper);
        devProductivityProfileDbService.ensureTableExistence(company);

        new DatabaseSchemaService(dataSource)
                .ensureSchemaExistence(company);
        integrationService = new IntegrationService(dataSource);
        integrationService.ensureTableExistence(company);
        integration = Integration.builder()
                .id("1")
                .name("name-1")
                .url("http-1")
                .status("active")
                .application("azure_devops")
                .description("desc")
                .satellite(false)
                .build();
        integrationService.insert(company, integration);
        productsDatabaseService = new ProductsDatabaseService(dataSource, MAPPER);
        productsDatabaseService.ensureTableExistence(company);
        userService.ensureTableExistence(company);
        ciCdInstancesDatabaseService.ensureTableExistence(company);
        ciCdJobsDatabaseService.ensureTableExistence(company);
        ciCdJobRunsDatabaseService.ensureTableExistence(company);
        ciCdJobConfigChangesDatabaseService.ensureTableExistence(company);
        ciCdAggsService.ensureTableExistence(company);
        ciCdScmCombinedAggsService.ensureTableExistence(company);
        tagsService.ensureTableExistence(company);
        tagItemService.ensureTableExistence(company);
        versionsService.ensureTableExistence(company);
        userIdentityService.ensureTableExistence(company);
        workspaceDatabaseService.ensureTableExistence(company);
        scmAggService.ensureTableExistence(company);
        ciCdScmMappingService.ensureTableExistence(company);
        orgUnitCategoryDatabaseService = new OrgUnitCategoryDatabaseService(dataSource, unitsHelper, mapper);
        orgUnitCategoryDatabaseService.ensureTableExistence(company);
        usersService.ensureTableExistence(company);
        unitsService.ensureTableExistence(company);


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

        var orgUser1 = DBOrgUser.builder()
                .email("email1")
                .fullName("fullName1")
                .active(true)
                .customFields(Map.of("test_name", "test1"))
                .ids(Set.of(DBOrgUser.LoginId.builder().cloudId("testread").username("cloudId").integrationType(integration.getApplication())
                        .integrationId(Integer.parseInt(integration.getId())).build()))
                .versions(Set.of(1))
                .build();
        var userId1 = usersService.upsert(company, orgUser1);

        var orgUser2 = DBOrgUser.builder()
                .email("email2")
                .fullName("fullName2")
                .active(true)
                .customFields(Map.of("sample_name", "sample"))
                .ids(Set.of(DBOrgUser.LoginId.builder().cloudId("SCMTrigger").integrationId(Integer.parseInt(integration.getId())).build()))
                .versions(Set.of(2, 3))
                .build();
        var userId2 = usersService.upsert(company, orgUser2);
        var orgUser3 = DBOrgUser.builder()
                .email("email3")
                .fullName("fullName3")
                .active(true)
                .customFields(Map.of("test_name", "test1"))
                .ids(Set.of(DBOrgUser.LoginId.builder().cloudId("SYSTEM").username("cloudId").integrationType(integration.getApplication())
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
        orgGroup1 = OrgUnitCategory.builder()
                .name("TEAM A")
                .description("Sample team")
                .isPredefined(true)
                .build();
        orgGroupId1 = orgUnitCategoryDatabaseService.insert(company, orgGroup1);
        unit1 = DBOrgUnit.builder()
                .name("unit1")
                .description("My unit1")
                .active(true)
                .versions(Set.of(1))
                .managers(managers)
                .ouCategoryId(UUID.fromString(orgGroupId1))
                .sections(Set.of(DBOrgContentSection.builder()
                        .integrationId(Integer.valueOf(integration.getId()))
                        .integrationFilters(Map.of("cicd_user_ids", List.of("testread", "SYSTEM")))
                        .defaultSection(false)
                        .users(Set.of(1, 2, 3))
                        .build()))
                .refId(1)
                .build();
        ids=unitsService.insertForId(company, unit1);
        unitsHelper.activateVersion(company,ids.getLeft());

        unit2 = DBOrgUnit.builder()
                .name("unit2")
                .description("My unit2")
                .active(true)
                .versions(Set.of(2))
                .managers(managers)
                .ouCategoryId(UUID.fromString(orgGroupId1))
                .sections(Set.of(DBOrgContentSection.builder()
                        .integrationId(Integer.valueOf(integration.getId()))
                        .integrationFilters(Map.of())
                        .defaultSection(false)
                        .users(Set.of(1, 2))
                        .build()))
                .refId(2)
                .build();
        ids2=unitsService.insertForId(company, unit2);
        unitsHelper.activateVersion(company,ids2.getLeft());

        unit3 = DBOrgUnit.builder()
                .name("unit3")
                .description("My unit3")
                .active(true)
                .versions(Set.of(2))
                .managers(managers)
                .ouCategoryId(UUID.fromString(orgGroupId1))
                .sections(Set.of(DBOrgContentSection.builder()
                        .integrationId(Integer.valueOf(integration.getId()))
                        .integrationFilters(Map.of())
                        .defaultSection(false)
                        .users(Set.of(1, 3))
                        .build()))
                .refId(3)
                .build();
        ids3=unitsService.insertForId(company, unit3);
        unitsHelper.activateVersion(company,ids3.getLeft());
    }

    @Before
    public void cleanup() throws SQLException {
        dataSource.getConnection().prepareStatement("DELETE FROM test.cicd_jobs;").execute();
    }

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
            Assert.assertNotNull(cicdJobIdString);
            ciCdJobs.add(cicdJob);
            UUID cicdJobId = UUID.fromString(cicdJobIdString);
            for (CiCdAggsServiceTest.JobRunDetails currentJobRun : currentJobAllRuns.getRuns()) {
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
                    for (CiCdAggsServiceTest.JobRunParam currentParam : currentJobRun.getParams()) {
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
                ciCdJobRunsDatabaseService.insert(company, cicdJobRun);
                ciCdJobRuns.add(cicdJobRun);
            }
        }
    }
    // endregion

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
    public void testGroupByAndCalculateCiCdJobsUsingOU() throws SQLException, IOException, BadRequestException {
        // region Setup
        String data = ResourceUtils.getResourceAsString("json/databases/jenkins_plugin_result.json");
        List<CiCdAggsServiceTest.JobDetails> allJobDetails = MAPPER.readValue(data, MAPPER.getTypeFactory().constructCollectionType(List.class, CiCdAggsServiceTest.JobDetails.class));
        allJobDetails = fixJobRunTimestamps(allJobDetails, calculateOffset());
        CICDInstance cicdInstance = CiCdInstanceUtils.createCiCdInstance(ciCdInstancesDatabaseService, company, 0);
        setupJobsAndJobRuns(null, allJobDetails);
        setupJobsAndJobRuns(cicdInstance.getId(), allJobDetails);

        //CalculateCiCdJobs
        Optional<DBOrgUnit> dbOrgUnit1 = unitsService.get(company, 1, true);
        DefaultListRequest defaultListRequest = DefaultListRequest.builder().ouIds(Set.of(1))
                .ouUserFilterDesignation(Map.of("jenkins", Set.of("user"))).build();
        OUConfiguration ouConfig = unitsHelper.getOuConfigurationFromDBOrgUnit(company,
                IntegrationType.getCICDIntegrationTypes(), defaultListRequest,
                dbOrgUnit1.orElseThrow(), false);
        defaultListRequest = ouConfig.getRequest();
        DbListResponse<DbAggregationResult> dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                createCiCdJobRunsFilter(defaultListRequest).across(CiCdJobRunsFilter.DISTINCT.job_status)
                        .calculation(CiCdJobRunsFilter.CALCULATION.count)
                        .aggInterval(CICD_AGG_INTERVAL.month)
                        .build(),
                VALUES_ONLY,
                ouConfig);
        Assertions.assertThat(dbAggsResponse.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .containsAnyOf("SUCCESS", "FAILURE", "ABORTED");
        Assertions.assertThat(dbAggsResponse.getTotalCount()).isEqualTo(3);

        Optional<DBOrgUnit> dbOrgUnit2 = unitsService.get(company, 2, true);
        defaultListRequest = DefaultListRequest.builder().ouIds(Set.of(2)).ouUserFilterDesignation(Map.of("jenkins", Set.of("user"))).build();
        ouConfig = unitsHelper.getOuConfigurationFromDBOrgUnit(company,
                IntegrationType.getCICDIntegrationTypes(), defaultListRequest,
                dbOrgUnit2.orElseThrow(), false);
        defaultListRequest = ouConfig.getRequest();
        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                createCiCdJobRunsFilter(defaultListRequest).across(CiCdJobRunsFilter.DISTINCT.cicd_user_id)
                        .calculation(CiCdJobRunsFilter.CALCULATION.count)
                        .build(),
                VALUES_ONLY,
                ouConfig);
        Assertions.assertThat(dbAggsResponse.getTotalCount()).isEqualTo(2);
        Assertions.assertThat(dbAggsResponse.getRecords().stream().map(DbAggregationResult::getKey)
                .collect(Collectors.toList())).containsExactlyInAnyOrder("testread", "SCMTrigger");

        Optional<DBOrgUnit> dbOrgUnit3 = unitsService.get(company, 3, true);
        defaultListRequest = DefaultListRequest.builder().ouIds(Set.of(3)).ouUserFilterDesignation(Map.of("jenkins", Set.of("user"))).build();
        ouConfig = unitsHelper.getOuConfigurationFromDBOrgUnit(company,
                IntegrationType.getCICDIntegrationTypes(), defaultListRequest,
                dbOrgUnit3.orElseThrow(), false);
        defaultListRequest = ouConfig.getRequest();
        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                createCiCdJobRunsFilter(defaultListRequest).across(CiCdJobRunsFilter.DISTINCT.cicd_user_id)
                        .calculation(CiCdJobRunsFilter.CALCULATION.count)
                        .build(),
                VALUES_ONLY,
                ouConfig);
        Assertions.assertThat(dbAggsResponse.getTotalCount()).isEqualTo(1);
        Assertions.assertThat(dbAggsResponse.getRecords().stream().map(DbAggregationResult::getKey)
                .collect(Collectors.toList())).containsExactlyInAnyOrder("testread");

        defaultListRequest = DefaultListRequest.builder().ouIds(Set.of(3)).ouUserFilterDesignation(Map.of("jenkins", Set.of("user")))
                .filter(Map.of("cicd_user_ids", List.of("testread")))
                .ouExclusions(List.of("cicd_user_ids")).build();
        ouConfig = unitsHelper.getOuConfigurationFromDBOrgUnit(company,
                IntegrationType.getCICDIntegrationTypes(), defaultListRequest,
                dbOrgUnit3.orElseThrow(), false);
        defaultListRequest = ouConfig.getRequest();
        dbAggsResponse = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                createCiCdJobRunsFilter(defaultListRequest).across(CiCdJobRunsFilter.DISTINCT.cicd_user_id)
                        .calculation(CiCdJobRunsFilter.CALCULATION.count)
                        .build(),
                VALUES_ONLY,
                ouConfig);
        Assertions.assertThat(dbAggsResponse.getTotalCount()).isEqualTo(1);
        Assertions.assertThat(dbAggsResponse.getRecords().stream().map(DbAggregationResult::getKey)
                .collect(Collectors.toList())).containsExactlyInAnyOrder("testread");


    }
}
