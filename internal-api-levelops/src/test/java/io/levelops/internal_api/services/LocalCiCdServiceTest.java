package io.levelops.internal_api.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.CICDInstance;
import io.levelops.commons.databases.models.database.CICDJob;
import io.levelops.commons.databases.models.database.CICDJobRun;
import io.levelops.commons.databases.models.database.CiCdScmMapping;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.QueryFilter;
import io.levelops.commons.databases.models.database.scm.DbScmCommit;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.databases.models.filters.CICD_TYPE;
import io.levelops.commons.databases.models.filters.VCS_TYPE;
import io.levelops.commons.databases.services.CiCdInstancesDatabaseService;
import io.levelops.commons.databases.services.CiCdJobRunStageDatabaseService;
import io.levelops.commons.databases.services.CiCdJobRunStageStepsDatabaseService;
import io.levelops.commons.databases.services.CiCdJobRunsDatabaseService;
import io.levelops.commons.databases.services.CiCdJobsDatabaseService;
import io.levelops.commons.databases.services.CiCdScmMappingService;
import io.levelops.commons.databases.services.DatabaseSchemaService;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.ScmAggService;
import io.levelops.commons.databases.services.SonarQubeAggService;
import io.levelops.commons.databases.services.SonarQubeProjectService;
import io.levelops.commons.databases.services.UserIdentityService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class LocalCiCdServiceTest {
    private static final UUID JOB_ID = UUID.randomUUID();
    private static final String company = "test";
    private static final ObjectMapper m = DefaultObjectMapper.get();
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;
    private static SonarQubeProjectService sonarQubeProjectService;
    private static ScmAggService scmAggService;
    private static CiCdScmMappingService ciCdScmMappingService;
    private static CiCdScmMappingService cicdScmMappingService;
    private static SonarQubeAggService sonarQubeAggService;
    private static UserIdentityService userIdentityService;
    private static String gitHubIntegrationId = "2";
    private static CiCdInstancesDatabaseService ciCdInstancesDatabaseService;
    private static CiCdJobsDatabaseService ciCdJobsDatabaseService;
    private static CiCdJobRunsDatabaseService ciCdJobRunsDatabaseService;
    private static CiCdJobRunsDatabaseService dbService;
    private static CiCdJobRunStageDatabaseService cicdStages;
    private static CiCdJobRunStageStepsDatabaseService cicdSteps;
    private static CICDInstance cicdInstance;
    final static DbScmUser testScmUser = DbScmUser.builder()
            .integrationId(gitHubIntegrationId)
            .cloudId("ashish-levelops")
            .displayName("ashish-levelops")
            .build();
    private static LocalCiCdService localCiCdService;
    private static String jobrunId;
    private static String jobrunId2;
    private static String jobrunId3;


    @BeforeClass
    public static void setup() throws SQLException, IOException {
        if (dataSource != null)
            return;

        dataSource = setUpDataSource(pg, company);
        IntegrationService integrationService = new IntegrationService(dataSource);
        new DatabaseSchemaService(dataSource).ensureSchemaExistence(company);
        integrationService.ensureTableExistence(company);
        userIdentityService = new UserIdentityService(dataSource);
        scmAggService = new ScmAggService(dataSource, userIdentityService);
        sonarQubeProjectService = new SonarQubeProjectService(dataSource);
        ciCdInstancesDatabaseService = new CiCdInstancesDatabaseService(dataSource);
        ciCdJobsDatabaseService = new CiCdJobsDatabaseService(dataSource);
        cicdScmMappingService = new CiCdScmMappingService(dataSource);
        ciCdJobRunsDatabaseService = new CiCdJobRunsDatabaseService(DefaultObjectMapper.get(), dataSource);
        dbService = new CiCdJobRunsDatabaseService(DefaultObjectMapper.get(), dataSource);
        cicdStages = new CiCdJobRunStageDatabaseService(dataSource, DefaultObjectMapper.get());
        cicdSteps = new CiCdJobRunStageStepsDatabaseService(dataSource);
        cicdScmMappingService = new CiCdScmMappingService(dataSource);
        userIdentityService.ensureTableExistence(company);
        scmAggService.ensureTableExistence(company);
        sonarQubeProjectService.ensureTableExistence(company);
        localCiCdService = new LocalCiCdService(ciCdJobRunsDatabaseService, cicdScmMappingService, ciCdJobsDatabaseService, scmAggService,
                cicdStages, null);
        ciCdInstancesDatabaseService.ensureTableExistence(company);
        ciCdJobsDatabaseService.ensureTableExistence(company);
        ciCdJobRunsDatabaseService.ensureTableExistence(company);
        dbService.ensureTableExistence(company);
        cicdStages.ensureTableExistence(company);
        cicdSteps.ensureTableExistence(company);
        cicdScmMappingService.ensureTableExistence(company);
        integrationService.insert(company, Integration.builder().application("github").name("github-integ")
                .status("enabled").id("1").build());
        integrationService.insert(company, Integration.builder().application("github").name("github-integ-1")
                .status("enabled").id("2").build());
        cicdInstance = createCiCdInstance(ciCdInstancesDatabaseService, company, 0);
        DbScmCommit scmCommit1 = DbScmCommit.builder()
                .integrationId("1")
                .repoIds(List.of("ctlo2020/repo1"))
                .project("test_project")
                .authorInfo(testScmUser)
                .committerInfo(testScmUser)
                .commitSha("sha1")
                .author("ashish-levelops")
                .committedAt(Instant.EPOCH.getEpochSecond())
                .committer("ashish-levelops")
                .filesCt(1)
                .additions(0)
                .deletions(0)
                .changes(0)
                .ingestedAt(Instant.EPOCH.getEpochSecond())
                .vcsType(VCS_TYPE.GIT)
                .createdAt(Instant.EPOCH.getEpochSecond())
                .build();
        DbScmCommit scmCommit2 = DbScmCommit.builder()
                .integrationId("1")
                .repoIds(List.of("ctlo2020/repo2"))
                .project("test_project")
                .authorInfo(testScmUser)
                .committerInfo(testScmUser)
                .commitSha("sha1")
                .author("ashish-levelops")
                .committedAt(Instant.EPOCH.getEpochSecond())
                .committer("ashish-levelops")
                .filesCt(1)
                .additions(0)
                .deletions(0)
                .changes(0)
                .ingestedAt(Instant.EPOCH.getEpochSecond())
                .vcsType(VCS_TYPE.GIT)
                .createdAt(Instant.EPOCH.getEpochSecond())
                .build();
        DbScmCommit scmCommit3 = DbScmCommit.builder()
                .integrationId("1")
                .repoIds(List.of("ctlo2020/repo3"))
                .project("test_project")
                .authorInfo(testScmUser)
                .committerInfo(testScmUser)
                .commitSha("sha2")
                .author("ashish-levelops")
                .committedAt(Instant.EPOCH.getEpochSecond())
                .committer("ashish-levelops")
                .filesCt(1)
                .additions(0)
                .deletions(0)
                .changes(0)
                .ingestedAt(Instant.EPOCH.getEpochSecond())
                .vcsType(VCS_TYPE.GIT)
                .createdAt(Instant.EPOCH.getEpochSecond())
                .build();
        DbScmCommit scmCommit4 = DbScmCommit.builder()
                .integrationId("1")
                .repoIds(List.of("ctlo2020/repo1"))
                .project("test_project")
                .authorInfo(testScmUser)
                .committerInfo(testScmUser)
                .commitSha("sha3")
                .author("ashish-levelops")
                .committedAt(Instant.EPOCH.getEpochSecond())
                .committer("ashish-levelops")
                .filesCt(1)
                .additions(0)
                .deletions(0)
                .changes(0)
                .ingestedAt(Instant.EPOCH.getEpochSecond())
                .vcsType(VCS_TYPE.GIT)
                .createdAt(Instant.EPOCH.getEpochSecond())
                .build();
        DbScmCommit scmCommit5 = DbScmCommit.builder()
                .integrationId("1")
                .repoIds(List.of("ctlo2020/repo2"))
                .project("test_project")
                .authorInfo(testScmUser)
                .committerInfo(testScmUser)
                .commitSha("sha4")
                .author("ashish-levelops")
                .committedAt(Instant.EPOCH.getEpochSecond())
                .committer("ashish-levelops")
                .filesCt(1)
                .additions(0)
                .deletions(0)
                .changes(0)
                .ingestedAt(Instant.EPOCH.getEpochSecond())
                .vcsType(VCS_TYPE.GIT)
                .createdAt(Instant.EPOCH.getEpochSecond())
                .build();
        DbScmCommit scmCommit6 = DbScmCommit.builder()
                .integrationId("2")
                .repoIds(List.of("ctlo2020/repo1"))
                .project("test_project")
                .authorInfo(testScmUser)
                .committerInfo(testScmUser)
                .commitSha("sha1")
                .author("ashish-levelops")
                .committedAt(Instant.EPOCH.getEpochSecond())
                .committer("ashish-levelops")
                .filesCt(1)
                .additions(0)
                .deletions(0)
                .changes(0)
                .ingestedAt(Instant.EPOCH.getEpochSecond())
                .vcsType(VCS_TYPE.GIT)
                .createdAt(Instant.EPOCH.getEpochSecond())
                .build();
        scmAggService.insertCommit(company, scmCommit1);
        scmAggService.insertCommit(company, scmCommit2);
        scmAggService.insertCommit(company, scmCommit3);
        scmAggService.insertCommit(company, scmCommit4);
        scmAggService.insertCommit(company, scmCommit5);
        scmAggService.insertCommit(company, scmCommit6);
        CICDJob cicdJob = CICDJob.builder().jobName("Sample Job")
                .branchName("smaple-1")
                .jobFullName("Sample")
                .scmUrl("https://gitlab.com/ctlo2020/repo2.git")
                .build();
        String jobId = ciCdJobsDatabaseService.insert(company, cicdJob);
        CICDJobRun cicdJobRun = CICDJobRun.builder().cicdJobId(UUID.randomUUID())
                .jobRunNumber(12L)
                .cicdJobId(UUID.fromString(jobId))
                .scmCommitIds(List.of("sha1", "sha2", "sha3"))
                .build();
        jobrunId = ciCdJobRunsDatabaseService.insert(company, cicdJobRun);
        cicdJob = CICDJob.builder().jobName("Sample Job 2")
                .branchName("smaple-2")
                .jobFullName("Sample 2")
                .scmUrl(null)
                .build();
        jobId = ciCdJobsDatabaseService.insert(company, cicdJob);
        cicdJobRun = CICDJobRun.builder().cicdJobId(UUID.randomUUID())
                .jobRunNumber(12L)
                .cicdJobId(UUID.fromString(jobId))
                .scmCommitIds(List.of("sha1"))
                .build();
        jobrunId2 = ciCdJobRunsDatabaseService.insert(company, cicdJobRun);

        cicdJob = CICDJob.builder().jobName("Sample Job 3")
                .branchName("smaple-3")
                .jobFullName("Sample 3")
                .scmUrl("https://gitlab.com/ctlo2020/repo1.git")
                .build();
        jobId = ciCdJobsDatabaseService.insert(company, cicdJob);
        cicdJobRun = CICDJobRun.builder().cicdJobId(UUID.randomUUID())
                .jobRunNumber(12L)
                .cicdJobId(UUID.fromString(jobId))
                .scmCommitIds(List.of("sha1"))
                .build();
        jobrunId3 = ciCdJobRunsDatabaseService.insert(company, cicdJobRun);

    }

    @Deprecated
    public static CICDInstance createCiCdInstance(CiCdInstancesDatabaseService ciCdInstancesDatabaseService, final String company, int i) throws SQLException {
        return createCiCdInstance(ciCdInstancesDatabaseService, company, "1", i);
    }

    private static CICDInstance createCiCdInstance(CiCdInstancesDatabaseService ciCdInstancesDatabaseService, final String company, String integrationId, int i) throws SQLException {
        CICDInstance.CICDInstanceBuilder bldr = CICDInstance.builder()
                .id(UUID.randomUUID())
                .name("instance-name-" + i)
                .url("https://jenkins.dev.levelops.io/")
                .integrationId(integrationId)
                .type(CICD_TYPE.jenkins.toString());
        CICDInstance cicdInstance = bldr.build();
        String id = ciCdInstancesDatabaseService.insert(company, cicdInstance);
        return cicdInstance.toBuilder().id(UUID.fromString(id)).build();
    }


    public static DataSource setUpDataSource(SingleInstancePostgresRule pg, String company) throws SQLException, IOException {
        DataSource dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        JdbcTemplate template = new JdbcTemplate(dataSource);
        List.of(
                "CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";",
                "DROP SCHEMA IF EXISTS " + company + " CASCADE; ",
                "CREATE SCHEMA " + company + " ; "
        ).forEach(template::execute);
        return dataSource;
    }


    @Test
    public void testGetValidRepoIdFromJobRun() {
        Assert.assertEquals(null, LocalCiCdService.parseRepoIdFromScmUrl(JOB_ID, null));
        Assert.assertEquals(null, LocalCiCdService.parseRepoIdFromScmUrl(JOB_ID, ""));
        Assert.assertEquals(null, LocalCiCdService.parseRepoIdFromScmUrl(JOB_ID, "   "));

        Assert.assertEquals("virajajgaonkar/leetcode", LocalCiCdService.parseRepoIdFromScmUrl(JOB_ID, "https://bitbucket.org/virajajgaonkar/leetcode.git"));
        Assert.assertEquals("virajajgaonkar/job-leetcode", LocalCiCdService.parseRepoIdFromScmUrl(JOB_ID, "git@bitbucket.org:virajajgaonkar/job-leetcode.git"));

        Assert.assertEquals("virajajgaonkar/leetcode.git", LocalCiCdService.parseRepoIdFromScmUrl(JOB_ID, "https://bitbucket.org/virajajgaonkar/leetcode.git.git"));
        Assert.assertEquals("virajajgaonkar/job-leetcode.git", LocalCiCdService.parseRepoIdFromScmUrl(JOB_ID, "git@bitbucket.org:virajajgaonkar/job-leetcode.git.git"));

        Assert.assertEquals("pr-demo-repo", LocalCiCdService.parseRepoIdFromScmUrl(JOB_ID, "https://dev.azure.com/cgn-test/project-test-11/_git/pr-demo-repo"));
        Assert.assertEquals("repo-test-7", LocalCiCdService.parseRepoIdFromScmUrl(JOB_ID, "https://dev.azure.com/cgn-test/project-test-7/_git/repo-test-7"));
    }
}