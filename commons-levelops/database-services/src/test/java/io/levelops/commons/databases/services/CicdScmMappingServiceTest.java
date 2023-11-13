package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.CICDJobRun;
import io.levelops.commons.databases.models.database.CiCdScmMapping;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.User;
import io.levelops.commons.databases.models.database.User.PasswordReset;
import io.levelops.commons.databases.models.database.access.RoleType;
import io.levelops.commons.databases.models.database.scm.DbScmCommit;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.databases.models.filters.VCS_TYPE;
import io.levelops.commons.databases.services.organization.TeamMembersDatabaseService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CicdScmMappingServiceTest {
    @Rule
    public SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    private UserService userService;
    private CiCdInstancesDatabaseService ciCdInstancesDatabaseService;
    private CiCdJobRunsDatabaseService jobRunService;
    private CiCdJobsDatabaseService jobService;
    private CiCdScmMappingService mappingService;
    private ScmAggService scmAggService;
    private IntegrationService integrationService;
    private static UserIdentityService userIdentityService;
    private static TeamMembersDatabaseService teamMembersDatabaseService;
    private DataSource dataSource;

    private String company = "test";


    @Before
    public void setup() throws SQLException {
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        JdbcTemplate template = new JdbcTemplate(dataSource);
        List.of(
                "CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";",

                "CREATE SCHEMA IF NOT EXISTS test; "
        ).forEach(template::execute);
        userService = new UserService(dataSource, DefaultObjectMapper.get());
        userService.ensureTableExistence(company);
        integrationService = new IntegrationService(dataSource);
        integrationService.ensureTableExistence(company);
        userIdentityService = new UserIdentityService(dataSource);
        scmAggService = new ScmAggService(dataSource, userIdentityService);
        teamMembersDatabaseService = new TeamMembersDatabaseService(dataSource, DefaultObjectMapper.get());
        userIdentityService.ensureTableExistence(company);
        scmAggService.ensureTableExistence(company);
        ciCdInstancesDatabaseService = new CiCdInstancesDatabaseService(dataSource);
        ciCdInstancesDatabaseService.ensureTableExistence(company);
        jobService = new CiCdJobsDatabaseService(dataSource);
        jobService.ensureTableExistence(company);
        jobRunService = new CiCdJobRunsDatabaseService(DefaultObjectMapper.get(), dataSource);
        jobRunService.ensureTableExistence(company);
        mappingService = new CiCdScmMappingService(dataSource);
        mappingService.ensureTableExistence(company);
        teamMembersDatabaseService.ensureTableExistence(company);
    }

    @Test
    public void test() throws SQLException {
        var integratonId = integrationService.insert(company, Integration.builder()
            .application("application")
            .description("description")
            .name("integ1")
            .metadata(Map.of())
            .satellite(false)
            .status("ok")
            .url("url")
            .build());
        final DbScmUser authorInfo = DbScmUser.builder().displayName("author").originalDisplayName("author").cloudId("author").integrationId(integratonId).build();
        final DbScmUser committerInfo = DbScmUser.builder().displayName("committer").originalDisplayName("committer").cloudId("committer").integrationId(integratonId).build();
        var commitId = scmAggService.insertCommit(company, DbScmCommit.builder()
                .integrationId(integratonId)
                .repoIds(List.of("test/test"))
                .project("test/test")
                .commitSha("124")
                .author("author")
                .authorInfo(authorInfo)
                .committer("committer")
                .committerInfo(committerInfo)
                .committedAt(Instant.now().toEpochMilli())
                .filesCt(1)
                .message("message")
                .vcsType(VCS_TYPE.GIT)
                .deletions(0)
                .additions(1)
                .changes(0)
                .ingestedAt(Instant.now().toEpochMilli())
                .build());
        var job = CiCdJobUtils.createCICDJob(jobService, company, 0, null);
        var jobId = UUID.fromString(jobService.insert(company, job));
        var user = User.builder()
            .email("test@test.test")
            .bcryptPassword("")
            .firstName("firstName")
            .lastName("lastName")
            .passwordAuthEnabled(true)
            .userType(RoleType.ADMIN)
            .samlAuthEnabled(false)
            .passwordResetDetails(new PasswordReset())
            .build();
        var userId = userService.insert(company, user);
        var jobRun = CICDJobRun.builder()
            .cicdUserId(userId)
            .duration(10)
            .jobRunNumber(1L)
            .status("ok")
            .scmCommitIds(List.of(commitId))
            .startTime(Instant.now())
            .cicdJobId(jobId)
            .build();
        var jobRunId = UUID.fromString(jobRunService.insert(company, jobRun));
        var mapping = CiCdScmMapping.builder().commitId(UUID.fromString(commitId)).jobRunId(jobRunId).source("ST_CICD_SCM_MAPPING").build();
        var id = mappingService.insert(company, mapping);

        var candidate = mappingService.get(company, id);

        Assertions.assertThat(candidate.isPresent()).isTrue();
        Assertions.assertThat(candidate.get().getId()).isEqualTo(UUID.fromString(id));
        Assertions.assertThat(candidate.get().getJobRunId()).isEqualTo(mapping.getJobRunId());
        Assertions.assertThat(candidate.get().getCommitId()).isEqualTo(mapping.getCommitId());
        Assertions.assertThat(candidate.get().getSource()).isEqualTo(mapping.getSource());
        Assertions.assertThat(candidate.get().getCreatedAt()).isNotNull();
        Assertions.assertThat(candidate.get().getUpdatedAt()).isNotNull();
    }
}