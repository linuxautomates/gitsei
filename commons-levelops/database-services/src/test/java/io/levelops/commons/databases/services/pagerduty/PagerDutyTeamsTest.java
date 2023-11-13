package io.levelops.commons.databases.services.pagerduty;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.QueryFilter;
import io.levelops.commons.databases.models.database.Service;
import io.levelops.commons.databases.models.database.organization.DBOrgProduct;
import io.levelops.commons.databases.models.database.organization.DBTeam;
import io.levelops.commons.databases.models.database.organization.DBTeamMember;
import io.levelops.commons.databases.models.database.organization.TeamMemberId;
import io.levelops.commons.databases.models.database.pagerduty.DbPagerDutyIncident;
import io.levelops.commons.databases.models.database.pagerduty.DbPagerDutyService;
import io.levelops.commons.databases.models.database.pagerduty.DbPagerDutyStatus;
import io.levelops.commons.databases.models.database.pagerduty.DbPagerDutyUser;
import io.levelops.commons.databases.services.CiCdInstancesDatabaseService;
import io.levelops.commons.databases.services.CiCdJobsDatabaseService;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.ProductService;
import io.levelops.commons.databases.services.ServicesDatabaseService;
import io.levelops.commons.databases.services.UserIdentityService;
import io.levelops.commons.databases.services.UserService;
import io.levelops.commons.databases.services.organization.ProductsDatabaseService;
import io.levelops.commons.databases.services.organization.TeamMembersDatabaseService;
import io.levelops.commons.databases.services.organization.TeamsDatabaseService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
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
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.services.pagerduty.PagerDutyIncidentsProductsServiceTest.getProductWithIntegAndTwoFilters;
import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("unused")
public class PagerDutyTeamsTest {
    private static final String company = "test";
    private static final ObjectMapper mapper = DefaultObjectMapper.get();
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;

    private static NamedParameterJdbcTemplate template;

    private static ServicesDatabaseService servicesDbService;
    private static PagerDutyServicesDatabaseService pdServicesDbService;
    private static PagerDutyIncidentsDatabaseService pdIncidentsDbService;
    private static PagerDutyAlertsDatabaseService pdAlertsDbService;
    private static CiCdJobsDatabaseService cicdJobsDbService;
    private static CiCdInstancesDatabaseService cicdInstancesDatabaseService;
    private static UserService userService;
    private static ProductService productService;
    private static IntegrationService integrationsService;
    private static UserIdentityService userIdentityService;
    private static TeamMembersDatabaseService teamMembersDatabaseService;
    private static ProductsDatabaseService productsDatabaseService;

    private static UUID pdServiceId1;
    private static UUID pdServiceId2;
    private static UUID pdServiceId3;
    private static String teamId1;
    private static String teamId2;

