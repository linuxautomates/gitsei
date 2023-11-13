package io.levelops.commons.databases.services.pagerduty;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.QueryFilter;
import io.levelops.commons.databases.models.database.Service;
import io.levelops.commons.databases.models.database.organization.DBOrgProduct;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class PagerDutyAlertsProductsServiceTest {
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


    @BeforeClass
    public static void setup() throws SQLException, IOException {
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
                .createdAt(Instant.ofEpochSecond(1600559000))
                .updatedAt(Instant.ofEpochSecond(1600559000))
                .lastStatusAt(Instant.ofEpochSecond(1600559000))
                .statuses(Set.of(DbPagerDutyStatus.builder()
                        .status("acknowledged")
                        .timestamp(Instant.ofEpochMilli(1600559000))
                        .user(DbPagerDutyUser.builder()
                                .pdId("PDU1")
                                .name("name")
                                .email("ivan@levelops.com")
                                .build())
                        .build()))
                .build();
        var pdIncidentId1 = pdIncidentsDbService.insert(company, pdIncident);
        var pdIncident2 = DbPagerDutyIncident.builder()
                .pdId("pdId")
                .pdServiceId(pdServiceId2)
                .priority("highest")
                .status("status")
                .summary("DONE")
                .urgency("low")
                .createdAt(Instant.ofEpochSecond(1600559000))
                .updatedAt(Instant.ofEpochSecond(1600559000))
                .lastStatusAt(Instant.ofEpochSecond(1600559000))
                .statuses(Set.of(DbPagerDutyStatus.builder()
                        .status("acknowledged")
                        .timestamp(Instant.ofEpochMilli(1600559000))
                        .user(DbPagerDutyUser.builder()
                                .pdId("PDU1")
                                .name("name")
                                .email("ivan@levelops.com")
                                .build())
                        .build()))
                .build();
        var pdIncidentId2 = pdIncidentsDbService.insert(company, pdIncident2);

        var pdIncident3 = DbPagerDutyIncident.builder()
                .pdId("pdId")
                .pdServiceId(pdServiceId3)
                .priority("high")
                .status("status")
                .summary("BACKLOG")
                .urgency("medium")
                .createdAt(Instant.ofEpochSecond(1600559000))
                .updatedAt(Instant.ofEpochSecond(1600559000))
                .lastStatusAt(Instant.ofEpochSecond(1600559000))
                .statuses(Set.of(DbPagerDutyStatus.builder()
                        .status("acknowledged")
                        .timestamp(Instant.ofEpochMilli(1600559000))
                        .user(DbPagerDutyUser.builder()
                                .pdId("PDU1")
                                .name("name")
                                .email("ivan@levelops.com")
                                .build())
                        .build()))
                .build();
        var pdIncidentId3 = pdIncidentsDbService.insert(company, pdIncident3);

        var pdAlert1 = DbPagerDutyAlert.builder()
                .pdId("pdId 1")
                .incidentId(UUID.fromString(pdIncidentId1))
                .pdServiceId(pdServiceId1)
                .severity("severity-1")
                .summary("DONE")
                .status("NEWEST")
                .createdAt(Instant.ofEpochSecond(1600637801))
                .updatedAt(Instant.ofEpochSecond(1600637801))
                .lastStatusAt(Instant.ofEpochSecond(1600637801))
                .build();
        pdAlertsDbService.insert(company, pdAlert1);
        var pdAlert2 = DbPagerDutyAlert.builder()
                .pdId("pdId 2")
                .incidentId(UUID.fromString(pdIncidentId2))
                .pdServiceId(pdServiceId2)
                .severity("severity-2")
                .summary("BACKLOG")
                .status("OLD")
                .createdAt(Instant.ofEpochSecond(1600637801))
                .updatedAt(Instant.ofEpochSecond(1600637801))
                .lastStatusAt(Instant.ofEpochSecond(1600637801))
                .build();
        pdAlertsDbService.insert(company, pdAlert2);

        var pdAlert3 = DbPagerDutyAlert.builder()
                .pdId("pdId 3")
                .incidentId(UUID.fromString(pdIncidentId3))
                .pdServiceId(pdServiceId3)
                .severity("severity-3")
                .summary("TODO")
                .status("NEW")
                .createdAt(Instant.ofEpochSecond(1600637801))
                .updatedAt(Instant.ofEpochSecond(1600637801))
                .lastStatusAt(Instant.ofEpochSecond(1600637801))
                .build();
        pdAlertsDbService.insert(company, pdAlert3);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testProductFiltersList() throws SQLException {
        DBOrgProduct product = getProductWithFilter();
        String uuid = productsDatabaseService.insert(company, product);
        DbListResponse<DbPagerDutyAlert> dbListResponse = pdAlertsDbService.list(company,
                QueryFilter.builder().build(),
                0,
                10,
                Set.of(UUID.fromString(uuid))
        );
        assertThat(dbListResponse).isNotNull();
        assertThat(dbListResponse.getTotalCount()).isEqualTo(1);
        assertThat(dbListResponse.getRecords().stream().map(DbPagerDutyAlert::getSummary).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("BACKLOG");

        DBOrgProduct product2 = getProductWithDifferentFilter();
        String uuid2 = productsDatabaseService.insert(company, product2);
        DbListResponse<DbPagerDutyAlert> dbListResponse2 = pdAlertsDbService.list(company,
                QueryFilter.builder().build(),
                0,
                10,
                Set.of(UUID.fromString(uuid2))
        );
        assertThat(dbListResponse2).isNotNull();
        assertThat(dbListResponse2.getTotalCount()).isEqualTo(1);
        assertThat(dbListResponse2.getRecords().stream().map(DbPagerDutyAlert::getSummary).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("TODO");

        DBOrgProduct product3 = getProductWithSeverityFilter();
        String uuid3 = productsDatabaseService.insert(company, product3);
        DbListResponse<DbPagerDutyAlert> dbListResponse3 = pdAlertsDbService.list(company,
                QueryFilter.builder().build(),
                0,
                10,
                Set.of(UUID.fromString(uuid3))
        );
        assertThat(dbListResponse3).isNotNull();
        assertThat(dbListResponse3.getTotalCount()).isEqualTo(1);
        assertThat(dbListResponse3.getRecords().stream().map(DbPagerDutyAlert::getSeverity).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("severity-3");

        DbListResponse<DbPagerDutyAlert> dbListResponse4 = pdAlertsDbService.list(company,
                QueryFilter.builder().build(),
                0,
                10,
                null
        );
        assertThat(dbListResponse4).isNotNull();
        assertThat(dbListResponse4.getTotalCount()).isEqualTo(3);
        assertThat(dbListResponse4.getRecords().stream().map(DbPagerDutyAlert::getStatus).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("NEW", "OLD", "NEWEST");
        DBOrgProduct product5 = getProductWithnoFilters();
        String uuid5 = productsDatabaseService.insert(company, product5);
        DbListResponse<DbPagerDutyAlert> dbListResponse5 = pdAlertsDbService.list(company,
                QueryFilter.builder().build(),
                0,
                10,
                Set.of(UUID.fromString(uuid5))
        );
        assertThat(dbListResponse5).isNotNull();
        assertThat(dbListResponse5.getTotalCount()).isEqualTo(3);
        assertThat(dbListResponse5.getRecords().stream().map(DbPagerDutyAlert::getStatus).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("NEW", "OLD", "NEWEST");

        List<String> uuidsList = new ArrayList<>();
        List<DBOrgProduct> orgProductList = getProductWithTwoIntegAndFilters();
        for (DBOrgProduct prod : orgProductList) {
            uuidsList.add(productsDatabaseService.insert(company, prod));
        }
        DbListResponse<DbPagerDutyAlert> dbListResponse6 = pdAlertsDbService.list(company,
                QueryFilter.builder().build(),
                0,
                10,
                Set.of(UUID.fromString(uuidsList.get(0))));
        assertThat(dbListResponse6).isNotNull();
        assertThat(dbListResponse6.getTotalCount()).isEqualTo(3);
        assertThat(dbListResponse6.getRecords().stream().map(DbPagerDutyAlert::getSummary).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("DONE", "TODO", "BACKLOG");

        /**
         * Aggregate tests from below..
         */
        var aggs = pdAlertsDbService.aggregate(company, "summary", "count",
                null, 0, 10, Set.of(UUID.fromString(uuid2)));
        assertThat(aggs).isNotNull();
        assertThat(aggs.getTotalCount()).isEqualTo(1);
        assertThat(aggs.getRecords().size()).isEqualTo(1);
        List<Map<String, Object>> aggregations = (List<Map<String, Object>>) aggs.getRecords().get(0).get("aggregations");
        Assertions.assertThat(aggregations.size()).isEqualTo(1);

        aggs = pdAlertsDbService.aggregate(company, "status", "count",
                null, 0, 10, Set.of(UUID.fromString(uuid)));
        assertThat(aggs).isNotNull();
        assertThat(aggs.getTotalCount()).isEqualTo(1);
        assertThat(aggs.getRecords().size()).isEqualTo(1);
        aggregations = (List<Map<String, Object>>) aggs.getRecords().get(0).get("aggregations");
        assertThat(aggregations.size()).isEqualTo(1);
        assertThat(aggregations.get(0).get("key")).isEqualTo("OLD");

        aggs = pdAlertsDbService.aggregate(company, "summary", "count",
                null, 0, 10, null);
        assertThat(aggs).isNotNull();
        assertThat(aggs.getTotalCount()).isEqualTo(3);
        assertThat(aggs.getRecords().size()).isEqualTo(3);
        aggregations = (List<Map<String, Object>>) aggs.getRecords().get(0).get("aggregations");
        List<Map<String, Object>> aggregations2 = (List<Map<String, Object>>) aggs.getRecords().get(1).get("aggregations");
        List<Map<String, Object>> aggregations3 = (List<Map<String, Object>>) aggs.getRecords().get(2).get("aggregations");
        assertThat(aggregations.size()).isEqualTo(1);
        assertThat(aggregations2.size()).isEqualTo(1);
        assertThat(aggregations3.size()).isEqualTo(1);

        aggs = pdAlertsDbService.aggregate(company, "summary", "count",
                null, 0, 10, Set.of(UUID.fromString(uuid5)));
        assertThat(aggs).isNotNull();
        assertThat(aggs.getTotalCount()).isEqualTo(3);
        assertThat(aggs.getRecords().size()).isEqualTo(3);
        aggregations2 = (List<Map<String, Object>>) aggs.getRecords().get(0).get("aggregations");
        assertThat(aggregations2.size()).isEqualTo(1);
    }


    public static DBOrgProduct getProductWithFilter() {
        return DBOrgProduct.builder()
                .name("Sample Product 1")
                .description("This is a sample pagerduty product")
                .integrations(Set.of(
                        DBOrgProduct.Integ.builder()
                                .integrationId(1)
                                .name("pagerduty-test-1")
                                .type("pagerduty")
                                .filters(Map.of("status", "OLD"))
                                .build()
                ))
                .build();
    }

    public static DBOrgProduct getProductWithDifferentFilter() {
        return DBOrgProduct.builder()
                .name("Sample Product 2")
                .description("This is a sample pagerduty product")
                .integrations(Set.of(
                        DBOrgProduct.Integ.builder()
                                .integrationId(1)
                                .name("pagerduty-test-2")
                                .type("pagerduty")
                                .filters(Map.of("status", "NEW"))
                                .build()
                ))
                .build();
    }

    public static DBOrgProduct getProductWithSeverityFilter() {
        return DBOrgProduct.builder()
                .name("Sample Product 3")
                .description("This is a sample pagerduty product")
                .integrations(Set.of(
                        DBOrgProduct.Integ.builder()
                                .integrationId(1)
                                .name("pagerduty-test-3")
                                .type("pagerduty")
                                .filters(Map.of("severity", "severity-3"))
                                .build()
                ))
                .build();
    }

    public static DBOrgProduct getProductWithnoFilters() {
        return DBOrgProduct.builder()
                .name("Sample Product 4")
                .description("This is a sample pagerduty product")
                .integrations(Set.of(
                        DBOrgProduct.Integ.builder()
                                .integrationId(1)
                                .name("pagerduty-test-4")
                                .type("pagerduty")
                                .filters(Map.of())
                                .build()
                ))
                .build();
    }

    public static List<DBOrgProduct> getProductWithTwoIntegAndFilters() {
        return List.of(DBOrgProduct.builder()
                .name("Sample Product 5")
                .description("This is a sample pagerduty product")
                .integrations(Set.of(
                        DBOrgProduct.Integ.builder()
                                .integrationId(1)
                                .name("pagerduty-test-4")
                                .type("pagerduty")
                                .filters(Map.of("summary", "TODO"))
                                .build(),
                        DBOrgProduct.Integ.builder()
                                .integrationId(2)
                                .name("pagerduty-test-2")
                                .type("pagerduty")
                                .filters(Map.of("severity", "severity-2"))
                                .build()
                ))
                .build());
    }
}
