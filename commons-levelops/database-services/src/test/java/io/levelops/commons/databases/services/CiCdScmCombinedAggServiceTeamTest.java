package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.CICDInstance;
import io.levelops.commons.databases.models.database.CICDJob;
import io.levelops.commons.databases.models.database.CICDJobRun;
import io.levelops.commons.databases.models.database.CiCdScmMapping;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.organization.DBTeam;
import io.levelops.commons.databases.models.database.organization.DBTeamMember;
import io.levelops.commons.databases.models.database.scm.DbScmCommit;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.databases.models.filters.CICD_TYPE;
import io.levelops.commons.databases.models.filters.CiCdScmFilter;
import io.levelops.commons.databases.services.organization.ProductsDatabaseService;
import io.levelops.commons.databases.services.organization.TeamMembersDatabaseService;
import io.levelops.commons.databases.services.organization.TeamsDatabaseService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class CiCdScmCombinedAggServiceTeamTest {
    private final static ObjectMapper MAPPER = DefaultObjectMapper.get();
    private final static String company = "test";
    private final static boolean VALUES_ONLY = false;
    private static String teamId1;

    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;
    private static UserService userService;
    private static IntegrationService integrationService;
    private static CiCdInstancesDatabaseService ciCdInstancesDatabaseService;
    private static CiCdJobsDatabaseService ciCdJobsDatabaseService;
    private static CiCdJobRunsDatabaseService ciCdJobRunsDatabaseService;
    private static ScmAggService scmAggService;
    private static CiCdScmMappingService ciCdScmMappingService;
    private static CiCdScmCombinedAggsService ciCdScmCombinedAggsService;
    private static TeamMembersDatabaseService teamMembersDatabaseService;
    private static TeamsDatabaseService teamsDatabaseService;
    private static ProductsDatabaseService productsDatabaseService;
    private static UserIdentityService userIdentityService;

    @BeforeClass
    public static void setup() throws SQLException, IOException {
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();

        userService = new UserService(dataSource, MAPPER);
        integrationService = new IntegrationService(dataSource);
        ciCdInstancesDatabaseService = new CiCdInstancesDatabaseService(dataSource);
        ciCdJobsDatabaseService = new CiCdJobsDatabaseService(dataSource);
        ciCdJobRunsDatabaseService = new CiCdJobRunsDatabaseService(DefaultObjectMapper.get(), dataSource);
        userIdentityService = new UserIdentityService(dataSource);
        scmAggService = new ScmAggService(dataSource, userIdentityService);
        ciCdScmMappingService = new CiCdScmMappingService(dataSource);
        ciCdScmCombinedAggsService = new CiCdScmCombinedAggsService(dataSource, ciCdJobRunsDatabaseService);
        teamMembersDatabaseService = new TeamMembersDatabaseService(dataSource, MAPPER);
        teamsDatabaseService = new TeamsDatabaseService(dataSource, MAPPER);

        new DatabaseSchemaService(dataSource)
                .ensureSchemaExistence(company);
        userService.ensureTableExistence(company);
        integrationService.ensureTableExistence(company);
        ciCdInstancesDatabaseService.ensureTableExistence(company);
        ciCdJobsDatabaseService.ensureTableExistence(company);
        ciCdJobRunsDatabaseService.ensureTableExistence(company);
        userIdentityService.ensureTableExistence(company);
        scmAggService.ensureTableExistence(company);
        ciCdScmMappingService.ensureTableExistence(company);
        ciCdScmCombinedAggsService.ensureTableExistence(company);
        teamMembersDatabaseService.ensureTableExistence(company);
        teamsDatabaseService.ensureTableExistence(company);
        Instant now = Instant.now();
        Instant before2Days = now.minus(2, ChronoUnit.DAYS);
        Instant before3Days = now.minus(3, ChronoUnit.DAYS);
        Instant before4Days = now.minus(4, ChronoUnit.DAYS);
        Integration integration = Integration.builder()
                .name("integration-name-" + 0)
                .status("status-"+0).application("jenkins").url("http://www.dummy.com")
                .satellite(false).build();
        String integrationId = integrationService.insert(company, integration);
        Assert.assertNotNull(integrationId);
        integration = integration.toBuilder().id(integrationId).build();
        productsDatabaseService = new ProductsDatabaseService(dataSource, MAPPER);
        productsDatabaseService.ensureTableExistence(company);
        CICDInstance cicdInstance = CiCdInstanceUtils.createCiCdInstance(ciCdInstancesDatabaseService, company, 0);
        CICDJob cicdJob = CiCdJobUtils.createCICDJob(ciCdJobsDatabaseService, company, 0, cicdInstance);
        CICDJobRun cicdJobRun1 = CICDJobRunUtils.createCICDJobRun(ciCdJobRunsDatabaseService, cicdJob, company, 0, before3Days, null, null, null);
        DbScmCommit scmCommit1 = ScmCommitUtils.createScmCommit(scmAggService, company, integration.getId(),before4Days);
        CiCdScmMapping mapping1 = CiCdScmMapping.builder().jobRunId(cicdJobRun1.getId()).commitId(UUID.fromString(scmCommit1.getId())).build();
        String mappingId1 = ciCdScmMappingService.insert(company, mapping1);
        Assert.assertNotNull(mappingId1);
        CICDJobRun cicdJobRun2 = CICDJobRunUtils.createCICDJobRun(ciCdJobRunsDatabaseService, cicdJob, company, 1, before3Days, null, null, null);
        DbScmCommit scmCommit2 = ScmCommitUtils.createScmCommit(scmAggService, company, integration.getId(), before2Days);
        CiCdScmMapping mapping2 = CiCdScmMapping.builder().jobRunId(cicdJobRun2.getId()).commitId(UUID.fromString(scmCommit2.getId())).build();
        String mappingId2 = ciCdScmMappingService.insert(company, mapping2);
        Assert.assertNotNull(mappingId2);
        var dbScmUsers = List.of(DbScmUser.builder()
                                .integrationId("1")
                                .cloudId("user-jenkins-0")
                                .displayName("Viraj")
                        .originalDisplayName("Viraj")
                                .build(),
                        DbScmUser.builder()
                                .integrationId("1")
                                .cloudId("user-jenkins-1")
                                .displayName("kush")
                                .originalDisplayName("kush")
                                .build());
        List<String> insertIds = List.of(
                userIdentityService.upsert(company, dbScmUsers.get(0)),
                userIdentityService.upsert(company, dbScmUsers.get(1))
                );
        List<UUID> teamMemberId = new ArrayList<>();
        var i = 0;
        for (var id : insertIds) {
            teamMembersDatabaseService.upsert(company, DBTeamMember.builder().fullName(dbScmUsers.get(i).getDisplayName()).build(), UUID.fromString(id));
            i++;
            teamMemberId.add(UUID.fromString(teamMembersDatabaseService.
                    getId(company, UUID.fromString(id)).get().getTeamMemberId()));
        }
        DBTeam team1 = DBTeam.builder()
                .name("name")
                .description("description")
                .managers(Set.of(DBTeam.TeamMemberId.builder().id(teamMemberId.get(0)).build()))
                .members(Set.of(DBTeam.TeamMemberId.builder().id(teamMemberId.get(0)).build(),
                        DBTeam.TeamMemberId.builder().id(teamMemberId.get(1)).build()))
                .build();
        teamId1 = teamsDatabaseService.insert(company, team1);
    }

    @Test
    public void testTeamsFilter() throws SQLException {
        Assertions.assertThat(ciCdScmCombinedAggsService.groupByAndCalculate(company,
                CiCdScmFilter.builder()
                        .across(CiCdScmFilter.DISTINCT.trend)
                        .calculation(CiCdScmFilter.CALCULATION.change_volume)
                        .cicdUserIds(List.of("team_id:" +teamId1))
                        .build(),
                VALUES_ONLY
        ).getTotalCount()).isEqualTo(1);

        Assertions.assertThat(ciCdScmCombinedAggsService.groupByAndCalculate(company,
                CiCdScmFilter.builder()
                        .across(CiCdScmFilter.DISTINCT.job_normalized_full_name)
                        .calculation(CiCdScmFilter.CALCULATION.count)
                        .cicdUserIds(List.of("team_id:" +teamId1))
                        .build(),
                VALUES_ONLY
        ).getTotalCount()).isEqualTo(1);

        Assertions.assertThat(ciCdScmCombinedAggsService.groupByAndCalculate(company,
                CiCdScmFilter.builder()
                        .across(CiCdScmFilter.DISTINCT.trend)
                        .calculation(CiCdScmFilter.CALCULATION.lead_time)
                        .cicdUserIds(List.of("team_id:" +teamId1))
                        .build(),
                VALUES_ONLY
        ).getTotalCount()).isEqualTo(1);

        Assertions.assertThat(ciCdScmCombinedAggsService.groupByAndCalculate(company,
                CiCdScmFilter.builder()
                        .across(CiCdScmFilter.DISTINCT.qualified_job_name)
                        .calculation(CiCdScmFilter.CALCULATION.lead_time)
                        .cicdUserIds(List.of("team_id:" +teamId1))
                        .build(),
                VALUES_ONLY
        ).getTotalCount()).isEqualTo(1);

        Assertions.assertThat(ciCdScmCombinedAggsService.groupByAndCalculate(company,
                CiCdScmFilter.builder()
                        .across(CiCdScmFilter.DISTINCT.repo)
                        .calculation(CiCdScmFilter.CALCULATION.count)
                        .cicdUserIds(List.of("team_id:" +teamId1))
                        .build(),
                VALUES_ONLY
        ).getTotalCount()).isEqualTo(1);

        Assertions.assertThat(ciCdScmCombinedAggsService.groupByAndCalculate(company,
                CiCdScmFilter.builder()
                        .across(CiCdScmFilter.DISTINCT.job_name)
                        .calculation(CiCdScmFilter.CALCULATION.lead_time)
                        .cicdUserIds(List.of("team_id:" +teamId1))
                        .build(),
                VALUES_ONLY
        ).getTotalCount()).isEqualTo(1);

        Assertions.assertThat(ciCdScmCombinedAggsService.groupByAndCalculate(company,
                CiCdScmFilter.builder()
                        .across(CiCdScmFilter.DISTINCT.job_name)
                        .calculation(CiCdScmFilter.CALCULATION.lead_time)
                        .cicdUserIds(List.of("team_id:" +teamId1))
                        .build(),
                VALUES_ONLY
        ).getTotalCount()).isEqualTo(1);

        Assertions.assertThat(ciCdScmCombinedAggsService.groupByAndCalculate(company,
                CiCdScmFilter.builder()
                        .across(CiCdScmFilter.DISTINCT.job_status)
                        .calculation(CiCdScmFilter.CALCULATION.count)
                        .cicdUserIds(List.of("team_id:" +teamId1))
                        .build(),
                VALUES_ONLY
        ).getTotalCount()).isEqualTo(1);

        Assertions.assertThat(ciCdScmCombinedAggsService.groupByAndCalculate(company,
                CiCdScmFilter.builder()
                        .across(CiCdScmFilter.DISTINCT.author)
                        .calculation(CiCdScmFilter.CALCULATION.count)
                        .cicdUserIds(List.of("team_id:" +teamId1))
                        .build(),
                VALUES_ONLY
        ).getTotalCount()).isEqualTo(1);

        Assertions.assertThat(ciCdScmCombinedAggsService.groupByAndCalculate(company,
                CiCdScmFilter.builder()
                        .across(CiCdScmFilter.DISTINCT.job_name)
                        .calculation(CiCdScmFilter.CALCULATION.count)
                        .cicdUserIds(List.of("team_id:" +teamId1))
                        .build(),
                VALUES_ONLY
        ).getTotalCount()).isEqualTo(1);

        Assertions.assertThat(ciCdScmCombinedAggsService.groupByAndCalculate(company,
                CiCdScmFilter.builder()
                        .across(CiCdScmFilter.DISTINCT.qualified_job_name)
                        .calculation(CiCdScmFilter.CALCULATION.count)
                        .cicdUserIds(List.of("team_id:" +teamId1))
                        .build(),
                VALUES_ONLY
        ).getTotalCount()).isEqualTo(1);

        Assertions.assertThat(ciCdScmCombinedAggsService.groupByAndCalculate(company,
                CiCdScmFilter.builder()
                        .across(CiCdScmFilter.DISTINCT.cicd_user_id)
                        .calculation(CiCdScmFilter.CALCULATION.count)
                        .cicdUserIds(List.of("team_id:" +teamId1))
                        .build(),
                VALUES_ONLY
        ).getTotalCount()).isEqualTo(2);

        Assertions.assertThat(ciCdScmCombinedAggsService.groupByAndCalculate(company,
                CiCdScmFilter.builder()
                        .across(CiCdScmFilter.DISTINCT.job_end)
                        .calculation(CiCdScmFilter.CALCULATION.count)
                        .cicdUserIds(List.of("team_id:" +teamId1))
                        .build(),
                VALUES_ONLY
        ).getTotalCount()).isEqualTo(1);

        Assertions.assertThat(ciCdScmCombinedAggsService.listCiCdScmCombinedData(company,
                CiCdScmFilter.builder()
                        .cicdUserIds(List.of("team_id:" +teamId1))
                        .types(List.of(CICD_TYPE.azure_devops))
                        .build(), 0, 100).getTotalCount()).isEqualTo(0);

        Assertions.assertThat(ciCdScmCombinedAggsService.listCiCdScmCombinedData(company,
                CiCdScmFilter.builder()
                        .cicdUserIds(List.of("team_id:" +teamId1))
                        .types(List.of(CICD_TYPE.jenkins))
                        .build(), 0, 100).getTotalCount()).isEqualTo(2);

        Assertions.assertThat(ciCdScmCombinedAggsService.listCiCdScmCombinedData(company,
                CiCdScmFilter.builder()
                        .cicdUserIds(List.of("team_id:" +teamId1))
                        .jobNames(List.of("jobname-0"))
                        .build(), 0, 100).getTotalCount()).isEqualTo(2);

    }
}