    @BeforeClass
    public static void setup() throws SQLException, IOException {
        var timeStamp = Instant.parse("2020-12-01T01:00:00-08:00");
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        template = new NamedParameterJdbcTemplate(dataSource);
        servicesDbService = new ServicesDatabaseService(mapper, dataSource);
        pdServicesDbService = new PagerDutyServicesDatabaseService(mapper, dataSource);
        pdIncidentsDbService = new PagerDutyIncidentsDatabaseService(mapper, dataSource);
        pdAlertsDbService = new PagerDutyAlertsDatabaseService(mapper, dataSource);
        userService = new UserService(dataSource, mapper);
        productService = new ProductService(dataSource);
        productsDatabaseService = new ProductsDatabaseService(dataSource, mapper);
        teamMembersDatabaseService = new TeamMembersDatabaseService(dataSource, mapper);
        userIdentityService = new UserIdentityService(dataSource);
        cicdInstancesDatabaseService = new CiCdInstancesDatabaseService(dataSource);
        cicdJobsDbService = new CiCdJobsDatabaseService(dataSource);
        integrationsService = new IntegrationService(dataSource);
        List.<String>of(
                "CREATE SCHEMA IF NOT EXISTS " + company,
                "CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";"
        )
                .forEach(template.getJdbcTemplate()::execute);
        integrationsService.ensureTableExistence(company);
        userIdentityService.ensureTableExistence(company);
        productsDatabaseService.ensureTableExistence(company);
        teamMembersDatabaseService.ensureTableExistence(company);
        servicesDbService.ensureTableExistence(company);
        pdServicesDbService.ensureTableExistence(company);
        pdIncidentsDbService.ensureTableExistence(company);
        pdAlertsDbService.ensureTableExistence(company);
        userService.ensureTableExistence(company);
        productService.ensureTableExistence(company);
        cicdInstancesDatabaseService.ensureTableExistence(company);
        cicdJobsDbService.ensureTableExistence(company);
        TeamsDatabaseService teamsDatabaseService = new TeamsDatabaseService(dataSource, mapper);
        teamsDatabaseService.ensureTableExistence(company);
        var serviceId1 = UUID.randomUUID();
        var service = Service.builder()
                .id(serviceId1)
                .type(Service.Type.PAGERDUTY)
                .name("My service1")
                .createdAt(Instant.ofEpochSecond(1600637801L))
                .build();
        servicesDbService.insert(company, service);
        var serviceId2 = UUID.randomUUID();
        var service2 = Service.builder()
                .id(serviceId2)
                .type(Service.Type.PAGERDUTY)
                .name("My service2")
                .createdAt(Instant.ofEpochSecond(1600637801L))
                .build();
        servicesDbService.insert(company, service2);

        var integration = Integration.builder()
                .description("description")
                .name("Integration 1")
                .satellite(false)
                .application("pagerduty")
                .status("ok")
                .build();
        var integrationId = integrationsService.insert(company, integration);
        var integration2 = Integration.builder()
                .description("description2")
                .name("Integration 2")
                .satellite(false)
                .application("pagerduty")
                .status("ok")
                .build();
        var integrationId2 = integrationsService.insert(company, integration2);

        var pdService = DbPagerDutyService.builder()
                .name("My Service 1")
                .pdId("pdId S1")
                .integrationId(Integer.valueOf(integrationId))
                .serviceId(serviceId1)
                .createdAt(Instant.ofEpochSecond(1600647692))
                .build();
        pdServiceId1 = UUID.fromString(pdServicesDbService.insert(company, pdService));
        var pdService2 = DbPagerDutyService.builder()
                .name("My Service 2")
                .pdId("pdId S2")
                .integrationId(Integer.valueOf(integrationId))
                .serviceId(serviceId2)
                .createdAt(Instant.ofEpochSecond(1600647681))
                .build();
        pdServiceId2 = UUID.fromString(pdServicesDbService.insert(company, pdService2));
        var pdService3 = DbPagerDutyService.builder()
                .name("My Service 3")
                .pdId("pdId S3")
                .integrationId(Integer.valueOf(integrationId2))
                .serviceId(serviceId1)
                .createdAt(Instant.ofEpochSecond(1600647692))
                .build();
        pdServiceId3 = UUID.fromString(pdServicesDbService.insert(company, pdService3));

        var pdIncident = DbPagerDutyIncident.builder()
                .pdId("pdId")
                .pdServiceId(pdServiceId1)
                .priority("lowest")
                .status("status")
                .summary("TODO")
                .urgency("high")
                .createdAt(Instant.ofEpochSecond(1600637801))
                .updatedAt(Instant.ofEpochSecond(1600637801))
                .lastStatusAt(Instant.ofEpochSecond(1600637801))
                .statuses(Set.of(DbPagerDutyStatus.builder()
                        .status("acknowledged")
                        .timestamp(timeStamp)
                        .user(DbPagerDutyUser.builder()
                                .pdId("PDU1")
                                .name("Ivan-levelops")
                                .email("ivan@gmail.com")
                                .build())
                        .build()))
                .build();
        var pdIncidentId1 = pdIncidentsDbService.insert(company, pdIncident);
        var pdIncident2 = DbPagerDutyIncident.builder()
                .pdId("pdId2")
                .pdServiceId(pdServiceId2)
                .priority("highest")
                .status("status")
                .summary("DONE")
                .urgency("low")
                .createdAt(Instant.ofEpochSecond(1600637801))
                .updatedAt(Instant.ofEpochSecond(1600637801))
                .lastStatusAt(Instant.ofEpochSecond(1600637801))
                .statuses(Set.of(DbPagerDutyStatus.builder()
                        .status("acknowledged")
                        .timestamp(timeStamp.plus(Duration.ofDays(2)))
                        .user(DbPagerDutyUser.builder()
                                .pdId("PDU2")
                                .name("meghana-levelops")
                                .email("meghana@levelops.com")
                                .build())
                        .build()))
                .build();
        var pdIncidentId2 = pdIncidentsDbService.insert(company, pdIncident2);

        var pdIncident3 = DbPagerDutyIncident.builder()
                .pdId("pdId3")
                .pdServiceId(pdServiceId3)
                .priority("high")
                .status("status")
                .summary("BACKLOG")
                .urgency("medium")
                .createdAt(Instant.ofEpochSecond(1600637801))
                .updatedAt(Instant.ofEpochSecond(1600637801))
                .lastStatusAt(Instant.ofEpochSecond(1600637801))
                .statuses(Set.of(DbPagerDutyStatus.builder()
                        .status("acknowledged")
                        .timestamp(timeStamp.plus(Duration.ofDays(2)))
                        .user(DbPagerDutyUser.builder()
                                .pdId("PDU3")
                                .name("viraj-levelops")
                                .email("viraj@levelops.com")
                                .build())
                        .build()))
                .build();
        var pdIncidentId3 = pdIncidentsDbService.insert(company, pdIncident3);
        String scmUserIdIvan = userIdentityService.getUser(company, String.valueOf(1), "ivan@gmail.com");
        String scmUserIdMeghana = userIdentityService.getUser(company, String.valueOf(1), "meghana@levelops.com");
        String scmUserIdViraj = userIdentityService.getUser(company, String.valueOf(1), "viraj@levelops.com");

        var dbUsers = List.of("meghana-levelops", "viraj-levelops", "Ivan-levelops");
        
        teamMembersDatabaseService.upsert(company, DBTeamMember.builder().fullName(dbUsers.get(0)).build(), UUID.fromString(scmUserIdMeghana));
        teamMembersDatabaseService.upsert(company, DBTeamMember.builder().fullName(dbUsers.get(1)).build(), UUID.fromString(scmUserIdViraj));
        teamMembersDatabaseService.upsert(company, DBTeamMember.builder().fullName(dbUsers.get(2)).build(), UUID.fromString(scmUserIdIvan));

        List<Optional<TeamMemberId>> teamMemberIds = List.of(scmUserIdIvan, scmUserIdMeghana, scmUserIdViraj).stream()
                .map(uuidInserted -> teamMembersDatabaseService.getId(company, UUID.fromString(uuidInserted))).collect(Collectors.toList());
        DBTeam team1 = DBTeam.builder()
                .name("PagerDuty Team 1")
                .description("This is a sample PagerDuty team meghana, viraj and ivan are part of...")
                .managers(Set.of(DBTeam.TeamMemberId.builder().id(UUID.fromString(teamMemberIds.get(0).get().getTeamMemberId())).build()))
                .members(Set.of(DBTeam.TeamMemberId.builder().id(UUID.fromString(teamMemberIds.get(0).get().getTeamMemberId())).build(),
                        DBTeam.TeamMemberId.builder().id(UUID.fromString(teamMemberIds.get(1).get().getTeamMemberId())).build()
                ))
                .build();
        teamId1 = teamsDatabaseService.insert(company, team1);
        DBTeam team2 = DBTeam.builder()
                .name("PagerDuty Team 2")
                .description("This is a sample PagerDuty team ivan are part of...")
                .managers(Set.of(DBTeam.TeamMemberId.builder().id(UUID.fromString(teamMemberIds.get(2).get().getTeamMemberId())).build()))
                .members(Set.of(DBTeam.TeamMemberId.builder().id(UUID.fromString(teamMemberIds.get(2).get().getTeamMemberId())).build()))
                .build();
        teamId2 = teamsDatabaseService.insert(company, team2);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testTeamFilter() throws SQLException {
        DbListResponse<Map<String, Object>> userIds = pdIncidentsDbService.getAckTrend(company, QueryFilter.builder()
                        .strictMatch("user_ids", List.of("team_id:" + teamId1)).build(),
                null, 0, 10);
        Assertions.assertThat(userIds).isNotNull();
        Assertions.assertThat(userIds.getTotalCount()).isEqualTo(2);

        userIds = pdIncidentsDbService.getAckTrend(company, QueryFilter.builder()
                        .strictMatch("user_ids", List.of("team_id:" + teamId2)).build(),
                null, 0, 10);
        Assertions.assertThat(userIds).isNotNull();
        Assertions.assertThat(userIds.getTotalCount()).isEqualTo(1);

        userIds = pdIncidentsDbService.getAckTrend(company, QueryFilter.builder()
                        .strictMatch("user_ids", List.of("team_id:" + teamId2, "team_id:" + teamId1)).build(),
                null, 0, 10);
        Assertions.assertThat(userIds).isNotNull();
        Assertions.assertThat(userIds.getTotalCount()).isEqualTo(3);
        List<Map<String, Object>> aggregations1 = (List<Map<String, Object>>) userIds.getRecords().get(0).get("aggregations");
        List<Map<String, Object>> aggregations2 = (List<Map<String, Object>>) userIds.getRecords().get(1).get("aggregations");
        assertThat(aggregations2.size()).isEqualTo(1);
        assertThat(aggregations1.size()).isEqualTo(1);

        userIds = pdIncidentsDbService.getAckTrend(company, QueryFilter.builder()
                        .strictMatch("user_ids", List.of("team_id:" + teamId2, "team_id:" + teamId1, "8f6174c9-dd42-41a2-b1ae-a8fa6b5776ab")).build(),
                null, 0, 10);
        Assertions.assertThat(userIds).isNotNull();
        Assertions.assertThat(userIds.getTotalCount()).isEqualTo(3);
        aggregations1 = (List<Map<String, Object>>) userIds.getRecords().get(0).get("aggregations");
        aggregations2 = (List<Map<String, Object>>) userIds.getRecords().get(1).get("aggregations");
        assertThat(aggregations2.size()).isEqualTo(1);
        assertThat(aggregations1.size()).isEqualTo(1);

        DBOrgProduct product = getProductWithIntegAndTwoFilters();
        String uuid = productsDatabaseService.insert(company, product);
        var results = pdIncidentsDbService.getTrend(company, QueryFilter.builder()
                .strictMatch("user_ids", List.of("team_id:" + teamId2, "team_id:" + teamId1, "8f6174c9-dd42-41a2-b1ae-a8fa6b5776ab"))
                .build(), 0, 30, Set.of(UUID.fromString(uuid)));
        assertThat(results.getRecords()).isNotNull();
        assertThat(results.getTotalCount()).isEqualTo(1);
    }

    @Test
    public void testTeamFilterAfterHours() throws SQLException {
        var timeStamp = Instant.parse("2020-12-01T01:00:00-08:00");
        var afterHours = pdIncidentsDbService.getAfterHoursMinutes(company,
                Long.valueOf(timeStamp.minus(Duration.ofDays(3)).getEpochSecond()).intValue(),
                Long.valueOf(timeStamp.plus(Duration.ofDays(3)).getEpochSecond()).intValue(),
                QueryFilter.builder().strictMatch("office_hours", Map.of("from", "09:00", "to", "10:00"))
                        .strictMatch("user_ids", List.of("team_id:" + teamId1)).build(),
                null, 0, 10);
        Assertions.assertThat(afterHours).isNotNull();
        Assertions.assertThat(afterHours.getTotalCount()).isEqualTo(2);
        Assertions.assertThat(afterHours.getRecords().stream().map(record -> record.get("name")).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("meghana-levelops", "Ivan-levelops");
        afterHours = pdIncidentsDbService.getAfterHoursMinutes(company,
                Long.valueOf(timeStamp.minus(Duration.ofDays(3)).getEpochSecond()).intValue(),
                Long.valueOf(timeStamp.plus(Duration.ofDays(3)).getEpochSecond()).intValue(),
                QueryFilter.builder().strictMatch("office_hours", Map.of("from", "09:00", "to", "10:00"))
                        .strictMatch("user_ids", List.of("team_id:" + teamId2, "team_id:" + teamId1)).build(),
                null, 0, 10);
        Assertions.assertThat(afterHours).isNotNull();
        Assertions.assertThat(afterHours.getTotalCount()).isEqualTo(3);
        Assertions.assertThat(afterHours.getRecords().stream().map(record -> record.get("name")).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("viraj-levelops", "meghana-levelops", "Ivan-levelops");

        afterHours = pdIncidentsDbService.getAfterHoursMinutes(company,
                Long.valueOf(timeStamp.minus(Duration.ofDays(3)).getEpochSecond()).intValue(),
                Long.valueOf(timeStamp.plus(Duration.ofDays(3)).getEpochSecond()).intValue(),
                QueryFilter.builder().strictMatch("office_hours", Map.of("from", "09:00", "to", "10:00"))
                        .strictMatch("user_ids", List.of("team_id:" + teamId2)).build(),
                null, 0, 10);
        Assertions.assertThat(afterHours).isNotNull();
        Assertions.assertThat(afterHours.getTotalCount()).isEqualTo(1);
        Assertions.assertThat(afterHours.getRecords().stream().map(record -> record.get("name")).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("viraj-levelops");

        afterHours = pdIncidentsDbService.getAfterHoursMinutes(company,
                Long.valueOf(timeStamp.minus(Duration.ofDays(3)).getEpochSecond()).intValue(),
                Long.valueOf(timeStamp.plus(Duration.ofDays(3)).getEpochSecond()).intValue(),
                QueryFilter.builder().strictMatch("office_hours", Map.of("from", "09:00", "to", "10:00"))
                        .strictMatch("user_ids", List.of("8f6174c9-dd42-41a2-b1ae-a8fa6b5776ab")).build(),
                null, 0, 10);
        Assertions.assertThat(afterHours).isNotNull();
        Assertions.assertThat(afterHours.getTotalCount()).isEqualTo(0);

        afterHours = pdIncidentsDbService.getAfterHoursMinutes(company,
                Long.valueOf(timeStamp.minus(Duration.ofDays(3)).getEpochSecond()).intValue(),
                Long.valueOf(timeStamp.plus(Duration.ofDays(3)).getEpochSecond()).intValue(),
                QueryFilter.builder().strictMatch("office_hours", Map.of("from", "09:00", "to", "10:00"))
                        .strictMatch("user_ids", List.of("8f6174c9-dd42-41a2-b1ae-a8fa6b5776ab"))
                        .strictMatch("user_ids", List.of("team_id:" + teamId2)).build(),
                null, 0, 10);
        Assertions.assertThat(afterHours).isNotNull();
        Assertions.assertThat(afterHours.getTotalCount()).isEqualTo(1);
        Assertions.assertThat(afterHours.getRecords().stream().map(record -> record.get("name")).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("viraj-levelops");
    }
}
