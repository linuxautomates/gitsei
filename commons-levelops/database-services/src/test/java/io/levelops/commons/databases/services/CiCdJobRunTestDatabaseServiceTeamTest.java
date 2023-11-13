package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.*;
import io.levelops.commons.databases.models.database.access.RoleType;
import io.levelops.commons.databases.models.database.jenkins.JUnitTestReport;
import io.levelops.commons.databases.models.database.organization.DBTeam;
import io.levelops.commons.databases.models.database.organization.DBTeamMember;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.databases.models.filters.CICD_TYPE;
import io.levelops.commons.databases.models.filters.CiCdJobRunTestsFilter;
import io.levelops.commons.databases.services.organization.ProductsDatabaseService;
import io.levelops.commons.databases.services.organization.TeamMembersDatabaseService;
import io.levelops.commons.databases.services.organization.TeamsDatabaseService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.commons.xml.DefaultXmlMapper;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class CiCdJobRunTestDatabaseServiceTeamTest {
    private static final String COMPANY = "test";
    private String teamId1;
    private static final ObjectMapper OBJECT_MAPPER = DefaultObjectMapper.get();

    @Rule
    public SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private DataSource dataSource;
    private CiCdJobRunTestDatabaseService testDatabaseService;
    private IntegrationService integrationService;
    private UserIdentityService userIdentityService;
    private TeamMembersDatabaseService teamMembersDatabaseService;
    private TeamsDatabaseService teamsDatabaseService;
    private ProductsDatabaseService productsDatabaseService;

    @Before
    public void setup() throws SQLException, IOException {
        if (dataSource != null)
            return;
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        integrationService = new IntegrationService(dataSource);
        CiCdInstancesDatabaseService ciCdInstancesDatabaseService = new CiCdInstancesDatabaseService(dataSource);
        CiCdJobsDatabaseService ciCdJobsDatabaseService = new CiCdJobsDatabaseService(dataSource);
        CiCdJobRunsDatabaseService ciCdJobRunsDatabaseService = new CiCdJobRunsDatabaseService(OBJECT_MAPPER, dataSource);
        testDatabaseService = new CiCdJobRunTestDatabaseService(dataSource);
        UserService userService = new UserService(dataSource, OBJECT_MAPPER);
        userIdentityService = new UserIdentityService(dataSource);
        teamMembersDatabaseService = new TeamMembersDatabaseService(dataSource, OBJECT_MAPPER);
        teamsDatabaseService = new TeamsDatabaseService(dataSource, OBJECT_MAPPER);

        new DatabaseSchemaService(dataSource).ensureSchemaExistence(COMPANY);
        integrationService.ensureTableExistence(COMPANY);
        userIdentityService.ensureTableExistence(COMPANY);
        teamMembersDatabaseService.ensureTableExistence(COMPANY);
        teamsDatabaseService.ensureTableExistence(COMPANY);
        Integration integration = Integration.builder()
                .id("1")
                .name("name")
                .url("http")
                .status("good")
                .application("azure_devops")
                .description("desc")
                .satellite(true)
                .build();
        integrationService.insert(COMPANY, integration);
        productsDatabaseService = new ProductsDatabaseService(dataSource, OBJECT_MAPPER);
        productsDatabaseService.ensureTableExistence(COMPANY);
        userService.ensureTableExistence(COMPANY);
        ciCdInstancesDatabaseService.ensureTableExistence(COMPANY);
        ciCdJobsDatabaseService.ensureTableExistence(COMPANY);
        ciCdJobRunsDatabaseService.ensureTableExistence(COMPANY);
        testDatabaseService.ensureTableExistence(COMPANY);
        var dbScmUsers = List.of(DbScmUser.builder()
                                .integrationId("1")
                                .cloudId("testuser")
                                .displayName("Viraj")
                                .originalDisplayName("Viraj")
                                .build(),
                            DbScmUser.builder()
                                .integrationId("1")
                                .cloudId("testuser1")
                                .displayName("kush")
                                    .originalDisplayName("kush")
                                .build());
        String insertId1 = userIdentityService.upsert(COMPANY, dbScmUsers.get(0));
        String insertId2 = userIdentityService.upsert(COMPANY, dbScmUsers.get(1));
        List<UUID> teamMemberId = new ArrayList<>();
        // commented out since this approach is no longer the correct one
        var i = 0;
        for (var id : List.of(insertId1, insertId2)) {
            teamMembersDatabaseService.upsert(
                COMPANY,
                DBTeamMember.builder().fullName(dbScmUsers.get(i).getDisplayName()).build(),
                UUID.fromString(id)
            );
            teamMemberId.add(UUID.fromString(teamMembersDatabaseService.
                    getId(COMPANY, UUID.fromString(id)).get().getTeamMemberId()));
            i++;
        }
        DBTeam team1 = DBTeam.builder()
                .name("name")
                .description("description")
                .managers(Set.of(DBTeam.TeamMemberId.builder().id(teamMemberId.get(0)).build()))
                .members(Set.of(DBTeam.TeamMemberId.builder().id(teamMemberId.get(0)).build(),
                        DBTeam.TeamMemberId.builder().id(teamMemberId.get(1)).build()))
                .build();
        teamId1 = teamsDatabaseService.insert(COMPANY, team1);
        final String userId = userService.insert(COMPANY, User.builder()
                .userType(RoleType.LIMITED_USER)
                .bcryptPassword("asd")
                .email("asd@asd.com")
                .passwordAuthEnabled(Boolean.FALSE)
                .samlAuthEnabled(false)
                .firstName("asd")
                .lastName("asd")
                .build());
        final String instanceId = ciCdInstancesDatabaseService.insert(COMPANY, CICDInstance.builder()
                .id(UUID.randomUUID())
                .name("test-instance")
                .url("testUrl")
                .integrationId("1")
                .type(CICD_TYPE.azure_devops.toString())
                .build());
        final String jobId = ciCdJobsDatabaseService.insert(COMPANY, CICDJob.builder()
                .jobName("testJobName")
                .jobFullName("testJobName")
                .jobNormalizedFullName("testJobName")
                .projectName("project-1")
                .cicdInstanceId(UUID.fromString(instanceId))
                .build());
        final Date date = new Date();
        final String jobRunId = ciCdJobRunsDatabaseService.insert(COMPANY, CICDJobRun.builder()
                .cicdJobId(UUID.fromString(jobId))
                .jobRunNumber(20L)
                .status("SUCCESS")
                .scmCommitIds(Collections.emptyList())
                .startTime(Instant.ofEpochSecond(1611075448))
                .endTime(Instant.ofEpochSecond(1614976448))
                .duration(100)
                .cicdUserId("testuser")
                .source(CICDJobRun.Source.JOB_RUN_COMPLETE_EVENT)
                .build());
        final String successXml = ResourceUtils.getResourceAsString(
                "json/databases/junit_test_results/TEST-io.levelops.internal_api.controllers.ComponentsControllerTest.xml");
        final String failureXml = ResourceUtils.getResourceAsString(
                "json/databases/junit_test_results/TEST-io.levelops.internal_api.controllers.DatabaseSetupControllerInjectionTest.xml");
        final JUnitTestReport successReport = DefaultXmlMapper.getXmlMapper().readValue(successXml, JUnitTestReport.class);
        final JUnitTestReport failureReport = DefaultXmlMapper.getXmlMapper().readValue(failureXml, JUnitTestReport.class);
        final UUID cicdJobRunId = UUID.fromString(jobRunId);
        final List<CiCdJobRunTest> ciCdJobRunTests = List.of(successReport, failureReport).stream()
                .flatMap(report -> CiCdJobRunTest.fromJUnitTestSuite(report, cicdJobRunId).stream())
                .collect(Collectors.toList());
        final List<String> insertedIds = testDatabaseService.batchInsert(COMPANY, ciCdJobRunTests);
        if (insertedIds.size() != ciCdJobRunTests.size()) {
            throw new RuntimeException("All the tests must be inserted successfully");
        }
    }

    @Test
    public void testTeamsFilter() throws SQLException {
        assertThat(testDatabaseService.list(COMPANY, CiCdJobRunTestsFilter.builder()
                .cicdUserIds(List.of("team_id:" + teamId1))
                .build(), 0, 100).getTotalCount()).isEqualTo(5);

        assertThat(testDatabaseService.list(COMPANY, CiCdJobRunTestsFilter.builder()
                .testStatuses(List.of("PASSED"))
                .jobNames(List.of("testJobName"))
                .cicdUserIds(List.of("team_id:" + teamId1))
                .instanceNames(List.of("does-not-exist"))
                .build(), 0, 100).getTotalCount()).isEqualTo(0);

        assertThat(testDatabaseService.list(COMPANY, CiCdJobRunTestsFilter.builder()
                .testStatuses(List.of("PASSED"))
                .jobNames(List.of("testJobName"))
                .cicdUserIds(List.of("team_id:" + teamId1))
                .instanceNames(List.of("test-instance"))
                .types(List.of(CICD_TYPE.azure_devops))
                .build(), 0, 100).getTotalCount()).isEqualTo(4);

        assertThat(testDatabaseService.list(COMPANY, CiCdJobRunTestsFilter.builder()
                .testStatuses(List.of("PASSED"))
                .jobNames(List.of("testJobName"))
                .cicdUserIds(List.of("team_id:" + teamId1))
                .instanceNames(List.of("test-instance"))
                .types(List.of(CICD_TYPE.jenkins))
                .build(), 0, 100).getTotalCount()).isEqualTo(0);

        assertThat(testDatabaseService.list(COMPANY, CiCdJobRunTestsFilter.builder()
                .testStatuses(List.of("FAILED"))
                .jobNames(List.of("testJobName"))
                .cicdUserIds(List.of("team_id:" + teamId1))
                .build(), 0, 100).getTotalCount()).isEqualTo(1);

        assertThat(testDatabaseService.list(COMPANY, CiCdJobRunTestsFilter.builder()
                .testStatuses(List.of("PASSED"))
                .jobNames(List.of("testJobName"))
                .cicdUserIds(List.of("team_id:" + teamId1))
                .build(), 0, 100).getTotalCount()).isEqualTo(4);

        assertThat(testDatabaseService.groupByAndCalculate(COMPANY, CiCdJobRunTestsFilter.builder()
                .cicdUserIds(List.of("team_id:" + teamId1))
                .DISTINCT(CiCdJobRunTestsFilter.DISTINCT.test_status)
                .CALCULATION(CiCdJobRunTestsFilter.CALCULATION.count)
                .build()).getTotalCount()).isEqualTo(2);

        assertThat(testDatabaseService.groupByAndCalculate(COMPANY, CiCdJobRunTestsFilter.builder()
                .cicdUserIds(List.of("team_id:" + teamId1))
                .DISTINCT(CiCdJobRunTestsFilter.DISTINCT.test_suite)
                .CALCULATION(CiCdJobRunTestsFilter.CALCULATION.duration)
                .build()).getTotalCount()).isEqualTo(2);

        assertThat(testDatabaseService.groupByAndCalculate(COMPANY, CiCdJobRunTestsFilter.builder()
                .cicdUserIds(List.of("team_id:" + teamId1))
                .DISTINCT(CiCdJobRunTestsFilter.DISTINCT.instance_name)
                .CALCULATION(CiCdJobRunTestsFilter.CALCULATION.count)
                .build()).getTotalCount()).isEqualTo(1);

        assertThat(testDatabaseService.groupByAndCalculate(COMPANY, CiCdJobRunTestsFilter.builder()
                .cicdUserIds(List.of("team_id:" + teamId1))
                .DISTINCT(CiCdJobRunTestsFilter.DISTINCT.job_end)
                .CALCULATION(CiCdJobRunTestsFilter.CALCULATION.count)
                .build()).getTotalCount()).isEqualTo(1);

        assertThat(testDatabaseService.groupByAndCalculate(COMPANY, CiCdJobRunTestsFilter.builder()
                .cicdUserIds(List.of("team_id:" + teamId1))
                .DISTINCT(CiCdJobRunTestsFilter.DISTINCT.project_name)
                .CALCULATION(CiCdJobRunTestsFilter.CALCULATION.count)
                .build()).getTotalCount()).isEqualTo(1);

        assertThat(testDatabaseService.groupByAndCalculate(COMPANY, CiCdJobRunTestsFilter.builder()
                .cicdUserIds(List.of("team_id:" + teamId1))
                .DISTINCT(CiCdJobRunTestsFilter.DISTINCT.job_run_id)
                .CALCULATION(CiCdJobRunTestsFilter.CALCULATION.count)
                .build()).getTotalCount()).isEqualTo(1);

        assertThat(testDatabaseService.groupByAndCalculate(COMPANY, CiCdJobRunTestsFilter.builder()
                .cicdUserIds(List.of("team_id:" + teamId1))
                .DISTINCT(CiCdJobRunTestsFilter.DISTINCT.cicd_user_id)
                .CALCULATION(CiCdJobRunTestsFilter.CALCULATION.count)
                .build()).getTotalCount()).isEqualTo(1);

        assertThat(testDatabaseService.groupByAndCalculate(COMPANY, CiCdJobRunTestsFilter.builder()
                .cicdUserIds(List.of("team_id:" + teamId1))
                .DISTINCT(CiCdJobRunTestsFilter.DISTINCT.trend)
                .CALCULATION(CiCdJobRunTestsFilter.CALCULATION.count)
                .build()).getTotalCount()).isEqualTo(1);

        assertThat(testDatabaseService.groupByAndCalculate(COMPANY, CiCdJobRunTestsFilter.builder()
                .cicdUserIds(List.of("team_id:" + teamId1))
                .DISTINCT(CiCdJobRunTestsFilter.DISTINCT.job_run_number)
                .CALCULATION(CiCdJobRunTestsFilter.CALCULATION.count)
                .build()).getTotalCount()).isEqualTo(1);
    }
}
