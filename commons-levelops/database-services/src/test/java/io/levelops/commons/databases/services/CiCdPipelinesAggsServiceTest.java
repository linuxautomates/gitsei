package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.CICDInstance;
import io.levelops.commons.databases.models.database.CICDJob;
import io.levelops.commons.databases.models.database.CICDJobRun;
import io.levelops.commons.databases.models.database.CICDJobRunDTO;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.organization.DBOrgProduct;
import io.levelops.commons.databases.models.filters.CICD_AGG_INTERVAL;
import io.levelops.commons.databases.models.filters.CICD_TYPE;
import io.levelops.commons.databases.models.filters.CiCdJobQualifiedName;
import io.levelops.commons.databases.models.filters.CiCdJobRunsFilter;
import io.levelops.commons.databases.models.filters.CiCdPipelineJobRunsFilter;
import io.levelops.commons.databases.models.response.AcrossUniqueKey;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.organization.ProductsDatabaseService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("unused")
public class CiCdPipelinesAggsServiceTest {
    private static final Integer PAGE_NUMBER = 0;
    private static final Integer PAGE_SIZE = 300;
    private final static ObjectMapper MAPPER = DefaultObjectMapper.get();
    private final static Map<String, SortingOrder> DEFAULT_SORT_BY = Collections.emptyMap();
    private final String company = "test";
    private static final boolean VALUES_ONLY = false;

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
        return cicdJob.toBuilder().id(UUID.fromString(cicdJobId)).build();
    }

