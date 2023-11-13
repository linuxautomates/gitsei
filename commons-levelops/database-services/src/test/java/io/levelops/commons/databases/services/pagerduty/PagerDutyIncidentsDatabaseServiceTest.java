package io.levelops.commons.databases.services.pagerduty;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.QueryFilter;
import io.levelops.commons.databases.models.database.Service;
import io.levelops.commons.databases.models.database.pagerduty.DbPagerDutyAlert;
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
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@SuppressWarnings("unused")
public class PagerDutyIncidentsDatabaseServiceTest {
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

    @BeforeClass
    public static void setup() throws SQLException, IOException {
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        template = new NamedParameterJdbcTemplate(dataSource);
        servicesDbService = new ServicesDatabaseService(DefaultObjectMapper.get(), dataSource);
        pdServicesDbService = new PagerDutyServicesDatabaseService(DefaultObjectMapper.get(), dataSource);
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
        servicesDbService.ensureTableExistence(company);
        teamMembersDatabaseService.ensureTableExistence(company);
        pdServicesDbService.ensureTableExistence(company);
        pdIncidentsDbService.ensureTableExistence(company);
        pdAlertsDbService.ensureTableExistence(company);
        userService.ensureTableExistence(company);
        productService.ensureTableExistence(company);
        cicdInstancesDatabaseService.ensureTableExistence(company);
        cicdJobsDbService.ensureTableExistence(company);

        var serviceId1 = UUID.randomUUID();
        var service = Service.builder()
            .id(serviceId1)
            .type(Service.Type.PAGERDUTY)
            .name("My service1")
            .createdAt(Instant.ofEpochSecond(1600637801L))
            .build();
        servicesDbService.insert(company, service);

        var integration = Integration.builder()
            .description("description")
            .name("Integration 1")
            .satellite(false)
            .application("application")
            .status("ok")
            .build();
        var integrationId = integrationsService.insert(company, integration);

        var pdService = DbPagerDutyService.builder()
            .name("My Service 1")
            .pdId("pdId S1")
            .integrationId(1)
            .serviceId(serviceId1)
            .createdAt(Instant.ofEpochSecond(1600647692))
            .build();
        pdServiceId1 = UUID.fromString(pdServicesDbService.insert(company, pdService));
    }

