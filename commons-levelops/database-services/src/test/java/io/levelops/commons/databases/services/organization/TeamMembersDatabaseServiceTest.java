package io.levelops.commons.databases.services.organization;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.QueryFilter;
import io.levelops.commons.databases.models.database.organization.DBTeamMember;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
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
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class TeamMembersDatabaseServiceTest {

    private static final String company = "test";
    private static final ObjectMapper mapper = DefaultObjectMapper.get();

    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;

    private static NamedParameterJdbcTemplate template;

    private static TeamMembersDatabaseService teamMembersDatabaseService;
    private static IntegrationService integrationService;
    private static UserIdentityService userIdentityService;

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
    }

    @Test
    public void test() throws SQLException {
        var integration1 = Integration.builder()
                .description("description1")
                .name("integ1")
                .url("url")
                .application("application")
                .status("active")
                .build();
        var id1 = Integer.valueOf(integrationService.insert(company, integration1));

        var integration2 = Integration.builder()
                .description("description1")
                .name("integ2")
                .url("url")
                .application("application")
                .status("active")
                .build();
        var id2 = Integer.valueOf(integrationService.insert(company, integration2));

        var integration3 = Integration.builder()
                .description("description1")
                .name("integ3")
                .url("url")
                .application("application")
                .status("active")
                .build();
        var id3 = Integer.valueOf(integrationService.insert(company, integration3));

        var item = DBTeamMember.builder()
                .email("email1")
                .fullName("fullName1")
                .build();
        var memberId = UUID.fromString(teamMembersDatabaseService.insert(company, item));

        var item2 = DBTeamMember.builder()
                .email("email2")
                .fullName("fullName2")
                .build();
        String insertedId1 = userIdentityService.upsert(company,
                DbScmUser.builder()
                        .integrationId(String.valueOf(id1))
                        .cloudId("sample-1")
                        .displayName("sample-Cog-1")
                        .originalDisplayName("sample-Cog-1")
                        .build());
        String insertedId2 = userIdentityService.upsert(company, DbScmUser.builder()
                .integrationId(String.valueOf(id1))
                .cloudId("sample-2")
                .displayName("sample-Cog-2")
                .originalDisplayName("sample-Cog-2")
                .build());
        var memberId2 = UUID.fromString(teamMembersDatabaseService.insert(company, item2));

        var dbUsers = List.of("sample-Cog-1", "sample-Cog-2");
        
        teamMembersDatabaseService.upsert(company, DBTeamMember.builder().fullName(dbUsers.get(0)).build(), UUID.fromString(insertedId1));
        teamMembersDatabaseService.upsert(company, DBTeamMember.builder().fullName(dbUsers.get(1)).build(), UUID.fromString(insertedId2));

        var results = teamMembersDatabaseService.filter(company, null, 0, 10);

        Assertions.assertThat(results).isNotNull();
        Assertions.assertThat(results.getRecords().stream().map(DBTeamMember::getFullName).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("sample-Cog-1", "sample-Cog-2", "fullName1", "fullName2");

        results = teamMembersDatabaseService.filter(company, QueryFilter.builder().strictMatch("full_name", "fullName1").build(), 0, 10);

        Assertions.assertThat(results.getTotalCount()).isEqualTo(1);
        Assertions.assertThat(results.getRecords().get(0).getFullName()).isEqualTo("fullName1");

        results = teamMembersDatabaseService.filter(company, QueryFilter.builder().strictMatch("integration_id", 1).build(), 0, 10);

        Assertions.assertThat(results.getTotalCount()).isEqualTo(4);
        Assertions.assertThat(results.getRecords().stream().map(DBTeamMember::getFullName).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("sample-Cog-1", "sample-Cog-2", "fullName1", "fullName2");

        results = teamMembersDatabaseService.filter(company, QueryFilter.builder().strictMatch("integration_id", 2).build(), 0, 10);
        Assertions.assertThat(results.getTotalCount()).isEqualTo(4);
        Assertions.assertThat(results.getRecords().stream().map(DBTeamMember::getEmail).collect(Collectors.toList()))
                .containsExactlyInAnyOrder(null, "email2", null, "email1");
        String uuidInserted = userIdentityService.upsert(company,
                DbScmUser.builder()
                        .integrationId(String.valueOf(id1))
                        .displayName("harsh-levelops")
                        .originalDisplayName("harsh-levelops")
                        .cloudId("qwerty")
                        .build());
        teamMembersDatabaseService.upsert(company, DBTeamMember.builder().fullName("harsh-levelops").build(), UUID.fromString(uuidInserted));
        results = teamMembersDatabaseService.filter(company, null, 0, 10);
        Assertions.assertThat(results.getTotalCount()).isEqualTo(5);
        Assertions.assertThat(results.getRecords().stream().map(DBTeamMember::getFullName).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("sample-Cog-1", "sample-Cog-2", "fullName1", "fullName2", "harsh-levelops");
        insertedId1 = userIdentityService.upsert(company,
                DbScmUser.builder()
                        .integrationId(String.valueOf(id2))
                        .displayName("meghana-levelops")
                        .originalDisplayName("meghana-levelops")
                        .cloudId("qwerty-e")
                        .build());
        insertedId2 = userIdentityService.upsert(company, DbScmUser.builder()
                .integrationId(String.valueOf(id1))
                .displayName("sample-cog")
                .originalDisplayName("sample-cog")
                .cloudId("qwertySample")
                .build());
        teamMembersDatabaseService.upsert(company, DBTeamMember.builder().fullName("qwerty-e").build(), UUID.fromString(insertedId1));
        teamMembersDatabaseService.upsert(company, DBTeamMember.builder().fullName("sample-cog").build(), UUID.fromString(insertedId2));
        Assertions.assertThat(results.getTotalCount()).isEqualTo(5);
        Assertions.assertThat(results.getRecords().stream().map(DBTeamMember::getFullName).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("sample-Cog-1", "sample-Cog-2", "fullName1", "fullName2", "harsh-levelops");
        String idInserted = teamMembersDatabaseService.getId(company, UUID.fromString(uuidInserted)).orElseThrow().getTeamMemberId();
        Assertions.assertThat(idInserted).isNotNull();
        teamMembersDatabaseService.update(company,
                DBTeamMember.builder()
                        .id(UUID.fromString(idInserted))
                        .fullName("Shrikesh")
                        .email("sample@gmail.com")
                        .build());
        Assertions.assertThat(teamMembersDatabaseService.get(company, UUID.fromString(idInserted)).orElseThrow().getEmail()).isEqualTo("sample@gmail.com");
        Assertions.assertThat(teamMembersDatabaseService.get(company, UUID.fromString(idInserted)).orElseThrow().getFullName()).isEqualTo("Shrikesh");
    }

    @Test
    public void testDelete() throws SQLException {
        var integration1 = Integration.builder()
                .description("description1")
                .name("integ1 to delete")
                .url("url")
                .application("application")
                .status("active")
                .build();
        var id1 = Integer.valueOf(integrationService.insert(company, integration1));

        var item = DBTeamMember.builder()
                .email("email1_to_delete")
                .fullName("fullName1 to delete")
                .ids(Set.of(
                        DBTeamMember.LoginId.builder()
                                .integrationId(id1)
                                .integrationType(integration1.getApplication())
                                .username("username1")
                                .build(),
                        DBTeamMember.LoginId.builder()
                                .integrationId(id1)
                                .integrationType(integration1.getApplication())
                                .username("username3")
                                .build()
                ))
                .build();
        var memberId = UUID.fromString(teamMembersDatabaseService.insert(company, item));
        var results = teamMembersDatabaseService.filter(company, null, 0, 10);

        var deleted = teamMembersDatabaseService.delete(company, memberId);
        Assertions.assertThat(deleted).isTrue();

        var results2 = teamMembersDatabaseService.filter(company, null, 0, 10);
        Assertions.assertThat(results2.getTotalCount()).isEqualTo(results.getTotalCount() - 1);
    }
}
