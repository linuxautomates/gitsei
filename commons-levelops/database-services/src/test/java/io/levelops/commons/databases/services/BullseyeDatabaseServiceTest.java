package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import io.levelops.commons.databases.models.database.CICDJob;
import io.levelops.commons.databases.models.database.CICDJobRun;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.bullseye.BullseyeBuild;
import io.levelops.commons.databases.models.database.bullseye.BullseyeFolder;
import io.levelops.commons.databases.models.database.bullseye.BullseyeSourceFile;
import io.levelops.commons.databases.models.database.bullseye.CodeCoverageReport;
import io.levelops.commons.databases.models.database.bullseye.DbBullseyeBuild;
import io.levelops.commons.databases.models.filters.BullseyeBuildFilter;
import io.levelops.commons.databases.models.filters.BullseyeFileFilter;
import io.levelops.commons.databases.models.filters.util.SortingConverter;
import io.levelops.commons.databases.models.response.AcrossUniqueKey;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.bullseye.BullseyeDatabaseService;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class BullseyeDatabaseServiceTest {

    private static final String COMPANY = "test";
    private static final String CICD_JOB_NAME = "job_name";
    private static final String CICD_JOB_FULL_NAME = "job_full_name";
    private static final String CICD_JOB_NORMALIZED_FULL_NAME = "job_normalized_full_name";
    private static final Long JOB_NUMBER = 1L;
    private static final Integer PRODUCT_ID = 1;
    private static IntegrationService integrationService;

    private static final XmlMapper XML_MAPPER = new XmlMapper();

    private static final List<String> buildNames = List.of("test.cov");
    private static final List<String> projectList = List.of("job_name");
    private static final List<String> directories = List.of("/Users/joe/Downloads/levelops_code_coverage/");


    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    private static DataSource dataSource;
    private static BullseyeDatabaseService bullseyeDatabaseService;
    private static CodeCoverageReport report;
    private static BullseyeBuild project;
    private static String jobId;
    private static CiCdJobsDatabaseService ciCdJobsDatabaseService;

    @BeforeClass
    public static void setup() throws SQLException, IOException {
        // setup postgres
        if (dataSource != null) {
            return;
        }
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        //setDataSource();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();

        // setup schema
        new DatabaseSchemaService(dataSource).ensureSchemaExistence(COMPANY);

        // user service
        UserService userService = new UserService(dataSource, XML_MAPPER);
        userService.ensureTableExistence(COMPANY);

        integrationService = new IntegrationService(dataSource);
        integrationService.ensureTableExistence(COMPANY);

        // product service
        ProductService productService = new ProductService(dataSource);
        productService.ensureTableExistence(COMPANY);

        Integration integration = Integration.builder()
                .id("1")
                .name("name")
                .url("http")
                .status("good")
                .application("jira")
                .description("desc")
                .satellite(true)
                .build();
        integrationService.insert(COMPANY, integration);

        // cicd instance
        CiCdInstancesDatabaseService ciCdInstancesDatabaseService = new CiCdInstancesDatabaseService(dataSource);
        ciCdInstancesDatabaseService.ensureTableExistence(COMPANY);

        // cicd job
        ciCdJobsDatabaseService = new CiCdJobsDatabaseService(dataSource);
        ciCdJobsDatabaseService.ensureTableExistence(COMPANY);
        CICDJob job = CICDJob.builder()
                .jobName(CICD_JOB_NAME)
                .jobFullName(CICD_JOB_FULL_NAME)
                .jobNormalizedFullName(CICD_JOB_NORMALIZED_FULL_NAME)
                .build();
        jobId = ciCdJobsDatabaseService.insert(COMPANY, job);

        // cicd job run
        CiCdJobRunsDatabaseService ciCdJobRunsDatabaseService = new CiCdJobRunsDatabaseService(XML_MAPPER, dataSource);
        ciCdJobRunsDatabaseService.ensureTableExistence(COMPANY);
        CICDJobRun jobRun = CICDJobRun.builder()
                .cicdJobId(UUID.fromString(jobId))
                .jobRunNumber(JOB_NUMBER)
                .startTime(Instant.ofEpochSecond(1599955200))
                .duration(30)
                .scmCommitIds(List.of())
                .build();
        String jobRunId = ciCdJobRunsDatabaseService.insert(COMPANY, jobRun);

        // bullseye service
        bullseyeDatabaseService = new BullseyeDatabaseService(dataSource);
        bullseyeDatabaseService.ensureTableExistence(COMPANY);

        // xml parsing
        XML_MAPPER.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        final String reportPath = ResourceUtils.getResourceAsString("json/databases/bullseye_report.xml");
        report = XML_MAPPER.readValue(reportPath, CodeCoverageReport.class);

        // insert final pojo
        project = BullseyeBuild.fromCodeCoverageReport(report, UUID.fromString(jobRunId), CICD_JOB_NAME, "123");
        bullseyeDatabaseService.insert(COMPANY, project);

    }

    @Test
    public void deserialize() {
        Assert.assertNotNull(report);
        Assertions.assertThat(report.getSourceFiles()).isNullOrEmpty();
        for (BullseyeFolder folder : CollectionUtils.emptyIfNull(project.getFolders())) {
            try {
                deserializeUtil(folder);
            } catch (Exception e) {
                Assert.fail("bullseye project is not deserialized correctly");
            }
        }
    }

    private void deserializeUtil(BullseyeFolder folder) {
        for (BullseyeFolder childFolder : CollectionUtils.emptyIfNull(folder.getFolders())) {
            deserializeUtil(childFolder);
        }
    }

    @Test
    public void projectMetrics() {
        Assertions.assertThat(report.getFunctionsCovered()).isEqualTo(784);
        Assertions.assertThat(report.getTotalFunctions()).isEqualTo(884);
        Assertions.assertThat(report.getDecisionsCovered()).isEqualTo(10294);
        Assertions.assertThat(report.getTotalDecisions()).isEqualTo(14637);
        Assertions.assertThat(report.getConditionsCovered()).isEqualTo(12167);
        Assertions.assertThat(report.getTotalConditions()).isEqualTo(17713);
    }

    @Test
    public void listProjects() {
        DbListResponse<DbBullseyeBuild> projects = bullseyeDatabaseService.listProjects(COMPANY,
                BullseyeBuildFilter.builder().build(),
                SortingConverter.fromFilter(List.of()), 0, 100);
        Assert.assertNotNull(projects);
        Assertions.assertThat(projects.getRecords().size()).isEqualTo(1);

        DbBullseyeBuild actualProject = projects.getRecords().stream().findFirst().get();
        Assertions.assertThat(project.getFunctionsCovered()).isEqualTo(actualProject.getFunctionsCovered());
        Assertions.assertThat(project.getTotalFunctions()).isEqualTo(actualProject.getTotalFunctions());
        Assertions.assertThat(project.getDecisionsCovered()).isEqualTo(actualProject.getDecisionsCovered());
        Assertions.assertThat(project.getTotalDecisions()).isEqualTo(actualProject.getTotalDecisions());
        Assertions.assertThat(project.getConditionsCovered()).isEqualTo(actualProject.getConditionsCovered());
        Assertions.assertThat(project.getTotalConditions()).isEqualTo(actualProject.getTotalConditions());
        Assertions.assertThat(project.getFileHash()).isEqualTo("123");
    }

    @Test
    public void listProjectsWithFilter() throws SQLException {
        DbListResponse<DbBullseyeBuild> projects = bullseyeDatabaseService.listProjects(COMPANY,
                BullseyeBuildFilter.builder()
                        .jobNames(List.of(CICD_JOB_NAME))
                        .build(),
                SortingConverter.fromFilter(List.of()), 0, 100);
        Assert.assertNotNull(projects);
        Assertions.assertThat(Integer.valueOf(1).equals(projects.getRecords().size()));
        DbBullseyeBuild actualProject = projects.getRecords().stream().findFirst().get();
        Assertions.assertThat(actualProject.getCicdJobName().equals(CICD_JOB_NAME));

        projects = bullseyeDatabaseService.listProjects(COMPANY,
                BullseyeBuildFilter.builder()
                        .jobFullNames(List.of(CICD_JOB_FULL_NAME))
                        .build(),
                SortingConverter.fromFilter(List.of()), 0, 100);
        Assert.assertNotNull(projects);
        Assertions.assertThat(Integer.valueOf(1).equals(projects.getRecords().size()));
        actualProject = projects.getRecords().stream().findFirst().get();
        Assertions.assertThat(actualProject.getCicdJobFullName().equals(CICD_JOB_FULL_NAME));

        projects = bullseyeDatabaseService.listProjects(COMPANY,
                BullseyeBuildFilter.builder()
                        .jobNormalizedFullNames(List.of(CICD_JOB_NORMALIZED_FULL_NAME))
                        .build(),
                SortingConverter.fromFilter(List.of()), 0, 100);
        Assert.assertNotNull(projects);
        Assertions.assertThat(Integer.valueOf(1).equals(projects.getRecords().size()));
        actualProject = projects.getRecords().stream().findFirst().get();
        Assertions.assertThat(actualProject.getCicdJobNormalizedFullName().equals(CICD_JOB_NORMALIZED_FULL_NAME));

        //negative results
        projects = bullseyeDatabaseService.listProjects(COMPANY,
                BullseyeBuildFilter.builder()
                        .jobNames(List.of(CICD_JOB_NAME))
                        .partialMatch(Map.of("name", Map.of("$contains", "abc")))
                        .build(),
                SortingConverter.fromFilter(List.of()), 0, 100);
        Assert.assertNotNull(projects);
        Assert.assertEquals(0, projects.getRecords().size());

        projects = bullseyeDatabaseService.listProjects(COMPANY,
                BullseyeBuildFilter.builder()
                        .jobNames(List.of(CICD_JOB_NAME))
                        .partialMatch(Map.of("name", Map.of("$begins", "ab")))
                        .build(),
                SortingConverter.fromFilter(List.of()), 0, 100);
        Assert.assertNotNull(projects);
        Assert.assertEquals(0, projects.getRecords().size());

        projects = bullseyeDatabaseService.listProjects(COMPANY,
                BullseyeBuildFilter.builder()
                        .jobNames(List.of(CICD_JOB_NAME))
                        .partialMatch(Map.of("name", Map.of("$ends", "ers")))
                        .build(),
                SortingConverter.fromFilter(List.of()), 0, 100);
        Assert.assertNotNull(projects);
        Assert.assertEquals(0, projects.getRecords().size());

        // positive results
        projects = bullseyeDatabaseService.listProjects(COMPANY,
                BullseyeBuildFilter.builder()
                        .jobNames(List.of(CICD_JOB_NAME))
                        .partialMatch(Map.of("name", Map.of("$contains", "tes")))
                        .build(),
                SortingConverter.fromFilter(List.of()), 0, 100);
        Assert.assertNotNull(projects);
        Assertions.assertThat(Integer.valueOf(1).equals(projects.getRecords().size()));
        actualProject = projects.getRecords().stream().findFirst().get();
        Assertions.assertThat(actualProject.getCicdJobNormalizedFullName().equals(CICD_JOB_NORMALIZED_FULL_NAME));

        projects = bullseyeDatabaseService.listProjects(COMPANY,
                BullseyeBuildFilter.builder()
                        .jobNames(List.of(CICD_JOB_NAME))
                        .partialMatch(Map.of("name", Map.of("$begins", "te")))
                        .build(),
                SortingConverter.fromFilter(List.of()), 0, 100);
        Assert.assertNotNull(projects);
        Assertions.assertThat(Integer.valueOf(1).equals(projects.getRecords().size()));
        actualProject = projects.getRecords().stream().findFirst().get();
        Assertions.assertThat(actualProject.getCicdJobNormalizedFullName().equals(CICD_JOB_NORMALIZED_FULL_NAME));

        projects = bullseyeDatabaseService.listProjects(COMPANY,
                BullseyeBuildFilter.builder()
                        .jobNames(List.of(CICD_JOB_NAME))
                        .partialMatch(Map.of("name", Map.of("$ends", "t.cov")))
                        .build(),
                SortingConverter.fromFilter(List.of()), 0, 100);
        Assert.assertNotNull(projects);
        Assertions.assertThat(Integer.valueOf(1).equals(projects.getRecords().size()));
        actualProject = projects.getRecords().stream().findFirst().get();
        Assertions.assertThat(actualProject.getCicdJobNormalizedFullName().equals(CICD_JOB_NORMALIZED_FULL_NAME));

        //sql injection attack
        projects = bullseyeDatabaseService.listProjects(COMPANY,
                BullseyeBuildFilter.builder()
                        .jobNames(List.of(CICD_JOB_NAME))
                        .partialMatch(Map.of("name", Map.of("$ends", "t.cov';drop table test.cicd_jobs;--")))
                        .build(),
                SortingConverter.fromFilter(List.of()), 0, 100);
        Assert.assertNotNull(projects);
        Assert.assertEquals(0, projects.getRecords().size());
        Optional<CICDJob> job = ciCdJobsDatabaseService.get(COMPANY, jobId);
        Assert.assertTrue(job.isPresent());

        //filehash
        projects = bullseyeDatabaseService.listProjects(COMPANY,
                BullseyeBuildFilter.builder()
                        .fileHashes(List.of("123"))
                        .build(),
                SortingConverter.fromFilter(List.of()), 0, 100);
        Assertions.assertThat(projects.getRecords()).hasSize(1);
        Assertions.assertThat(projects.getRecords().size() == 2);
        Assertions.assertThat(false);
    }

    @Test
    public void groupByProject() {

        //postive results
        DbListResponse<DbAggregationResult> response = bullseyeDatabaseService.groupByAndCalculateProjects(COMPANY,
                BullseyeBuildFilter.builder()
                        .across(BullseyeBuildFilter.Distinct.name)
                        .partialMatch(Map.of("name", Map.of("$contains", "est")))
                        .build(), null);
        List<DbAggregationResult> records = response.getRecords();
        Assertions.assertThat(CollectionUtils.isNotEmpty(records));
        Assertions.assertThat(records.stream().findFirst().get().getKey().equals(CICD_JOB_NAME));
        List<DbAggregationResult> expected = List.of(
                DbAggregationResult.builder().key(CICD_JOB_NAME).build()
        );
        verifyRecords(expected, response.getRecords(), true);

        response = bullseyeDatabaseService.groupByAndCalculateProjects(COMPANY,
                BullseyeBuildFilter.builder()
                        .partialMatch(Map.of("name", Map.of("$begins", "tes")))
                        .across(BullseyeBuildFilter.Distinct.job_full_name)
                        .build(), null);
        records = response.getRecords();
        Assertions.assertThat(CollectionUtils.isNotEmpty(records));
        Assertions.assertThat(records.stream().findFirst().get().getKey().equals(CICD_JOB_FULL_NAME));
        expected = List.of(DbAggregationResult.builder().key(CICD_JOB_FULL_NAME).build());
        verifyRecords(expected, response.getRecords(), true);

        response = bullseyeDatabaseService.groupByAndCalculateProjects(COMPANY,
                BullseyeBuildFilter.builder()
                        .partialMatch(Map.of("name", Map.of("$ends", "st.cov")))
                        .across(BullseyeBuildFilter.Distinct.job_full_name)
                        .build(), null);
        records = response.getRecords();
        Assertions.assertThat(CollectionUtils.isNotEmpty(records));
        Assertions.assertThat(records.stream().findFirst().get().getKey().equals(CICD_JOB_FULL_NAME));
        expected = List.of(DbAggregationResult.builder().key(CICD_JOB_FULL_NAME).build());
        verifyRecords(expected, response.getRecords(), true);

        //negative results
        response = bullseyeDatabaseService.groupByAndCalculateProjects(COMPANY,
                BullseyeBuildFilter.builder()
                        .partialMatch(Map.of("name", Map.of("$ends", "et.cov")))
                        .across(BullseyeBuildFilter.Distinct.job_full_name)
                        .build(), null);
        Assert.assertEquals(0, response.getRecords().size());

        response = bullseyeDatabaseService.groupByAndCalculateProjects(COMPANY,
                BullseyeBuildFilter.builder()
                        .partialMatch(Map.of("name", Map.of("$contains", "sdfv")))
                        .across(BullseyeBuildFilter.Distinct.job_full_name)
                        .build(), null);
        Assert.assertEquals(0, response.getRecords().size());

        response = bullseyeDatabaseService.groupByAndCalculateProjects(COMPANY,
                BullseyeBuildFilter.builder()
                        .partialMatch(Map.of("name", Map.of("$begins", "nwer")))
                        .across(BullseyeBuildFilter.Distinct.job_full_name)
                        .build(), null);
        ;
        Assert.assertEquals(0, response.getRecords().size());


        response = bullseyeDatabaseService.groupByAndCalculateProjects(COMPANY,
                BullseyeBuildFilter.builder()
                        .across(BullseyeBuildFilter.Distinct.job_full_name)
                        .build(), null);
        records = response.getRecords();
        Assertions.assertThat(CollectionUtils.isNotEmpty(records));
        Assertions.assertThat(records.stream().findFirst().get().getKey().equals(CICD_JOB_FULL_NAME));
        expected = List.of(DbAggregationResult.builder().key(CICD_JOB_FULL_NAME).build());
        verifyRecords(expected, response.getRecords(), true);

        response = bullseyeDatabaseService.groupByAndCalculateProjects(COMPANY,
                BullseyeBuildFilter.builder()
                        .across(BullseyeBuildFilter.Distinct.job_normalized_full_name)
                        .build(), null);
        records = response.getRecords();
        Assertions.assertThat(CollectionUtils.isNotEmpty(records));
        Assertions.assertThat(records.stream().findFirst().get().getKey().equals(CICD_JOB_NORMALIZED_FULL_NAME));
        expected = List.of(DbAggregationResult.builder().key(CICD_JOB_NORMALIZED_FULL_NAME).build());
        verifyRecords(expected, response.getRecords(), true);

        response = bullseyeDatabaseService.groupByAndCalculateProjects(COMPANY,
                BullseyeBuildFilter.builder()
                        .across(BullseyeBuildFilter.Distinct.project)
                        .build(), null);
        records = response.getRecords();
        Assertions.assertThat(CollectionUtils.isNotEmpty(records));
        Assertions.assertThat(records.stream().findFirst().get().getKey().equals(projectList.stream().findFirst()));
        expected = List.of(DbAggregationResult.builder().key(projectList.stream().findFirst().get()).build());
        verifyRecords(expected, response.getRecords(), true);

        response = bullseyeDatabaseService.groupByAndCalculateProjects(COMPANY,
                BullseyeBuildFilter.builder()
                        .across(BullseyeBuildFilter.Distinct.directory)
                        .build(), null);
        records = response.getRecords();
        Assertions.assertThat(CollectionUtils.isNotEmpty(records));
        Assertions.assertThat(records.stream().findFirst().get().getKey().equals(directories.stream().findFirst()));
        expected = List.of(DbAggregationResult.builder().key(directories.stream().findFirst().get()).build());
        verifyRecords(expected, response.getRecords(), true);

        response = bullseyeDatabaseService.groupByAndCalculateProjects(COMPANY,
                BullseyeBuildFilter.builder()
                        .across(BullseyeBuildFilter.Distinct.name)
                        .build(), null);
        records = response.getRecords();
        Assertions.assertThat(CollectionUtils.isNotEmpty(records));
        Assertions.assertThat(records.stream().findFirst().get().getKey().equals(buildNames.stream().findFirst()));
        expected = List.of(DbAggregationResult.builder().key(buildNames.stream().findFirst().get()).build());
        verifyRecords(expected, response.getRecords(), true);

        response = bullseyeDatabaseService.groupByAndCalculateProjects(COMPANY,
                BullseyeBuildFilter.builder()
                        .across(BullseyeBuildFilter.Distinct.trend)
                        .build(), null);
        records = response.getRecords();
        Assertions.assertThat(CollectionUtils.isNotEmpty(records));

        response = bullseyeDatabaseService.groupByAndCalculateProjects(COMPANY,
                BullseyeBuildFilter.builder()
                        .across(BullseyeBuildFilter.Distinct.job_run_id)
                        .build(), null);
        records = response.getRecords();
        Assertions.assertThat(CollectionUtils.isNotEmpty(records));
        Assertions.assertThat(StringUtils.isNotEmpty(records.stream().findFirst().get().getAdditionalKey()));
        Assertions.assertThat(records.stream().findFirst().get().getAdditionalKey().equals(projectList.stream().findFirst()));
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

    @Test
    public void listFiles() {
        DbListResponse<BullseyeSourceFile> actualFiles = bullseyeDatabaseService.listFiles(COMPANY,
                BullseyeBuildFilter.builder().build(), BullseyeFileFilter.builder().build(),
                SortingConverter.fromFilter(List.of()), 0, 100);
        Assertions.assertThat(Integer.valueOf(90).equals(actualFiles.getRecords().size()));
        String path1 = " ../source/programs/protected_cpp/!shared/";
        String path2 = "../source/programs/protected_cpp/af_allfiles/";
        actualFiles.getRecords().stream().forEach(file -> {
            Assertions.assertThat(file.getName().startsWith(path1) || file.getName().startsWith(path2));
        });
    }
}
