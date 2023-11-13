package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.QueryFilter;
import io.levelops.commons.databases.models.database.Service;
import io.levelops.commons.databases.models.database.organization.DBOrgProduct;
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

@SuppressWarnings("unused")
public class ServicesDatabaseServiceTest {
    private static final String company = "test";
    private static final ObjectMapper mapper = DefaultObjectMapper.get();
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;

    private static NamedParameterJdbcTemplate template;

    private static ServicesDatabaseService servicesDbService;
    private static IntegrationService integrationsService;
    private static ProductsDatabaseService productsDatabaseService;

    @BeforeClass
    public static void setup() throws SQLException, IOException {
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        template = new NamedParameterJdbcTemplate(dataSource);
        servicesDbService = new ServicesDatabaseService(mapper, dataSource);
        productsDatabaseService = new ProductsDatabaseService(dataSource, mapper);
        integrationsService = new IntegrationService(dataSource);
        List.<String>of(
                "CREATE SCHEMA IF NOT EXISTS " + company,
                "CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";"
        )
                .forEach(template.getJdbcTemplate()::execute);
        integrationsService.ensureTableExistence(company);
        productsDatabaseService.ensureTableExistence(company);
        servicesDbService.ensureTableExistence(company);
        var integration = Integration.builder()
                .description("description")
                .name("Integration 1")
                .satellite(false)
                .application("pagerduty")
                .status("ok")
                .build();
        integrationsService.insert(company, integration);
        var integration2 = Integration.builder()
                .description("description 2")
                .name("Integration 2")
                .satellite(false)
                .application("pagerduty")
                .status("ok")
                .build();
        integrationsService.insert(company, integration2);
    }

    @Test
    public void test() throws SQLException {
        var originalId = UUID.randomUUID();
        var service = Service.builder()
                .id(originalId)
                .type(Service.Type.PAGERDUTY)
                .name("My service1")
                .createdAt(Instant.ofEpochSecond(1600637801L))
                .build();
        var id = servicesDbService.insert(company, service);
        var id2 = servicesDbService.insert(company, service);
    }

    @Test
    public void testProductFiltersList() throws SQLException {
        var originalId = UUID.randomUUID();
        var service = Service.builder()
                .id(originalId)
                .type(Service.Type.PAGERDUTY)
                .name("My service1")
                .createdAt(Instant.ofEpochSecond(1600637808L))
                .build();
        var id = servicesDbService.insert(company, service);

        var originalId2 = UUID.randomUUID();
        var service2 = Service.builder()
                .id(originalId2)
                .type(Service.Type.PAGERDUTY)
                .name("My service2")
                .createdAt(Instant.ofEpochSecond(1600637809L))
                .build();
        var id2 = servicesDbService.insert(company, service2);
        var originalId3 = UUID.randomUUID();
        var service3 = Service.builder()
                .id(originalId3)
                .type(Service.Type.PAGERDUTY)
                .name("My service3")
                .createdAt(Instant.ofEpochSecond(1600637801L))
                .build();
        var id3 = servicesDbService.insert(company, service3);
        var results = servicesDbService.list(company, QueryFilter.builder()
                .strictMatch("name", "My service2")
                .build(), 0, 10, null);
        Assertions.assertThat(results.getRecords()).isNotNull();
        Assertions.assertThat(results.getTotalCount()).isEqualTo(1);

        DBOrgProduct product = getProductWithFilter();
        String uuid = productsDatabaseService.insert(company, product);
        results = servicesDbService.list(company, null, 0, 10, Set.of(UUID.fromString(uuid)));
        Assertions.assertThat(results.getRecords()).isNotNull();
        Assertions.assertThat(results.getTotalCount()).isEqualTo(1);
        Assertions.assertThat(results.getRecords().stream().map(Service::getName).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("My service2");
        List<String> uuidsList = new ArrayList<>();
        List<DBOrgProduct> orgProductList = getProductWithFilters();
        for (DBOrgProduct prod : orgProductList) {
            uuidsList.add(productsDatabaseService.insert(company, prod));
        }
        results = servicesDbService.list(company, null, 0, 10,
                Set.of(UUID.fromString(uuidsList.get(0))));
        Assertions.assertThat(results.getRecords()).isNotNull();
        Assertions.assertThat(results.getTotalCount()).isEqualTo(1);
        Assertions.assertThat(results.getRecords().stream().map(Service::getName).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("My service2");
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
                                .filters(Map.of("name", "My service2"))
                                .build()
                ))
                .build();
    }

    public static List<DBOrgProduct> getProductWithFilters() {
        return List.of(DBOrgProduct.builder()
                .name("Sample Product 2")
                .description("This is a sample pagerduty product")
                .integrations(Set.of(
                        DBOrgProduct.Integ.builder()
                                .integrationId(1)
                                .name("pagerduty-test-4")
                                .type("pagerduty")
                                .filters(Map.of("name", "My service2"))
                                .build(),
                        DBOrgProduct.Integ.builder()
                                .integrationId(2)
                                .name("pagerduty-test-2")
                                .type("pagerduty")
                                .filters(Map.of("type", "PAGERDUTY"))
                                .build()
                ))
                .build());
    }
}

