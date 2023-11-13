package io.levelops.commons.databases.services.pagerduty;

import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.QueryFilter;
import io.levelops.commons.databases.models.database.Service;
import io.levelops.commons.databases.models.database.organization.DBOrgProduct;
import io.levelops.commons.databases.models.database.pagerduty.DbPagerDutyService;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.ServicesDatabaseService;
import io.levelops.commons.databases.services.organization.ProductsDatabaseService;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class PagerDutyServicesDatabaseServiceTest {
    private static final String company = "test";

    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;

    private static NamedParameterJdbcTemplate template;

    private static ServicesDatabaseService servicesDbService;
    private static PagerDutyServicesDatabaseService pdServicesDbService;
    private static IntegrationService integrationsService;
    private static ProductsDatabaseService productsDatabaseService;

    @BeforeClass
    public static void setup() throws SQLException, IOException {
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        template = new NamedParameterJdbcTemplate(dataSource);
        pdServicesDbService = new PagerDutyServicesDatabaseService(DefaultObjectMapper.get(), dataSource);
        servicesDbService = new ServicesDatabaseService(DefaultObjectMapper.get(),dataSource);
        integrationsService = new IntegrationService(dataSource);
        productsDatabaseService= new ProductsDatabaseService(dataSource, DefaultObjectMapper.get());
        List.<String>of(
            "CREATE SCHEMA IF NOT EXISTS " + company,
            "CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";"
            )
            .forEach(template.getJdbcTemplate()::execute);
        servicesDbService.ensureTableExistence(company);
        integrationsService.ensureTableExistence(company);
        pdServicesDbService.ensureTableExistence(company);
        productsDatabaseService.ensureTableExistence(company);
        var integration = Integration.builder()
            .description("description")
            .name("Integration 1")
            .satellite(false)
            .application("pagerduty")
            .status("ok")
            .build();
        integrationsService.insert(company, integration);
        var integration2 = Integration.builder()
                .description("description")
                .name("Integration 2")
                .satellite(false)
                .application("pagerduty")
                .status("ok")
                .build();
        integrationsService.insert(company, integration2);
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
    }

    @Test
    public void testWrite() throws SQLException {
        var services = servicesDbService.list(company, QueryFilter.builder().build(), 0, 10, null);

        var id = UUID.randomUUID();
        var pdService = DbPagerDutyService.builder()
            .id(id)
            .name("sample1")
            .pdId("pdId")
            .integrationId(1)
            .serviceId(services.getRecords().get(0).getId())
            .createdAt(Instant.ofEpochSecond(1600650699))
            .build();

        var pdServiceId1 = pdServicesDbService.insert(company, pdService);
        var id2 = UUID.randomUUID();
        var pdService2 = DbPagerDutyService.builder()
                .id(id2)
                .name("sample2")
                .pdId("pdId2")
                .integrationId(1)
                .serviceId(services.getRecords().get(1).getId())
                .createdAt(Instant.ofEpochSecond(1600650690))
                .build();

        var pdServiceId2 = pdServicesDbService.insert(company, pdService2);
        Assertions.assertThat(pdServiceId2).isEqualTo(id2.toString());
    }

    @Test
    public void testRead() throws SQLException {
        var results = pdServicesDbService.list(company, 0, 10);
        Assertions.assertThat(results.getCount()).isGreaterThan(0);
    }

    @Test
    public void testList() throws SQLException {
        DBOrgProduct product = getProductWithFilter();
        String uuid = productsDatabaseService.insert(company, product);
        var results  = pdServicesDbService.list(company, null, 0, 10, Set.of(UUID.fromString(uuid)));
        Assertions.assertThat(results.getRecords()).isNotNull();
        Assertions.assertThat(results.getTotalCount()).isEqualTo(1);
        Assertions.assertThat(results.getRecords().stream().map(DbPagerDutyService::getName).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("sample1");

        DBOrgProduct product2 = getProductWithNoFilter();
        String uuid2 = productsDatabaseService.insert(company, product2);
        results  = pdServicesDbService.list(company, null, 0, 10, Set.of(UUID.fromString(uuid2)));
        Assertions.assertThat(results.getRecords()).isNotNull();
        Assertions.assertThat(results.getTotalCount()).isEqualTo(2);
        Assertions.assertThat(results.getRecords().stream().map(DbPagerDutyService::getName).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("sample1", "sample2");

        List<String> uuidsList = new ArrayList<>();
        List<DBOrgProduct> orgProductList = getProductWithFilters();
        for (DBOrgProduct prod : orgProductList) {
            uuidsList.add(productsDatabaseService.insert(company, prod));
        }
        results  = pdServicesDbService.list(company, null, 0, 10, Set.of(UUID.fromString(uuidsList.get(0))));
        Assertions.assertThat(results.getRecords()).isNotNull();
        Assertions.assertThat(results.getTotalCount()).isEqualTo(1);
        Assertions.assertThat(results.getRecords().stream().map(DbPagerDutyService::getName).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("sample1");
        Assertions.assertThat(results.getRecords().stream().map(DbPagerDutyService::getPdId).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("pdId");
    }
    public static DBOrgProduct getProductWithNoFilter() {
        return DBOrgProduct.builder()
                .name("Sample Product 1")
                .description("This is a sample pagerduty product")
                .integrations(Set.of(
                        DBOrgProduct.Integ.builder()
                                .integrationId(1)
                                .name("pagerduty-test-1")
                                .type("pagerduty")
                                .filters(Map.of())
                                .build()
                ))
                .build();
    }

    public static DBOrgProduct getProductWithFilter() {
        return DBOrgProduct.builder()
                .name("Sample Product 2")
                .description("This is a sample pagerduty product")
                .integrations(Set.of(
                        DBOrgProduct.Integ.builder()
                                .integrationId(1)
                                .name("pagerduty-test-1")
                                .type("pagerduty")
                                .filters(Map.of("name", "sample1"))
                                .build()
                ))
                .build();
    }

    public static List<DBOrgProduct> getProductWithFilters() {
        return List.of(DBOrgProduct.builder()
                .name("Sample Product 3")
                .description("This is a sample pagerduty product")
                .integrations(Set.of(
                        DBOrgProduct.Integ.builder()
                                .integrationId(1)
                                .name("pagerduty-test-4")
                                .type("pagerduty")
                                .filters(Map.of("name", "sample1"))
                                .build(),
                        DBOrgProduct.Integ.builder()
                                .integrationId(2)
                                .name("pagerduty-test-2")
                                .type("pagerduty")
                                .filters(Map.of("pd_id", "pdId"))
                                .build()
                ))
                .build());
    }
    
}
