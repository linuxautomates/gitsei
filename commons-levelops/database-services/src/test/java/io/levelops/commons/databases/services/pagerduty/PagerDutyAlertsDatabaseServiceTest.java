package io.levelops.commons.databases.services.pagerduty;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.QueryFilter;
import io.levelops.commons.databases.models.database.Service;
import io.levelops.commons.databases.models.database.pagerduty.DbPagerDutyAlert;
import io.levelops.commons.databases.models.database.pagerduty.DbPagerDutyIncident;
import io.levelops.commons.databases.models.database.pagerduty.DbPagerDutyService;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.ServicesDatabaseService;
import io.levelops.commons.databases.services.UserIdentityService;
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
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PagerDutyAlertsDatabaseServiceTest {
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
    private static IntegrationService integrationsService;
    private static UserIdentityService userIdentityService;
    private static TeamMembersDatabaseService teamMembersDatabaseService;
    private static UUID pdIncidentId1;
    private static UUID pdServiceId1;

    @BeforeClass
    public static void setup() throws SQLException, IOException {
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        template = new NamedParameterJdbcTemplate(dataSource);
        servicesDbService = new ServicesDatabaseService(DefaultObjectMapper.get(), dataSource);
        pdServicesDbService = new PagerDutyServicesDatabaseService(DefaultObjectMapper.get(), dataSource);
        pdIncidentsDbService = new PagerDutyIncidentsDatabaseService(mapper, dataSource);
        pdAlertsDbService = new PagerDutyAlertsDatabaseService(mapper, dataSource);
        integrationsService = new IntegrationService(dataSource);
        teamMembersDatabaseService = new TeamMembersDatabaseService(dataSource, mapper);
        userIdentityService = new UserIdentityService(dataSource);
        List.<String>of(
            "CREATE SCHEMA IF NOT EXISTS " + company,
            "CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";"
            )
            .forEach(template.getJdbcTemplate()::execute);
        integrationsService.ensureTableExistence(company);
        userIdentityService.ensureTableExistence(company);
        teamMembersDatabaseService.ensureTableExistence(company);
        servicesDbService.ensureTableExistence(company);
        pdServicesDbService.ensureTableExistence(company);
        pdIncidentsDbService.ensureTableExistence(company);
        pdAlertsDbService.ensureTableExistence(company);

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
        
        var pdIncident = DbPagerDutyIncident.builder()
            .pdId("pdId")
            .pdServiceId(pdServiceId1)
            .priority("priority")
            .status("status")
            .summary("summary")
            .urgency("urgency")
            .createdAt(Instant.ofEpochSecond(1600637801))
            .updatedAt(Instant.ofEpochSecond(1600637801))
            .lastStatusAt(Instant.ofEpochSecond(1600637801))
            .build();
        pdIncidentId1 = UUID.fromString(pdIncidentsDbService.insert(company, pdIncident));
    }

    @Test
    public void test() throws SQLException {
        var id = UUID.randomUUID();
        var pdAlert1 = DbPagerDutyAlert.builder()
            .id(id)
            .incidentId(pdIncidentId1)
            .severity("severity")
            .pdId("pdId")
            .pdServiceId(pdServiceId1)
            .summary("summary")
            .status("status")
            .createdAt(Instant.ofEpochSecond(1600637801))
            .updatedAt(Instant.ofEpochSecond(1600637801))
            .lastStatusAt(Instant.ofEpochSecond(1600637801))
            .build();
        var pdAlertId1 = pdAlertsDbService.insert(company, pdAlert1);

        Assertions.assertThat(pdAlertId1).isEqualTo(id.toString());
    }

    @Test
    public void testRead() throws SQLException {
        var pdAlert1 = DbPagerDutyAlert.builder()
            .incidentId(pdIncidentId1)
            .severity("severity1")
            .pdId("pdId1")
            .pdServiceId(pdServiceId1)
            .summary("summary")
            .status("status")
            .createdAt(Instant.ofEpochSecond(1600637801))
            .updatedAt(Instant.ofEpochSecond(1600637801))
            .lastStatusAt(Instant.ofEpochSecond(1600637801))
            .build();
        var pdAlertId1 = pdAlertsDbService.insert(company, pdAlert1);

        var results = pdAlertsDbService.list(company, QueryFilter.builder().strictMatch("severity", "severity1").build(), 0, 10, null);
        
        Assertions.assertThat(results.getCount()).isGreaterThan(0);
        Assertions.assertThat(results.getRecords().get(0).getId().toString()).isEqualTo(pdAlertId1);

        var dbPdAlert1 = pdAlertsDbService.get(company, pdAlertId1);
        var dbPdAlert2 = pdAlertsDbService.getByPagerDutyId(company, "pdId1");
        Assertions.assertThat(dbPdAlert1).isEqualTo(dbPdAlert2);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testAggregations() throws SQLException {
        var pdAlert1 = DbPagerDutyAlert.builder()
            .pdId("pdId 1")
            .incidentId(pdIncidentId1)
            .pdServiceId(pdServiceId1)
            .severity("severity 1")
            .summary("summary")
            .status("status")
            .createdAt(Instant.ofEpochSecond(1600637801))
            .updatedAt(Instant.ofEpochSecond(1600637801))
            .lastStatusAt(Instant.ofEpochSecond(1600637801))
            .build();
        pdAlertsDbService.insert(company, pdAlert1);

        pdAlert1 = DbPagerDutyAlert.builder()
            .pdId("pdId 2")
            .incidentId(pdIncidentId1)
            .pdServiceId(pdServiceId1)
            .severity("severity 2")
            .summary("summary")
            .status("status")
            .createdAt(Instant.ofEpochSecond(1600637801))
            .updatedAt(Instant.ofEpochSecond(1600637801))
            .lastStatusAt(Instant.ofEpochSecond(1600637801))
            .build();
        pdAlertsDbService.insert(company, pdAlert1);

        pdAlert1 = DbPagerDutyAlert.builder()
            .pdId("pdId 3")
            .incidentId(pdIncidentId1)
            .pdServiceId(pdServiceId1)
            .severity("severity 1")
            .summary("summary")
            .status("status")
            .createdAt(Instant.ofEpochSecond(1600637801))
            .updatedAt(Instant.ofEpochSecond(1600637801))
            .lastStatusAt(Instant.ofEpochSecond(1600637801))
            .build();
        pdAlertsDbService.insert(company, pdAlert1);

        pdAlert1 = DbPagerDutyAlert.builder()
            .pdId("pdId 4")
            .incidentId(pdIncidentId1)
            .pdServiceId(pdServiceId1)
            .severity("severity 3")
            .summary("summary")
            .status("status")
            .createdAt(Instant.ofEpochSecond(1600637801))
            .updatedAt(Instant.ofEpochSecond(1600637801))
            .lastStatusAt(Instant.ofEpochSecond(1600637801))
            .build();
        pdAlertsDbService.insert(company, pdAlert1);

        pdAlert1 = DbPagerDutyAlert.builder()
            .pdId("pdId 5")
            .incidentId(pdIncidentId1)
            .pdServiceId(pdServiceId1)
            .severity("severity 1")
            .summary("summary")
            .status("status")
            .createdAt(Instant.ofEpochSecond(1600637801))
            .updatedAt(Instant.ofEpochSecond(1600637801))
            .lastStatusAt(Instant.ofEpochSecond(1600637801))
            .build();
        pdAlertsDbService.insert(company, pdAlert1);

        pdAlert1 = DbPagerDutyAlert.builder()
            .pdId("pdId 6")
            .incidentId(pdIncidentId1)
            .pdServiceId(pdServiceId1)
            .severity("severity 3")
            .summary("summary")
            .status("status")
            .createdAt(Instant.ofEpochSecond(1600637801))
            .updatedAt(Instant.ofEpochSecond(1600637801))
            .lastStatusAt(Instant.ofEpochSecond(1600637801))
            .build();
        pdAlertsDbService.insert(company, pdAlert1);

        var aggs = pdAlertsDbService.aggregate(company, "severity", "count", null, 0, 10, null);
        Assertions.assertThat(aggs).isNotNull();
        Assertions.assertThat(aggs.getCount()).isEqualTo(1);
        Assertions.assertThat(aggs.getTotalCount()).isEqualTo(1);
        var aggregations = (List<Map<String, Object>>) aggs.getRecords().get(0).get("aggregations");
        Assertions.assertThat(aggregations.size()).isEqualTo(5);

        aggs = pdAlertsDbService.aggregate(company, "severity", "trend", null, 0, 10, null);
        Assertions.assertThat(aggs).isNotNull();
        Assertions.assertThat(aggs.getTotalCount()).isEqualTo(1);
        aggregations = (List<Map<String, Object>>) aggs.getRecords().get(0).get("aggregations");
        Assertions.assertThat(aggregations.size()).isEqualTo(5);
    }
    
}
