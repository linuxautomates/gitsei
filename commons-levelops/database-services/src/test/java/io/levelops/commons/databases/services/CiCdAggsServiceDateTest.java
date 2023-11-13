package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.CICDInstance;
import io.levelops.commons.databases.models.database.CICDJob;
import io.levelops.commons.databases.models.database.CICDJobRun;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.filters.CICD_AGG_INTERVAL;
import io.levelops.commons.databases.models.filters.CiCdJobRunsFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.organization.ProductsDatabaseService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
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
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.services.CiCdDateUtils.extractDataComponentForDbResults;
import static org.assertj.core.api.Assertions.assertThat;

@Log4j2
@SuppressWarnings("unused")
public class CiCdAggsServiceDateTest {
    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();
    private static final String company = "test";
    private static final boolean VALUES_ONLY = false;

    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;
    private static UserService userService;
    private static CiCdInstancesDatabaseService ciCdInstancesDatabaseService;
    private static CiCdJobsDatabaseService ciCdJobsDatabaseService;
    private static CiCdJobRunsDatabaseService ciCdJobRunsDatabaseService;
    private static CiCdJobConfigChangesDatabaseService ciCdJobConfigChangesDatabaseService;
    private static CiCdAggsService ciCdAggsService;
    private static IntegrationService integrationService;
    private static ProductsDatabaseService productsDatabaseService;

    private static Integration integration;
    private static Integration integration2;
    private static Integration integration3;
    private static String integrationId2;
    private static String integrationId3;
    private final List<CICDJobRun> ciCdJobRuns = new ArrayList<>();
    private final List<CICDJob> ciCdJobs = new ArrayList<>();

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
        productsDatabaseService = new ProductsDatabaseService(dataSource, MAPPER);
        productsDatabaseService.ensureTableExistence(company);
        userService.ensureTableExistence(company);
        ciCdInstancesDatabaseService.ensureTableExistence(company);
        ciCdJobsDatabaseService.ensureTableExistence(company);
        ciCdJobRunsDatabaseService.ensureTableExistence(company);
        ciCdJobConfigChangesDatabaseService.ensureTableExistence(company);
        ciCdAggsService.ensureTableExistence(company);
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

    private List<CiCdAggsServiceTest.JobDetails> fixJobRunTimestamps(List<CiCdAggsServiceTest.JobDetails> allJobDetails, Long offset) {
        if (CollectionUtils.isEmpty(allJobDetails)) {
            return allJobDetails;
        }
        return allJobDetails.stream()
                .map(x -> {
                    if (CollectionUtils.isEmpty(x.getRuns())) {
                        return x;
                    }
                    x.setRuns(
                            x.getRuns().stream()
                                    .map(r -> {
                                        r.setStartTime(r.getStartTime() + offset);
                                        return r;
                                    })
                                    .collect(Collectors.toList())
                    );
                    return x;
                })
                .collect(Collectors.toList());
    }

