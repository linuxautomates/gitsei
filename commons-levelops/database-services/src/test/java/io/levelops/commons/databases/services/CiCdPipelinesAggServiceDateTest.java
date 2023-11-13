package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.CICDInstance;
import io.levelops.commons.databases.models.database.CICDJob;
import io.levelops.commons.databases.models.database.CICDJobRun;
import io.levelops.commons.databases.models.database.CICDJobTrigger;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.filters.CICD_AGG_INTERVAL;
import io.levelops.commons.databases.models.filters.CiCdPipelineJobRunsFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.organization.ProductsDatabaseService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import lombok.extern.log4j.Log4j2;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.platform.commons.util.StringUtils;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.services.CICDJobRunUtils.contructJobRunParams;
import static io.levelops.commons.databases.services.CiCdDateUtils.extractDataComponentForDbResults;
import static org.assertj.core.api.Assertions.assertThat;

@Log4j2
@SuppressWarnings("unused")
public class CiCdPipelinesAggServiceDateTest {
    private static final Integer PAGE_NUMBER = 0;
    private static final Integer PAGE_SIZE = 300;
    private final static ObjectMapper MAPPER = DefaultObjectMapper.get();
    private final static Map<String, SortingOrder> DEFAULT_SORT_BY = Collections.emptyMap();
    private final String company = "test";
    private static final boolean VALUES_ONLY = false;
    private static List<CICDJob> ciCdJobs = new ArrayList<>();
    private static List<CICDJobRun> ciCdJobRuns = new ArrayList<>();
    private final static Random random = new Random();

    @Rule
    public SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private DataSource dataSource;
    private UserService userService;
    private CiCdInstancesDatabaseService ciCdInstancesDatabaseService;
    private CiCdJobsDatabaseService ciCdJobsDatabaseService;
    private CiCdJobRunsDatabaseService ciCdJobRunsDatabaseService;
    private CiCdJobConfigChangesDatabaseService ciCdJobConfigChangesDatabaseService;
    private CiCdPipelinesAggsService ciCdPipelinesAggsService;
    private IntegrationService integrationService;
    private ProductsDatabaseService productsDatabaseService;

    @Before
    public void setup() throws SQLException {
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        integrationService = new IntegrationService(dataSource);
        userService = new UserService(dataSource, MAPPER);
        ciCdInstancesDatabaseService = new CiCdInstancesDatabaseService(dataSource);
        ciCdJobsDatabaseService = new CiCdJobsDatabaseService(dataSource);
        ciCdJobRunsDatabaseService = new CiCdJobRunsDatabaseService(DefaultObjectMapper.get(), dataSource);
        ciCdJobConfigChangesDatabaseService = new CiCdJobConfigChangesDatabaseService(dataSource);
        ciCdPipelinesAggsService = new CiCdPipelinesAggsService(dataSource, ciCdJobRunsDatabaseService);

        new DatabaseSchemaService(dataSource)
                .ensureSchemaExistence(company);
        integrationService.ensureTableExistence(company);
        Integration integration = Integration.builder()
                .id("1")
                .name("name")
                .url("http")
                .status("good")
                .application("jira")
                .description("desc")
                .satellite(true)
                .build();
        integrationService.insert(company, integration);
        productsDatabaseService = new ProductsDatabaseService(dataSource, MAPPER);
        productsDatabaseService.ensureTableExistence(company);
        userService.ensureTableExistence(company);
        ciCdInstancesDatabaseService.ensureTableExistence(company);
        ciCdJobsDatabaseService.ensureTableExistence(company);
        ciCdJobRunsDatabaseService.ensureTableExistence(company);
        ciCdJobConfigChangesDatabaseService.ensureTableExistence(company);
        ciCdPipelinesAggsService.ensureTableExistence(company);

    }

