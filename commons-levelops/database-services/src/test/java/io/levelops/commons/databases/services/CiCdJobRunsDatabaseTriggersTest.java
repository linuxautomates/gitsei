package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.CICDInstance;
import io.levelops.commons.databases.models.database.CICDJob;
import io.levelops.commons.databases.models.database.CICDJobRun;
import io.levelops.commons.databases.models.database.CICDJobTrigger;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.services.organization.ProductsDatabaseService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.collections4.CollectionUtils;
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
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class CiCdJobRunsDatabaseTriggersTest {

    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();
    private static final String company = "test";

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
    private final List<String> ciCdJobRunIds = new ArrayList<>();
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
                String jobrunId = ciCdJobRunsDatabaseService.insert(company, cicdJobRun);
                ciCdJobRunIds.add(jobrunId);
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
    public void testGetJobRuns() throws SQLException, IOException {
        String data = ResourceUtils.getResourceAsString("json/databases/jenkins_plugin_result.json");
        List<CiCdAggsServiceTest.JobDetails> allJobDetails = MAPPER.readValue(data, MAPPER.getTypeFactory().constructCollectionType(List.class, CiCdAggsServiceTest.JobDetails.class));
        allJobDetails = fixJobRunTimestamps(allJobDetails, calculateOffset());
        CICDInstance cicdInstance = CiCdInstanceUtils.createCiCdInstance(ciCdInstancesDatabaseService, company, 0);
        setupJobsAndJobRuns(null, allJobDetails);
        setupJobsAndJobRuns(cicdInstance.getId(), allJobDetails);
        ciCdJobRunsDatabaseService.insertCICDJobRunTriggers(company, UUID.fromString(ciCdJobRunIds.get(0)), Set.of(CICDJobTrigger.builder()
                .buildNumber("123")
                .id("test-freestyle")
                .type("UpstreamBuild")
                .directParents(Set.of(
                        CICDJobTrigger.builder()
                                .buildNumber("11")
                                .id("test-pipeline")
                                .type("UpstreamBuild")
                                .directParents(Set.of(
                                        CICDJobTrigger.builder()
                                                .buildNumber("1")
                                                .id("test-multipipline")
                                                .type("UpstreamBuild")
                                                .build()))
                                .build()
                ))
                .build()), dataSource.getConnection());
        List<UUID> parentJobRunIds = ciCdJobRunsDatabaseService.
                getParentJobRunIds(company, List.of(UUID.fromString(ciCdJobRunIds.get(0))));
        Assertions.assertThat(parentJobRunIds.size()).isEqualTo(6);
        Assertions.assertThat(parentJobRunIds).isNotEmpty();

        ciCdJobRunsDatabaseService.insertCICDJobRunTriggers(company, UUID.fromString(ciCdJobRunIds.get(1)), Set.of(CICDJobTrigger.builder()
                .buildNumber("111")
                .id("test-freestyle-build")
                .type("UpstreamBuild")
                .directParents(Set.of(
                        CICDJobTrigger.builder()
                                .buildNumber("222")
                                .id("test-pipeline-build")
                                .type("UpstreamBuild")
                                .directParents(Set.of())
                                .build()
                ))
                .build()), dataSource.getConnection());
        parentJobRunIds = ciCdJobRunsDatabaseService.
                getParentJobRunIds(company, List.of(UUID.fromString(ciCdJobRunIds.get(0))));
        Assertions.assertThat(parentJobRunIds.size()).isEqualTo(6);
        Assertions.assertThat(parentJobRunIds).isNotEmpty();

        UUID insertJobRunTrigger = ciCdJobRunsDatabaseService.insertJobRunTrigger(company, UUID.fromString(ciCdJobRunIds.get(2)), CICDJobTrigger.builder()
                .buildNumber("111")
                .id("test-freestyle-build")
                .type("UpstreamBuild")
                .directParents(Set.of(
                        CICDJobTrigger.builder()
                                .buildNumber("222")
                                .id("test-pipeline-build")
                                .type("UpstreamBuild")
                                .directParents(Set.of())
                                .build()
                ))
                .build(), dataSource.getConnection());
        List<UUID> resultList = new ArrayList<>();
        ciCdJobRunsDatabaseService.populateAncestorJobRunIds(company, resultList, List.of(insertJobRunTrigger));
        Assertions.assertThat(resultList).isNotEmpty();
    }

}