    @Test
    public void testGroupBy() throws SQLException, IOException {
        String data = ResourceUtils.getResourceAsString("json/databases/jenkins_plugin_result.json");
        List<CiCdAggsServiceTest.JobDetails> allJobDetails = MAPPER.readValue(data, MAPPER.getTypeFactory().constructCollectionType(List.class, CiCdAggsServiceTest.JobDetails.class));
        allJobDetails = fixJobRunTimestamps(allJobDetails, calculateOffset());
        CICDInstance cicdInstance = CiCdInstanceUtils.createCiCdInstance(ciCdInstancesDatabaseService, company, 0);
        setupJobsAndJobRuns(null, allJobDetails);
        setupJobsAndJobRuns(cicdInstance.getId(), allJobDetails);
        // region trend - count
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
        log.info("Input dates :{}", expectedList);
        DbListResponse<DbAggregationResult> result = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder().across(CiCdJobRunsFilter.DISTINCT.trend)
                        .calculation(CiCdJobRunsFilter.CALCULATION.count).build(),
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

        expectedList = listOfInputDates.stream()
                .filter(Objects::nonNull)
                .map(instant -> extractDataComponentForDbResults(new Date(TimeUnit.MILLISECONDS.toMicros(instant)),
                        Calendar.YEAR, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        log.info("Expected List:{}", expectedList);
        result = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder().across(CiCdJobRunsFilter.DISTINCT.trend)
                        .aggInterval(CICD_AGG_INTERVAL.year)
                        .calculation(CiCdJobRunsFilter.CALCULATION.count).build(),
                VALUES_ONLY
        );
        actualList = result.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .map(key -> new Date(TimeUnit.MILLISECONDS.toMicros(Long.parseLong(key))))
                .map(date -> extractDataComponentForDbResults(date, Calendar.YEAR, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        log.info("Actual List:{}", actualList);
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
        result = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder().across(CiCdJobRunsFilter.DISTINCT.trend)
                        .aggInterval(CICD_AGG_INTERVAL.week)
                        .calculation(CiCdJobRunsFilter.CALCULATION.count).build(),
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
        assertThat(expectedList.containsAll(actualList)).isEqualTo(true);
        assertThat(result.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()))
                .isNotNull();


        expectedList = listOfInputDates.stream()
                .filter(Objects::nonNull)
                .map(instant -> extractDataComponentForDbResults(new Date(TimeUnit.MILLISECONDS.toMicros(instant)),
                        Calendar.MONTH, true))
                .map(String::valueOf)
                .collect(Collectors.toList());
        result = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder().across(CiCdJobRunsFilter.DISTINCT.trend)
                        .aggInterval(CICD_AGG_INTERVAL.quarter)
                        .calculation(CiCdJobRunsFilter.CALCULATION.count).build(),
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

        expectedList = listOfInputDates.stream()
                .filter(Objects::nonNull)
                .map(instant -> extractDataComponentForDbResults(new Date(TimeUnit.MILLISECONDS.toMicros(instant)),
                        Calendar.DAY_OF_YEAR, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        result = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder().across(CiCdJobRunsFilter.DISTINCT.trend)
                        .calculation(CiCdJobRunsFilter.CALCULATION.count).build(),
                VALUES_ONLY
        );

        actualList = result.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .map(key -> new Date(TimeUnit.MILLISECONDS.toMicros(Long.parseLong(key))))
                .map(date -> extractDataComponentForDbResults(date, Calendar.DAY_OF_YEAR, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        Assert.assertNotNull(result);
        assertThat(expectedList.containsAll(actualList)).isEqualTo(true);
        assertThat(result.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()))
                .isNotNull();
        expectedList = listOfInputDates.stream()
                .filter(Objects::nonNull)
                .map(instant -> extractDataComponentForDbResults(new Date(TimeUnit.MILLISECONDS.toMicros(instant)),
                        Calendar.DAY_OF_YEAR, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        result = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder().across(CiCdJobRunsFilter.DISTINCT.trend)
                        .stacks(List.of(CiCdJobRunsFilter.DISTINCT.job_name))
                        .calculation(CiCdJobRunsFilter.CALCULATION.count).build(),
                VALUES_ONLY
        );
        actualList = result.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .map(key -> new Date(TimeUnit.MILLISECONDS.toMicros(Long.parseLong(key))))
                .map(date -> extractDataComponentForDbResults(date, Calendar.DAY_OF_YEAR, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        Assert.assertNotNull(result);
        assertThat(expectedList.containsAll(actualList)).isEqualTo(true);
        assertThat(result.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()))
                .isNotNull();
        assertThat(result.getRecords().stream().map(DbAggregationResult::getStacks).collect(Collectors.toList())).isNotNull();

        expectedList = listOfInputDates.stream()
                .filter(Objects::nonNull)
                .map(instant -> extractDataComponentForDbResults(new Date(TimeUnit.MILLISECONDS.toMicros(instant)),
                        Calendar.MONTH, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        result = ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder().across(CiCdJobRunsFilter.DISTINCT.trend)
                        .stacks(List.of(CiCdJobRunsFilter.DISTINCT.job_name))
                        .aggInterval(CICD_AGG_INTERVAL.month)
                        .calculation(CiCdJobRunsFilter.CALCULATION.count).build(),
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
        assertThat(result.getRecords().stream().map(DbAggregationResult::getStacks).collect(Collectors.toList())).isNotNull();
    }


    private long calculateOffset() {
        long diff = Instant.now().getEpochSecond() - 1593062362;
        long days = diff / (TimeUnit.DAYS.toSeconds(1));
        long offset = days * (TimeUnit.DAYS.toSeconds(1));
        return offset;
    }

}
