package io.levelops.commons.databases.services.organization;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.QueryFilter;
import io.levelops.commons.databases.models.database.organization.DBTeam;
import io.levelops.commons.databases.models.database.organization.DBTeam.TeamMemberId;
import io.levelops.commons.databases.models.database.organization.DBTeamMember;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.UserIdentityService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.assertj.core.api.Assertions;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@SuppressWarnings("unused")
public class TeamsDatabaseServiceTest {
    
    private static final String company = "test";
    private static final ObjectMapper mapper = DefaultObjectMapper.get();
    
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;

    private static NamedParameterJdbcTemplate template;
    private static UserIdentityService userIdentityService;
    private static TeamsDatabaseService teamsDatabaseService;
    private static TeamMembersDatabaseService teamMembersDatabaseService;
    private static IntegrationService integrationService;

    @BeforeClass
    public static void setup() throws SQLException, IOException {
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        template = new NamedParameterJdbcTemplate(dataSource);
        List.<String>of(
            "CREATE SCHEMA IF NOT EXISTS " + company,
            "CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";"
            )
            .forEach(template.getJdbcTemplate()::execute);

        integrationService = new IntegrationService(dataSource);
        integrationService.ensureTableExistence(company);
        userIdentityService = new UserIdentityService(dataSource);
        userIdentityService.ensureTableExistence(company);
        teamMembersDatabaseService = new TeamMembersDatabaseService(dataSource, mapper);
        teamMembersDatabaseService.ensureTableExistence(company);
        teamsDatabaseService = new TeamsDatabaseService(dataSource, mapper);
        teamsDatabaseService.ensureTableExistence(company);
    }

    @Test
    public void test() throws SQLException{
        var integration1 = Integration.builder()
            .description("description1")
            .name("integ1")
            .url("url")
            .application("application")
            .status("active")
            .build();
        var id1 = integrationService.insert(company, integration1);

        var integration2 = Integration.builder()
            .description("description1")
            .name("integ2")
            .url("url")
            .application("application")
            .status("active")
            .build();
        var id2 = integrationService.insert(company, integration2);

        var integration3 = Integration.builder()
            .description("description1")
            .name("integ3")
            .url("url")
            .application("application")
            .status("active")
            .build();
        var id3 = integrationService.insert(company, integration3);

        var teamMember1 = DBTeamMember.builder()
            .email("email1")
            .fullName("fullName1")
            .ids(Set.of(
                DBTeamMember.LoginId.builder()
                    .integrationId(1)
                    .username("username1")
                    .build(),
                DBTeamMember.LoginId.builder()
                    .integrationId(2)
                    .username("username2")
                    .build()
                ))
            .build();
        var memberId1 = UUID.fromString(teamMembersDatabaseService.insert(company, teamMember1));

        var teamMember2 = DBTeamMember.builder()
            .email("email2")
            .fullName("fullName2")
            .ids(Set.of(
                DBTeamMember.LoginId.builder()
                    .integrationId(1)
                    .username("username3")
                    .build(),
                DBTeamMember.LoginId.builder()
                    .integrationId(3)
                    .username("username4")
                    .build()
                ))
            .build();
        var memberId2 = UUID.fromString(teamMembersDatabaseService.insert(company, teamMember2));

        var teamMember3 = DBTeamMember.builder()
            .email("email3")
            .fullName("fullName3")
            .ids(Set.of(
                DBTeamMember.LoginId.builder()
                    .integrationId(1)
                    .username("username5")
                    .build()
                ))
            .build();
        var memberId3 = UUID.fromString(teamMembersDatabaseService.insert(company, teamMember3));

        var team1 = DBTeam.builder()
            .name("name1")
            .description("description1")
            .managers(Set.of(TeamMemberId.builder()
                .id(memberId1)
                .email(teamMember1.getEmail())
                .fullName(teamMember1.getFullName())
                .build()))
            .members(Set.of(
                TeamMemberId.builder()
                    .id(memberId2)
                    .email(teamMember2.getEmail())
                    .fullName(teamMember2.getFullName())
                    .build(), 
                TeamMemberId.builder()
                    .id(memberId3)
                    .email(teamMember3.getEmail())
                    .fullName(teamMember3.getFullName())
                .build()))
            .build();
        var teamId1 = UUID.fromString(teamsDatabaseService.insert(company, team1));

        var team2 = DBTeam.builder()
            .name("name2")
            .description("description2")
            .managers(Set.of(TeamMemberId.builder()
                .id(memberId2)
                .email(teamMember2.getEmail())
                .fullName(teamMember2.getFullName())
                .build()))
            .members(Set.of(
                TeamMemberId.builder()
                    .id(memberId1)
                    .email(teamMember1.getEmail())
                    .fullName(teamMember1.getFullName())
                    .build(), 
                TeamMemberId.builder()
                    .id(memberId3)
                    .email(teamMember3.getEmail())
                    .fullName(teamMember3.getFullName())
                .build()))
            .build();
        var teamId2 = UUID.fromString(teamsDatabaseService.insert(company, team2));

        var results = teamsDatabaseService.filter(company, null, 0, 10);

        Assertions.assertThat(results).isNotNull();
        Assertions.assertThat(results.getRecords()).containsExactlyInAnyOrder(team1.toBuilder().id(teamId1).build(),team2.toBuilder().id(teamId2).build());

        results = teamsDatabaseService.filter(company, QueryFilter.builder().strictMatch("name", "name").build(), 0, 10);

        Assertions.assertThat(results).isNotNull();
        Assertions.assertThat(results.getRecords().size()).isEqualTo(0);

        results = teamsDatabaseService.filter(company, QueryFilter.builder().strictMatch("name", "name1").build(), 0, 10);

        Assertions.assertThat(results).isNotNull();
        Assertions.assertThat(results.getRecords().get(0)).isEqualTo(team1.toBuilder().id(teamId1).build());

        results = teamsDatabaseService.filter(company, QueryFilter.builder().strictMatch("name", "name2").build(), 0, 10);

        Assertions.assertThat(results).isNotNull();
        Assertions.assertThat(results.getRecords().get(0)).isEqualTo(team2.toBuilder().id(teamId2).build());
    }