    @Test
    public void testWrite() throws SQLException {
        var pdIncident = DbPagerDutyIncident.builder()
            .pdId("pdId")
            .pdServiceId(pdServiceId1)
            .priority("priority")
            .status("status")
            .summary("summary")
            .urgency("urgency")
            .createdAt(Instant.ofEpochSecond(1600559000))
            .updatedAt(Instant.ofEpochSecond(1600559000))
            .lastStatusAt(Instant.ofEpochSecond(1600559000))
            .statuses(Set.of(DbPagerDutyStatus.builder()
                .status("acknowledged")
                .timestamp(Instant.ofEpochMilli(1600559000))
                .user(DbPagerDutyUser.builder()
                    .pdId("PDU1")
                    .name("name")
                    .email("sample@email")
                    // .timeZone("")
                    .build())
                .build()))
            .build();
        var pdIncidentId1 = pdIncidentsDbService.insert(company, pdIncident);
        var filter = QueryFilter.fromRequestFilters(Map.of("org_product_ids", "462e2c68-e934-11eb-9a03-0242ac130003"));
            pdIncidentsDbService.list(company, filter, 0, 10, null);
        // Assertions.assertThat(pdIncidentId1).isEqualTo(id.toString());

        var dbPdIncident1 = pdIncidentsDbService.getByPagerDutyId(company, "pdId");
        Assertions.assertThat(dbPdIncident1).isPresent();

        // var dbPdIncident1b = pdIncidentsDbService.getByPagerDutyId(company, "pdid");
        // Assertions.assertThat(dbPdIncident1b).isPresent();
        // Assertions.assertThat(dbPdIncident1).isEqualTo(dbPdIncident1b);

        var dbPdIncident2 = pdIncidentsDbService.get(company, pdIncidentId1);
        Assertions.assertThat(dbPdIncident2).isPresent();

        Assertions.assertThat(dbPdIncident1.get()).isEqualTo(dbPdIncident2.get());
        Assertions.assertThat(dbPdIncident2.get().toBuilder()
            .details(null)
            .build())
            .isEqualTo(pdIncident.toBuilder()
                .id(UUID.fromString(pdIncidentId1))
                .statuses(null)
                .build());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testAggregations() throws SQLException {
        var timeStamp = Instant.parse("2020-12-01T01:00:00-08:00");
        var pdIncident = DbPagerDutyIncident.builder()
            .pdId("pdId 1")
            .pdServiceId(pdServiceId1)
            .priority("Priority 1")
            .status("open")
            .summary("Issue 1")
            .urgency("")
            .createdAt(Instant.ofEpochSecond(1600637801))
            .updatedAt(Instant.ofEpochSecond(1600637801))
            .lastStatusAt(Instant.ofEpochSecond(1600637801))
            .statuses(Set.of(DbPagerDutyStatus.builder()
                .status("acknowledged")
                .timestamp(timeStamp)
                .user(DbPagerDutyUser.builder()
                    .pdId("PDU1_1")
                    .name("user1_1")
                    .email("sample2@email.com")
                    // .timeZone("")
                    .build())
                .build(),
                DbPagerDutyStatus.builder()
                .status("acknowledged")
                .timestamp(timeStamp.plus(Duration.ofDays(1)))
                .user(DbPagerDutyUser.builder()
                    .pdId("PDU1_2")
                    .name("user1_2")
                    .email("sample@ramil.com")
                    // .timeZone("")
                    .build())
                .build()
                ))
            .build();
        var incidentId1 = pdIncidentsDbService.insert(company, pdIncident);

        pdIncident = DbPagerDutyIncident.builder()
            .pdId("pdId 2")
            .pdServiceId(pdServiceId1)
            .priority("Priority 1")
            .status("acknoledged")
            .summary("Incident 2")
            .urgency("low")
            .createdAt(Instant.ofEpochSecond(1600637801))
            .updatedAt(Instant.ofEpochSecond(1600637801))
            .lastStatusAt(Instant.ofEpochSecond(1600637801))
            .statuses(Set.of(DbPagerDutyStatus.builder()
                .status("acknowledged")
                .timestamp(timeStamp.plus(Duration.ofDays(2)))
                .user(DbPagerDutyUser.builder()
                    .pdId("PDU2_1")
                    .name("user2_1")
                    .email("sample@email.com")
                    // .timeZone("")
                    .build())
                .build()))
            .build();
        pdIncidentsDbService.insert(company, pdIncident);

        pdIncident = DbPagerDutyIncident.builder()
            .pdId("pdId 3")
            .pdServiceId(pdServiceId1)
            .priority("Priority 2")
            .status("closed")
            .summary("Incident 3")
            .urgency("medium")
            .createdAt(Instant.ofEpochSecond(1600637801))
            .updatedAt(Instant.ofEpochSecond(1600637801))
            .lastStatusAt(Instant.ofEpochSecond(1600637801))
            .build();
        var incidentId2 = pdIncidentsDbService.insert(company, pdIncident);

        pdIncident = DbPagerDutyIncident.builder()
            .pdId("pdId 4")
            .pdServiceId(pdServiceId1)
            .priority("Priority 1")
            .status("open")
            .summary("Incident 4")
            .urgency("low")
            .createdAt(Instant.ofEpochSecond(1600637801))
            .updatedAt(Instant.ofEpochSecond(1600637801))
            .lastStatusAt(Instant.ofEpochSecond(1600637801))
            .build();
        var incidentId3 = pdIncidentsDbService.insert(company, pdIncident);

        pdIncident = DbPagerDutyIncident.builder()
            .pdId("pdId 5")
            .pdServiceId(pdServiceId1)
            .priority("Priority 2")
            .status("closed")
            .summary("Incident 5")
            .urgency("high")
            .createdAt(Instant.ofEpochSecond(1600637801))
            .updatedAt(Instant.ofEpochSecond(1600637801))
            .lastStatusAt(Instant.ofEpochSecond(1600637801))
            .build();
        var incidentId4 = pdIncidentsDbService.insert(company, pdIncident);

        pdIncident = DbPagerDutyIncident.builder()
                .pdId("pdId 6")
                .pdServiceId(pdServiceId1)
                .priority("Priority 2")
                .status("closed")
                .summary("Incident 6")
                .urgency("high")
                .createdAt(Instant.ofEpochSecond(1600237801))
                .updatedAt(Instant.ofEpochSecond(1600237801))
                .lastStatusAt(Instant.ofEpochSecond(1600237801))
                .build();
        pdIncidentsDbService.insert(company, pdIncident);

        var aggs = pdIncidentsDbService.aggregate(company, "priority", "count", null, 0, 10, null);
        Assertions.assertThat(aggs).isNotNull();
        Assertions.assertThat(aggs.getCount()).isEqualTo(1);
        Assertions.assertThat(aggs.getTotalCount()).isEqualTo(1);
        var aggregations = (List<Map<String, Object>>) aggs.getRecords().get(0).get("aggregations");
        Assertions.assertThat(aggregations.size()).isGreaterThanOrEqualTo(2);

        // region - incident trend report - priority
        aggs = pdIncidentsDbService.aggregate(company, "priority", "trend", null, 0, 10, null);
        Assertions.assertThat(aggs.getTotalCount()).isEqualTo(3);
        Assertions.assertThat(aggs.getRecords().size()).isEqualTo(3);
        aggregations = (List<Map<String, Object>>) aggs.getRecords().get(0).get("aggregations");
        Assertions.assertThat(aggregations.size()).isEqualTo(1);
        aggregations = (List<Map<String, Object>>) aggs.getRecords().get(1).get("aggregations");
        Assertions.assertThat(aggregations.size()).isEqualTo(1);
        aggregations = (List<Map<String, Object>>) aggs.getRecords().get(2).get("aggregations");
        Assertions.assertThat(aggregations.size()).isEqualTo(2);
        // end region

        // region - incident trend report - urgency
        aggs = pdIncidentsDbService.aggregate(company, "urgency", "trend", null, 0, 10, null);
        Assertions.assertThat(aggs.getTotalCount()).isEqualTo(3);
        Assertions.assertThat(aggs.getRecords().size()).isEqualTo(3);
        aggregations = (List<Map<String, Object>>) aggs.getRecords().get(0).get("aggregations");
        Assertions.assertThat(aggregations.size()).isEqualTo(1);
        aggregations = (List<Map<String, Object>>) aggs.getRecords().get(1).get("aggregations");
        Assertions.assertThat(aggregations.size()).isEqualTo(1);
        aggregations = (List<Map<String, Object>>) aggs.getRecords().get(2).get("aggregations");
        Assertions.assertThat(aggregations.size()).isEqualTo(4);
        // end region

        var results = pdIncidentsDbService.getTrend(company, QueryFilter.builder()
            .strictMatch("pd_service_id", pdServiceId1)
            .strictMatch("from_created", 1600637800)
            .build(), 0, 30, null);

        Assertions.assertThat(results.getTotalCount()).isEqualTo(1);
        Assertions.assertThat(results.getRecords()).containsAll(List.of(Map.of("key", 1600560000, "count", 5)));

        var trend = pdIncidentsDbService.getAckTrend(company, null, null, 0, 10);
        Assertions.assertThat(trend).isNotNull();

        trend = pdIncidentsDbService.getAckTrend(company, QueryFilter.builder().strictMatch("priority", Set.of("priority 2")).build(), null, 0, 10);
        Assertions.assertThat(trend).isNotNull();

        var afterHours = pdIncidentsDbService.getAfterHoursMinutes(company, 
            Long.valueOf(timeStamp.minus(Duration.ofDays(3)).getEpochSecond()).intValue(), 
            Long.valueOf(timeStamp.plus(Duration.ofDays(3)).getEpochSecond()).intValue(),
            QueryFilter.builder().strictMatch("office_hours", Map.of("from", "09:00", "to", "10:00")).build(),
                null, 0, 10);
        Assertions.assertThat(afterHours).isNotNull();
        var values = pdIncidentsDbService.getValues(company, "incident_priority", null, 0, 100, null);
        Assertions.assertThat(values).isNotNull();

        values = pdIncidentsDbService.getValues(company, "incident_urgency", null, 0, 100, null);
        Assertions.assertThat(values).isNotNull();

        values = pdIncidentsDbService.getValues(company, "pd_service", null, 0, 100, null);
        Assertions.assertThat(values).isNotNull();

        values = pdIncidentsDbService.getValues(company, "user_id", null, 0, 100, null);
        Assertions.assertThat(values).isNotNull();

        var alert1 = DbPagerDutyAlert.builder()
            .createdAt(Instant.now())
            .details(null)
            .incidentId(UUID.fromString(incidentId1))
            .lastStatusAt(Instant.now())
            .pdId("PDA1")
            .pdServiceId(pdServiceId1)
            .severity("high")
            .status("open")
            .summary("Alert 1")
            .updatedAt(Instant.now())
            .build();
        pdAlertsDbService.insert(company, alert1);

        values = pdIncidentsDbService.getValues(company, "alert_severity", null, 0, 100, null);
        Assertions.assertThat(values).isNotNull();

        values = pdIncidentsDbService.getValues(company, "cicd_job_id", null, 0, 100, null);
        Assertions.assertThat(values).isNotNull();
    }
}
