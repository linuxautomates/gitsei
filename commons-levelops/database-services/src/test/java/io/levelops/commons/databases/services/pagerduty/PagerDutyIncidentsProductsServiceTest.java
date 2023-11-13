package io.levelops.commons.databases.services.pagerduty;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.QueryFilter;
import io.levelops.commons.databases.models.database.Service;
import io.levelops.commons.databases.models.database.organization.DBOrgProduct;
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
import lombok.extern.log4j.Log4j2;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@Log4j2
@SuppressWarnings("unused")
public class PagerDutyIncidentsProductsServiceTest {

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
                                .name("name")
                                .email("sample")
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
                .createdAt(Instant.ofEpochSecond(1600637801))
                .updatedAt(Instant.ofEpochSecond(1600637801))
                .lastStatusAt(Instant.ofEpochSecond(1600637801))
                .statuses(Set.of(DbPagerDutyStatus.builder()
                        .status("acknowledged")
                        .timestamp(timeStamp.plus(Duration.ofDays(2)))
                        .user(DbPagerDutyUser.builder()
                                .pdId("PDU1")
                                .name("name")
                                .email("sample")
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
                .createdAt(Instant.ofEpochSecond(1600637801))
                .updatedAt(Instant.ofEpochSecond(1600637801))
                .lastStatusAt(Instant.ofEpochSecond(1600637801))
                .statuses(Set.of(DbPagerDutyStatus.builder()
                        .status("acknowledged")
                        .timestamp(timeStamp.plus(Duration.ofDays(2)))
                        .user(DbPagerDutyUser.builder()
                                .pdId("PDU1")
                                .name("name")
                                .email("sample@gmail.com")
                                .build())
                        .build()))
                .build();
        var pdIncidentId3 = pdIncidentsDbService.insert(company, pdIncident3);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testProductFiltersListAndAggregate() throws SQLException {

        DBOrgProduct product = getProductWithIntegAndTwoFilters();
        String uuid = productsDatabaseService.insert(company, product);
        DbListResponse<DbPagerDutyIncident> dbListResponse = pdIncidentsDbService.list(company,
                QueryFilter.builder().build(),
                0, 10, Set.of(UUID.fromString(uuid))
        );
        assertThat(dbListResponse).isNotNull();
        assertThat(dbListResponse.getTotalCount()).isEqualTo(1);
        assertThat(dbListResponse.getRecords().stream().map(DbPagerDutyIncident::getUrgency).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("low");

        DBOrgProduct product2 = getProductWithIntegAndOneFilters();
        String uuid2 = productsDatabaseService.insert(company, product2);
        DbListResponse<DbPagerDutyIncident> dbListResponse2 = pdIncidentsDbService.list(company,
                QueryFilter.builder().build(),
                0,
                10,
                Set.of(UUID.fromString(uuid2))
        );
        assertThat(dbListResponse2).isNotNull();
        assertThat(dbListResponse2.getTotalCount()).isEqualTo(1);
        assertThat(dbListResponse2.getRecords().stream().map(DbPagerDutyIncident::getPriority).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("lowest");

        DBOrgProduct product3 = getProductWithIntegAndFilters();
        String uuid3 = productsDatabaseService.insert(company, product3);
        DbListResponse<DbPagerDutyIncident> dbListResponse3 = pdIncidentsDbService.list(company,
                QueryFilter.builder().build(),
                0,
                10,
                Set.of(UUID.fromString(uuid3))
        );
        assertThat(dbListResponse3).isNotNull();
        assertThat(dbListResponse3.getTotalCount()).isEqualTo(1);
        assertThat(dbListResponse3.getRecords().stream().map(DbPagerDutyIncident::getSummary).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("DONE");
        DbListResponse<DbPagerDutyIncident> dbListResponse4 = pdIncidentsDbService.list(company,
                QueryFilter.builder().build(),
                0,
                10,
                null
        );
        assertThat(dbListResponse4).isNotNull();
        assertThat(dbListResponse4.getTotalCount()).isEqualTo(3);
        assertThat(dbListResponse4.getRecords().stream().map(DbPagerDutyIncident::getSummary).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("DONE", "TODO", "BACKLOG");

        DBOrgProduct product5 = getProductWithIntegAndNoFilters();
        String uuid5 = productsDatabaseService.insert(company, product5);
        DbListResponse<DbPagerDutyIncident> dbListResponse5 = pdIncidentsDbService.list(company,
                QueryFilter.builder().build(),
                0,
                10,
                Set.of(UUID.fromString(uuid5))
        );
        assertThat(dbListResponse5).isNotNull();
        assertThat(dbListResponse5.getTotalCount()).isEqualTo(3);
        assertThat(dbListResponse5.getRecords().stream().map(DbPagerDutyIncident::getSummary).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("DONE", "TODO", "BACKLOG");
        List<String> uuidsList = new ArrayList<>();
        List<DBOrgProduct> orgProductList = getProductWithTwoIntegAndFilters();
        for (DBOrgProduct prod : orgProductList) {
            uuidsList.add(productsDatabaseService.insert(company, prod));
        }
        DbListResponse<DbPagerDutyIncident> dbListResponse6 = pdIncidentsDbService.list(company,
                QueryFilter.builder().build(),
                0,
                10,
                Set.of(UUID.fromString(uuidsList.get(0))));
        assertThat(dbListResponse6).isNotNull();
        assertThat(dbListResponse6.getTotalCount()).isEqualTo(1);
        assertThat(dbListResponse6.getRecords().stream().map(DbPagerDutyIncident::getSummary).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("BACKLOG");
        /**
         * Aggregate tests from below..
         */
        var aggs = pdIncidentsDbService.aggregate(company, "priority", "count",
                null, 0, 10, Set.of(UUID.fromString(uuid2)));
        assertThat(aggs).isNotNull();
        assertThat(aggs.getTotalCount()).isEqualTo(1);
        assertThat(aggs.getRecords().size()).isEqualTo(1);
        List<Map<String, Object>> aggregations = (List<Map<String, Object>>) aggs.getRecords().get(0).get("aggregations");
        Assertions.assertThat(aggregations.size()).isEqualTo(1);

        aggs = pdIncidentsDbService.aggregate(company, "urgency", "count",
                null, 0, 10, Set.of(UUID.fromString(uuid)));
        assertThat(aggs).isNotNull();
        assertThat(aggs.getTotalCount()).isEqualTo(1);
        assertThat(aggs.getRecords().size()).isEqualTo(1);
        aggregations = (List<Map<String, Object>>) aggs.getRecords().get(0).get("aggregations");
        Assertions.assertThat(aggregations.size()).isEqualTo(1);

        aggs = pdIncidentsDbService.aggregate(company, "urgency", "count",
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
        aggs = pdIncidentsDbService.aggregate(company, "summary", "count",
                null, 0, 10, Set.of(UUID.fromString(uuid5)));
        assertThat(aggs).isNotNull();
        assertThat(aggs.getTotalCount()).isEqualTo(3);
        assertThat(aggs.getRecords().size()).isEqualTo(3);
        aggregations2 = (List<Map<String, Object>>) aggs.getRecords().get(0).get("aggregations");
        assertThat(aggregations2.size()).isEqualTo(1);
    }

    @Test
    public void testGetValues() throws SQLException {
        DBOrgProduct product = getProductWithIntegAndTwoFilters();
        String uuid = productsDatabaseService.insert(company, product);
        var values = pdIncidentsDbService.getValues(company, "pd_service", null, 0, 100,
                Set.of(UUID.fromString(uuid)));
        assertThat(values).isNotNull();
        assertThat(values.getTotalCount()).isEqualTo(3);
        values = pdIncidentsDbService.getValues(company, "incident_urgency", null, 0, 100, null);
        assertThat(values).isNotNull();
        assertThat(values.getTotalCount()).isEqualTo(3);

        DBOrgProduct product2 = getProductWithIntegAndOneFilters();
        String uuid2 = productsDatabaseService.insert(company, product2);
        values = pdIncidentsDbService.getValues(company, "pd_service", null, 0, 100, Set.of(UUID.fromString(uuid2)));
        assertThat(values).isNotNull();
        assertThat(values.getTotalCount()).isEqualTo(3);

        DBOrgProduct product3 = getProductWithIntegAndFilters();
        String uuid3 = productsDatabaseService.insert(company, product3);
        values = pdIncidentsDbService.getValues(company, "pd_service", null, 0, 100, Set.of(UUID.fromString(uuid3)));
        assertThat(values).isNotNull();
        assertThat(values.getTotalCount()).isEqualTo(3);

        DBOrgProduct product5 = getProductWithIntegAndNoFilters();
        String uuid5 = productsDatabaseService.insert(company, product5);
        values = pdIncidentsDbService.getValues(company, "pd_service", null, 0, 100,
                Set.of(UUID.fromString(uuid5)));
        Assertions.assertThat(values).isNotNull();
        assertThat(values.getTotalCount()).isEqualTo(3);

        List<String> uuidsList = new ArrayList<>();
        List<DBOrgProduct> orgProductList = getProductWithTwoIntegAndFilters();
        for (DBOrgProduct prod : orgProductList) {
            uuidsList.add(productsDatabaseService.insert(company, prod));
        }
        values = pdIncidentsDbService.getValues(company, "pd_service", null, 0, 100,
                Set.of(UUID.fromString(uuidsList.get(0))));
        assertThat(values).isNotNull();
        assertThat(values.getTotalCount()).isEqualTo(3);
    }

    @Test
    public void testGetTrend() throws SQLException {
        DBOrgProduct product = getProductWithIntegAndTwoFilters();
        String uuid = productsDatabaseService.insert(company, product);
        var results = pdIncidentsDbService.getTrend(company, QueryFilter.builder()
                .build(), 0, 30, Set.of(UUID.fromString(uuid)));
        assertThat(results.getRecords()).isNotNull();
        assertThat(results.getTotalCount()).isEqualTo(1);

        DBOrgProduct product2 = getProductWithIntegAndOneFilters();
        String uuid2 = productsDatabaseService.insert(company, product2);
        results = pdIncidentsDbService.getTrend(company, QueryFilter.builder()
                .build(), 0, 30, Set.of(UUID.fromString(uuid2)));
        assertThat(results.getRecords()).isNotNull();
        assertThat(results.getTotalCount()).isEqualTo(1);

        DBOrgProduct product3 = getProductWithIntegAndFilters();
        String uuid3 = productsDatabaseService.insert(company, product3);
        results = pdIncidentsDbService.getTrend(company, QueryFilter.builder()
                .build(), 0, 30, Set.of(UUID.fromString(uuid3)));
        assertThat(results.getRecords()).isNotNull();
        assertThat(results.getTotalCount()).isEqualTo(1);

        DBOrgProduct product5 = getProductWithIntegAndNoFilters();
        String uuid5 = productsDatabaseService.insert(company, product5);
        results = pdIncidentsDbService.getTrend(company, QueryFilter.builder()
                .build(), 0, 30, Set.of(UUID.fromString(uuid5)));
        assertThat(results.getRecords()).isNotNull();
        assertThat(results.getTotalCount()).isEqualTo(1);

        List<String> uuidsList = new ArrayList<>();
        List<DBOrgProduct> orgProductList = getProductWithTwoIntegAndFilters();
        for (DBOrgProduct prod : orgProductList) {
            uuidsList.add(productsDatabaseService.insert(company, prod));
        }
        results = pdIncidentsDbService.getTrend(company, QueryFilter.builder()
                .build(), 0, 30, Set.of(UUID.fromString(uuid5)));
        assertThat(results.getRecords()).isNotNull();
        assertThat(results.getTotalCount()).isEqualTo(1);

        DBOrgProduct product6 = getProductWithIntegandUUID();
        String uuid6 = productsDatabaseService.insert(company, product6);
        results = pdIncidentsDbService.getTrend(company, QueryFilter.builder()
                .build(), 0, 30, Set.of(UUID.fromString(uuid6)));
        assertThat(results.getRecords()).isNotNull();
        assertThat(results.getTotalCount()).isEqualTo(1);

    }

    @Test
    public void testAfterHours() throws SQLException {
        var timeStamp = Instant.parse("2020-12-01T01:00:00-08:00");
        DBOrgProduct product = getProductWithIntegAndTwoFilters();
        String uuid = productsDatabaseService.insert(company, product);
        var afterHours = pdIncidentsDbService.getAfterHoursMinutes(company,
                Long.valueOf(timeStamp.minus(Duration.ofDays(3)).getEpochSecond()).intValue(),
                Long.valueOf(timeStamp.plus(Duration.ofDays(3)).getEpochSecond()).intValue(),
                QueryFilter.builder().strictMatch("office_hours", Map.of("from", "09:00", "to", "10:00")).build(),
                Set.of(UUID.fromString(uuid)), 0, 10);
        Assertions.assertThat(afterHours).isNotNull();
        Assertions.assertThat(afterHours.getTotalCount()).isEqualTo(1);

        DBOrgProduct product2 = getProductWithIntegAndOneFilters();
        String uuid2 = productsDatabaseService.insert(company, product2);
        afterHours = pdIncidentsDbService.getAfterHoursMinutes(company,
                Long.valueOf(timeStamp.minus(Duration.ofDays(3)).getEpochSecond()).intValue(),
                Long.valueOf(timeStamp.plus(Duration.ofDays(3)).getEpochSecond()).intValue(),
                QueryFilter.builder().strictMatch("office_hours", Map.of("from", "09:00", "to", "10:00")).build(),
                Set.of(UUID.fromString(uuid2)), 0, 10);
        Assertions.assertThat(afterHours).isNotNull();
        Assertions.assertThat(afterHours.getTotalCount()).isEqualTo(1);

        DBOrgProduct product3 = getProductWithIntegAndFilters();
        String uuid3 = productsDatabaseService.insert(company, product3);
        afterHours = pdIncidentsDbService.getAfterHoursMinutes(company,
                Long.valueOf(timeStamp.minus(Duration.ofDays(3)).getEpochSecond()).intValue(),
                Long.valueOf(timeStamp.plus(Duration.ofDays(3)).getEpochSecond()).intValue(),
                QueryFilter.builder().strictMatch("office_hours", Map.of("from", "09:00", "to", "10:00")).build(),
                Set.of(UUID.fromString(uuid3)), 0, 10);
        Assertions.assertThat(afterHours).isNotNull();
        Assertions.assertThat(afterHours.getTotalCount()).isEqualTo(1);

        DBOrgProduct product4 = getProductWithIntegAndNoFilters();
        String uuid4 = productsDatabaseService.insert(company, product4);
        afterHours = pdIncidentsDbService.getAfterHoursMinutes(company,
                Long.valueOf(timeStamp.minus(Duration.ofDays(3)).getEpochSecond()).intValue(),
                Long.valueOf(timeStamp.plus(Duration.ofDays(3)).getEpochSecond()).intValue(),
                QueryFilter.builder().strictMatch("office_hours", Map.of("from", "09:00", "to", "10:00")).build(),
                Set.of(UUID.fromString(uuid4)), 0, 10);
        Assertions.assertThat(afterHours).isNotNull();
        Assertions.assertThat(afterHours.getTotalCount()).isEqualTo(1);

    }

    public static DBOrgProduct getProductWithIntegAndTwoFilters() {
        return DBOrgProduct.builder()
                .name("Sample Product 1")
                .description("This is a sample pagerduty product")
                .integrations(Set.of(
                        DBOrgProduct.Integ.builder()
                                .integrationId(1)
                                .name("pagerduty-test-1")
                                .type("pagerduty")
                                .filters(Map.of("urgency", "low"))
                                .build()
                ))
                .build();
    }

    public static DBOrgProduct getProductWithIntegAndOneFilters() {
        return DBOrgProduct.builder()
                .name("Sample Product 2")
                .description("This is a sample pagerduty product")
                .integrations(Set.of(
                        DBOrgProduct.Integ.builder()
                                .integrationId(1)
                                .name("pagerduty-test-2")
                                .type("pagerduty")
                                .filters(Map.of("priority", "lowest"))
                                .build()
                ))
                .build();
    }

    public static DBOrgProduct getProductWithIntegAndFilters() {
        return DBOrgProduct.builder()
                .name("Sample Product 3")
                .description("This is a sample pagerduty product")
                .integrations(Set.of(
                        DBOrgProduct.Integ.builder()
                                .integrationId(1)
                                .name("pagerduty-test-3")
                                .type("pagerduty")
                                .filters(Map.of("summary", "DONE"))
                                .build()
                ))
                .build();
    }

    public static DBOrgProduct getProductWithIntegAndNoFilters() {
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

    public static DBOrgProduct getProductWithIntegandUUID() {
        return DBOrgProduct.builder()
                .name("Sample Product 7")
                .description("This is a sample pagerduty product")
                .integrations(Set.of(
                        DBOrgProduct.Integ.builder()
                                .integrationId(1)
                                .name("pagerduty-test-7")
                                .type("pagerduty")
                                .filters(Map.of("pd_service_id", pdServiceId1))
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
                                .filters(Map.of("priority", "high"))
                                .build(),
                        DBOrgProduct.Integ.builder()
                                .integrationId(2)
                                .name("pagerduty-test-2")
                                .type("pagerduty")
                                .filters(Map.of("summary", "BACKLOG"))
                                .build()
                ))
                .build());
    }
}