    @Test
    public void testDelete() throws SQLException{
        var integration1 = Integration.builder()
            .description("description1")
            .name("integ1 to delete")
            .url("url")
            .application("application")
            .status("active")
            .build();
        var id1 = integrationService.insert(company, integration1);

        var teamMember1 = DBTeamMember.builder()
            .email("email1 to delete")
            .fullName("fullName1")
            .ids(Set.of(
                DBTeamMember.LoginId.builder()
                    .integrationId(1)
                    .username("username1_delete")
                    .build(),
                DBTeamMember.LoginId.builder()
                    .integrationId(2)
                    .username("username2_delete")
                    .build()
                ))
            .build();
        var memberId1 = UUID.fromString(teamMembersDatabaseService.insert(company, teamMember1));

        var teamMember2 = DBTeamMember.builder()
            .email("email2 to delete")
            .fullName("fullName2")
            .ids(Set.of(
                DBTeamMember.LoginId.builder()
                    .integrationId(1)
                    .username("username3_delete")
                    .build(),
                DBTeamMember.LoginId.builder()
                    .integrationId(3)
                    .username("username4_delete")
                    .build()
                ))
            .build();
        var memberId2 = UUID.fromString(teamMembersDatabaseService.insert(company, teamMember2));

        var teamMember3 = DBTeamMember.builder()
            .email("email3 to delete")
            .fullName("fullName3")
            .ids(Set.of(
                DBTeamMember.LoginId.builder()
                    .integrationId(1)
                    .username("username5_delete")
                    .build()
                ))
            .build();
        var memberId3 = UUID.fromString(teamMembersDatabaseService.insert(company, teamMember3));

        var team1 = DBTeam.builder()
            .name("name1_delete")
            .description("description1")
            .managers(Set.of(TeamMemberId.builder()
                .id(memberId1)
                .email(teamMember1.getEmail())
                .fullName(teamMember1.getFullName())
                .build()))
            .members(Set.of(
                TeamMemberId.builder()
                    .id(memberId2)
                    .email(teamMember2.getEmail())
                    .fullName(teamMember2.getFullName())
                    .build(), 
                TeamMemberId.builder()
                    .id(memberId3)
                    .email(teamMember3.getEmail())
                    .fullName(teamMember3.getFullName())
                .build()))
            .build();
        var teamId1 = UUID.fromString(teamsDatabaseService.insert(company, team1));
        var results = teamsDatabaseService.filter(company, null, 0, 10);

        var deleted = teamsDatabaseService.delete(company, teamId1);
        Assertions.assertThat(deleted).isTrue();

        var results2 = teamsDatabaseService.filter(company, null, 0, 10);
        Assertions.assertThat(results2.getTotalCount()).isEqualTo(results.getTotalCount()-1);
    }
}