    private CICDJob buildAndSaveCiCdJob(CiCdJobsDatabaseService ciCdJobsDatabaseService, String company, String jobName, UUID cicdInstanceId) throws SQLException {
        CICDJob cicdJob = CICDJob.builder()
                .jobName(jobName)
                .projectName("project-1")
                .jobFullName("Folder1/jobs/Folder2/jobs/BBMaven1New/jobs/" + jobName + "/branches/master")
                .jobFullName("Folder1/Folder2/BBMaven1New/" + jobName + "/master")
                .jobNormalizedFullName("Folder1/Folder2/BBMaven1New/" + jobName + "/master")
                .branchName("master")
                .moduleName(null)
                .scmUrl("https://bitbucket.org/virajajgaonkar/" + jobName + ".git")
                .scmUserId(null)
                .cicdInstanceId(cicdInstanceId)
                .build();
        String cicdJobId = ciCdJobsDatabaseService.insert(company, cicdJob);
        Assert.assertNotNull(cicdJobId);
        ciCdJobs.add(cicdJob);
        return cicdJob.toBuilder().id(UUID.fromString(cicdJobId)).build();
    }

    @Ignore
    @Test
    public void test() throws SQLException {
        // region Setup

        CICDInstance cicdInstance = CiCdInstanceUtils.createCiCdInstance(ciCdInstancesDatabaseService, company, 0);
        CICDJob jobFalcon = buildAndSaveCiCdJob(ciCdJobsDatabaseService, company, "falcon", cicdInstance.getId());
        CICDJob job1 = buildAndSaveCiCdJob(ciCdJobsDatabaseService, company, "job1", cicdInstance.getId());
        CICDJob job2 = buildAndSaveCiCdJob(ciCdJobsDatabaseService, company, "job2", cicdInstance.getId());
        CICDJob job3 = buildAndSaveCiCdJob(ciCdJobsDatabaseService, company, "job3", cicdInstance.getId());
        CICDJob job1_1 = buildAndSaveCiCdJob(ciCdJobsDatabaseService, company, "job1.1", cicdInstance.getId());
        CICDJob job1_2 = buildAndSaveCiCdJob(ciCdJobsDatabaseService, company, "job1.2", cicdInstance.getId());
        CICDJob job1_3 = buildAndSaveCiCdJob(ciCdJobsDatabaseService, company, "job1.3", cicdInstance.getId());

        int n = 10;
        Instant start = Instant.now().minus(n, ChronoUnit.DAYS);

        for (int i = 0; i < n; i++) {
            Instant day = start.plus(i, ChronoUnit.DAYS);
            CICDJobRun cicdJobFalconRun = createCICDJobRun(ciCdJobRunsDatabaseService, jobFalcon, company, i, day, 70, null, null);

            CICDJobRun cicdJob1Run = createCICDJobRun(ciCdJobRunsDatabaseService, job1, company, i, day, 40, jobFalcon.getJobFullName(), cicdJobFalconRun.getJobRunNumber());
            //CiCdJobRunTriggerUtils.createCiCdJobRunTrigger(ciCdJobRunTriggersDBService, company, cicdJob1Run, jobFalcon.getJobFullName(), cicdJobFalconRun.getJobRunNumber());

            CICDJobRun cicdJob2Run = createCICDJobRun(ciCdJobRunsDatabaseService, job2, company, i, day, 50, jobFalcon.getJobFullName(), cicdJobFalconRun.getJobRunNumber());
            //CiCdJobRunTriggerUtils.createCiCdJobRunTrigger(ciCdJobRunTriggersDBService, company, cicdJob2Run, jobFalcon.getJobFullName(), cicdJobFalconRun.getJobRunNumber());

            CICDJobRun cicdJob3Run = createCICDJobRun(ciCdJobRunsDatabaseService, job3, company, i, day, 60, jobFalcon.getJobFullName(), cicdJobFalconRun.getJobRunNumber());
            //CiCdJobRunTriggerUtils.createCiCdJobRunTrigger(ciCdJobRunTriggersDBService, company, cicdJob3Run, jobFalcon.getJobFullName(), cicdJobFalconRun.getJobRunNumber());

            CICDJobRun cicdJob1_1Run = createCICDJobRun(ciCdJobRunsDatabaseService, job1_1, company, i, day, 30, job1.getJobFullName(), cicdJob1Run.getJobRunNumber());
            //CiCdJobRunTriggerUtils.createCiCdJobRunTrigger(ciCdJobRunTriggersDBService, company, cicdJob1_1Run, job1.getJobFullName(), cicdJob1Run.getJobRunNumber());

            CICDJobRun cicdJob1_2Run = createCICDJobRun(ciCdJobRunsDatabaseService, job1_2, company, i, day, 20, job1.getJobFullName(), cicdJob1Run.getJobRunNumber());
            //CiCdJobRunTriggerUtils.createCiCdJobRunTrigger(ciCdJobRunTriggersDBService, company, cicdJob1_2Run, job1.getJobFullName(), cicdJob1Run.getJobRunNumber());

            CICDJobRun cicdJob1_3Run = createCICDJobRun(ciCdJobRunsDatabaseService, job1_3, company, i, day, 10, job1.getJobFullName(), cicdJob1Run.getJobRunNumber());
            //CiCdJobRunTriggerUtils.createCiCdJobRunTrigger(ciCdJobRunTriggersDBService, company, cicdJob1_3Run, job1.getJobFullName(), cicdJob1Run.getJobRunNumber());
        }
        // endregion
        List<Long> listOfInputDates = ciCdJobRuns.stream().map(CICDJobRun::getStartTime)
                .map(Instant::getEpochSecond)
                .collect(Collectors.toList());
        List<String> actualList;
        List<String> expectedList;
        expectedList = listOfInputDates.stream()
                .filter(Objects::nonNull)
                .map(instant -> extractDataComponentForDbResults(new Date(TimeUnit.MILLISECONDS.toMicros(instant)),
                        Calendar.MONTH, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        DbListResponse<DbAggregationResult> result = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.trend)
                        .aggInterval(CICD_AGG_INTERVAL.month)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.count).build(),
                VALUES_ONLY,
                null
        );
        Assert.assertNotNull(result);
        actualList = result.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .map(key -> new Date(TimeUnit.MILLISECONDS.toMicros(Long.parseLong(key))))
                .map(date -> extractDataComponentForDbResults(date, Calendar.MONTH, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        Assert.assertNotNull(result);
        assertThat(actualList).containsAnyElementsOf(expectedList);
        assertThat(result.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()))
                .isNotNull();

        expectedList = List.copyOf(listOfInputDates.stream()
                .filter(Objects::nonNull)
                .map(instant -> extractDataComponentForDbResults(new Date(TimeUnit.MILLISECONDS.toMicros(instant)),
                        Calendar.WEEK_OF_YEAR, false))
                .map(String::valueOf)
                .collect(Collectors.toSet()));
        result = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.trend)
                        .aggInterval(CICD_AGG_INTERVAL.week)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.count).build(),
                VALUES_ONLY,
                null
        );
        Assert.assertNotNull(result);
        actualList = result.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .map(key -> new Date(TimeUnit.MILLISECONDS.toMicros(Long.parseLong(key))))
                .map(date -> extractDataComponentForDbResults(date, Calendar.WEEK_OF_YEAR, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        Assert.assertNotNull(result);
        Assertions.assertThat(actualList).containsAnyElementsOf(expectedList);
        assertThat(result.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()))
                .isNotNull();
        expectedList = listOfInputDates.stream()
                .filter(Objects::nonNull)
                .map(instant -> extractDataComponentForDbResults(new Date(TimeUnit.MILLISECONDS.toMicros(instant)),
                        Calendar.MONTH, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        result = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.trend)
                        .aggInterval(CICD_AGG_INTERVAL.month)
                        .stacks(List.of(CiCdPipelineJobRunsFilter.DISTINCT.job_name))
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.count).build(),
                VALUES_ONLY,
                null
        );
        actualList = result.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .map(key -> new Date(TimeUnit.MILLISECONDS.toMicros(Long.parseLong(key))))
                .map(date -> extractDataComponentForDbResults(date, Calendar.MONTH, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        Assert.assertNotNull(result);
        assertThat(actualList).containsAnyElementsOf(expectedList);
        assertThat(result.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()))
                .isNotNull();

        expectedList = listOfInputDates.stream()
                .filter(Objects::nonNull)
                .map(instant -> extractDataComponentForDbResults(new Date(TimeUnit.MILLISECONDS.toMicros(instant)),
                        Calendar.YEAR, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        result = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.trend)
                        .aggInterval(CICD_AGG_INTERVAL.year)
                        .stacks(List.of(CiCdPipelineJobRunsFilter.DISTINCT.job_name))
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.count).build(),
                VALUES_ONLY,
                null
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
                        Calendar.YEAR, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        DbListResponse<DbAggregationResult> dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.trend)
                        .aggInterval(CICD_AGG_INTERVAL.year)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.duration).build(),
                VALUES_ONLY,
                null
        );
        Assert.assertNotNull(dbAggsResponse);
        int count = new HashSet<>(expectedList).size();
        Assert.assertEquals(count, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(count, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(count, dbAggsResponse.getRecords().size());
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

        listOfInputDates = ciCdJobRuns.stream().map(CICDJobRun::getEndTime)
                .map(Instant::getEpochSecond)
                .collect(Collectors.toList());
        expectedList = listOfInputDates.stream()
                .filter(Objects::nonNull)
                .map(instant -> extractDataComponentForDbResults(new Date(TimeUnit.MILLISECONDS.toMicros(instant)),
                        Calendar.YEAR, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        result = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.job_end)
                        .aggInterval(CICD_AGG_INTERVAL.year)
                        .stacks(List.of(CiCdPipelineJobRunsFilter.DISTINCT.job_name))
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.count).build(),
                VALUES_ONLY,
                null
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
                        Calendar.MONTH, true))
                .map(String::valueOf)
                .collect(Collectors.toList());
        result = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.job_end)
                        .aggInterval(CICD_AGG_INTERVAL.quarter)
                        .stacks(List.of(CiCdPipelineJobRunsFilter.DISTINCT.job_name))
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.count).build(),
                VALUES_ONLY,
                null
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

        expectedList = listOfInputDates.stream()
                .filter(Objects::nonNull)
                .map(instant -> extractDataComponentForDbResults(new Date(TimeUnit.MILLISECONDS.toMicros(instant)),
                        Calendar.WEEK_OF_YEAR, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        result = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.job_end)
                        .aggInterval(CICD_AGG_INTERVAL.week)
                        .stacks(List.of(CiCdPipelineJobRunsFilter.DISTINCT.job_name))
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.count).build(),
                VALUES_ONLY,
                null
        );
        actualList = result.getRecords()
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

    public static CICDJobRun createCICDJobRun(CiCdJobRunsDatabaseService ciCdJobRunsDatabaseService, CICDJob cicdJob, String company, int i, Instant startTime, Integer duration, String parentJobFullName, Long parentJobRunNumber) throws SQLException {
        List<CICDJobRun.JobRunParam> params = contructJobRunParams();
        CICDJobRun.CICDJobRunBuilder bldr = CICDJobRun.builder()
                .cicdJobId(cicdJob.getId())
                .jobRunNumber(Long.valueOf(i))
                .status("SUCCESS")
                .startTime((startTime != null) ? startTime : Instant.now())
                .endTime(Instant.ofEpochSecond(1614499200))
                .duration((duration == null) ? random.nextInt(1000) : duration)
                .cicdUserId("user-jenkins-" + i)
                .source(CICDJobRun.Source.ANALYTICS_PERIODIC_PUSH)
                .referenceId(UUID.randomUUID().toString())
                .scmCommitIds(List.of("commit-id-1","commit-id-2","commit-id-3"))
                .params(params);

        if((StringUtils.isNotBlank(parentJobFullName) && (parentJobRunNumber != null))){
            bldr.triggers(Set.of(CICDJobTrigger.builder()
                    .id(parentJobFullName)
                    .type("UpstreamCause")
                    .buildNumber(String.valueOf(parentJobRunNumber))
                    .build()
            ));
        }
        CICDJobRun cicdJobRun = bldr.build();
        String id = ciCdJobRunsDatabaseService.insert(company, cicdJobRun);
        Assert.assertNotNull(id);
        CICDJobRun expected = cicdJobRun.toBuilder().id(UUID.fromString(id)).build();
        ciCdJobRuns.add(expected);
        return expected;
    }



}
