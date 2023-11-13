package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.CICDInstance;
import io.levelops.commons.databases.models.database.CICDJob;
import io.levelops.commons.databases.models.database.CICDJobRun;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.organization.DBTeam;
import io.levelops.commons.databases.models.database.organization.DBTeamMember;
import io.levelops.commons.databases.models.database.organization.TeamMemberId;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.databases.models.filters.CiCdPipelineJobRunsFilter;
import io.levelops.commons.databases.services.organization.ProductsDatabaseService;
import io.levelops.commons.databases.services.organization.TeamMembersDatabaseService;
import io.levelops.commons.databases.services.organization.TeamsDatabaseService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class CiCdPipelinesAggServiceTeamTest {
    private final static ObjectMapper MAPPER = DefaultObjectMapper.get();
    private final String company = "test";
    private String teamId1;
    private static final boolean VALUES_ONLY = false;
    private final static Map<String, SortingOrder> DEFAULT_SORT_BY = Collections.emptyMap();

    @Rule
    public SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private DataSource dataSource;
    private UserService userService;
    private CiCdInstancesDatabaseService ciCdInstancesDatabaseService;
    private CiCdJobsDatabaseService ciCdJobsDatabaseService;
    private CiCdJobRunsDatabaseService ciCdJobRunsDatabaseService;
    private CiCdJobConfigChangesDatabaseService ciCdJobConfigChangesDatabaseService;
    private CiCdPipelinesAggsService ciCdPipelinesAggsService;
    private static UserIdentityService userIdentityService;
    private IntegrationService integrationService;
    private TeamMembersDatabaseService teamMembersDatabaseService;
    private TeamsDatabaseService teamsDatabaseService;
    private ProductsDatabaseService productsDatabaseService;

    @Before
    public void setup() throws SQLException {
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        integrationService = new IntegrationService(dataSource);
        userService = new UserService(dataSource, MAPPER);
        ciCdInstancesDatabaseService = new CiCdInstancesDatabaseService(dataSource);
        ciCdJobsDatabaseService = new CiCdJobsDatabaseService(dataSource);
        userIdentityService = new UserIdentityService(dataSource);
        ciCdJobRunsDatabaseService = new CiCdJobRunsDatabaseService(DefaultObjectMapper.get(), dataSource);
        ciCdJobConfigChangesDatabaseService = new CiCdJobConfigChangesDatabaseService(dataSource);
        ciCdPipelinesAggsService = new CiCdPipelinesAggsService(dataSource, ciCdJobRunsDatabaseService);
        teamMembersDatabaseService = new TeamMembersDatabaseService(dataSource, MAPPER);
        teamsDatabaseService = new TeamsDatabaseService(dataSource, MAPPER);

        new DatabaseSchemaService(dataSource)
                .ensureSchemaExistence(company);
        integrationService.ensureTableExistence(company);

        Integration integration = Integration.builder()
                .name("jenkins1")
                .url("http")
                .status("good")
                .application("jenkins")
                .description("desc")
                .satellite(true)
                .build();
        String integId1 = integrationService.insert(company, integration);
        Integration integration1 = Integration.builder()
                .name("azure1")
                .url("http")
                .status("good")
                .application("azure_devops")
                .description("desc")
                .satellite(true)
                .build();
        String integId2 = integrationService.insert(company, integration1);
        productsDatabaseService = new ProductsDatabaseService(dataSource, MAPPER);
        productsDatabaseService.ensureTableExistence(company);
        userService.ensureTableExistence(company);
        userIdentityService.ensureTableExistence(company);
        teamMembersDatabaseService.ensureTableExistence(company);
        teamsDatabaseService.ensureTableExistence(company);
        ciCdInstancesDatabaseService.ensureTableExistence(company);
        ciCdJobsDatabaseService.ensureTableExistence(company);
        ciCdJobRunsDatabaseService.ensureTableExistence(company);
        ciCdJobConfigChangesDatabaseService.ensureTableExistence(company);
        ciCdPipelinesAggsService.ensureTableExistence(company);

        var dbScmUsers = List.of(
                DbScmUser.builder()
                        .integrationId(integId1)
                        .displayName("harsh-levelops")
                        .originalDisplayName("harsh-levelops")
                        .cloudId("user-jenkins-3")
                        .build(),
                DbScmUser.builder()
                        .integrationId(integId2)
                        .displayName("viraj-levelops")
                        .originalDisplayName("viraj-levelops")
                        .cloudId("user-jenkins-0")
                        .build()
        );
        List<String> newUuidsInserted = List.of(
                userIdentityService.upsert(company, dbScmUsers.get(0)),
                userIdentityService.upsert(company, dbScmUsers.get(1))
        );
        teamMembersDatabaseService.upsert(company, DBTeamMember.builder().fullName(dbScmUsers.get(0).getDisplayName()).build(), UUID.fromString(newUuidsInserted.get(0)));
        teamMembersDatabaseService.upsert(company, DBTeamMember.builder().fullName(dbScmUsers.get(1).getDisplayName()).build(), UUID.fromString(newUuidsInserted.get(1)));

        List<Optional<TeamMemberId>> teamMemberIds = newUuidsInserted.stream()
                .map(uuidInserted -> teamMembersDatabaseService.getId(company, UUID.fromString(uuidInserted))).collect(Collectors.toList());
        DBTeam team1 = DBTeam.builder()
                .name("name")
                .description("description")
                .managers(Set.of(DBTeam.TeamMemberId.builder().id(UUID.fromString(teamMemberIds.get(0).get().getTeamMemberId())).build()))
                .members(Set.of(DBTeam.TeamMemberId.builder().id(UUID.fromString(teamMemberIds.get(0).get().getTeamMemberId())).build(),
                        DBTeam.TeamMemberId.builder().id(UUID.fromString(teamMemberIds.get(1).get().getTeamMemberId())).build()))
                .build();
        teamId1 = teamsDatabaseService.insert(company, team1);
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

    @Test
    public void testTeamsFilter() throws SQLException {
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

            Assertions.assertThat(ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                    CiCdPipelineJobRunsFilter.builder()
                            .across(CiCdPipelineJobRunsFilter.DISTINCT.qualified_job_name)
                            .calculation(CiCdPipelineJobRunsFilter.CALCULATION.count)
                            .cicdUserIds(List.of("team_id:" + teamId1))
                            .build(),
                    VALUES_ONLY,
                    null
            ).getTotalCount()).isEqualTo(1);

            Assertions.assertThat(ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                    CiCdPipelineJobRunsFilter.builder()
                            .across(CiCdPipelineJobRunsFilter.DISTINCT.instance_name)
                            .calculation(CiCdPipelineJobRunsFilter.CALCULATION.count)
                            .cicdUserIds(List.of("team_id:" + teamId1))
                            .build(),
                    VALUES_ONLY,
                    null
            ).getTotalCount()).isEqualTo(1);
        }

       Assertions.assertThat( ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder()
                        .across(CiCdPipelineJobRunsFilter.DISTINCT.qualified_job_name)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.count)
                        .cicdUserIds(List.of("team_id:" + teamId1))
                        .build(),
                VALUES_ONLY,
                null
        ).getTotalCount()).isEqualTo(1);

        Assertions.assertThat(ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder()
                        .across(CiCdPipelineJobRunsFilter.DISTINCT.job_normalized_full_name)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.count)
                        .cicdUserIds(List.of("team_id:" + teamId1))
                        .build(),
                VALUES_ONLY,
                null
        ).getTotalCount()).isEqualTo(1);

        Assertions.assertThat(ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder()
                        .across(CiCdPipelineJobRunsFilter.DISTINCT.qualified_job_name)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.count)
                        .parentCiCdJobIds(List.of(job1.getId().toString()))
                        .cicdUserIds(List.of("team_id:" + teamId1))
                        .build(),
                VALUES_ONLY,
                null
        ).getTotalCount()).isEqualTo(3);

       Assertions.assertThat( ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder()
                        .across(CiCdPipelineJobRunsFilter.DISTINCT.qualified_job_name)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.count)
                        .cicdUserIds(List.of("team_id:" +teamId1))
                        .build(),
                VALUES_ONLY,
                null
        ).getTotalCount()).isEqualTo(1);

        Assertions.assertThat(ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.job_status)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.count)
                        .cicdUserIds(List.of("team_id:" +teamId1))
                        .build(),
                VALUES_ONLY,
                null
        ).getTotalCount()).isEqualTo(1);

        Assertions.assertThat(ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.cicd_user_id)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.count)
                        .cicdUserIds(List.of("team_id:" +teamId1))
                        .build(),
                VALUES_ONLY,
                null
        ).getTotalCount()).isEqualTo(2);

        Assertions.assertThat(ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder().across(CiCdPipelineJobRunsFilter.DISTINCT.instance_name)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.count)
                        .cicdUserIds(List.of("team_id:" +teamId1))
                        .build(),
                VALUES_ONLY,
                null
        ).getTotalCount()).isEqualTo(1);

        Assertions.assertThat(ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder()
                        .across(CiCdPipelineJobRunsFilter.DISTINCT.cicd_user_id)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.count)
                        .cicdUserIds(List.of("team_id:" +teamId1))
                        .build(),
                VALUES_ONLY,
                null
        ).getTotalCount()).isEqualTo(2);

        Assertions.assertThat(ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder()
                        .across(CiCdPipelineJobRunsFilter.DISTINCT.trend)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.count)
                        .cicdUserIds(List.of("team_id:" +teamId1))
                        .build(),
                VALUES_ONLY,
                null
        ).getTotalCount()).isEqualTo(2);

        Assertions.assertThat(ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder()
                        .across(CiCdPipelineJobRunsFilter.DISTINCT.job_normalized_full_name)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.duration)
                        .cicdUserIds(List.of("team_id:" +teamId1))
                        .build(),
                VALUES_ONLY,
                null
        ).getTotalCount()).isEqualTo(1);

        Assertions.assertThat(ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder()
                        .across(CiCdPipelineJobRunsFilter.DISTINCT.cicd_job_id)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.duration)
                        .cicdUserIds(List.of("team_id:" +teamId1))
                        .build(),
                VALUES_ONLY,
                null
        ).getTotalCount()).isEqualTo(1);

        Assertions.assertThat(ciCdPipelinesAggsService.listCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder()
                        .cicdUserIds(List.of("team_id:" +teamId1)).build(),
                DEFAULT_SORT_BY, null, 0, 50).getTotalCount()).isEqualTo(14);

       Assertions.assertThat(ciCdPipelinesAggsService.listCiCdJobRuns(company,
               CiCdPipelineJobRunsFilter.builder()
                       .cicdUserIds(List.of("team_id:" +teamId1)).build(),
                Map.of("start_time", SortingOrder.ASC), null, 0, 50).getTotalCount()).isEqualTo(14);

        Assertions.assertThat(ciCdPipelinesAggsService.listCiCdJobRuns(company, CiCdPipelineJobRunsFilter.builder()
                        .cicdUserIds(List.of("team_id:" +teamId1))
                        .instanceNames(List.of("instance-name-0"))
                        .across(CiCdPipelineJobRunsFilter.DISTINCT.trend)
                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.count).build(), 0, 50, false, false, null).getTotalCount()).isEqualTo(14);

        Assertions.assertThat(ciCdPipelinesAggsService.listCiCdJobRuns(company,
                CiCdPipelineJobRunsFilter.builder()
                        .cicdUserIds(List.of("team_id:" +teamId1))
                        .jobNames(List.of("falcon")).build(),
                DEFAULT_SORT_BY, null, 0,50).getTotalCount()).isEqualTo(14);
    }
}