//     @Test disabled due to flakiness - probably due to time differences... needs to be set in a more deterministic way
    public void testGroupByAndCalculateCiCdPipelineJobs() throws SQLException {
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
            CICDJobRun cicdJobFalconRun = CICDJobRunUtils.createCICDJobRun(ciCdJobRunsDatabaseService, jobFalcon, company, i, day, 70, null, null);

            CICDJobRun cicdJob1Run = CICDJobRunUtils.createCICDJobRun(ciCdJobRunsDatabaseService, job1, company, i, day, 40, jobFalcon.getJobFullName(), cicdJobFalconRun.getJobRunNumber());
            //CiCdJobRunTriggerUtils.createCiCdJobRunTrigger(ciCdJobRunTriggersDBService, company, cicdJob1Run, jobFalcon.getJobFullName(), cicdJobFalconRun.getJobRunNumber());

            CICDJobRun cicdJob2Run = CICDJobRunUtils.createCICDJobRun(ciCdJobRunsDatabaseService, job2, company, i, day, 50, jobFalcon.getJobFullName(), cicdJobFalconRun.getJobRunNumber());
            //CiCdJobRunTriggerUtils.createCiCdJobRunTrigger(ciCdJobRunTriggersDBService, company, cicdJob2Run, jobFalcon.getJobFullName(), cicdJobFalconRun.getJobRunNumber());

            CICDJobRun cicdJob3Run = CICDJobRunUtils.createCICDJobRun(ciCdJobRunsDatabaseService, job3, company, i, day, 60, jobFalcon.getJobFullName(), cicdJobFalconRun.getJobRunNumber());
            //CiCdJobRunTriggerUtils.createCiCdJobRunTrigger(ciCdJobRunTriggersDBService, company, cicdJob3Run, jobFalcon.getJobFullName(), cicdJobFalconRun.getJobRunNumber());

            CICDJobRun cicdJob1_1Run = CICDJobRunUtils.createCICDJobRun(ciCdJobRunsDatabaseService, job1_1, company, i, day, 30, job1.getJobFullName(), cicdJob1Run.getJobRunNumber());
            //CiCdJobRunTriggerUtils.createCiCdJobRunTrigger(ciCdJobRunTriggersDBService, company, cicdJob1_1Run, job1.getJobFullName(), cicdJob1Run.getJobRunNumber());

            CICDJobRun cicdJob1_2Run = CICDJobRunUtils.createCICDJobRun(ciCdJobRunsDatabaseService, job1_2, company, i, day, 20, job1.getJobFullName(), cicdJob1Run.getJobRunNumber());
            //CiCdJobRunTriggerUtils.createCiCdJobRunTrigger(ciCdJobRunTriggersDBService, company, cicdJob1_2Run, job1.getJobFullName(), cicdJob1Run.getJobRunNumber());

            CICDJobRun cicdJob1_3Run = CICDJobRunUtils.createCICDJobRun(ciCdJobRunsDatabaseService, job1_3, company, i, day, 10, job1.getJobFullName(), cicdJob1Run.getJobRunNumber());
            //CiCdJobRunTriggerUtils.createCiCdJobRunTrigger(ciCdJobRunTriggersDBService, company, cicdJob1_3Run, job1.getJobFullName(), cicdJob1Run.getJobRunNumber());
        }
        // endregion

        // region qualified name - count - no parent id
        DbListResponse<DbAggregationResult> dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.qualified_job_name).calculation(CiCdPipelineJobRunsFilter.CALCULATION.count).build(),
                VALUES_ONLY,
                null
        );

        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(1, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getRecords().size());
        List<DbAggregationResult> expected = List.of(
                DbAggregationResult.builder().key("falcon").additionalKey("instance-name-0").count(70L).build()
        );
        verifyRecords(dbAggsResponse.getRecords(), expected, false);
        // endregion

        // region qualified name - count - with parent id - falcon
        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.qualified_job_name)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.count)
                        .parentCiCdJobIds(List.of(jobFalcon.getId().toString()))
                        .build(),
                VALUES_ONLY,
                null
        );

        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(3, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(3, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(3, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("job1").additionalKey("instance-name-0").count(40L).build(),
                DbAggregationResult.builder().key("job2").additionalKey("instance-name-0").count(10L).build(),
                DbAggregationResult.builder().key("job3").additionalKey("instance-name-0").count(10L).build()
        );
        verifyRecords(dbAggsResponse.getRecords(), expected, false);
        // endregion

        // region instance name - count
        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.instance_name)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.count)
                        .build(),
                VALUES_ONLY,
                null
        );

        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(1, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("instance-name-0").count(70L).build()
        );
        verifyRecords(dbAggsResponse.getRecords(), expected, false);
        // endregion

        // region instance name - count - filter by type
        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.instance_name)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.count)
                        .types(List.of(CICD_TYPE.jenkins))
                        .build(),
                VALUES_ONLY,
                null
        );

        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(1, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("instance-name-0").count(70L).build()
        );
        verifyRecords(dbAggsResponse.getRecords(), expected, false);
        // endregion

        // region instance name - count - filter by type
        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.instance_name)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.count)
                        .types(List.of(CICD_TYPE.azure_devops))
                        .build(),
                VALUES_ONLY,
                null
        );

        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(0, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(0, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("instance-name-0").count(0L).build()
        );
        // endregion

        // region job normalized full name - count - with partial type filter
        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder()
                        .partialMatch(Map.of("type", Map.of("$contains", "jenkins")))
                        .across(CiCdPipelineJobRunsFilter.DISTINCT.job_normalized_full_name)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.count)
                        .build(),
                VALUES_ONLY,
                null
        );

        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(1, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("Folder1/Folder2/BBMaven1New/falcon/master").count(70L).build()
        );
        verifyRecords(dbAggsResponse.getRecords(), expected, false);
        // endregion

        // region qualified name - count - with parent id - job1
        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.qualified_job_name)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.count)
                        .parentCiCdJobIds(List.of(job1.getId().toString()))
                        .build(),
                VALUES_ONLY,
                null
        );

        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(3, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(3, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(3, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("job1.1").additionalKey("instance-name-0").count(10L).build(),
                DbAggregationResult.builder().key("job1.2").additionalKey("instance-name-0").count(10L).build(),
                DbAggregationResult.builder().key("job1.3").additionalKey("instance-name-0").count(10L).build()
        );
        verifyRecords(dbAggsResponse.getRecords(), expected, false);
        // endregion

        // region across qualified_job_name - stack job_name - count
        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.qualified_job_name)
                        .stacks(List.of(CiCdPipelineJobRunsFilter.DISTINCT.job_name))
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.count)
                        .build(),
                VALUES_ONLY,
                null
        );

        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(1, dbAggsResponse.getRecords().size());
        // end region

        // region across instance_name - stack job_name - count
        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.job_status)
                        .stacks(List.of(CiCdPipelineJobRunsFilter.DISTINCT.job_name))
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.count)
                        .build(),
                VALUES_ONLY,
                null
        );

        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(1, dbAggsResponse.getRecords().size());

        // region across instance_name - stack job_name - count
        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.instance_name)
                        .stacks(List.of(CiCdPipelineJobRunsFilter.DISTINCT.job_name))
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.count)
                        .build(),
                VALUES_ONLY,
                null
        );

        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(1, dbAggsResponse.getRecords().size());
        // end region

        // region across instance_name - stack cicd_job_id - count
        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.instance_name)
                        .stacks(List.of(CiCdPipelineJobRunsFilter.DISTINCT.cicd_job_id))
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.count)
                        .build(),
                VALUES_ONLY,
                null
        );

        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(1, dbAggsResponse.getRecords().size());
        // end region

        // region across cicd_user_id - stack cicd_job_id - count
        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.cicd_user_id)
                        .stacks(List.of(CiCdPipelineJobRunsFilter.DISTINCT.cicd_job_id))
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.count)
                        .build(),
                VALUES_ONLY,
                null
        );

        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(10, dbAggsResponse.getRecords().size());
        // end region

        // region across cicd_user_id - stack job_normalized_full_name - count
        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.cicd_user_id)
                        .stacks(List.of(CiCdPipelineJobRunsFilter.DISTINCT.job_normalized_full_name))
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.count)
                        .build(),
                VALUES_ONLY,
                null
        );

        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(10, dbAggsResponse.getRecords().size());
        // end region

        // region qualified name - count - with parent id - job1.1
        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.qualified_job_name)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.count)
                        .parentCiCdJobIds(List.of(job1_1.getId().toString()))
                        .build(),
                VALUES_ONLY,
                null
        );

        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(0, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(0, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(0, dbAggsResponse.getRecords().size());
        Assert.assertTrue(CollectionUtils.isEmpty(dbAggsResponse.getRecords()));
        // endregion

        // region job name - count - no parent id
        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.job_name).calculation(CiCdPipelineJobRunsFilter.CALCULATION.count).build(),
                VALUES_ONLY,
                null
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(1, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("falcon").count(70L).build()
        );
        verifyRecords(dbAggsResponse.getRecords(), expected, false);
        // endregion

        // region job normalized full name - count

        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder()
                        .across(CiCdPipelineJobRunsFilter.DISTINCT.job_normalized_full_name)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.count)
                        .build(),
                VALUES_ONLY,
                null
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(1, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("Folder1/Folder2/BBMaven1New/falcon/master").count(70L).build()
        );
        verifyRecords(dbAggsResponse.getRecords(), expected, false);
        // endregion

        // region job normalized full name - count - stacks cicd user id

        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder()
                        .across(CiCdPipelineJobRunsFilter.DISTINCT.job_normalized_full_name)
                        .stacks(List.of(CiCdPipelineJobRunsFilter.DISTINCT.cicd_user_id))
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.count)
                        .build(),
                VALUES_ONLY,
                null
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(1, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("Folder1/Folder2/BBMaven1New/falcon/master").count(70L)
                        .stacks(
                                List.of(
                                        DbAggregationResult.builder().key("user-jenkins-0").count(7L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-1").count(7L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-2").count(7L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-3").count(7L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-4").count(7L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-5").count(7L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-6").count(7L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-7").count(7L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-8").count(7L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-9").count(7L).build()
                                )
                        )
                        .build()
        );
        verifyRecords(dbAggsResponse.getRecords(), expected, false);
        // endregion

        // region job normalized full name - count - stacks cicd user id - with job normalized full name filter
        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().jobNormalizedFullNames(List.of("Folder1/Folder2/BBMaven1New/falcon/master"))
                        .across(CiCdPipelineJobRunsFilter.DISTINCT.job_normalized_full_name)
                        .stacks(List.of(CiCdPipelineJobRunsFilter.DISTINCT.cicd_user_id))
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.count)
                        .build(),
                VALUES_ONLY,
                null
        );

        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(1, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("Folder1/Folder2/BBMaven1New/falcon/master").count(70L)
                        .stacks(
                                List.of(
                                        DbAggregationResult.builder().key("user-jenkins-0").count(7L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-1").count(7L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-2").count(7L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-3").count(7L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-4").count(7L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-5").count(7L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-6").count(7L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-7").count(7L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-8").count(7L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-9").count(7L).build()
                                )
                        )
                        .build()
        );
        verifyRecords(dbAggsResponse.getRecords(), expected, false);
        // endregion

        // region job normalized full name - count - with invalid job normalized full name filter

        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().jobNormalizedFullNames(List.of("invalid"))
                        .across(CiCdPipelineJobRunsFilter.DISTINCT.job_normalized_full_name)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.count)
                        .build(),
                VALUES_ONLY,
                null
        );

        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(0, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(0, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(0, dbAggsResponse.getRecords().size());
        // endregion

        // region job normalized full name - count - with partial job normalized full name filter
        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder()
                        .partialMatch(Map.of("job_normalized_full_name", Map.of("$contains", "BBMaven1New")))
                        .across(CiCdPipelineJobRunsFilter.DISTINCT.job_normalized_full_name)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.count)
                        .build(),
                VALUES_ONLY,
                null
        );

        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(1, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("Folder1/Folder2/BBMaven1New/falcon/master").count(70L).build()
        );
        verifyRecords(dbAggsResponse.getRecords(), expected, false);
        // endregion

        // region job normalized full name - count - with invalid partial job normalized full name filter
        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder()
                        .partialMatch(Map.of("job_normalized_full_name", Map.of("$contains", "BB1New")))
                        .across(CiCdPipelineJobRunsFilter.DISTINCT.job_normalized_full_name)
                        .stacks(List.of(CiCdPipelineJobRunsFilter.DISTINCT.cicd_user_id))
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.count)
                        .build(),
                VALUES_ONLY,
                null
        );

        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(0, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(0, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(0, dbAggsResponse.getRecords().size());

        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder()
                        .partialMatch(Map.of("job_normalized_full_name", Map.of("$contains", "BB';DROP TABLE test.cicd_jobs;--")))
                        .across(CiCdPipelineJobRunsFilter.DISTINCT.job_normalized_full_name)
                        .stacks(List.of(CiCdPipelineJobRunsFilter.DISTINCT.cicd_user_id))
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.count)
                        .build(),
                VALUES_ONLY,
                null
        );
        DefaultObjectMapper.prettyPrint(dbAggsResponse);
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(0, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(0, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(0, dbAggsResponse.getRecords().size());
        Optional<CICDJob> job = ciCdJobsDatabaseService.get(company, job1.getId().toString());
        Assert.assertTrue(job.isPresent());

        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder()
                        .across(CiCdPipelineJobRunsFilter.DISTINCT.job_normalized_full_name)
                        .jobNormalizedFullNames(List.of("Folder1/Folder2/BBMaven1New/falcon/master"))
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.count)
                        .build(),
                VALUES_ONLY,
                null
        );
        DefaultObjectMapper.prettyPrint(dbAggsResponse);
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(1, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getRecords().size());

        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder()
                        .across(CiCdPipelineJobRunsFilter.DISTINCT.job_normalized_full_name)
                        .jobNormalizedFullNames(List.of("Folder1/Folder2/BBMaven1New/falcon/master"))
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.duration)
                        .build(),
                VALUES_ONLY,
                null
        );
        DefaultObjectMapper.prettyPrint(dbAggsResponse);
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(1, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getRecords().size());
        // endregion

        // region job name - count - with parent id - falcon
        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.job_name)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.count)
                        .parentCiCdJobIds(List.of(jobFalcon.getId().toString()))
                        .build(),
                VALUES_ONLY,
                null
        );

        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(3, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(3, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(3, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("job1").count(40L).build(),
                DbAggregationResult.builder().key("job2").count(10L).build(),
                DbAggregationResult.builder().key("job3").count(10L).build()
        );
        verifyRecords(dbAggsResponse.getRecords(), expected, false);
        // endregion

        // region job name - count - with parent id - job1
        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.job_name)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.count)
                        .parentCiCdJobIds(List.of(job1.getId().toString()))
                        .build(),
                VALUES_ONLY,
                null
        );

        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(3, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(3, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(3, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("job1.1").count(10L).build(),
                DbAggregationResult.builder().key("job1.2").count(10L).build(),
                DbAggregationResult.builder().key("job1.3").count(10L).build()
        );
        verifyRecords(dbAggsResponse.getRecords(), expected, false);
        // endregion

        // region job name - count - with parent id - job1.1
        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.job_name)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.count)
                        .parentCiCdJobIds(List.of(job1_1.getId().toString()))
                        .build(),
                VALUES_ONLY,
                null
        );

        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(0, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(0, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(0, dbAggsResponse.getRecords().size());
        Assert.assertTrue(CollectionUtils.isEmpty(dbAggsResponse.getRecords()));
        // endregion

        // region qualified name - count - no parent id
        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.cicd_job_id).calculation(CiCdPipelineJobRunsFilter.CALCULATION.count).build(),
                VALUES_ONLY,
                null
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(1, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("falcon").additionalKey("instance-name-0").ciCdJobId(jobFalcon.getId().toString()).count(70L).build()
        );
        verifyRecords(dbAggsResponse.getRecords(), expected, false);
        // endregion

        // region qualified name - count - with parent id - falcon
        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.cicd_job_id)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.count)
                        .parentCiCdJobIds(List.of(jobFalcon.getId().toString()))
                        .build(),
                VALUES_ONLY,
                null
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(3, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(3, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(3, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("job1").additionalKey("instance-name-0").ciCdJobId(job1.getId().toString()).count(40L).build(),
                DbAggregationResult.builder().key("job2").additionalKey("instance-name-0").ciCdJobId(job2.getId().toString()).count(10L).build(),
                DbAggregationResult.builder().key("job3").additionalKey("instance-name-0").ciCdJobId(job3.getId().toString()).count(10L).build()
        );
        verifyRecords(dbAggsResponse.getRecords(), expected, false);
        // endregion

        // region qualified name - count - with parent id - job1
        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.cicd_job_id)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.count)
                        .parentCiCdJobIds(List.of(job1.getId().toString()))
                        .build(),
                VALUES_ONLY,
                null
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(3, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(3, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(3, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("job1.1").additionalKey("instance-name-0").ciCdJobId(job1_1.getId().toString()).count(10L).build(),
                DbAggregationResult.builder().key("job1.2").additionalKey("instance-name-0").ciCdJobId(job1_2.getId().toString()).count(10L).build(),
                DbAggregationResult.builder().key("job1.3").additionalKey("instance-name-0").ciCdJobId(job1_3.getId().toString()).count(10L).build()
        );
        verifyRecords(dbAggsResponse.getRecords(), expected, false);
        // endregion

        // region qualified name - count - with parent id - job1.1
        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.cicd_job_id)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.count)
                        .parentCiCdJobIds(List.of(job1_1.getId().toString()))
                        .build(),
                VALUES_ONLY,
                null
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(0, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(0, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(0, dbAggsResponse.getRecords().size());
        Assert.assertTrue(CollectionUtils.isEmpty(dbAggsResponse.getRecords()));
        // endregion

        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.job_end).aggInterval(CICD_AGG_INTERVAL.day).calculation(CiCdPipelineJobRunsFilter.CALCULATION.count).build(),
                VALUES_ONLY,
                null
        );
        DefaultObjectMapper.prettyPrint(dbAggsResponse);
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(1, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("1614450600").additionalKey("28-2-2021").count(70L).build()
        );
        verifyRecords(dbAggsResponse.getRecords(), expected, true);

        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.job_end)
                        .aggInterval(CICD_AGG_INTERVAL.week).calculation(CiCdPipelineJobRunsFilter.CALCULATION.count).build(),
                VALUES_ONLY,
                null
        );
        DefaultObjectMapper.prettyPrint(dbAggsResponse);
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(1, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("1613980800").additionalKey("8-2021").count(70L).build()
        );
        verifyRecords(dbAggsResponse.getRecords(), expected, true);

        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.job_end).aggInterval(CICD_AGG_INTERVAL.month).calculation(CiCdPipelineJobRunsFilter.CALCULATION.count).build(),
                VALUES_ONLY,
                null
        );
        DefaultObjectMapper.prettyPrint(dbAggsResponse);
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(1, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getRecords().size());

        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.job_end).aggInterval(CICD_AGG_INTERVAL.quarter).calculation(CiCdPipelineJobRunsFilter.CALCULATION.count).build(),
                VALUES_ONLY,
                null
        );
        DefaultObjectMapper.prettyPrint(dbAggsResponse);
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(1, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("1609488000").additionalKey("Q1-2021").count(70L).build()
        );
        verifyRecords(dbAggsResponse.getRecords(), expected, true);

        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.job_end).aggInterval(CICD_AGG_INTERVAL.year).calculation(CiCdPipelineJobRunsFilter.CALCULATION.count).build(),
                VALUES_ONLY,
                null
        );
        DefaultObjectMapper.prettyPrint(dbAggsResponse);
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(1, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("1609488000").additionalKey("2021").count(70L).build()
        );
        verifyRecords(dbAggsResponse.getRecords(), expected, true);

        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.job_end).aggInterval(CICD_AGG_INTERVAL.year).calculation(CiCdPipelineJobRunsFilter.CALCULATION.count).build(),
                VALUES_ONLY,
                null
        );
        DefaultObjectMapper.prettyPrint(dbAggsResponse);
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(1, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("1609488000").additionalKey("2021").count(70L).build()
        );
        verifyRecords(dbAggsResponse.getRecords(), expected, true);

        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.project_name)
                        .projects(List.of("project-1"))
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.count).build(),
                VALUES_ONLY,
                null
        );
        DefaultObjectMapper.prettyPrint(dbAggsResponse);
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(1, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("project-1").count(70L).build()
        );
        Assert.assertEquals(expected, dbAggsResponse.getRecords());

        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.project_name)
                        .stacks(List.of(CiCdPipelineJobRunsFilter.DISTINCT.job_name))
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.count).build(),
                VALUES_ONLY,
                null
        );
        DefaultObjectMapper.prettyPrint(dbAggsResponse);
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(1, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("project-1").count(70L)
                        .stacks(List.of(DbAggregationResult.builder().key("falcon").count(70L).build())).build()
        );
        Assert.assertEquals(expected, dbAggsResponse.getRecords());

        // region trend - count - with parent id - falcon
        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.trend)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.count)
                        .parentCiCdJobIds(List.of(jobFalcon.getId().toString()))
                        .build(),
                VALUES_ONLY,
                null
        );
        Assert.assertNotNull(dbAggsResponse);
        Assertions.assertThat(dbAggsResponse.getCount().intValue()).isBetween(9, 10);
        Assertions.assertThat(dbAggsResponse.getTotalCount().intValue()).isBetween(9, 10);
        Assertions.assertThat(dbAggsResponse.getRecords().size()).isBetween(9, 10);
        // endregion

        // region trend - count - with parent id - job1
        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.trend)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.count)
                        .parentCiCdJobIds(List.of(job1.getId().toString()))
                        .build(),
                VALUES_ONLY,
                null
        );

        Assert.assertNotNull(dbAggsResponse);
        Assertions.assertThat(dbAggsResponse.getCount().intValue()).isBetween(9, 10);
        Assertions.assertThat(dbAggsResponse.getTotalCount().intValue()).isBetween(9, 10);
        Assertions.assertThat(dbAggsResponse.getRecords().size()).isBetween(9, 10);
        // endregion

        // region trend - count - with parent id - job1.1
        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.trend)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.count)
                        .parentCiCdJobIds(List.of(job1_1.getId().toString()))
                        .build(),
                VALUES_ONLY,
                null
        );

        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(0, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(0, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(0, dbAggsResponse.getRecords().size());
        Assert.assertTrue(CollectionUtils.isEmpty(dbAggsResponse.getRecords()));
        // endregion

        // region qualified name - count - stack cicd_user_id - no parent id
        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.qualified_job_name)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.count)
                        .stacks(List.of(CiCdPipelineJobRunsFilter.DISTINCT.cicd_user_id))
                        .build(),
                VALUES_ONLY,
                null
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(1, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("falcon").additionalKey("instance-name-0").count(70L)
                        .stacks(
                                List.of(
                                        DbAggregationResult.builder().key("user-jenkins-0").count(7L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-1").count(7L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-2").count(7L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-3").count(7L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-4").count(7L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-5").count(7L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-6").count(7L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-7").count(7L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-8").count(7L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-9").count(7L).build()
                                )
                        )
                        .build()
        );
        verifyRecords(dbAggsResponse.getRecords(), expected, false);
        // endregion

        // region qualified name - count - stack cicd_user_id - with parent id - falcon
        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.qualified_job_name)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.count)
                        .stacks(List.of(CiCdPipelineJobRunsFilter.DISTINCT.cicd_user_id))
                        .parentCiCdJobIds(List.of(jobFalcon.getId().toString()))
                        .build(),
                VALUES_ONLY,
                null
        );

        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(3, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(3, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(3, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("job1").additionalKey("instance-name-0").count(40L)
                        .stacks(
                                List.of(
                                        DbAggregationResult.builder().key("user-jenkins-0").count(4L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-1").count(4L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-2").count(4L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-3").count(4L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-4").count(4L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-5").count(4L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-6").count(4L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-7").count(4L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-8").count(4L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-9").count(4L).build()

                                )
                        )
                        .build(),
                DbAggregationResult.builder().key("job2").additionalKey("instance-name-0").count(10L)
                        .stacks(
                                List.of(
                                        DbAggregationResult.builder().key("user-jenkins-0").count(1L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-1").count(1L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-2").count(1L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-3").count(1L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-4").count(1L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-5").count(1L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-6").count(1L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-7").count(1L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-8").count(1L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-9").count(1L).build()

                                )
                        )
                        .build(),
                DbAggregationResult.builder().key("job3").additionalKey("instance-name-0").count(10L)
                        .stacks(
                                List.of(
                                        DbAggregationResult.builder().key("user-jenkins-0").count(1L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-1").count(1L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-2").count(1L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-3").count(1L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-4").count(1L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-5").count(1L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-6").count(1L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-7").count(1L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-8").count(1L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-9").count(1L).build()

                                )
                        )
                        .build()
        );
        verifyRecords(dbAggsResponse.getRecords(), expected, false);
        // endregion

        // region qualified name - count - stack cicd_user_id - with parent id - job1
        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.qualified_job_name)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.count)
                        .stacks(List.of(CiCdPipelineJobRunsFilter.DISTINCT.cicd_user_id))
                        .parentCiCdJobIds(List.of(job1.getId().toString()))
                        .build(),
                VALUES_ONLY,
                null
        );

        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(3, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(3, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(3, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("job1.1").additionalKey("instance-name-0").count(10L)
                        .stacks(
                                List.of(
                                        DbAggregationResult.builder().key("user-jenkins-0").count(1L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-1").count(1L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-2").count(1L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-3").count(1L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-4").count(1L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-5").count(1L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-6").count(1L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-7").count(1L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-8").count(1L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-9").count(1L).build()

                                )
                        )
                        .build(),
                DbAggregationResult.builder().key("job1.2").additionalKey("instance-name-0").count(10L)
                        .stacks(
                                List.of(
                                        DbAggregationResult.builder().key("user-jenkins-0").count(1L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-1").count(1L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-2").count(1L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-3").count(1L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-4").count(1L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-5").count(1L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-6").count(1L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-7").count(1L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-8").count(1L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-9").count(1L).build()

                                )
                        )
                        .build(),
                DbAggregationResult.builder().key("job1.3").additionalKey("instance-name-0").count(10L)
                        .stacks(
                                List.of(
                                        DbAggregationResult.builder().key("user-jenkins-0").count(1L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-1").count(1L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-2").count(1L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-3").count(1L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-4").count(1L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-5").count(1L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-6").count(1L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-7").count(1L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-8").count(1L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-9").count(1L).build()

                                )
                        )
                        .build()
        );
        verifyRecords(dbAggsResponse.getRecords(), expected, false);
        // endregion

        // region qualified name - count - stack cicd_user_id - with parent id - job1.1
        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.qualified_job_name)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.count)
                        .stacks(List.of(CiCdPipelineJobRunsFilter.DISTINCT.cicd_user_id))
                        .parentCiCdJobIds(List.of(job1_1.getId().toString()))
                        .build(),
                VALUES_ONLY,
                null
        );

        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(0, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(0, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(0, dbAggsResponse.getRecords().size());
        Assert.assertTrue(CollectionUtils.isEmpty(dbAggsResponse.getRecords()));
        // endregion

        // region qualified name - count - stack status - no parent id
        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.qualified_job_name)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.count)
                        .stacks(List.of(CiCdPipelineJobRunsFilter.DISTINCT.job_status))
                        .build(),
                VALUES_ONLY,
                null
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(1, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("falcon").additionalKey("instance-name-0").count(70L)
                        .stacks(
                                List.of(
                                        DbAggregationResult.builder().key("SUCCESS").count(70L).build()
                                )
                        )
                        .build()
        );
        verifyRecords(dbAggsResponse.getRecords(), expected, false);
        // endregion

        // region qualified name - count - stack status - with parent id - falcon
        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.qualified_job_name)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.count)
                        .stacks(List.of(CiCdPipelineJobRunsFilter.DISTINCT.job_status))
                        .parentCiCdJobIds(List.of(jobFalcon.getId().toString()))
                        .build(),
                VALUES_ONLY,
                null
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(3, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(3, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(3, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("job1").additionalKey("instance-name-0").count(40L)
                        .stacks(
                                List.of(
                                        DbAggregationResult.builder().key("SUCCESS").count(40L).build()
                                )
                        )
                        .build(),
                DbAggregationResult.builder().key("job2").additionalKey("instance-name-0").count(10L)
                        .stacks(
                                List.of(
                                        DbAggregationResult.builder().key("SUCCESS").count(10L).build()
                                )
                        )
                        .build(),
                DbAggregationResult.builder().key("job3").additionalKey("instance-name-0").count(10L)
                        .stacks(
                                List.of(
                                        DbAggregationResult.builder().key("SUCCESS").count(10L).build()
                                )
                        )
                        .build()
        );
        verifyRecords(dbAggsResponse.getRecords(), expected, false);
        // endregion

        // region qualified name - count - stack status - with parent id - job1
        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.qualified_job_name)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.count)
                        .stacks(List.of(CiCdPipelineJobRunsFilter.DISTINCT.job_status))
                        .parentCiCdJobIds(List.of(job1.getId().toString()))
                        .build(),
                VALUES_ONLY,
                null
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(3, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(3, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(3, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("job1.1").additionalKey("instance-name-0").count(10L)
                        .stacks(
                                List.of(
                                        DbAggregationResult.builder().key("SUCCESS").count(10L).build()
                                )
                        )
                        .build(),
                DbAggregationResult.builder().key("job1.2").additionalKey("instance-name-0").count(10L)
                        .stacks(
                                List.of(
                                        DbAggregationResult.builder().key("SUCCESS").count(10L).build()
                                )
                        )
                        .build(),
                DbAggregationResult.builder().key("job1.3").additionalKey("instance-name-0").count(10L)
                        .stacks(
                                List.of(
                                        DbAggregationResult.builder().key("SUCCESS").count(10L).build()
                                )
                        )
                        .build()
        );
        verifyRecords(dbAggsResponse.getRecords(), expected, false);
        // endregion

        // region qualified name - count - stack status - with parent id - job1.1
        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.qualified_job_name)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.count)
                        .stacks(List.of(CiCdPipelineJobRunsFilter.DISTINCT.job_status))
                        .parentCiCdJobIds(List.of(job1_1.getId().toString()))
                        .build(),
                VALUES_ONLY,
                null
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(0, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(0, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(0, dbAggsResponse.getRecords().size());
        Assert.assertTrue(CollectionUtils.isEmpty(dbAggsResponse.getRecords()));
        // endregion

        // region trend - count - stack cicd_user_id - no parent id
        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.trend)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.count)
                        .stacks(List.of(CiCdPipelineJobRunsFilter.DISTINCT.cicd_user_id))
                        .build(), Map.of("trend", SortingOrder.ASC),
                VALUES_ONLY,
                null
        );
        DefaultObjectMapper.prettyPrint(dbAggsResponse);
        Assert.assertNotNull(dbAggsResponse);
        Assertions.assertThat(dbAggsResponse.getCount().intValue()).isBetween(9, 10);
        Assertions.assertThat(dbAggsResponse.getTotalCount().intValue()).isBetween(9, 10);
        Assertions.assertThat(dbAggsResponse.getRecords().size()).isBetween(9, 10);
        expected = List.of(
                DbAggregationResult.builder().key("falcon").count(7L)
                        .stacks(
                                List.of(
                                        DbAggregationResult.builder().key("user-jenkins-0").count(7L).build()
                                )
                        )
                        .build(),
                DbAggregationResult.builder().key("falcon").count(7L)
                        .stacks(
                                List.of(
                                        DbAggregationResult.builder().key("user-jenkins-1").count(7L).build()
                                )
                        )
                        .build(),
                DbAggregationResult.builder().key("falcon").count(7L)
                        .stacks(
                                List.of(
                                        DbAggregationResult.builder().key("user-jenkins-2").count(7L).build()
                                )
                        )
                        .build(),
                DbAggregationResult.builder().key("falcon").count(7L)
                        .stacks(
                                List.of(
                                        DbAggregationResult.builder().key("user-jenkins-3").count(7L).build()
                                )
                        )
                        .build(),
                DbAggregationResult.builder().key("falcon").count(7L)
                        .stacks(
                                List.of(
                                        DbAggregationResult.builder().key("user-jenkins-4").count(7L).build()
                                )
                        )
                        .build(),
                DbAggregationResult.builder().key("falcon").count(7L)
                        .stacks(
                                List.of(
                                        DbAggregationResult.builder().key("user-jenkins-5").count(7L).build()
                                )
                        )
                        .build(),
                DbAggregationResult.builder().key("falcon").count(7L)
                        .stacks(
                                List.of(
                                        DbAggregationResult.builder().key("user-jenkins-6").count(7L).build()
                                )
                        )
                        .build(),
                DbAggregationResult.builder().key("falcon").count(7L)
                        .stacks(
                                List.of(
                                        DbAggregationResult.builder().key("user-jenkins-7").count(7L).build()
                                )
                        )
                        .build(),
                DbAggregationResult.builder().key("falcon").count(7L)
                        .stacks(
                                List.of(
                                        DbAggregationResult.builder().key("user-jenkins-8").count(7L).build()
                                )
                        )
                        .build(),
                DbAggregationResult.builder().key("falcon").count(7L)
                        .stacks(
                                List.of(
                                        DbAggregationResult.builder().key("user-jenkins-9").count(7L).build()
                                )
                        )
                        .build()
        );
        // endregion

        // region trend - count - stack cicd_user_id - with parent id - falcon
        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.trend)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.count)
                        .stacks(List.of(CiCdPipelineJobRunsFilter.DISTINCT.cicd_user_id))
                        .parentCiCdJobIds(List.of(jobFalcon.getId().toString()))
                        .build(),
                VALUES_ONLY,
                null
        );
        Assert.assertNotNull(dbAggsResponse);
        Assertions.assertThat(dbAggsResponse.getCount().intValue()).isBetween(9, 10);
        Assertions.assertThat(dbAggsResponse.getTotalCount().intValue()).isBetween(9, 10);
        Assertions.assertThat(dbAggsResponse.getRecords().size()).isBetween(9, 10);
        expected = List.of(
                DbAggregationResult.builder().key("falcon").count(6L)
                        .stacks(
                                List.of(
                                        DbAggregationResult.builder().key("user-jenkins-0").count(6L).build()
                                )
                        )
                        .build(),
                DbAggregationResult.builder().key("falcon").count(6L)
                        .stacks(
                                List.of(
                                        DbAggregationResult.builder().key("user-jenkins-1").count(6L).build()
                                )
                        )
                        .build(),
                DbAggregationResult.builder().key("falcon").count(6L)
                        .stacks(
                                List.of(
                                        DbAggregationResult.builder().key("user-jenkins-2").count(6L).build()
                                )
                        )
                        .build(),
                DbAggregationResult.builder().key("falcon").count(6L)
                        .stacks(
                                List.of(
                                        DbAggregationResult.builder().key("user-jenkins-3").count(6L).build()
                                )
                        )
                        .build(),
                DbAggregationResult.builder().key("falcon").count(6L)
                        .stacks(
                                List.of(
                                        DbAggregationResult.builder().key("user-jenkins-4").count(6L).build()
                                )
                        )
                        .build(),
                DbAggregationResult.builder().key("falcon").count(6L)
                        .stacks(
                                List.of(
                                        DbAggregationResult.builder().key("user-jenkins-5").count(6L).build()
                                )
                        )
                        .build(),
                DbAggregationResult.builder().key("falcon").count(6L)
                        .stacks(
                                List.of(
                                        DbAggregationResult.builder().key("user-jenkins-6").count(6L).build()
                                )
                        )
                        .build(),
                DbAggregationResult.builder().key("falcon").count(6L)
                        .stacks(
                                List.of(
                                        DbAggregationResult.builder().key("user-jenkins-7").count(6L).build()
                                )
                        )
                        .build(),
                DbAggregationResult.builder().key("falcon").count(6L)
                        .stacks(
                                List.of(
                                        DbAggregationResult.builder().key("user-jenkins-8").count(6L).build()
                                )
                        )
                        .build(),
                DbAggregationResult.builder().key("falcon").count(6L)
                        .stacks(
                                List.of(
                                        DbAggregationResult.builder().key("user-jenkins-9").count(6L).build()
                                )
                        )
                        .build()
        );
        // endregion

        // region trend - count - stack cicd_user_id - with parent id - job1
        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.trend)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.count)
                        .stacks(List.of(CiCdPipelineJobRunsFilter.DISTINCT.cicd_user_id))
                        .parentCiCdJobIds(List.of(job1.getId().toString()))
                        .build(),
                VALUES_ONLY,
                null
        );

        Assert.assertNotNull(dbAggsResponse);
        Assertions.assertThat(dbAggsResponse.getCount().intValue()).isBetween(9, 10);
        Assertions.assertThat(dbAggsResponse.getTotalCount().intValue()).isBetween(9, 10);
        Assertions.assertThat(dbAggsResponse.getRecords().size()).isBetween(9, 10);
        expected = List.of(
                DbAggregationResult.builder().key("falcon").count(3L)
                        .stacks(
                                List.of(
                                        DbAggregationResult.builder().key("user-jenkins-0").count(3L).build()
                                )
                        )
                        .build(),
                DbAggregationResult.builder().key("falcon").count(3L)
                        .stacks(
                                List.of(
                                        DbAggregationResult.builder().key("user-jenkins-1").count(3L).build()
                                )
                        )
                        .build(),
                DbAggregationResult.builder().key("falcon").count(3L)
                        .stacks(
                                List.of(
                                        DbAggregationResult.builder().key("user-jenkins-2").count(3L).build()
                                )
                        )
                        .build(),
                DbAggregationResult.builder().key("falcon").count(3L)
                        .stacks(
                                List.of(
                                        DbAggregationResult.builder().key("user-jenkins-3").count(3L).build()
                                )
                        )
                        .build(),
                DbAggregationResult.builder().key("falcon").count(3L)
                        .stacks(
                                List.of(
                                        DbAggregationResult.builder().key("user-jenkins-4").count(3L).build()
                                )
                        )
                        .build(),
                DbAggregationResult.builder().key("falcon").count(3L)
                        .stacks(
                                List.of(
                                        DbAggregationResult.builder().key("user-jenkins-5").count(3L).build()
                                )
                        )
                        .build(),
                DbAggregationResult.builder().key("falcon").count(3L)
                        .stacks(
                                List.of(
                                        DbAggregationResult.builder().key("user-jenkins-6").count(3L).build()
                                )
                        )
                        .build(),
                DbAggregationResult.builder().key("falcon").count(3L)
                        .stacks(
                                List.of(
                                        DbAggregationResult.builder().key("user-jenkins-7").count(3L).build()
                                )
                        )
                        .build(),
                DbAggregationResult.builder().key("falcon").count(3L)
                        .stacks(
                                List.of(
                                        DbAggregationResult.builder().key("user-jenkins-8").count(3L).build()
                                )
                        )
                        .build(),
                DbAggregationResult.builder().key("falcon").count(3L)
                        .stacks(
                                List.of(
                                        DbAggregationResult.builder().key("user-jenkins-9").count(3L).build()
                                )
                        )
                        .build()
        );
        // endregion

        // region trend - count - stack cicd_user_id - with parent id - job1.1
        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.trend)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.count)
                        .stacks(List.of(CiCdPipelineJobRunsFilter.DISTINCT.cicd_user_id))
                        .parentCiCdJobIds(List.of(job1_1.getId().toString()))
                        .build(),
                VALUES_ONLY,
                null
        );

        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(0, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(0, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(0, dbAggsResponse.getRecords().size());
        Assert.assertTrue(CollectionUtils.isEmpty(dbAggsResponse.getRecords()));
        // endregion

        // region qualified name - duration - no parent id
        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.qualified_job_name).calculation(CiCdPipelineJobRunsFilter.CALCULATION.duration).build(),
                VALUES_ONLY,
                null
        );

        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(1, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("falcon").additionalKey("instance-name-0").count(70L).min(10L).max(70L).median(40L).sum(2800L).build()
        );
        // endregion

        // region qualified name - duration - with parent id - falcon
        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.qualified_job_name)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.duration)
                        .parentCiCdJobIds(List.of(jobFalcon.getId().toString()))
                        .build(),
                VALUES_ONLY,
                null
        );

        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(3, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(3, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(3, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("job1").additionalKey("instance-name-0").count(40L).min(10L).max(40L).median(20L).sum(1000L).build(),
                DbAggregationResult.builder().key("job2").additionalKey("instance-name-0").count(10L).min(50L).max(50L).median(50L).sum(500L).build(),
                DbAggregationResult.builder().key("job3").additionalKey("instance-name-0").count(10L).min(60L).max(60L).median(60L).sum(600L).build()
        );
        verifyRecords(dbAggsResponse.getRecords(), expected, false);
        // endregion

        // region job normalized full name - duration

        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder()
                        .across(CiCdPipelineJobRunsFilter.DISTINCT.job_normalized_full_name)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.duration)
                        .build(),
                VALUES_ONLY,
                null
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(1, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("Folder1/Folder2/BBMaven1New/falcon/master").count(70L).min(10L).max(70L).median(40L).sum(2800L).build()
        );
        verifyRecords(dbAggsResponse.getRecords(), expected, false);
        // endregion

        // region job normalized full name - duration - stacks cicd user id
        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder()
                        .across(CiCdPipelineJobRunsFilter.DISTINCT.job_normalized_full_name)
                        .stacks(List.of(CiCdPipelineJobRunsFilter.DISTINCT.cicd_user_id))
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.duration)
                        .build(),
                VALUES_ONLY,
                null
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(1, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("Folder1/Folder2/BBMaven1New/falcon/master").count(70L).min(10L).max(70L).median(40L).sum(2800L)
                        .stacks(List.of(
                                DbAggregationResult.builder().key("user-jenkins-0").count(7L).min(10L).max(70L).median(40L).sum(280L).build(),
                                DbAggregationResult.builder().key("user-jenkins-1").count(7L).min(10L).max(70L).median(40L).sum(280L).build(),
                                DbAggregationResult.builder().key("user-jenkins-2").count(7L).min(10L).max(70L).median(40L).sum(280L).build(),
                                DbAggregationResult.builder().key("user-jenkins-3").count(7L).min(10L).max(70L).median(40L).sum(280L).build(),
                                DbAggregationResult.builder().key("user-jenkins-4").count(7L).min(10L).max(70L).median(40L).sum(280L).build(),
                                DbAggregationResult.builder().key("user-jenkins-5").count(7L).min(10L).max(70L).median(40L).sum(280L).build(),
                                DbAggregationResult.builder().key("user-jenkins-6").count(7L).min(10L).max(70L).median(40L).sum(280L).build(),
                                DbAggregationResult.builder().key("user-jenkins-7").count(7L).min(10L).max(70L).median(40L).sum(280L).build(),
                                DbAggregationResult.builder().key("user-jenkins-8").count(7L).min(10L).max(70L).median(40L).sum(280L).build(),
                                DbAggregationResult.builder().key("user-jenkins-9").count(7L).min(10L).max(70L).median(40L).sum(280L).build()
                                )
                        )
                        .build()
        );
        verifyRecords(dbAggsResponse.getRecords(), expected, false);
        // endregion

        // region job normalized full name - duration - stacks cicd user id - with job normalized full name filter
        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().jobNormalizedFullNames(List.of("Folder1/Folder2/BBMaven1New/falcon/master"))
                        .across(CiCdPipelineJobRunsFilter.DISTINCT.job_normalized_full_name)
                        .stacks(List.of(CiCdPipelineJobRunsFilter.DISTINCT.cicd_user_id))
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.duration)
                        .build(),
                VALUES_ONLY,
                null
        );

        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(1, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("Folder1/Folder2/BBMaven1New/falcon/master").count(70L).min(10L).max(70L).median(40L).sum(2800L)
                        .stacks(List.of(
                                DbAggregationResult.builder().key("user-jenkins-0").count(7L).min(10L).max(70L).median(40L).sum(280L).build(),
                                DbAggregationResult.builder().key("user-jenkins-1").count(7L).min(10L).max(70L).median(40L).sum(280L).build(),
                                DbAggregationResult.builder().key("user-jenkins-2").count(7L).min(10L).max(70L).median(40L).sum(280L).build(),
                                DbAggregationResult.builder().key("user-jenkins-3").count(7L).min(10L).max(70L).median(40L).sum(280L).build(),
                                DbAggregationResult.builder().key("user-jenkins-4").count(7L).min(10L).max(70L).median(40L).sum(280L).build(),
                                DbAggregationResult.builder().key("user-jenkins-5").count(7L).min(10L).max(70L).median(40L).sum(280L).build(),
                                DbAggregationResult.builder().key("user-jenkins-6").count(7L).min(10L).max(70L).median(40L).sum(280L).build(),
                                DbAggregationResult.builder().key("user-jenkins-7").count(7L).min(10L).max(70L).median(40L).sum(280L).build(),
                                DbAggregationResult.builder().key("user-jenkins-8").count(7L).min(10L).max(70L).median(40L).sum(280L).build(),
                                DbAggregationResult.builder().key("user-jenkins-9").count(7L).min(10L).max(70L).median(40L).sum(280L).build()
                                )
                        )
                        .build()
        );
        verifyRecords(dbAggsResponse.getRecords(), expected, false);
        // endregion

        // region job normalized full name - duration - with invalid job normalized full name filter

        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().jobNormalizedFullNames(List.of("invalid"))
                        .across(CiCdPipelineJobRunsFilter.DISTINCT.job_normalized_full_name)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.duration)
                        .build(),
                VALUES_ONLY,
                null
        );

        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(0, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(0, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(0, dbAggsResponse.getRecords().size());
        // endregion

        // region job normalized full name - duration - with partial job normalized full name filter
        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder()
                        .partialMatch(Map.of("job_normalized_full_name", Map.of("$contains", "BBMaven1New")))
                        .across(CiCdPipelineJobRunsFilter.DISTINCT.job_normalized_full_name)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.duration)
                        .build(),
                VALUES_ONLY,
                null
        );

        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(1, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("Folder1/Folder2/BBMaven1New/falcon/master").count(70L).min(10L).max(70L).median(40L).sum(2800L).build()
        );
        verifyRecords(dbAggsResponse.getRecords(), expected, false);
        // endregion

        // region job normalized full name - duration - with invalid partial job normalized full name filter
        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder()
                        .partialMatch(Map.of("job_normalized_full_name", Map.of("$contains", "BB1New")))
                        .across(CiCdPipelineJobRunsFilter.DISTINCT.job_normalized_full_name)
                        .stacks(List.of(CiCdPipelineJobRunsFilter.DISTINCT.cicd_user_id))
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.duration)
                        .build(),
                VALUES_ONLY,
                null
        );

        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(0, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(0, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(0, dbAggsResponse.getRecords().size());
        // endregion

        // region instance name - duration
        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.instance_name)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.duration)
                        .build(),
                VALUES_ONLY,
                null
        );

        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(1, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("instance-name-0").count(70L).min(10L).max(70L).median(40L).sum(2800L).build()
        );
        verifyRecords(dbAggsResponse.getRecords(), expected, false);
        // endregion

        // region qualified name - duration - with parent id - job1
        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.qualified_job_name)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.duration)
                        .parentCiCdJobIds(List.of(job1.getId().toString()))
                        .build(),
                VALUES_ONLY,
                null
        );

        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(3, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(3, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(3, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("job1.1").additionalKey("instance-name-0").count(10L).min(30L).max(30L).median(30L).sum(300L).build(),
                DbAggregationResult.builder().key("job1.2").additionalKey("instance-name-0").count(10L).min(20L).max(20L).median(20L).sum(200L).build(),
                DbAggregationResult.builder().key("job1.3").additionalKey("instance-name-0").count(10L).min(10L).max(10L).median(10L).sum(100L).build()
        );
        verifyRecords(dbAggsResponse.getRecords(), expected, false);
        // endregion

        // region qualified name - duration - with parent id - job1.1
        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.qualified_job_name)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.duration)
                        .parentCiCdJobIds(List.of(job1_1.getId().toString()))
                        .build(),
                VALUES_ONLY,
                null
        );

        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(0, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(0, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(0, dbAggsResponse.getRecords().size());
        Assert.assertTrue(CollectionUtils.isEmpty(dbAggsResponse.getRecords()));
        // endregion

        // region job name - duration - no parent id
        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.job_name).calculation(CiCdPipelineJobRunsFilter.CALCULATION.duration).build(),
                VALUES_ONLY,
                null
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(1, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("falcon").count(70L).min(10L).max(70L).median(40L).sum(2800L).build()
        );
        verifyRecords(dbAggsResponse.getRecords(), expected, false);
        // endregion

        // region job name - duration - with parent id - falcon
        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.job_name)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.duration)
                        .parentCiCdJobIds(List.of(jobFalcon.getId().toString()))
                        .build(),
                VALUES_ONLY,
                null
        );

        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(3, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(3, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(3, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("job1").count(40L).min(10L).max(40L).median(20L).sum(1000L).build(),
                DbAggregationResult.builder().key("job2").count(10L).min(50L).max(50L).median(50L).sum(500L).build(),
                DbAggregationResult.builder().key("job3").count(10L).min(60L).max(60L).median(60L).sum(600L).build()
        );
        verifyRecords(dbAggsResponse.getRecords(), expected, false);
        // endregion

        // region job name - duration - with parent id - job1
        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.job_name)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.duration)
                        .parentCiCdJobIds(List.of(job1.getId().toString()))
                        .build(),
                VALUES_ONLY,
                null
        );

        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(3, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(3, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(3, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("job1.1").count(10L).min(30L).max(30L).median(30L).sum(300L).build(),
                DbAggregationResult.builder().key("job1.2").count(10L).min(20L).max(20L).median(20L).sum(200L).build(),
                DbAggregationResult.builder().key("job1.3").count(10L).min(10L).max(10L).median(10L).sum(100L).build()
        );
        verifyRecords(dbAggsResponse.getRecords(), expected, false);
        // endregion

        // region job name - duration - with parent id - job1.1
        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.job_name)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.duration)
                        .parentCiCdJobIds(List.of(job1_1.getId().toString()))
                        .build(),
                VALUES_ONLY,
                null
        );

        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(0, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(0, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(0, dbAggsResponse.getRecords().size());
        Assert.assertTrue(CollectionUtils.isEmpty(dbAggsResponse.getRecords()));
        // endregion

        // region qualified name - duration - no parent id
        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.cicd_job_id).calculation(CiCdPipelineJobRunsFilter.CALCULATION.duration).build(),
                VALUES_ONLY,
                null
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(1, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("falcon").additionalKey("instance-name-0").ciCdJobId(jobFalcon.getId().toString()).count(70L).min(10L).max(70L).median(40L).sum(2800L).build()
        );
        verifyRecords(dbAggsResponse.getRecords(), expected, false);
        // endregion

        // region qualified name - duration - with parent id - falcon
        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.cicd_job_id)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.duration)
                        .parentCiCdJobIds(List.of(jobFalcon.getId().toString()))
                        .build(),
                VALUES_ONLY,
                null
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(3, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(3, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(3, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("job1").additionalKey("instance-name-0").ciCdJobId(job1.getId().toString()).count(40L).min(10L).max(40L).median(20L).sum(1000L).build(),
                DbAggregationResult.builder().key("job2").additionalKey("instance-name-0").ciCdJobId(job2.getId().toString()).count(10L).min(50L).max(50L).median(50L).sum(500L).build(),
                DbAggregationResult.builder().key("job3").additionalKey("instance-name-0").ciCdJobId(job3.getId().toString()).count(10L).min(60L).max(60L).median(60L).sum(600L).build()
        );
        verifyRecords(dbAggsResponse.getRecords(), expected, false);
        // endregion

        // region qualified name - duration - with parent id - job1
        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.cicd_job_id)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.duration)
                        .parentCiCdJobIds(List.of(job1.getId().toString()))
                        .build(),
                VALUES_ONLY,
                null
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(3, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(3, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(3, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("job1.1").additionalKey("instance-name-0").ciCdJobId(job1_1.getId().toString()).count(10L).min(30L).max(30L).median(30L).sum(300L).build(),
                DbAggregationResult.builder().key("job1.2").additionalKey("instance-name-0").ciCdJobId(job1_2.getId().toString()).count(10L).min(20L).max(20L).median(20L).sum(200L).build(),
                DbAggregationResult.builder().key("job1.3").additionalKey("instance-name-0").ciCdJobId(job1_3.getId().toString()).count(10L).min(10L).max(10L).median(10L).sum(100L).build()
        );
        verifyRecords(dbAggsResponse.getRecords(), expected, false);
        // endregion

        // region qualified name - duration - with parent id - job1.1
        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.cicd_job_id)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.duration)
                        .parentCiCdJobIds(List.of(job1_1.getId().toString()))
                        .build(),
                VALUES_ONLY,
                null
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(0, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(0, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(0, dbAggsResponse.getRecords().size());
        Assert.assertTrue(CollectionUtils.isEmpty(dbAggsResponse.getRecords()));
        // endregion

        // region trend - duration - no parent id
        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.trend)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.duration).build(),
                VALUES_ONLY,
                null
        );
        Assert.assertNotNull(dbAggsResponse);
        Assertions.assertThat(dbAggsResponse.getCount().intValue()).isBetween(9, 10);
        Assertions.assertThat(dbAggsResponse.getTotalCount().intValue()).isBetween(9, 10);
        Assertions.assertThat(dbAggsResponse.getRecords().size()).isBetween(9, 10);
        // endregion

        // region trend - duration - with parent id - falcon
        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.trend)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.duration)
                        .parentCiCdJobIds(List.of(jobFalcon.getId().toString()))
                        .build(),
                VALUES_ONLY,
                null
        );
        Assert.assertNotNull(dbAggsResponse);
        Assertions.assertThat(dbAggsResponse.getCount().intValue()).isBetween(9, 10);
        Assertions.assertThat(dbAggsResponse.getTotalCount().intValue()).isBetween(9, 10);
        Assertions.assertThat(dbAggsResponse.getRecords().size()).isBetween(9, 10);
        expected = List.of(
                DbAggregationResult.builder().key("falcon").count(6L).min(10L).max(60L).median(30L).sum(210L).build(),
                DbAggregationResult.builder().key("falcon").count(6L).min(10L).max(60L).median(30L).sum(210L).build(),
                DbAggregationResult.builder().key("falcon").count(6L).min(10L).max(60L).median(30L).sum(210L).build(),
                DbAggregationResult.builder().key("falcon").count(6L).min(10L).max(60L).median(30L).sum(210L).build(),
                DbAggregationResult.builder().key("falcon").count(6L).min(10L).max(60L).median(30L).sum(210L).build(),
                DbAggregationResult.builder().key("falcon").count(6L).min(10L).max(60L).median(30L).sum(210L).build(),
                DbAggregationResult.builder().key("falcon").count(6L).min(10L).max(60L).median(30L).sum(210L).build(),
                DbAggregationResult.builder().key("falcon").count(6L).min(10L).max(60L).median(30L).sum(210L).build(),
                DbAggregationResult.builder().key("falcon").count(6L).min(10L).max(60L).median(30L).sum(210L).build(),
                DbAggregationResult.builder().key("falcon").count(6L).min(10L).max(60L).median(30L).sum(210L).build()
        );
        verifyRecords(dbAggsResponse.getRecords(), expected, true);
        // endregion

        // region trend - duration - with parent id - job1
        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.trend)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.duration)
                        .parentCiCdJobIds(List.of(job1.getId().toString()))
                        .build(),
                VALUES_ONLY,
                null
        );

        Assert.assertNotNull(dbAggsResponse);
        Assertions.assertThat(dbAggsResponse.getCount().intValue()).isBetween(9, 10);
        Assertions.assertThat(dbAggsResponse.getTotalCount().intValue()).isBetween(9, 10);
        Assertions.assertThat(dbAggsResponse.getRecords().size()).isBetween(9, 10);
        expected = List.of(
                DbAggregationResult.builder().key("falcon").count(3L).min(10L).max(30L).median(20L).sum(60L).build(),
                DbAggregationResult.builder().key("falcon").count(3L).min(10L).max(30L).median(20L).sum(60L).build(),
                DbAggregationResult.builder().key("falcon").count(3L).min(10L).max(30L).median(20L).sum(60L).build(),
                DbAggregationResult.builder().key("falcon").count(3L).min(10L).max(30L).median(20L).sum(60L).build(),
                DbAggregationResult.builder().key("falcon").count(3L).min(10L).max(30L).median(20L).sum(60L).build(),
                DbAggregationResult.builder().key("falcon").count(3L).min(10L).max(30L).median(20L).sum(60L).build(),
                DbAggregationResult.builder().key("falcon").count(3L).min(10L).max(30L).median(20L).sum(60L).build(),
                DbAggregationResult.builder().key("falcon").count(3L).min(10L).max(30L).median(20L).sum(60L).build(),
                DbAggregationResult.builder().key("falcon").count(3L).min(10L).max(30L).median(20L).sum(60L).build(),
                DbAggregationResult.builder().key("falcon").count(3L).min(10L).max(30L).median(20L).sum(60L).build()
        );
        verifyRecords(dbAggsResponse.getRecords(), expected, true);
        // endregion

        // region trend - duration - with parent id - job1.1
        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.trend)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.duration)
                        .parentCiCdJobIds(List.of(job1_1.getId().toString()))
                        .build(),
                VALUES_ONLY,
                null
        );

        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(0, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(0, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(0, dbAggsResponse.getRecords().size());
        Assert.assertTrue(CollectionUtils.isEmpty(dbAggsResponse.getRecords()));
        // endregion

        // region qualified name - duration - stack cicd_user_id - no parent id
        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.qualified_job_name)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.duration)
                        .stacks(List.of(CiCdPipelineJobRunsFilter.DISTINCT.cicd_user_id))
                        .build(),
                VALUES_ONLY,
                null
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(1, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("falcon").additionalKey("instance-name-0").count(70L).min(10L).max(70L).median(40L).sum(2800L)
                        .stacks(
                                List.of(
                                        DbAggregationResult.builder().key("user-jenkins-0").count(7L).min(10L).max(70L).median(40L).sum(280L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-1").count(7L).min(10L).max(70L).median(40L).sum(280L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-2").count(7L).min(10L).max(70L).median(40L).sum(280L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-3").count(7L).min(10L).max(70L).median(40L).sum(280L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-4").count(7L).min(10L).max(70L).median(40L).sum(280L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-5").count(7L).min(10L).max(70L).median(40L).sum(280L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-6").count(7L).min(10L).max(70L).median(40L).sum(280L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-7").count(7L).min(10L).max(70L).median(40L).sum(280L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-8").count(7L).min(10L).max(70L).median(40L).sum(280L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-9").count(7L).min(10L).max(70L).median(40L).sum(280L).build()
                                )
                        )
                        .build()
        );
        verifyRecords(dbAggsResponse.getRecords(), expected, false);
        // endregion

        // region qualified name - duration - stack cicd_user_id - with parent id - falcon
        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.qualified_job_name)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.duration)
                        .stacks(List.of(CiCdPipelineJobRunsFilter.DISTINCT.cicd_user_id))
                        .parentCiCdJobIds(List.of(jobFalcon.getId().toString()))
                        .build(),
                VALUES_ONLY,
                null
        );

        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(3, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(3, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(3, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("job1").additionalKey("instance-name-0").count(40L).min(10L).max(40L).median(20L).sum(1000L)
                        .stacks(
                                List.of(
                                        DbAggregationResult.builder().key("user-jenkins-0").count(4L).min(10L).max(40L).median(20L).sum(100L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-1").count(4L).min(10L).max(40L).median(20L).sum(100L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-2").count(4L).min(10L).max(40L).median(20L).sum(100L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-3").count(4L).min(10L).max(40L).median(20L).sum(100L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-4").count(4L).min(10L).max(40L).median(20L).sum(100L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-5").count(4L).min(10L).max(40L).median(20L).sum(100L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-6").count(4L).min(10L).max(40L).median(20L).sum(100L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-7").count(4L).min(10L).max(40L).median(20L).sum(100L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-8").count(4L).min(10L).max(40L).median(20L).sum(100L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-9").count(4L).min(10L).max(40L).median(20L).sum(100L).build()

                                )
                        )
                        .build(),
                DbAggregationResult.builder().key("job2").additionalKey("instance-name-0").count(10L).min(50L).max(50L).median(50L).sum(500L)
                        .stacks(
                                List.of(
                                        DbAggregationResult.builder().key("user-jenkins-0").count(1L).min(50L).max(50L).median(50L).sum(50L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-1").count(1L).min(50L).max(50L).median(50L).sum(50L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-2").count(1L).min(50L).max(50L).median(50L).sum(50L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-3").count(1L).min(50L).max(50L).median(50L).sum(50L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-4").count(1L).min(50L).max(50L).median(50L).sum(50L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-5").count(1L).min(50L).max(50L).median(50L).sum(50L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-6").count(1L).min(50L).max(50L).median(50L).sum(50L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-7").count(1L).min(50L).max(50L).median(50L).sum(50L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-8").count(1L).min(50L).max(50L).median(50L).sum(50L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-9").count(1L).min(50L).max(50L).median(50L).sum(50L).build()

                                )
                        )
                        .build(),
                DbAggregationResult.builder().key("job3").additionalKey("instance-name-0").count(10L).min(60L).max(60L).median(60L).sum(600L)
                        .stacks(
                                List.of(
                                        DbAggregationResult.builder().key("user-jenkins-0").count(1L).min(60L).max(60L).median(60L).sum(60L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-1").count(1L).min(60L).max(60L).median(60L).sum(60L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-2").count(1L).min(60L).max(60L).median(60L).sum(60L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-3").count(1L).min(60L).max(60L).median(60L).sum(60L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-4").count(1L).min(60L).max(60L).median(60L).sum(60L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-5").count(1L).min(60L).max(60L).median(60L).sum(60L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-6").count(1L).min(60L).max(60L).median(60L).sum(60L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-7").count(1L).min(60L).max(60L).median(60L).sum(60L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-8").count(1L).min(60L).max(60L).median(60L).sum(60L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-9").count(1L).min(60L).max(60L).median(60L).sum(60L).build()

                                )
                        )
                        .build()
        );
        verifyRecords(dbAggsResponse.getRecords(), expected, false);
        // endregion

        // region qualified name - duration - stack cicd_user_id - with parent id - job1
        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.qualified_job_name)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.duration)
                        .stacks(List.of(CiCdPipelineJobRunsFilter.DISTINCT.cicd_user_id))
                        .parentCiCdJobIds(List.of(job1.getId().toString()))
                        .build(),
                VALUES_ONLY,
                null
        );

        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(3, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(3, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(3, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("job1.1").additionalKey("instance-name-0").count(10L).min(30L).max(30L).median(30L).sum(300L)
                        .stacks(
                                List.of(
                                        DbAggregationResult.builder().key("user-jenkins-0").count(1L).min(30L).max(30L).median(30L).sum(30L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-1").count(1L).min(30L).max(30L).median(30L).sum(30L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-2").count(1L).min(30L).max(30L).median(30L).sum(30L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-3").count(1L).min(30L).max(30L).median(30L).sum(30L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-4").count(1L).min(30L).max(30L).median(30L).sum(30L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-5").count(1L).min(30L).max(30L).median(30L).sum(30L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-6").count(1L).min(30L).max(30L).median(30L).sum(30L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-7").count(1L).min(30L).max(30L).median(30L).sum(30L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-8").count(1L).min(30L).max(30L).median(30L).sum(30L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-9").count(1L).min(30L).max(30L).median(30L).sum(30L).build()

                                )
                        )
                        .build(),
                DbAggregationResult.builder().key("job1.2").additionalKey("instance-name-0").count(10L).min(20L).max(20L).median(20L).sum(200L)
                        .stacks(
                                List.of(
                                        DbAggregationResult.builder().key("user-jenkins-0").count(1L).min(20L).max(20L).median(20L).sum(20L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-1").count(1L).min(20L).max(20L).median(20L).sum(20L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-2").count(1L).min(20L).max(20L).median(20L).sum(20L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-3").count(1L).min(20L).max(20L).median(20L).sum(20L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-4").count(1L).min(20L).max(20L).median(20L).sum(20L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-5").count(1L).min(20L).max(20L).median(20L).sum(20L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-6").count(1L).min(20L).max(20L).median(20L).sum(20L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-7").count(1L).min(20L).max(20L).median(20L).sum(20L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-8").count(1L).min(20L).max(20L).median(20L).sum(20L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-9").count(1L).min(20L).max(20L).median(20L).sum(20L).build()

                                )
                        )
                        .build(),
                DbAggregationResult.builder().key("job1.3").additionalKey("instance-name-0").count(10L).min(10L).max(10L).median(10L).sum(100L)
                        .stacks(
                                List.of(
                                        DbAggregationResult.builder().key("user-jenkins-0").count(1L).min(10L).max(10L).median(10L).sum(10L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-1").count(1L).min(10L).max(10L).median(10L).sum(10L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-2").count(1L).min(10L).max(10L).median(10L).sum(10L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-3").count(1L).min(10L).max(10L).median(10L).sum(10L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-4").count(1L).min(10L).max(10L).median(10L).sum(10L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-5").count(1L).min(10L).max(10L).median(10L).sum(10L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-6").count(1L).min(10L).max(10L).median(10L).sum(10L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-7").count(1L).min(10L).max(10L).median(10L).sum(10L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-8").count(1L).min(10L).max(10L).median(10L).sum(10L).build(),
                                        DbAggregationResult.builder().key("user-jenkins-9").count(1L).min(10L).max(10L).median(10L).sum(10L).build()

                                )
                        )
                        .build()
        );
        verifyRecords(dbAggsResponse.getRecords(), expected, false);
        // endregion

        // region qualified name - duration - stack cicd_user_id - with parent id - job1.1
        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.qualified_job_name)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.duration)
                        .stacks(List.of(CiCdPipelineJobRunsFilter.DISTINCT.cicd_user_id))
                        .parentCiCdJobIds(List.of(job1_1.getId().toString()))
                        .build(),
                VALUES_ONLY,
                null
        );

        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(0, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(0, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(0, dbAggsResponse.getRecords().size());
        Assert.assertTrue(CollectionUtils.isEmpty(dbAggsResponse.getRecords()));
        // endregion

        // region qualified name - duration - stack status - no parent id
        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.qualified_job_name)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.duration)
                        .stacks(List.of(CiCdPipelineJobRunsFilter.DISTINCT.job_status))
                        .build(),
                VALUES_ONLY,
                null
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(1, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(1, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("falcon").additionalKey("instance-name-0").count(70L).min(10L).max(70L).median(40L).sum(2800L)
                        .stacks(
                                List.of(
                                        DbAggregationResult.builder().key("SUCCESS").count(70L).min(10L).max(70L).median(40L).sum(2800L).build()
                                )
                        )
                        .build()
        );
        verifyRecords(dbAggsResponse.getRecords(), expected, false);
        // endregion

        // region qualified name - duration - stack status - with parent id - falcon
        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.qualified_job_name)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.duration)
                        .stacks(List.of(CiCdPipelineJobRunsFilter.DISTINCT.job_status))
                        .parentCiCdJobIds(List.of(jobFalcon.getId().toString()))
                        .build(),
                VALUES_ONLY,
                null
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(3, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(3, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(3, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("job1").additionalKey("instance-name-0").count(40L).min(10L).max(40L).median(20L).sum(1000L)
                        .stacks(
                                List.of(
                                        DbAggregationResult.builder().key("SUCCESS").count(40L).min(10L).max(40L).median(20L).sum(1000L).build()
                                )
                        )
                        .build(),
                DbAggregationResult.builder().key("job2").additionalKey("instance-name-0").count(10L).min(50L).max(50L).median(50L).sum(500L)
                        .stacks(
                                List.of(
                                        DbAggregationResult.builder().key("SUCCESS").count(10L).min(50L).max(50L).median(50L).sum(500L).build()
                                )
                        )
                        .build(),
                DbAggregationResult.builder().key("job3").additionalKey("instance-name-0").count(10L).min(60L).max(60L).median(60L).sum(600L)
                        .stacks(
                                List.of(
                                        DbAggregationResult.builder().key("SUCCESS").count(10L).min(60L).max(60L).median(60L).sum(600L).build()
                                )
                        )
                        .build()
        );
        verifyRecords(dbAggsResponse.getRecords(), expected, false);
        // endregion

        // region qualified name - duration - stack status - with parent id - job1
        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.qualified_job_name)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.duration)
                        .stacks(List.of(CiCdPipelineJobRunsFilter.DISTINCT.job_status))
                        .parentCiCdJobIds(List.of(job1.getId().toString()))
                        .build(),
                VALUES_ONLY,
                null
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(3, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(3, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(3, dbAggsResponse.getRecords().size());
        expected = List.of(
                DbAggregationResult.builder().key("job1.1").additionalKey("instance-name-0").count(10L).min(30L).max(30L).median(30L).sum(300L)
                        .stacks(
                                List.of(
                                        DbAggregationResult.builder().key("SUCCESS").count(10L).min(30L).max(30L).median(30L).sum(300L).build()
                                )
                        )
                        .build(),
                DbAggregationResult.builder().key("job1.2").additionalKey("instance-name-0").count(10L).min(20L).max(20L).median(20L).sum(200L)
                        .stacks(
                                List.of(
                                        DbAggregationResult.builder().key("SUCCESS").count(10L).min(20L).max(20L).median(20L).sum(200L).build()
                                )
                        )
                        .build(),
                DbAggregationResult.builder().key("job1.3").additionalKey("instance-name-0").count(10L).min(10L).max(10L).median(10L).sum(100L)
                        .stacks(
                                List.of(
                                        DbAggregationResult.builder().key("SUCCESS").count(10L).min(10L).max(10L).median(10L).sum(100L).build()
                                )
                        )
                        .build()
        );
        verifyRecords(dbAggsResponse.getRecords(), expected, false);
        // endregion

        // region qualified name - duration - stack status - with parent id - job1.1
        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.qualified_job_name)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.duration)
                        .stacks(List.of(CiCdPipelineJobRunsFilter.DISTINCT.job_status))
                        .parentCiCdJobIds(List.of(job1_1.getId().toString()))
                        .build(),
                VALUES_ONLY,
                null
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(0, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(0, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(0, dbAggsResponse.getRecords().size());
        Assert.assertTrue(CollectionUtils.isEmpty(dbAggsResponse.getRecords()));
        // endregion

        // region trend - duration - stack cicd_user_id - no parent id
        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.trend)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.duration)
                        .stacks(List.of(CiCdPipelineJobRunsFilter.DISTINCT.cicd_user_id))
                        .build(),
                VALUES_ONLY,
                null
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(10, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(10, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(10, dbAggsResponse.getRecords().size());
        // endregion

        // region trend - duration - stack cicd_user_id - with parent id - falcon
        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.trend)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.duration)
                        .stacks(List.of(CiCdPipelineJobRunsFilter.DISTINCT.cicd_user_id))
                        .parentCiCdJobIds(List.of(jobFalcon.getId().toString()))
                        .build(),
                VALUES_ONLY,
                null
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(10, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(10, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(10, dbAggsResponse.getRecords().size());
        // endregion

        // region trend - duration - stack cicd_user_id - with parent id - job1
        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.trend)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.duration)
                        .stacks(List.of(CiCdPipelineJobRunsFilter.DISTINCT.cicd_user_id))
                        .parentCiCdJobIds(List.of(job1.getId().toString()))
                        .build(),
                VALUES_ONLY,
                null
        );

        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(10, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(10, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(10, dbAggsResponse.getRecords().size());
        // endregion

        // region trend - duration - stack cicd_user_id - with parent id - job1.1
        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.trend)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.duration)
                        .stacks(List.of(CiCdPipelineJobRunsFilter.DISTINCT.cicd_user_id))
                        .parentCiCdJobIds(List.of(job1_1.getId().toString()))
                        .build(),
                VALUES_ONLY,
                null
        );

        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(0, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(0, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(0, dbAggsResponse.getRecords().size());
        Assert.assertTrue(CollectionUtils.isEmpty(dbAggsResponse.getRecords()));
        // endregion

        // region - list - no filters
        DbListResponse<CICDJobRunDTO> dbListResponse = ciCdPipelinesAggsService.listCiCdJobRuns(company, CiCdPipelineJobRunsFilter.builder().build(),
                DEFAULT_SORT_BY, null, PAGE_NUMBER, PAGE_SIZE);
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(70, dbListResponse.getCount().intValue());
        Assert.assertEquals(70, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(70, dbListResponse.getRecords().size());
        // endregion

        // region - list - no filters - small page size
        dbListResponse = ciCdPipelinesAggsService.listCiCdJobRuns(company, CiCdPipelineJobRunsFilter.builder().build(),
                DEFAULT_SORT_BY, null, PAGE_NUMBER, 50);
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(50, dbListResponse.getCount().intValue());
        Assert.assertEquals(70, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(50, dbListResponse.getRecords().size());
        // endregion

        // region - list - no filters - Sort by start_time desc
        dbListResponse = ciCdPipelinesAggsService.listCiCdJobRuns(company, CiCdPipelineJobRunsFilter.builder().build(),
                Map.of("start_time", SortingOrder.DESC), null, PAGE_NUMBER, PAGE_SIZE);
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(70, dbListResponse.getCount().intValue());
        Assert.assertEquals(70, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(70, dbListResponse.getRecords().size());
        // endregion

        // region - list - no filters - Sort by start_time desc
        dbListResponse = ciCdPipelinesAggsService.listCiCdJobRuns(company, CiCdPipelineJobRunsFilter.builder().build(),
                Map.of("start_time", SortingOrder.ASC), null, PAGE_NUMBER, PAGE_SIZE);
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(70, dbListResponse.getCount().intValue());
        Assert.assertEquals(70, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(70, dbListResponse.getRecords().size());
        // endregion

        // region - list - job_name partial - with results
        dbListResponse = ciCdPipelinesAggsService.listCiCdJobRuns(company, CiCdPipelineJobRunsFilter.builder().jobNamePartial("lco").build(),
                DEFAULT_SORT_BY, null, PAGE_NUMBER, PAGE_SIZE);
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(70, dbListResponse.getCount().intValue());
        Assert.assertEquals(70, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(70, dbListResponse.getRecords().size());
        // endregion

        // region - list - job_name partial - with out results
        dbListResponse = ciCdPipelinesAggsService.listCiCdJobRuns(company, CiCdPipelineJobRunsFilter.builder().jobNamePartial("lzo").build(),
                DEFAULT_SORT_BY, null, PAGE_NUMBER, PAGE_SIZE);
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(0, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(0, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(0, dbAggsResponse.getRecords().size());
        Assert.assertTrue(CollectionUtils.isEmpty(dbAggsResponse.getRecords()));
        // endregion

        // region - triage job runs list - instance name
        dbListResponse = ciCdPipelinesAggsService.listCiCdJobRuns(company, CiCdPipelineJobRunsFilter.builder()
                        .instanceNames(List.of("instance-name-0"))
                        .across(CiCdPipelineJobRunsFilter.DISTINCT.trend).calculation(CiCdPipelineJobRunsFilter.CALCULATION.count).build(), PAGE_NUMBER, PAGE_SIZE, false, false, null);
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(70, dbListResponse.getCount().intValue());
        Assert.assertEquals(70, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(70, dbListResponse.getRecords().size());
        // endregion

        // region - triage job runs list - instance name invalid
        dbListResponse = ciCdPipelinesAggsService.listCiCdJobRuns(company, CiCdPipelineJobRunsFilter.builder()
                        .instanceNames(List.of("instance-name-invalid"))
                        .across(CiCdPipelineJobRunsFilter.DISTINCT.trend).calculation(CiCdPipelineJobRunsFilter.CALCULATION.count).build(), PAGE_NUMBER, PAGE_SIZE, false, false, null);
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(0, dbListResponse.getCount().intValue());
        Assert.assertEquals(0, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(0, dbListResponse.getRecords().size());
        // endregion

        // region - list - cicd_user_id valid
        dbListResponse = ciCdPipelinesAggsService.listCiCdJobRuns(company, CiCdPipelineJobRunsFilter.builder()
                        .cicdUserIds(List.of("user-jenkins-0", "user-jenkins-1"))
                        .build(),
                DEFAULT_SORT_BY, null, PAGE_NUMBER, PAGE_SIZE);
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(14, dbListResponse.getCount().intValue());
        Assert.assertEquals(14, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(14, dbListResponse.getRecords().size());
        // endregion

        // region - list - cicd_user_id - job name
        dbListResponse = ciCdPipelinesAggsService.listCiCdJobRuns(company, CiCdPipelineJobRunsFilter.builder()
                        .cicdUserIds(List.of("user-jenkins-0")).jobNames(List.of("job1.1"))
                        .build(),
                DEFAULT_SORT_BY, null, PAGE_NUMBER, PAGE_SIZE);
        Assert.assertEquals(1, dbListResponse.getCount().intValue());
        Assert.assertEquals(1, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(1, dbListResponse.getRecords().size());
        Assert.assertEquals("https://jenkins.dev.levelops.io/job/Folder1/Folder2/BBMaven1New/job1.1/master/0/",
                dbListResponse.getRecords().get(0).getCicdBuildUrl());

        // region - list - cicd_user_id invalid
        dbListResponse = ciCdPipelinesAggsService.listCiCdJobRuns(company, CiCdPipelineJobRunsFilter.builder()
                        .cicdUserIds(List.of("does-not-exist"))
                        .build(),
                DEFAULT_SORT_BY, null, PAGE_NUMBER, PAGE_SIZE);
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(0, dbListResponse.getCount().intValue());
        Assert.assertEquals(0, dbListResponse.getTotalCount().intValue());
        Assert.assertTrue(CollectionUtils.isEmpty(dbListResponse.getRecords()));
        // endregion

        // region - list - job_name valid - falcon
        dbListResponse = ciCdPipelinesAggsService.listCiCdJobRuns(company, CiCdPipelineJobRunsFilter.builder()
                        .jobNames(List.of("falcon"))
                        .build(),
                DEFAULT_SORT_BY, null, PAGE_NUMBER, PAGE_SIZE);
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(70, dbListResponse.getCount().intValue());
        Assert.assertEquals(70, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(70, dbListResponse.getRecords().size());
        // endregion

        // region - list - job_name valid - job1
        dbListResponse = ciCdPipelinesAggsService.listCiCdJobRuns(company, CiCdPipelineJobRunsFilter.builder()
                        .jobNames(List.of("job1"))
                        .build(),
                DEFAULT_SORT_BY, null, PAGE_NUMBER, PAGE_SIZE);
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(40, dbListResponse.getCount().intValue());
        Assert.assertEquals(40, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(40, dbListResponse.getRecords().size());
        // endregion

        // region - list - job_name invalid
        dbListResponse = ciCdPipelinesAggsService.listCiCdJobRuns(company, CiCdPipelineJobRunsFilter.builder()
                        .jobNames(List.of("does-not-exist"))
                        .build(),
                DEFAULT_SORT_BY, null, PAGE_NUMBER, PAGE_SIZE);
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(0, dbListResponse.getCount().intValue());
        Assert.assertEquals(0, dbListResponse.getTotalCount().intValue());
        Assert.assertTrue(CollectionUtils.isEmpty(dbListResponse.getRecords()));
        // endregion

        // region - list - job_status valid
        dbListResponse = ciCdPipelinesAggsService.listCiCdJobRuns(company, CiCdPipelineJobRunsFilter.builder()
                        .jobStatuses(List.of("SUCCESS"))
                        .build(),
                DEFAULT_SORT_BY, null, PAGE_NUMBER, PAGE_SIZE);
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(70, dbListResponse.getCount().intValue());
        Assert.assertEquals(70, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(70, dbListResponse.getRecords().size());
        // endregion

        // region - list - job_status invalid
        dbListResponse = ciCdPipelinesAggsService.listCiCdJobRuns(company, CiCdPipelineJobRunsFilter.builder()
                        .jobStatuses(List.of("does-not-exist"))
                        .build(),
                DEFAULT_SORT_BY, null, PAGE_NUMBER, PAGE_SIZE);
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(0, dbListResponse.getCount().intValue());
        Assert.assertEquals(0, dbListResponse.getTotalCount().intValue());
        Assert.assertTrue(CollectionUtils.isEmpty(dbListResponse.getRecords()));
        // endregion

        // region - list - qualified job name valid - falcon
        dbListResponse = ciCdPipelinesAggsService.listCiCdJobRuns(company, CiCdPipelineJobRunsFilter.builder()
                        .qualifiedJobNames(List.of(CiCdJobQualifiedName.builder().instanceName("instance-name-0").jobName("falcon").build()))
                        .build(),
                DEFAULT_SORT_BY, null, PAGE_NUMBER, PAGE_SIZE);
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(70, dbListResponse.getCount().intValue());
        Assert.assertEquals(70, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(70, dbListResponse.getRecords().size());
        // endregion

        // region - list - qualified job name valid - job1
        dbListResponse = ciCdPipelinesAggsService.listCiCdJobRuns(company, CiCdPipelineJobRunsFilter.builder()
                        .qualifiedJobNames(List.of(CiCdJobQualifiedName.builder().instanceName("instance-name-0").jobName("job1").build()))
                        .build(),
                DEFAULT_SORT_BY, null, PAGE_NUMBER, PAGE_SIZE);
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(40, dbListResponse.getCount().intValue());
        Assert.assertEquals(40, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(40, dbListResponse.getRecords().size());
        // endregion

        // region - list - qualified job name invalid
        dbListResponse = ciCdPipelinesAggsService.listCiCdJobRuns(company, CiCdPipelineJobRunsFilter.builder()
                        .qualifiedJobNames(List.of(CiCdJobQualifiedName.builder().instanceName("instance-name-1").jobName("crow").build()))
                        .build(),
                DEFAULT_SORT_BY, null, PAGE_NUMBER, PAGE_SIZE);
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(0, dbListResponse.getCount().intValue());
        Assert.assertEquals(0, dbListResponse.getTotalCount().intValue());
        Assert.assertTrue(CollectionUtils.isEmpty(dbListResponse.getRecords()));
        // endregion

        // region - list - instance
        dbListResponse = ciCdPipelinesAggsService.listCiCdJobRuns(company, CiCdPipelineJobRunsFilter.builder()
                        .instanceNames(List.of("instance-name-0"))
                        .build(),
                DEFAULT_SORT_BY, null, PAGE_NUMBER, PAGE_SIZE);
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(70, dbListResponse.getCount().intValue());
        Assert.assertEquals(70, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(70, dbListResponse.getRecords().size());
        // endregion

        // region - list - instance - invalid
        dbListResponse = ciCdPipelinesAggsService.listCiCdJobRuns(company, CiCdPipelineJobRunsFilter.builder()
                        .instanceNames(List.of("instance-invalid"))
                        .build(),
                DEFAULT_SORT_BY, null, PAGE_NUMBER, PAGE_SIZE);
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(0, dbListResponse.getCount().intValue());
        Assert.assertEquals(0, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(0, dbListResponse.getRecords().size());
        // endregion

        // region - list - valid type filter
        dbListResponse = ciCdPipelinesAggsService.listCiCdJobRuns(company, CiCdPipelineJobRunsFilter.builder()
                        .types(List.of(CICD_TYPE.jenkins))
                        .build(),
                DEFAULT_SORT_BY, null, PAGE_NUMBER, PAGE_SIZE);
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(70, dbListResponse.getCount().intValue());
        Assert.assertEquals(70, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(70, dbListResponse.getRecords().size());
        // endregion

        // region - list - invalid type filter
        dbListResponse = ciCdPipelinesAggsService.listCiCdJobRuns(company, CiCdPipelineJobRunsFilter.builder()
                        .types(List.of(CICD_TYPE.azure_devops))
                        .build(),
                DEFAULT_SORT_BY, null, PAGE_NUMBER, PAGE_SIZE);
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(0, dbListResponse.getCount().intValue());
        Assert.assertEquals(0, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(0, dbListResponse.getRecords().size());
        // endregion

        // region - list - parent cicd job id valid - falcon
        dbListResponse = ciCdPipelinesAggsService.listCiCdJobRuns(company, CiCdPipelineJobRunsFilter.builder()
                        .parentCiCdJobIds(List.of(jobFalcon.getId().toString()))
                        .build(),
                DEFAULT_SORT_BY, null, PAGE_NUMBER, PAGE_SIZE);
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(60, dbListResponse.getCount().intValue());
        Assert.assertEquals(60, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(60, dbListResponse.getRecords().size());
        // endregion

        // region - list - parent cicd job id valid - job1
        dbListResponse = ciCdPipelinesAggsService.listCiCdJobRuns(company, CiCdPipelineJobRunsFilter.builder()
                        .parentCiCdJobIds(List.of(job1.getId().toString()))
                        .build(),
                DEFAULT_SORT_BY, null, PAGE_NUMBER, PAGE_SIZE);
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(30, dbListResponse.getCount().intValue());
        Assert.assertEquals(30, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(30, dbListResponse.getRecords().size());
        // endregion

        // region - list - parent cicd job id valid - job1_1
        dbListResponse = ciCdPipelinesAggsService.listCiCdJobRuns(company, CiCdPipelineJobRunsFilter.builder()
                        .parentCiCdJobIds(List.of(job1_1.getId().toString()))
                        .build(),
                DEFAULT_SORT_BY, null, PAGE_NUMBER, PAGE_SIZE);
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(0, dbListResponse.getCount().intValue());
        Assert.assertEquals(0, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(0, dbListResponse.getRecords().size());
        // endregion

        // region - list - parent cicd job id invalid
        dbListResponse = ciCdPipelinesAggsService.listCiCdJobRuns(company, CiCdPipelineJobRunsFilter.builder()
                        .parentCiCdJobIds(List.of(UUID.randomUUID().toString()))
                        .build(),
                DEFAULT_SORT_BY, null, PAGE_NUMBER, PAGE_SIZE);
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(0, dbListResponse.getCount().intValue());
        Assert.assertEquals(0, dbListResponse.getTotalCount().intValue());
        Assert.assertTrue(CollectionUtils.isEmpty(dbListResponse.getRecords()));
        // endregion
    }

    // region Verify Records
    private void verifyRecord(DbAggregationResult a, DbAggregationResult e, boolean ignoreKey) {
        Assert.assertEquals((e == null), (a == null));
        if (e == null) {
            return;
        }
        if (!ignoreKey) {
            Assert.assertEquals(a.getKey(), e.getKey());
        }
        Assert.assertEquals(a.getCiCdJobId(), e.getCiCdJobId());
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
    @Test
    public void testPipelineProductFilters() throws SQLException {
        Instant now = Instant.now();
        Instant before2Days = now.minus(2, ChronoUnit.DAYS);
        Instant before3Days = now.minus(3, ChronoUnit.DAYS);
        Instant before4Days = now.minus(4, ChronoUnit.DAYS);

        Integration integration1 = Integration.builder()
                .name("integration-name-" + 1)
                .status("status-" + 0).application("azure_devops").url("http://www.dummy.com")
                .satellite(false).build();
        String integrationId1 = integrationService.insert(company, integration1);

        Integration integration2 = Integration.builder()
                .name("integration-name-" + 2)
                .status("status-" + 0).application("jenkins").url("http://www.dummy.com")
                .satellite(false).build();
        String integrationId2 = integrationService.insert(company, integration2);
        CICDInstance cicdInstance1 = CiCdInstanceUtils.createCiCdInstance(ciCdInstancesDatabaseService, company, integration1.toBuilder().id(integrationId1).build(), 0);
        CICDJob cicdJob1 = CiCdJobUtils.createCICDJob(ciCdJobsDatabaseService, company, 0, cicdInstance1);
        CICDJobRun cicdJobRun1 = CICDJobRunUtils.createCICDJobRun(ciCdJobRunsDatabaseService, cicdJob1, company, 0,
                before3Days, null, null, null);

        CICDInstance cicdInstance2 = CiCdInstanceUtils.createCiCdInstance(ciCdInstancesDatabaseService, company, integration2.toBuilder().id(integrationId2).build(), 0);
        CICDJob cicdJob2 = CiCdJobUtils.createCICDJob(ciCdJobsDatabaseService, company, 0, cicdInstance2);
        CICDJobRun cicdJobRun2 = CICDJobRunUtils.createCICDJobRun(ciCdJobRunsDatabaseService, cicdJob2, company, 0,
                before3Days, null, null, null);

        Assert.assertNotNull(integrationId1);

        DBOrgProduct orgProduct1 = DBOrgProduct.builder()
                .name("product-1")
                .description("prod-1")
                .integrations(Set.of(DBOrgProduct.Integ.builder()
                        .integrationId(Integer.parseInt(integrationId1))
                        .name(integration1.getName())
                        .type("azure_devops")
                        .filters(Map.of("job_names", List.of("jobname-0"),
                                "job_statuses", List.of("SUCCESS")))
                        .build())).build();

        DBOrgProduct orgProduct2 = DBOrgProduct.builder()
                .name("product-2")
                .description("prod-2")
                .integrations(Set.of(DBOrgProduct.Integ.builder()
                        .integrationId(Integer.parseInt(integrationId1))
                        .name(integration1.getName())
                        .type("azure_devops")
                        .filters(Map.of("job_names", List.of("jobname-12"),
                                "projects", List.of("project-123"),
                                "job_statuses", List.of("SUCCESS")))
                        .build())).build();
        DBOrgProduct orgProduct3 = DBOrgProduct.builder()
                .name("product-3")
                .description("prod-3")
                .integrations(Set.of(DBOrgProduct.Integ.builder()
                        .integrationId(Integer.parseInt(integrationId1))
                        .name(integration1.getName())
                        .type("azure_devops")
                        .filters(Map.of("job_statuses", List.of("FAILURE")))
                        .build()))
                .build();
        DBOrgProduct orgProduct4 = DBOrgProduct.builder()
                .name("product-4")
                .description("prod-4")
                .integrations(Set.of(DBOrgProduct.Integ.builder()
                        .integrationId(Integer.parseInt(integrationId2))
                        .name(integration2.getName())
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
                        .integrationId(Integer.parseInt(integrationId1))
                        .name(integration1.getName())
                        .type("azure_devops")
                        .filters(Map.of("cicd_job_run_ids", List.of(cicdJobRun1.getId()),
                                "partial_match", Map.of("job_normalized_full_name", Map.of("$contains", "jobname"))))
                        .build()))
                .build();
        DBOrgProduct orgProduct6 = DBOrgProduct.builder()
                .name("product-6")
                .description("prod-6")
                .integrations(Set.of(DBOrgProduct.Integ.builder()
                        .integrationId(Integer.parseInt(integrationId1))
                        .name(integration1.getName())
                        .type("azure_devops")
                        .filters(Map.of("cicd_job_ids", List.of(cicdJob1.getId())))
                        .build()))
                .build();
        String orgProductId1 = productsDatabaseService.insert(company, orgProduct1);
        String orgProductId2 = productsDatabaseService.insert(company, orgProduct2);
        String orgProductId3 = productsDatabaseService.insert(company, orgProduct3);
        String orgProductId4 = productsDatabaseService.insert(company, orgProduct4);
        String orgProductId5 = productsDatabaseService.insert(company, orgProduct5);
        String orgProductId6 = productsDatabaseService.insert(company, orgProduct6);
        integration1 = integration1.toBuilder().id(integrationId1).build();


        // endregion
        //Without stacks

        var dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder()
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.count)
                        .across(CiCdPipelineJobRunsFilter.DISTINCT.job_name)
                        .orgProductsIds(Set.of(UUID.fromString(orgProductId1)))
                        .build(), VALUES_ONLY, null);
        DefaultObjectMapper.prettyPrint(dbAggsResponse);
        Assert.assertEquals(1, dbAggsResponse.getTotalCount().intValue());

        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company, CiCdPipelineJobRunsFilter.builder()
                .calculation(CiCdPipelineJobRunsFilter.CALCULATION.count)
                .across(CiCdPipelineJobRunsFilter.DISTINCT.job_name)
                .orgProductsIds(Set.of(UUID.fromString(orgProductId5)))
                .build(), VALUES_ONLY, null);
        DefaultObjectMapper.prettyPrint(dbAggsResponse);
        Assert.assertEquals(1, dbAggsResponse.getTotalCount().intValue());

        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company, CiCdPipelineJobRunsFilter.builder()
                .calculation(CiCdPipelineJobRunsFilter.CALCULATION.count)
                .across(CiCdPipelineJobRunsFilter.DISTINCT.cicd_job_id)
                .orgProductsIds(Set.of(UUID.fromString(orgProductId6)))
                .build(), VALUES_ONLY, null);
        DefaultObjectMapper.prettyPrint(dbAggsResponse);
        Assert.assertEquals(1, dbAggsResponse.getTotalCount().intValue());

        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company, CiCdPipelineJobRunsFilter.builder()
                .calculation(CiCdPipelineJobRunsFilter.CALCULATION.count)
                .across(CiCdPipelineJobRunsFilter.DISTINCT.job_name)
                .orgProductsIds(Set.of(UUID.fromString(orgProductId1), UUID.fromString(orgProductId4)))
                .build(), VALUES_ONLY, null);
        DefaultObjectMapper.prettyPrint(dbAggsResponse);
        Assert.assertEquals(1, dbAggsResponse.getTotalCount().intValue());

        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company, CiCdPipelineJobRunsFilter.builder()
                .calculation(CiCdPipelineJobRunsFilter.CALCULATION.duration)
                .across(CiCdPipelineJobRunsFilter.DISTINCT.trend)
                .orgProductsIds(Set.of(UUID.fromString(orgProductId1)))
                .build(), VALUES_ONLY, null);
        DefaultObjectMapper.prettyPrint(dbAggsResponse);
        Assert.assertEquals(1, dbAggsResponse.getTotalCount().intValue());

        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company, CiCdPipelineJobRunsFilter.builder()
                .calculation(CiCdPipelineJobRunsFilter.CALCULATION.duration)
                .across(CiCdPipelineJobRunsFilter.DISTINCT.cicd_user_id)
                .orgProductsIds(Set.of(UUID.fromString(orgProductId4)))
                .build(), VALUES_ONLY, null);
        DefaultObjectMapper.prettyPrint(dbAggsResponse);
        Assert.assertEquals(1, dbAggsResponse.getTotalCount().intValue());

        //negative
        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company, CiCdPipelineJobRunsFilter.builder()
                .calculation(CiCdPipelineJobRunsFilter.CALCULATION.count)
                .across(CiCdPipelineJobRunsFilter.DISTINCT.project_name)
                .orgProductsIds(Set.of(UUID.fromString(orgProductId2)))
                .build(), VALUES_ONLY, null);
        DefaultObjectMapper.prettyPrint(dbAggsResponse);
        Assert.assertEquals(0, dbAggsResponse.getTotalCount().intValue());

        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company, CiCdPipelineJobRunsFilter.builder()
                .calculation(CiCdPipelineJobRunsFilter.CALCULATION.duration)
                .across(CiCdPipelineJobRunsFilter.DISTINCT.job_status)
                .orgProductsIds(Set.of(UUID.fromString(orgProductId2), UUID.fromString(orgProductId3)))
                .build(), VALUES_ONLY, null);
        DefaultObjectMapper.prettyPrint(dbAggsResponse);
        Assert.assertEquals(0, dbAggsResponse.getTotalCount().intValue());

        //With stacks
        //positive test
        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company, CiCdPipelineJobRunsFilter.builder()
                .calculation(CiCdPipelineJobRunsFilter.CALCULATION.count)
                .across(CiCdPipelineJobRunsFilter.DISTINCT.qualified_job_name)
                .orgProductsIds(Set.of(UUID.fromString(orgProductId1)))
                .stacks(List.of(CiCdPipelineJobRunsFilter.DISTINCT.job_status))
                .build(), VALUES_ONLY, null);
        DefaultObjectMapper.prettyPrint(dbAggsResponse);
        Assert.assertEquals(1, dbAggsResponse.getTotalCount().intValue());


        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company, CiCdPipelineJobRunsFilter.builder()
                .calculation(CiCdPipelineJobRunsFilter.CALCULATION.duration)
                .across(CiCdPipelineJobRunsFilter.DISTINCT.trend)
                .orgProductsIds(Set.of(UUID.fromString(orgProductId1)))
                .stacks(List.of(CiCdPipelineJobRunsFilter.DISTINCT.instance_name))
                .build(), VALUES_ONLY, null);
        DefaultObjectMapper.prettyPrint(dbAggsResponse);
        Assert.assertEquals(1, dbAggsResponse.getTotalCount().intValue());

        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company, CiCdPipelineJobRunsFilter.builder()
                .calculation(CiCdPipelineJobRunsFilter.CALCULATION.duration)
                .across(CiCdPipelineJobRunsFilter.DISTINCT.cicd_user_id)
                .orgProductsIds(Set.of(UUID.fromString(orgProductId4)))
                .stacks(List.of(CiCdPipelineJobRunsFilter.DISTINCT.job_normalized_full_name))
                .build(), VALUES_ONLY, null);
        DefaultObjectMapper.prettyPrint(dbAggsResponse);
        Assert.assertEquals(1, dbAggsResponse.getTotalCount().intValue());

        //negative
        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company, CiCdPipelineJobRunsFilter.builder()
                .calculation(CiCdPipelineJobRunsFilter.CALCULATION.count)
                .across(CiCdPipelineJobRunsFilter.DISTINCT.project_name)
                .orgProductsIds(Set.of(UUID.fromString(orgProductId2)))
                .stacks(List.of(CiCdPipelineJobRunsFilter.DISTINCT.project_name))
                .build(), VALUES_ONLY, null);
        DefaultObjectMapper.prettyPrint(dbAggsResponse);
        Assert.assertEquals(0, dbAggsResponse.getTotalCount().intValue());

        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company, CiCdPipelineJobRunsFilter.builder()
                .calculation(CiCdPipelineJobRunsFilter.CALCULATION.duration)
                .across(CiCdPipelineJobRunsFilter.DISTINCT.job_status)
                .orgProductsIds(Set.of(UUID.fromString(orgProductId2), UUID.fromString(orgProductId3)))
                .stacks(List.of(CiCdPipelineJobRunsFilter.DISTINCT.job_name))
                .build(), VALUES_ONLY, null);
        DefaultObjectMapper.prettyPrint(dbAggsResponse);
        Assert.assertEquals(0, dbAggsResponse.getTotalCount().intValue());

        //list
        var dbListResponse = ciCdPipelinesAggsService.listCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder()
                        .orgProductsIds(Set.of(UUID.fromString(orgProductId1)))
                        .build(), DEFAULT_SORT_BY, null, PAGE_NUMBER, PAGE_SIZE);
        DefaultObjectMapper.prettyPrint(dbListResponse);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(1, dbListResponse.getCount().intValue());
        Assert.assertEquals(1, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(1, dbListResponse.getRecords().size());

        dbListResponse = ciCdPipelinesAggsService.listCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder()
                        .orgProductsIds(Set.of(UUID.fromString(orgProductId4), UUID.fromString(orgProductId1)))
                        .build(), DEFAULT_SORT_BY, null, PAGE_NUMBER, PAGE_SIZE);
        DefaultObjectMapper.prettyPrint(dbListResponse);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(2, dbListResponse.getCount().intValue());
        Assert.assertEquals(2, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(2, dbListResponse.getRecords().size());

        dbListResponse = ciCdPipelinesAggsService.listCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder()
                        .orgProductsIds(Set.of(UUID.fromString(orgProductId4)))
                        .build(), DEFAULT_SORT_BY, null, PAGE_NUMBER, PAGE_SIZE);
        DefaultObjectMapper.prettyPrint(dbListResponse);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(1, dbListResponse.getCount().intValue());
        Assert.assertEquals(1, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(1, dbListResponse.getRecords().size());

        dbListResponse = ciCdPipelinesAggsService.listCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder()
                        .orgProductsIds(Set.of(UUID.fromString(orgProductId2), UUID.fromString(orgProductId3)))
                        .build(), DEFAULT_SORT_BY, null, PAGE_NUMBER, PAGE_SIZE);
        DefaultObjectMapper.prettyPrint(dbListResponse);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(0, dbListResponse.getCount().intValue());
        Assert.assertEquals(0, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(0, dbListResponse.getRecords().size());

        dbListResponse = ciCdPipelinesAggsService.listCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder()
                        .orgProductsIds(Set.of(UUID.fromString(orgProductId3)))
                        .build(), DEFAULT_SORT_BY, null, PAGE_NUMBER, PAGE_SIZE);
        DefaultObjectMapper.prettyPrint(dbListResponse);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(0, dbListResponse.getCount().intValue());
        Assert.assertEquals(0, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(0, dbListResponse.getRecords().size());

    }

    @Test
    public void testCiCdPipelineExcludeFilters() throws SQLException {
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
            CICDJobRun cicdJobFalconRun = CICDJobRunUtils.createCICDJobRun(ciCdJobRunsDatabaseService, jobFalcon, company, i, day, 70, null, null);
            CICDJobRun cicdJob1Run = CICDJobRunUtils.createCICDJobRun(ciCdJobRunsDatabaseService, job1, company, i, day, 40, jobFalcon.getJobFullName(), cicdJobFalconRun.getJobRunNumber());
            CICDJobRun cicdJob2Run = CICDJobRunUtils.createCICDJobRun(ciCdJobRunsDatabaseService, job2, company, i, day, 50, jobFalcon.getJobFullName(), cicdJobFalconRun.getJobRunNumber());
            CICDJobRun cicdJob3Run = CICDJobRunUtils.createCICDJobRun(ciCdJobRunsDatabaseService, job3, company, i, day, 60, jobFalcon.getJobFullName(), cicdJobFalconRun.getJobRunNumber());
            CICDJobRun cicdJob1_1Run = CICDJobRunUtils.createCICDJobRun(ciCdJobRunsDatabaseService, job1_1, company, i, day, 30, job1.getJobFullName(), cicdJob1Run.getJobRunNumber());
            CICDJobRun cicdJob1_2Run = CICDJobRunUtils.createCICDJobRun(ciCdJobRunsDatabaseService, job1_2, company, i, day, 20, job1.getJobFullName(), cicdJob1Run.getJobRunNumber());
            CICDJobRun cicdJob1_3Run = CICDJobRunUtils.createCICDJobRun(ciCdJobRunsDatabaseService, job1_3, company, i, day, 10, job1.getJobFullName(), cicdJob1Run.getJobRunNumber());
        }

        DbListResponse<CICDJobRunDTO> dbListResponse = ciCdPipelinesAggsService.listCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder()
                        .excludeJobNames(List.of("job1.1"))
                        .build(), DEFAULT_SORT_BY, null, PAGE_NUMBER, PAGE_SIZE);
        DefaultObjectMapper.prettyPrint(dbListResponse);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(70, dbListResponse.getCount().intValue());
        Assert.assertEquals(70, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(70, dbListResponse.getRecords().size());

        dbListResponse = ciCdPipelinesAggsService.listCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder()
                        .excludeJobNames(List.of("job1.1"))
                        .jobNames(List.of("job1.1"))
                        .build(), DEFAULT_SORT_BY, null, PAGE_NUMBER, PAGE_SIZE);
        DefaultObjectMapper.prettyPrint(dbListResponse);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(0, dbListResponse.getCount().intValue());
        Assert.assertEquals(0, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(0, dbListResponse.getRecords().size());

        dbListResponse = ciCdPipelinesAggsService.listCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder()
                        .excludeProjects(List.of("project-2"))
                        .build(), DEFAULT_SORT_BY, null, PAGE_NUMBER, PAGE_SIZE);
        DefaultObjectMapper.prettyPrint(dbListResponse);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(70, dbListResponse.getCount().intValue());
        Assert.assertEquals(70, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(70, dbListResponse.getRecords().size());

        dbListResponse = ciCdPipelinesAggsService.listCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder()
                        .excludeProjects(List.of("project-2"))
                        .projects(List.of("project-2"))
                        .build(), DEFAULT_SORT_BY, null, PAGE_NUMBER, PAGE_SIZE);
        DefaultObjectMapper.prettyPrint(dbListResponse);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(0, dbListResponse.getCount().intValue());
        Assert.assertEquals(0, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(0, dbListResponse.getRecords().size());

        dbListResponse = ciCdPipelinesAggsService.listCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder()
                        .excludeJobStatuses(List.of("FAILURE"))
                        .build(), DEFAULT_SORT_BY, null, PAGE_NUMBER, PAGE_SIZE);
        DefaultObjectMapper.prettyPrint(dbListResponse);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(70, dbListResponse.getCount().intValue());
        Assert.assertEquals(70, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(70, dbListResponse.getRecords().size());

        dbListResponse = ciCdPipelinesAggsService.listCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder()
                        .excludeJobStatuses(List.of("FAILURE"))
                        .jobStatuses(List.of("FAILURE"))
                        .build(), DEFAULT_SORT_BY, null, PAGE_NUMBER, PAGE_SIZE);
        DefaultObjectMapper.prettyPrint(dbListResponse);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(0, dbListResponse.getCount().intValue());
        Assert.assertEquals(0, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(0, dbListResponse.getRecords().size());

        dbListResponse = ciCdPipelinesAggsService.listCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder()
                        .excludeInstanceNames(List.of("instance-name-1"))
                        .build(), DEFAULT_SORT_BY, null, PAGE_NUMBER, PAGE_SIZE);
        DefaultObjectMapper.prettyPrint(dbListResponse);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(70, dbListResponse.getCount().intValue());
        Assert.assertEquals(70, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(70, dbListResponse.getRecords().size());

        dbListResponse = ciCdPipelinesAggsService.listCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder()
                        .excludeInstanceNames(List.of("instance-name-0"))
                        .instanceNames(List.of("instance-name-0"))
                        .build(), DEFAULT_SORT_BY, null, PAGE_NUMBER, PAGE_SIZE);
        DefaultObjectMapper.prettyPrint(dbListResponse);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(0, dbListResponse.getCount().intValue());
        Assert.assertEquals(0, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(0, dbListResponse.getRecords().size());

        dbListResponse = ciCdPipelinesAggsService.listCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder()
                        .excludeJobNormalizedFullNames(List.of("Folder1/Folder2/BBMaven1New/falcon/master"))
                        .build(), DEFAULT_SORT_BY, null, PAGE_NUMBER, PAGE_SIZE);
        DefaultObjectMapper.prettyPrint(dbListResponse);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(60, dbListResponse.getCount().intValue());
        Assert.assertEquals(60, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(60, dbListResponse.getRecords().size());

        dbListResponse = ciCdPipelinesAggsService.listCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder()
                        .excludeJobNormalizedFullNames(List.of("Folder1/Folder2/BBMaven1New/falcon/master"))
                        .jobNormalizedFullNames(List.of("Folder1/Folder2/BBMaven1New/falcon/master"))
                        .build(), DEFAULT_SORT_BY, null, PAGE_NUMBER, PAGE_SIZE);
        DefaultObjectMapper.prettyPrint(dbListResponse);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(0, dbListResponse.getCount().intValue());
        Assert.assertEquals(0, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(0, dbListResponse.getRecords().size());

        dbListResponse = ciCdPipelinesAggsService.listCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder()
                        .excludeTypes(List.of(CICD_TYPE.azure_devops))
                        .build(), DEFAULT_SORT_BY, null, PAGE_NUMBER, PAGE_SIZE);
        DefaultObjectMapper.prettyPrint(dbListResponse);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(70, dbListResponse.getCount().intValue());
        Assert.assertEquals(70, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(70, dbListResponse.getRecords().size());

        dbListResponse = ciCdPipelinesAggsService.listCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder()
                        .excludeTypes(List.of(CICD_TYPE.azure_devops))
                        .types(List.of(CICD_TYPE.azure_devops))
                        .build(), DEFAULT_SORT_BY, null, PAGE_NUMBER, PAGE_SIZE);
        DefaultObjectMapper.prettyPrint(dbListResponse);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(0, dbListResponse.getCount().intValue());
        Assert.assertEquals(0, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(0, dbListResponse.getRecords().size());

        dbListResponse = ciCdPipelinesAggsService.listCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder()
                        .excludeCiCdUserIds(List.of("user-jenkins-0"))
                        .build(), DEFAULT_SORT_BY, null, PAGE_NUMBER, PAGE_SIZE);
        DefaultObjectMapper.prettyPrint(dbListResponse);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(63, dbListResponse.getCount().intValue());
        Assert.assertEquals(63, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(63, dbListResponse.getRecords().size());

        dbListResponse = ciCdPipelinesAggsService.listCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder()
                        .excludeCiCdUserIds(List.of("user-jenkins-0"))
                        .cicdUserIds(List.of("user-jenkins-0"))
                        .build(), DEFAULT_SORT_BY, null, PAGE_NUMBER, PAGE_SIZE);
        DefaultObjectMapper.prettyPrint(dbListResponse);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(0, dbListResponse.getCount().intValue());
        Assert.assertEquals(0, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(0, dbListResponse.getRecords().size());

        dbListResponse = ciCdPipelinesAggsService.listCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder()
                        .excludeCiCdJobRunIds(List.of("b85b31fc-4f1e-440a-b6b2-47059b38f99d"))
                        .build(), DEFAULT_SORT_BY, null, PAGE_NUMBER, PAGE_SIZE);
        DefaultObjectMapper.prettyPrint(dbListResponse);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(70, dbListResponse.getCount().intValue());
        Assert.assertEquals(70, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(70, dbListResponse.getRecords().size());

        dbListResponse = ciCdPipelinesAggsService.listCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder()
                        .excludeCiCdJobRunIds(List.of("b85b31fc-4f1e-440a-b6b2-47059b38f99d"))
                        .ciCdJobRunIds(List.of("b85b31fc-4f1e-440a-b6b2-47059b38f99d"))
                        .build(), DEFAULT_SORT_BY, null, PAGE_NUMBER, PAGE_SIZE);
        DefaultObjectMapper.prettyPrint(dbListResponse);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(0, dbListResponse.getCount().intValue());
        Assert.assertEquals(0, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(0, dbListResponse.getRecords().size());

        dbListResponse = ciCdPipelinesAggsService.listCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder()
                        .excludeJobIds(List.of("b611c788-f15f-4f23-aa63-ee511f758061"))
                        .build(), DEFAULT_SORT_BY, null, PAGE_NUMBER, PAGE_SIZE);
        DefaultObjectMapper.prettyPrint(dbListResponse);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(70, dbListResponse.getCount().intValue());
        Assert.assertEquals(70, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(70, dbListResponse.getRecords().size());

        dbListResponse = ciCdPipelinesAggsService.listCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder()
                        .excludeJobIds(List.of("b611c788-f15f-4f23-aa63-ee511f758061"))
                        .jobIds(List.of("b611c788-f15f-4f23-aa63-ee511f758061"))
                        .build(), DEFAULT_SORT_BY, null, PAGE_NUMBER, PAGE_SIZE);
        DefaultObjectMapper.prettyPrint(dbListResponse);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(0, dbListResponse.getCount().intValue());
        Assert.assertEquals(0, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(0, dbListResponse.getRecords().size());

        CiCdJobQualifiedName ciCdJobQualifiedName=CiCdJobQualifiedName.builder().instanceName("instance-name-1").jobName("job1").build();
        dbListResponse = ciCdPipelinesAggsService.listCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder()
                        .excludeQualifiedJobNames(List.of(ciCdJobQualifiedName))
                        .build(), DEFAULT_SORT_BY, null, PAGE_NUMBER, PAGE_SIZE);
        DefaultObjectMapper.prettyPrint(dbListResponse);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(70, dbListResponse.getCount().intValue());
        Assert.assertEquals(70, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(70, dbListResponse.getRecords().size());

        dbListResponse = ciCdPipelinesAggsService.listCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder()
                        .excludeQualifiedJobNames(List.of(ciCdJobQualifiedName))
                        .qualifiedJobNames(List.of(ciCdJobQualifiedName))
                        .build(), DEFAULT_SORT_BY, null, PAGE_NUMBER, PAGE_SIZE);
        DefaultObjectMapper.prettyPrint(dbListResponse);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(0, dbListResponse.getCount().intValue());
        Assert.assertEquals(0, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(0, dbListResponse.getRecords().size());
    }

   // @Test
    public void testSortBy() throws SQLException {
        Instant now = Instant.now();
        Instant before2Days = now.minus(2, ChronoUnit.DAYS);
        Instant before3Days = now.minus(3, ChronoUnit.DAYS);
        Instant before4Days = now.minus(4, ChronoUnit.DAYS);

        Integration integration1 = Integration.builder()
                .name("integration-name-" + 1)
                .status("status-" + 0).application("azure_devops").url("http://www.dummy.com")
                .satellite(false).build();
        String integrationId1 = integrationService.insert(company, integration1);

        Integration integration2 = Integration.builder()
                .name("integration-name-" + 2)
                .status("status-" + 0).application("jenkins").url("http://www.dummy.com")
                .satellite(false).build();
        String integrationId2 = integrationService.insert(company, integration2);
        CICDInstance cicdInstance1 = CiCdInstanceUtils.createCiCdInstance(ciCdInstancesDatabaseService, company, integration1.toBuilder().id(integrationId1).build(), 0);
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

        CICDInstance cicdInstance2 = CiCdInstanceUtils.createCiCdInstance(ciCdInstancesDatabaseService, company, integration2.toBuilder().id(integrationId2).build(), 1);
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

        var dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.job_status)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.count)
                        .stacks(List.of(CiCdPipelineJobRunsFilter.DISTINCT.instance_name))
                        .build(), Map.of("job_status", SortingOrder.ASC), VALUES_ONLY,
                        null
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(2, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getRecords().size());
        assertThat(dbAggsResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(DbAggregationResult::getKey));

        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.job_status)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.duration)
                        .build(), Map.of("job_status", SortingOrder.DESC), VALUES_ONLY,
                        null
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(2, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getRecords().size());
        assertThat(dbAggsResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(DbAggregationResult::getKey).reversed());

        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.job_status)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.duration)
                        .build(), Map.of("duration", SortingOrder.DESC), VALUES_ONLY,
                        null
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(2, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getRecords().size());
        assertThat(dbAggsResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(DbAggregationResult::getMedian).reversed());

        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.job_name)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.count)
                        .build(), Map.of("job_name", SortingOrder.ASC), VALUES_ONLY,
                        null
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(2, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getRecords().size());
        assertThat(dbAggsResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(DbAggregationResult::getKey));

        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.job_name)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.duration)
                        .build(), Map.of("job_name", SortingOrder.DESC), VALUES_ONLY,
                        null
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(2, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getRecords().size());
        assertThat(dbAggsResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(DbAggregationResult::getKey).reversed());

        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.job_name)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.duration)
                        .build(), Map.of("duration", SortingOrder.DESC), VALUES_ONLY,
                        null
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(2, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getRecords().size());
        assertThat(dbAggsResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(DbAggregationResult::getMedian).reversed());

        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.qualified_job_name)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.count)
                        .build(), Map.of("qualified_job_name", SortingOrder.ASC), VALUES_ONLY,
                        null
        );
        DefaultObjectMapper.prettyPrint(dbAggsResponse);
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(2, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getRecords().size());
        assertThat(dbAggsResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(DbAggregationResult::getKey));

        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.qualified_job_name)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.duration)
                        .build(), Map.of("qualified_job_name", SortingOrder.DESC), VALUES_ONLY,
                        null
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(2, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getRecords().size());
        assertThat(dbAggsResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(DbAggregationResult::getKey).reversed());

        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.qualified_job_name)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.count)
                        .build(), Map.of("count", SortingOrder.ASC), VALUES_ONLY,
                        null
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(2, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getRecords().size());
        assertThat(dbAggsResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(DbAggregationResult::getCount));

        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.job_normalized_full_name)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.count)
                        .build(), Map.of("job_normalized_full_name", SortingOrder.ASC), VALUES_ONLY,
                        null
        );
        DefaultObjectMapper.prettyPrint(dbAggsResponse);
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(2, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getRecords().size());
        assertThat(dbAggsResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(DbAggregationResult::getKey));

        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.job_normalized_full_name)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.duration)
                        .build(), Map.of("job_normalized_full_name", SortingOrder.DESC), VALUES_ONLY,
                        null
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(2, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getRecords().size());
        assertThat(dbAggsResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(DbAggregationResult::getKey).reversed());

        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.job_normalized_full_name)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.count)
                        .build(), Map.of("count", SortingOrder.ASC), VALUES_ONLY,
                        null
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(2, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getRecords().size());
        assertThat(dbAggsResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(DbAggregationResult::getCount));


        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.instance_name)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.count)
                        .build(), Map.of("instance_name", SortingOrder.ASC), VALUES_ONLY,
                        null
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(2, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getRecords().size());
        assertThat(dbAggsResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(DbAggregationResult::getKey));

        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.instance_name)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.duration)
                        .build(), Map.of("instance_name", SortingOrder.DESC), VALUES_ONLY,
                        null
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(2, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getRecords().size());
        assertThat(dbAggsResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(DbAggregationResult::getKey).reversed());

        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.cicd_user_id)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.count)
                        .build(), Map.of("cicd_user_id", SortingOrder.ASC), VALUES_ONLY,
                        null
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(2, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getRecords().size());
        assertThat(getKeys(dbAggsResponse)).isSortedAccordingTo(Comparator.nullsFirst(Comparator.naturalOrder()));

        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.cicd_user_id)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.duration)
                        .build(), Map.of("cicd_user_id", SortingOrder.DESC), VALUES_ONLY,
                        null
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(2, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getRecords().size());
        assertThat(getKeys(dbAggsResponse)).isSortedAccordingTo(Comparator.nullsLast(Comparator.reverseOrder()));

        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.cicd_job_id)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.count)
                        .build(), Map.of("cicd_job_id", SortingOrder.ASC), VALUES_ONLY,
                        null
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(2, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getRecords().size());
        assertThat(dbAggsResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(DbAggregationResult::getKey));

        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.cicd_job_id)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.duration)
                        .build(), Map.of("cicd_job_id", SortingOrder.DESC), VALUES_ONLY,
                        null
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(2, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getRecords().size());
        assertThat(dbAggsResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(DbAggregationResult::getKey).reversed());

        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.job_end)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.count)
                        .build(), Map.of("job_end", SortingOrder.ASC), VALUES_ONLY,
                        null
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(2, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getRecords().size());
        assertThat(dbAggsResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(DbAggregationResult::getKey));

        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.job_end)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.duration)
                        .build(), Map.of("job_end", SortingOrder.DESC), VALUES_ONLY,
                        null
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(2, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getRecords().size());
        assertThat(dbAggsResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(DbAggregationResult::getKey).reversed());

        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.project_name)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.count)
                        .build(), Map.of("project_name", SortingOrder.ASC), VALUES_ONLY,
                        null
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(2, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getRecords().size());
        assertThat(dbAggsResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(DbAggregationResult::getKey));

        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.project_name)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.duration)
                        .build(), Map.of("project_name", SortingOrder.DESC), VALUES_ONLY,
                        null
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(2, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getRecords().size());
        assertThat(dbAggsResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(DbAggregationResult::getKey).reversed());

        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.trend)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.count)
                        .build(), Map.of("trend", SortingOrder.ASC), VALUES_ONLY,
                        null
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(2, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getRecords().size());
        assertThat(dbAggsResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(DbAggregationResult::getKey));

        dbAggsResponse = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.trend)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.duration)
                        .build(), Map.of("trend", SortingOrder.DESC), VALUES_ONLY,
                        null
        );
        Assert.assertNotNull(dbAggsResponse);
        Assert.assertEquals(2, dbAggsResponse.getCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getTotalCount().intValue());
        Assert.assertEquals(2, dbAggsResponse.getRecords().size());
        assertThat(dbAggsResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(DbAggregationResult::getKey).reversed());

        //drilldown

        DbListResponse<CICDJobRunDTO> dbListResponse = ciCdPipelinesAggsService.listCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().build(), Map.of("status", SortingOrder.ASC), null, PAGE_NUMBER, PAGE_SIZE);
        Assert.assertNotNull(dbListResponse);
        assertThat(dbListResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(CICDJobRunDTO::getStatus));

        dbListResponse = ciCdPipelinesAggsService.listCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().build(), Map.of("status", SortingOrder.DESC), null, PAGE_NUMBER, PAGE_SIZE);
        Assert.assertNotNull(dbListResponse);
        assertThat(dbListResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(CICDJobRunDTO::getStatus).reversed());

        dbListResponse = ciCdPipelinesAggsService.listCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().build(), Map.of("cicd_user_id", SortingOrder.ASC), null, PAGE_NUMBER, PAGE_SIZE);
        Assert.assertNotNull(dbListResponse);
        assertThat(getDbListKeys(dbListResponse)).isSortedAccordingTo(Comparator.nullsFirst(Comparator.naturalOrder()));

        dbListResponse = ciCdPipelinesAggsService.listCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().build(), Map.of("cicd_user_id", SortingOrder.DESC), null, PAGE_NUMBER, PAGE_SIZE);
        Assert.assertNotNull(dbListResponse);
        assertThat(getDbListKeys(dbListResponse)).isSortedAccordingTo(Comparator.nullsLast(Comparator.reverseOrder()));

        dbListResponse = ciCdPipelinesAggsService.listCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().build(), Map.of("duration", SortingOrder.ASC), null, PAGE_NUMBER, PAGE_SIZE);
        Assert.assertNotNull(dbListResponse);
        assertThat(dbListResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(CICDJobRunDTO::getDuration));

        dbListResponse = ciCdPipelinesAggsService.listCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().build(), Map.of("duration", SortingOrder.DESC), null, PAGE_NUMBER, PAGE_SIZE);
        Assert.assertNotNull(dbListResponse);
        assertThat(dbListResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(CICDJobRunDTO::getDuration).reversed());

        dbListResponse = ciCdPipelinesAggsService.listCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().build(), Map.of("job_run_number", SortingOrder.ASC), null, PAGE_NUMBER, PAGE_SIZE);
        Assert.assertNotNull(dbListResponse);
        assertThat(dbListResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(CICDJobRunDTO::getJobRunNumber));

        dbListResponse = ciCdPipelinesAggsService.listCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().build(), Map.of("job_run_number", SortingOrder.DESC), null, PAGE_NUMBER, PAGE_SIZE);
        Assert.assertNotNull(dbListResponse);
        assertThat(dbListResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(CICDJobRunDTO::getJobRunNumber).reversed());

        dbListResponse = ciCdPipelinesAggsService.listCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().build(), Map.of("instance_name", SortingOrder.ASC), null, PAGE_NUMBER, PAGE_SIZE);
        Assert.assertNotNull(dbListResponse);
        assertThat(dbListResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(CICDJobRunDTO::getCicdInstanceName));

        dbListResponse = ciCdPipelinesAggsService.listCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().build(), Map.of("instance_name", SortingOrder.DESC), null, PAGE_NUMBER, PAGE_SIZE);
        Assert.assertNotNull(dbListResponse);
        assertThat(dbListResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(CICDJobRunDTO::getCicdInstanceName).reversed());

        dbListResponse = ciCdPipelinesAggsService.listCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().build(), Map.of("job_normalized_full_name", SortingOrder.ASC), null, PAGE_NUMBER, PAGE_SIZE);
        Assert.assertNotNull(dbListResponse);
        assertThat(dbListResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(CICDJobRunDTO::getJobNormalizedFullName));

        dbListResponse = ciCdPipelinesAggsService.listCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().build(), Map.of("job_normalized_full_name", SortingOrder.DESC), null, PAGE_NUMBER, PAGE_SIZE);
        Assert.assertNotNull(dbListResponse);
        assertThat(dbListResponse.getRecords()).isSortedAccordingTo(Comparator.comparing(CICDJobRunDTO::getJobNormalizedFullName).reversed());

    }

    @Test
    public void testGroupByAndCalculateCiCdJobRuns() throws SQLException {
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

        List<Map<String, Object>> metadatas = List.of(
                generateMetdata(List.of("CD_only_Docker_Registry_Env1"), List.of("CD_only_Docker_Registry_Infrastructure1"), List.of("Kubernetes1"), List.of("Deploy_to_GCP1"), "https://github.com/test-org/repo1.git", "branch1", false, List.of("delegate1")),
                generateMetdata(List.of("env2"), List.of("if0", "if2"), List.of("deploy"), List.of("fieldService"), "https://github.com/test-org/repo2.git", "PROP-3004-main", true, List.of("tag2")),
                generateMetdata(List.of("env1", "env3"), List.of("if1", "if2"), List.of("Kubernetes"), List.of("gitService"), "https://github.com/test-org/repo3.git", "dev", false, List.of("0.1123-tag")),
                generateMetdata(List.of("CD_only_Docker_Registry_Env1"), List.of("CD_only_Docker_Registry_Infrastructure1"), List.of("Kubernetes1"), List.of("Deploy_to_GCP1"), "https://github.com/test-org/repo1.git", "branch1", false, List.of("delegate1")),
                generateMetdata(List.of("env2"), List.of("if0", "if2"), List.of("deploy"), List.of("fieldService"), "https://github.com/test-org/repo2.git", "PROP-3004-main", true, List.of("tag2")),
                generateMetdata(List.of("env1", "env3"), List.of("if1", "if2"), List.of("Kubernetes"), List.of("gitService"), "https://github.com/test-org/repo3.git", "dev", false, List.of("0.1123-tag")),
                generateMetdata(List.of("CD_only_Docker_Registry_Env1"), List.of("CD_only_Docker_Registry_Infrastructure1"), List.of("Kubernetes1"), List.of("Deploy_to_GCP1"), "https://github.com/test-org/repo1.git", "branch1", false, List.of("delegate1")),
                generateMetdata(List.of("env2"), List.of("if0", "if2"), List.of("deploy"), List.of("fieldService"), "https://github.com/test-org/repo2.git", "PROP-3004-main", true, List.of("tag2")),
                generateMetdata(List.of("env1", "env3"), List.of("if1", "if2"), List.of("Kubernetes"), List.of("gitService"), "https://github.com/test-org/repo3.git", "dev", false, List.of("0.1123-tag")),
                generateMetdata(List.of("env2"), List.of("if3", "if1"), List.of("deploy1"), List.of("fieldService", "gitService"), "https://github.com/test-org/repo4.git", "dev2", false, List.of("tag3"))
        );

        for (int i = 0; i < n; i++) {
            Instant day = start.plus(i, ChronoUnit.DAYS);
            CICDJobRun cicdJobFalconRun = CICDJobRunUtils.createCICDJobRunWithMetadata(ciCdJobRunsDatabaseService, jobFalcon, company, i, day, 70, null, null, metadatas.get(i));

            CICDJobRun cicdJob1Run = CICDJobRunUtils.createCICDJobRunWithMetadata(ciCdJobRunsDatabaseService, job1, company, i, day, 40, jobFalcon.getJobFullName(), cicdJobFalconRun.getJobRunNumber(), metadatas.get(i));
            //CiCdJobRunTriggerUtils.createCiCdJobRunTrigger(ciCdJobRunTriggersDBService, company, cicdJob1Run, jobFalcon.getJobFullName(), cicdJobFalconRun.getJobRunNumber());

            CICDJobRun cicdJob2Run = CICDJobRunUtils.createCICDJobRunWithMetadata(ciCdJobRunsDatabaseService, job2, company, i, day, 50, jobFalcon.getJobFullName(), cicdJobFalconRun.getJobRunNumber(), metadatas.get(i));
            //CiCdJobRunTriggerUtils.createCiCdJobRunTrigger(ciCdJobRunTriggersDBService, company, cicdJob2Run, jobFalcon.getJobFullName(), cicdJobFalconRun.getJobRunNumber());
            CICDJobRun cicdJob3Run = CICDJobRunUtils.createCICDJobRunWithMetadata(ciCdJobRunsDatabaseService, job3, company, i, day, 60, jobFalcon.getJobFullName(), cicdJobFalconRun.getJobRunNumber(), metadatas.get(i));
            //CiCdJobRunTriggerUtils.createCiCdJobRunTrigger(ciCdJobRunTriggersDBService, company, cicdJob3Run, jobFalcon.getJobFullName(), cicdJobFalconRun.getJobRunNumber());

            CICDJobRun cicdJob1_1Run = CICDJobRunUtils.createCICDJobRunWithMetadata(ciCdJobRunsDatabaseService, job1_1, company, i, day, 30, job1.getJobFullName(), cicdJob1Run.getJobRunNumber(), metadatas.get(i));
            //CiCdJobRunTriggerUtils.createCiCdJobRunTrigger(ciCdJobRunTriggersDBService, company, cicdJob1_1Run, job1.getJobFullName(), cicdJob1Run.getJobRunNumber());

            CICDJobRun cicdJob1_2Run = CICDJobRunUtils.createCICDJobRunWithMetadata(ciCdJobRunsDatabaseService, job1_2, company, i, day, 20, job1.getJobFullName(), cicdJob1Run.getJobRunNumber(), metadatas.get(i));
            //CiCdJobRunTriggerUtils.createCiCdJobRunTrigger(ciCdJobRunTriggersDBService, company, cicdJob1_2Run, job1.getJobFullName(), cicdJob1Run.getJobRunNumber());

            CICDJobRun cicdJob1_3Run = CICDJobRunUtils.createCICDJobRunWithMetadata(ciCdJobRunsDatabaseService, job1_3, company, i, day, 10, job1.getJobFullName(), cicdJob1Run.getJobRunNumber(), metadatas.get(i));
            //CiCdJobRunTriggerUtils.createCiCdJobRunTrigger(ciCdJobRunTriggersDBService, company, cicdJob1_3Run, job1.getJobFullName(), cicdJob1Run.getJobRunNumber());
        }
        // endregion

        DbListResponse<DbAggregationResult> results = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(
                company, CiCdPipelineJobRunsFilter.builder()
                        .across(CiCdPipelineJobRunsFilter.DISTINCT.trend)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.count)
                        .services(List.of("fieldService"))
                        .environments(List.of("env2"))
                        .excludeDeploymentTypes(List.of("Kubernetes"))
                        .partialMatch(Map.of("tags", Map.of("$begins", "tag")))
                        .build(), false, null);
        Assert.assertEquals(results.getRecords().size(), 4);
    }

    private Map<String, Object> generateMetdata(List<String> env_ids, List<String> infra_ids,
                                                List<String> service_types, List<String> service_ids,
                                                String repository, String branch,
                                                boolean rollback, List<String> tags) {
        return Map.of("env_ids", env_ids, "infra_ids", infra_ids, "service_types", service_types,
                "service_ids", service_ids, "repo_url", repository, "branch", branch,
                "rollback", rollback, "tags", tags);
    }

    private List<String> getKeys(DbListResponse<DbAggregationResult> dbAggsResponse) {
        return dbAggsResponse.getRecords().stream()
                .map(result -> (StringUtils.isEmpty(result.getKey()) || "null".equals(result.getKey())) ? null : result.getKey())
                .collect(Collectors.toList());
    }

    private List<String> getDbListKeys(DbListResponse<CICDJobRunDTO> dbListResponse) {
        return dbListResponse.getRecords().stream().map(CICDJobRunDTO::getCicdUserId).collect(Collectors.toList());
    }
}