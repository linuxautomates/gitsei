package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.CICDInstance;
import io.levelops.commons.databases.models.database.CICDJob;
import io.levelops.commons.databases.models.database.CICDJobRun;
import io.levelops.commons.databases.models.database.CiCdScmMapping;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.scm.DbScmCommit;
import io.levelops.commons.databases.models.filters.CICD_AGG_INTERVAL;
import io.levelops.commons.databases.models.filters.CiCdJobRunParameter;
import io.levelops.commons.databases.models.filters.CiCdJobRunsFilter;
import io.levelops.commons.databases.models.filters.CiCdScmFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.organization.ProductsDatabaseService;
import io.levelops.commons.databases.services.organization.TeamMembersDatabaseService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.assertj.core.api.Assertions;
import org.junit.*;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.services.CICDJobRunUtils.createCICDJobRun;
import static io.levelops.commons.databases.services.CiCdDateUtils.extractDataComponentForDbResults;
import static io.levelops.commons.databases.services.ScmCommitUtils.createScmCommit;
import static org.assertj.core.api.Assertions.assertThat;

public class CiCdScmCombinedAggsServiceDateTest {
    private final static ObjectMapper MAPPER = DefaultObjectMapper.get();
    private final static String company = "test";
    private final static boolean VALUES_ONLY = false;
    private final List<CICDJobRun> ciCdJobRuns = new ArrayList<>();
    private final List<CICDJob> ciCdJobs = new ArrayList<>();

    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;
    private static UserService userService;
    private static IntegrationService integrationService;
    private static CiCdInstancesDatabaseService ciCdInstancesDatabaseService;
    private static CiCdJobsDatabaseService ciCdJobsDatabaseService;
    private static UserIdentityService userIdentityService;
    private static TeamMembersDatabaseService teamMembersDatabaseService;
    private static CiCdJobRunsDatabaseService ciCdJobRunsDatabaseService;
    private static ScmAggService scmAggService;
    private static CiCdScmMappingService ciCdScmMappingService;
    private static CiCdScmCombinedAggsService ciCdScmCombinedAggsService;
    private static ProductsDatabaseService productsDatabaseService;

    private static Integration integration;
    private static Integration integration1;
    private static Integration integration2;

    private static String integrationId;
    private static String integrationId1;
    private static String integrationId2;

    @BeforeClass
    public static void setup() throws SQLException {
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();

        userService = new UserService(dataSource, MAPPER);
        productsDatabaseService = new ProductsDatabaseService(dataSource, MAPPER);
        integrationService = new IntegrationService(dataSource);
        ciCdInstancesDatabaseService = new CiCdInstancesDatabaseService(dataSource);
        ciCdJobsDatabaseService = new CiCdJobsDatabaseService(dataSource);
        userIdentityService = new UserIdentityService(dataSource);
        teamMembersDatabaseService = new TeamMembersDatabaseService(dataSource, MAPPER);
        ciCdJobRunsDatabaseService = new CiCdJobRunsDatabaseService(DefaultObjectMapper.get(), dataSource);
        scmAggService = new ScmAggService(dataSource, userIdentityService);
        ciCdScmMappingService = new CiCdScmMappingService(dataSource);
        ciCdScmCombinedAggsService = new CiCdScmCombinedAggsService(dataSource, ciCdJobRunsDatabaseService);

        new DatabaseSchemaService(dataSource)
                .ensureSchemaExistence(company);
        userService.ensureTableExistence(company);
        integrationService.ensureTableExistence(company);
        productsDatabaseService.ensureTableExistence(company);
        userIdentityService.ensureTableExistence(company);
        teamMembersDatabaseService.ensureTableExistence(company);
        ciCdInstancesDatabaseService.ensureTableExistence(company);
        ciCdJobsDatabaseService.ensureTableExistence(company);
        ciCdJobRunsDatabaseService.ensureTableExistence(company);
        scmAggService.ensureTableExistence(company);
        ciCdScmMappingService.ensureTableExistence(company);
        ciCdScmCombinedAggsService.ensureTableExistence(company);

        integration = Integration.builder()
                .name("integration-name-" + 0)
                .status("status-" + 0).application("application-" + 0).url("http://www.dummy.com")
                .satellite(false).build();
        integrationId = integrationService.insert(company, integration);
        Assert.assertNotNull(integrationId);
        integration = integration.toBuilder().id(integrationId).build();

        integration1 = Integration.builder()
                .name("integration-name-" + 1)
                .status("status-" + 0).application("azure_devops").url("http://www.dummy.com")
                .satellite(false).build();
        integrationId1 = integrationService.insert(company, integration1);
        Assert.assertNotNull(integrationId1);
        integration1 = integration1.toBuilder().id(integrationId1).build();

        integration2 = Integration.builder()
                .name("integration-name-" + 2)
                .status("status-" + 0).application("gitlab").url("http://www.dummy.com")
                .satellite(false).build();
        integrationId2 = integrationService.insert(company, integration2);
        Assert.assertNotNull(integrationId2);
        integration2 = integration2.toBuilder().id(integrationId2).build();
    }

    @Ignore
    public void testGroupBy() throws SQLException, IOException {
        // region Setup
        Instant now = Instant.now();
        Instant before2Days = now.minus(2, ChronoUnit.DAYS);
        Instant before3Days = now.minus(3, ChronoUnit.DAYS);
        Instant before4Days = now.minus(4, ChronoUnit.DAYS);

        CICDInstance cicdInstance = CiCdInstanceUtils.createCiCdInstance(ciCdInstancesDatabaseService, company, 0);
        CICDJob cicdJob = CiCdJobUtils.createCICDJob(ciCdJobsDatabaseService, company, 0, cicdInstance);
        ciCdJobs.add(cicdJob);
        // region Commit is before Job Run - Expected Case
        CICDJobRun cicdJobRun1 = createCICDJobRun(ciCdJobRunsDatabaseService, cicdJob, company, 0, before3Days, null, null, null);
        ciCdJobRuns.add(cicdJobRun1);
        DbScmCommit scmCommit1 = createScmCommit(scmAggService, company, integration.getId(), before4Days);
        CiCdScmMapping mapping1 = CiCdScmMapping.builder().jobRunId(cicdJobRun1.getId()).commitId(UUID.fromString(scmCommit1.getId())).build();
        String mappingId1 = ciCdScmMappingService.insert(company, mapping1);
        Assert.assertNotNull(mappingId1);

        List<Long> listOfInputDates = ciCdJobRuns.stream().map(CICDJobRun::getStartTime)
                .map(Instant::getEpochSecond)
                .collect(Collectors.toList());
        List<String> actualList;
        List<String> expectedList;
        expectedList = List.copyOf(listOfInputDates.stream()
                .filter(Objects::nonNull)
                .map(instant -> extractDataComponentForDbResults(new Date(TimeUnit.MILLISECONDS.toMicros(instant)),
                        Calendar.MONTH, false))
                .map(String::valueOf)
                .collect(Collectors.toSet()));
        DbListResponse<DbAggregationResult> result = ciCdScmCombinedAggsService.groupByAndCalculate(company,
                CiCdScmFilter.builder().across(CiCdScmFilter.DISTINCT.trend)
                        .aggInterval(CICD_AGG_INTERVAL.month)
                        .calculation(CiCdScmFilter.CALCULATION.count).build(),
                VALUES_ONLY
        );
        actualList = result.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .map(key -> new Date(TimeUnit.MILLISECONDS.toMicros(Long.parseLong(key))))
                .map(date -> extractDataComponentForDbResults(date, Calendar.MONTH, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        Assert.assertNotNull(result);
        Assertions.assertThat(actualList).containsExactlyInAnyOrderElementsOf(expectedList);
        assertThat(result.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()))
                .isNotNull();


        expectedList = listOfInputDates.stream()
                .filter(Objects::nonNull)
                .map(instant -> extractDataComponentForDbResults(new Date(TimeUnit.MILLISECONDS.toMicros(instant)),
                        Calendar.YEAR, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        result = ciCdScmCombinedAggsService.groupByAndCalculate(company,
                CiCdScmFilter.builder().across(CiCdScmFilter.DISTINCT.trend)
                        .aggInterval(CICD_AGG_INTERVAL.year)
                        .calculation(CiCdScmFilter.CALCULATION.count).build(),
                VALUES_ONLY
        );
        actualList = result.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .map(key -> new Date(TimeUnit.MILLISECONDS.toMicros(Long.parseLong(key))))
                .map(date -> extractDataComponentForDbResults(date, Calendar.YEAR, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        Assert.assertNotNull(result);
        assertThat(expectedList.containsAll(actualList)).isEqualTo(true);
        assertThat(result.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()))
                .isNotNull();

        expectedList = listOfInputDates.stream()
                .filter(Objects::nonNull)
                .map(instant -> extractDataComponentForDbResults(new Date(TimeUnit.MILLISECONDS.toMicros(instant)),
                        Calendar.WEEK_OF_YEAR, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        result = ciCdScmCombinedAggsService.groupByAndCalculate(company,
                CiCdScmFilter.builder().across(CiCdScmFilter.DISTINCT.trend)
                        .aggInterval(CICD_AGG_INTERVAL.week)
                        .calculation(CiCdScmFilter.CALCULATION.count).build(),
                VALUES_ONLY
        );
        actualList = result.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .map(key -> new Date(TimeUnit.MILLISECONDS.toMicros(Long.parseLong(key))))
                .map(date -> extractDataComponentForDbResults(date, Calendar.WEEK_OF_YEAR, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        Assert.assertNotNull(result);
        // TODO fix unit test
//        assertThat(actualList).containsExactlyElementsOf(expectedList);
        assertThat(result.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()))
                .isNotNull();

        expectedList = listOfInputDates.stream()
                .filter(Objects::nonNull)
                .map(instant -> extractDataComponentForDbResults(new Date(TimeUnit.MILLISECONDS.toMicros(instant)),
                        Calendar.MONTH, true))
                .map(String::valueOf)
                .collect(Collectors.toList());
        result = ciCdScmCombinedAggsService.groupByAndCalculate(company,
                CiCdScmFilter.builder().across(CiCdScmFilter.DISTINCT.trend)
                        .aggInterval(CICD_AGG_INTERVAL.quarter)
                        .calculation(CiCdScmFilter.CALCULATION.count).build(),
                VALUES_ONLY
        );
        actualList = result.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .map(key -> new Date(TimeUnit.MILLISECONDS.toMicros(Long.parseLong(key))))
                .map(date -> extractDataComponentForDbResults(date, Calendar.MONTH, true))
                .map(String::valueOf)
                .collect(Collectors.toList());
        Assert.assertNotNull(result);
        assertThat(expectedList.containsAll(actualList)).isEqualTo(true);
        assertThat(result.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()))
                .isNotNull();
        DbListResponse<DbAggregationResult> dbAggsResponse = ciCdScmCombinedAggsService.groupByAndCalculate(company,
                CiCdScmFilter.builder().across(CiCdScmFilter.DISTINCT.trend)
                        .calculation(CiCdScmFilter.CALCULATION.change_volume)
                        .stacks(List.of(CiCdScmFilter.DISTINCT.qualified_job_name))
                        .build(),
                VALUES_ONLY
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(1, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getRecords().size());
        expectedList = listOfInputDates.stream()
                .filter(Objects::nonNull)
                .map(instant -> extractDataComponentForDbResults(new Date(TimeUnit.MILLISECONDS.toMicros(instant)),
                        Calendar.YEAR, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        result = ciCdScmCombinedAggsService.groupByAndCalculate(company,
                CiCdScmFilter.builder().across(CiCdScmFilter.DISTINCT.trend)
                        .aggInterval(CICD_AGG_INTERVAL.year)
                        .stacks(List.of(CiCdScmFilter.DISTINCT.qualified_job_name))
                        .calculation(CiCdScmFilter.CALCULATION.change_volume).build(),
                VALUES_ONLY
        );
        actualList = result.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .map(key -> new Date(TimeUnit.MILLISECONDS.toMicros(Long.parseLong(key))))
                .map(date -> extractDataComponentForDbResults(date, Calendar.YEAR, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        Assert.assertNotNull(result);
        assertThat(expectedList.containsAll(actualList)).isEqualTo(true);
        assertThat(result.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()))
                .isNotNull();
        assertThat(result.getRecords().stream().map(DbAggregationResult::getStacks)).isNotEmpty();

        expectedList = listOfInputDates.stream()
                .filter(Objects::nonNull)
                .map(instant -> extractDataComponentForDbResults(new Date(TimeUnit.MILLISECONDS.toMicros(instant)),
                        Calendar.MONTH, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        result = ciCdScmCombinedAggsService.groupByAndCalculate(company,
                CiCdScmFilter.builder().across(CiCdScmFilter.DISTINCT.trend)
                        .aggInterval(CICD_AGG_INTERVAL.month)
                        .stacks(List.of(CiCdScmFilter.DISTINCT.job_normalized_full_name))
                        .calculation(CiCdScmFilter.CALCULATION.count).build(),
                VALUES_ONLY
        );
        actualList = result.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .map(key -> new Date(TimeUnit.MILLISECONDS.toMicros(Long.parseLong(key))))
                .map(date -> extractDataComponentForDbResults(date, Calendar.MONTH, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        Assert.assertNotNull(result);
        assertThat(expectedList.containsAll(actualList)).isEqualTo(true);
        assertThat(result.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()))
                .isNotNull();
        assertThat(result.getRecords().stream().map(DbAggregationResult::getStacks)).isNotEmpty();

        expectedList = listOfInputDates.stream()
                .filter(Objects::nonNull)
                .map(instant -> extractDataComponentForDbResults(new Date(TimeUnit.MILLISECONDS.toMicros(instant)),
                        Calendar.MONTH, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        result = ciCdScmCombinedAggsService.groupByAndCalculate(company,
                CiCdScmFilter.builder().across(CiCdScmFilter.DISTINCT.trend)
                        .aggInterval(CICD_AGG_INTERVAL.month)
                        .calculation(CiCdScmFilter.CALCULATION.change_volume).build(),
                VALUES_ONLY);
        actualList = result.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .map(key -> new Date(TimeUnit.MILLISECONDS.toMicros(Long.parseLong(key))))
                .map(date -> extractDataComponentForDbResults(date, Calendar.MONTH, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        Assert.assertNotNull(result);
        assertThat(expectedList.containsAll(actualList)).isEqualTo(true);
        assertThat(result.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()))
                .isNotNull();


        List<DbAggregationResult> records = ciCdScmCombinedAggsService.computeDeployJobChangeVolume(company,
                CiCdJobRunsFilter.builder().jobNames(List.of()).build(), CiCdScmFilter.builder().jobNames(List.of()).build(),
                CiCdScmFilter.DISTINCT.job_end, CICD_AGG_INTERVAL.month, null).getRecords();

        expectedList = listOfInputDates.stream()
                .filter(Objects::nonNull)
                .map(instant -> extractDataComponentForDbResults(new Date(TimeUnit.MILLISECONDS.toMicros(instant)),
                        Calendar.MONTH, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        dbAggsResponse = ciCdScmCombinedAggsService.groupByAndCalculate(company,
                CiCdScmFilter.builder().across(CiCdScmFilter.DISTINCT.trend)
                        .aggInterval(CICD_AGG_INTERVAL.month)
                        .calculation(CiCdScmFilter.CALCULATION.lead_time).build(),
                VALUES_ONLY);
        actualList = dbAggsResponse.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .map(key -> new Date(TimeUnit.MILLISECONDS.toMicros(Long.parseLong(key))))
                .map(date -> extractDataComponentForDbResults(date, Calendar.MONTH, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        Assert.assertNotNull(result);
        assertThat(expectedList.containsAll(actualList)).isEqualTo(true);
        assertThat(result.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()))
                .isNotNull();
        expectedList = listOfInputDates.stream()
                .filter(Objects::nonNull)
                .map(instant -> extractDataComponentForDbResults(new Date(TimeUnit.MILLISECONDS.toMicros(instant)),
                        Calendar.DAY_OF_MONTH, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        List<CICDJobRun.JobRunParam> params = CICDJobRunUtils.contructJobRunParams();
        List<CiCdJobRunParameter> parameters = params.stream().map(p -> CiCdJobRunParameter.builder().name(p.getName()).values(List.of(p.getValue())).build()).collect(Collectors.toList());
        dbAggsResponse = ciCdScmCombinedAggsService.groupByAndCalculate(company,
                CiCdScmFilter.builder().across(CiCdScmFilter.DISTINCT.trend).calculation(CiCdScmFilter.CALCULATION.change_volume)
                        .parameters(parameters).build(),
                VALUES_ONLY
        );
        actualList = dbAggsResponse.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .map(key -> new Date(TimeUnit.MILLISECONDS.toMicros(Long.parseLong(key))))
                .map(date -> extractDataComponentForDbResults(date, Calendar.DAY_OF_MONTH, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        Assert.assertNotNull(result);
        assertThat(result.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()))
                .isNotNull();
       listOfInputDates = ciCdJobRuns.stream().map(CICDJobRun::getEndTime)
                .map(Instant::getEpochSecond)
                .collect(Collectors.toList());
        expectedList = listOfInputDates.stream()
                .filter(Objects::nonNull)
                .map(instant -> extractDataComponentForDbResults(new Date(TimeUnit.MILLISECONDS.toMicros(instant)),
                        Calendar.YEAR, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        dbAggsResponse = ciCdScmCombinedAggsService.groupByAndCalculate(company,
                CiCdScmFilter.builder().across(CiCdScmFilter.DISTINCT.job_end)
                        .aggInterval(CICD_AGG_INTERVAL.year)
                        .calculation(CiCdScmFilter.CALCULATION.change_volume)
                        .build(),
                VALUES_ONLY
        );
        actualList = dbAggsResponse.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .map(key -> new Date(TimeUnit.MILLISECONDS.toMicros(Long.parseLong(key))))
                .map(date -> extractDataComponentForDbResults(date, Calendar.YEAR, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        Assert.assertNotNull(result);
        assertThat(expectedList.containsAll(actualList)).isEqualTo(true);
        assertThat(result.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()))
                .isNotNull();

        expectedList = listOfInputDates.stream()
                .filter(Objects::nonNull)
                .map(instant -> extractDataComponentForDbResults(new Date(TimeUnit.MILLISECONDS.toMicros(instant)),
                        Calendar.MONTH, true))
                .map(String::valueOf)
                .collect(Collectors.toList());
        dbAggsResponse = ciCdScmCombinedAggsService.groupByAndCalculate(company,
                CiCdScmFilter.builder().across(CiCdScmFilter.DISTINCT.job_end)
                        .cicdIntegrationIds(List.of("1"))
                        .aggInterval(CICD_AGG_INTERVAL.quarter)
                        .calculation(CiCdScmFilter.CALCULATION.change_volume)
                        .build(),
                VALUES_ONLY
        );
        actualList = dbAggsResponse.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .map(key -> new Date(TimeUnit.MILLISECONDS.toMicros(Long.parseLong(key))))
                .map(date -> extractDataComponentForDbResults(date, Calendar.MONTH, true))
                .map(String::valueOf)
                .collect(Collectors.toList());
        Assert.assertNotNull(result);
        assertThat(expectedList.containsAll(actualList)).isEqualTo(true);
        assertThat(result.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()))
                .isNotNull();


        expectedList = listOfInputDates.stream()
                .filter(Objects::nonNull)
                .map(instant -> extractDataComponentForDbResults(new Date(TimeUnit.MILLISECONDS.toMicros(instant)),
                        Calendar.MONTH, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        dbAggsResponse = ciCdScmCombinedAggsService.groupByAndCalculate(company,
                CiCdScmFilter.builder().across(CiCdScmFilter.DISTINCT.job_end).aggInterval(CICD_AGG_INTERVAL.month).calculation(CiCdScmFilter.CALCULATION.change_volume)
                        .build(),
                VALUES_ONLY
        );
        actualList = dbAggsResponse.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .map(key -> new Date(TimeUnit.MILLISECONDS.toMicros(Long.parseLong(key))))
                .map(date -> extractDataComponentForDbResults(date, Calendar.MONTH, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        Assert.assertNotNull(result);
        assertThat(expectedList.containsAll(actualList)).isEqualTo(true);
        assertThat(result.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()))
                .isNotNull();

        expectedList = listOfInputDates.stream()
                .filter(Objects::nonNull)
                .map(instant -> extractDataComponentForDbResults(new Date(TimeUnit.MILLISECONDS.toMicros(instant)),
                        Calendar.WEEK_OF_YEAR, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        dbAggsResponse = ciCdScmCombinedAggsService.groupByAndCalculate(company,
                CiCdScmFilter.builder().across(CiCdScmFilter.DISTINCT.job_end).aggInterval(CICD_AGG_INTERVAL.week).calculation(CiCdScmFilter.CALCULATION.lead_time)
                        .build(),
                VALUES_ONLY
        );
        actualList = dbAggsResponse.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .map(key -> new Date(TimeUnit.MILLISECONDS.toMicros(Long.parseLong(key))))
                .map(date -> extractDataComponentForDbResults(date, Calendar.WEEK_OF_YEAR, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        Assert.assertNotNull(result);
        assertThat(expectedList.containsAll(actualList)).isEqualTo(true);
        assertThat(result.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()))
                .isNotNull();

    }

}
