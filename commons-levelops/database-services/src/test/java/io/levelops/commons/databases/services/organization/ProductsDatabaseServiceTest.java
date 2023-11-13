package io.levelops.commons.databases.services.organization;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.QueryFilter;
import io.levelops.commons.databases.models.database.organization.DBOrgProduct;
import io.levelops.commons.databases.services.IntegrationService;
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
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@SuppressWarnings("unused")
public class ProductsDatabaseServiceTest {
    
    private static final String company = "test";
    private static final ObjectMapper mapper = DefaultObjectMapper.get();
    
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;

    private static NamedParameterJdbcTemplate template;

    private static ProductsDatabaseService productsDatabaseService;
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

        productsDatabaseService = new ProductsDatabaseService(dataSource, mapper);
        productsDatabaseService.ensureTableExistence(company);
    }

    @Test
    public void test() throws SQLException{
        var integration1 = Integration.builder()
            .description("description1")
            .name("integ1")
            .url("url")
            .application("application1")
            .status("active")
            .build();
        var id1 = Integer.valueOf(integrationService.insert(company, integration1));

        var integration2 = Integration.builder()
            .description("description1")
            .name("integ2")
            .url("url")
            .application("application1")
            .status("active")
            .build();
        var id2 = Integer.valueOf(integrationService.insert(company, integration2));

        var integration3 = Integration.builder()
            .description("description1")
            .name("integ3")
            .url("url")
            .application("application2")
            .status("active")
            .build();
        var id3 = Integer.valueOf(integrationService.insert(company, integration3));

        var item = DBOrgProduct.builder()
            .name("product1")
            .description("description1")
            .integrations(Set.of(
                DBOrgProduct.Integ.builder()
                    .integrationId(id1)
                    .name(integration1.getName())
                    .type(integration1.getApplication())
                    .filters(Map.of("project", "LFE"))
                    .build(),
                DBOrgProduct.Integ.builder()
                    .integrationId(id2)
                    .name(integration2.getName())
                    .type(integration2.getApplication())
                    .filters(Map.of())
                    .build(),
                DBOrgProduct.Integ.builder()
                    .integrationId(id1)
                    .name(integration1.getName())
                    .type(integration1.getApplication())
                    .filters(Map.of("project", "lev"))
                    .build(),
                DBOrgProduct.Integ.builder()
                    .integrationId(id3)
                    .name(integration3.getName())
                    .type(integration3.getApplication())
                    .filters(Map.of())
                    .build()
                ))
            .build();
        var productId = productsDatabaseService.insert(company, item);

        var results = productsDatabaseService.filter(company, null, 0, 10);

        Assertions.assertThat(results).isNotNull();
        Assertions.assertThat(results.getRecords().get(0)).isEqualTo(item.toBuilder().id(UUID.fromString(productId)).build());

        var item2 = DBOrgProduct.builder()
            .name("product2")
            .description("description2")
            .integrations(Set.of(
                DBOrgProduct.Integ.builder()
                    .integrationId(id2)
                    .name(integration2.getName())
                    .type(integration2.getApplication())
                    .filters(Map.of("project", "LFE2"))
                    .build()
                ))
            .build();
        var productId2 = productsDatabaseService.insert(company, item2);

        var item3 = DBOrgProduct.builder()
            .name("product3")
            .description("description3")
            .integrations(Set.of(
                DBOrgProduct.Integ.builder()
                    .integrationId(id3)
                    .name(integration3.getName())
                    .type(integration3.getApplication())
                    .filters(Map.of("project", "LFE3"))
                    .build(),
                DBOrgProduct.Integ.builder()
                    .integrationId(id2)
                    .name(integration2.getName())
                    .type(integration2.getApplication())
                    .filters(Map.of())
                    .build()
                ))
            .build();
        var productId3 = productsDatabaseService.insert(company, item3);

        var item4 = DBOrgProduct.builder()
            .name("product4")
            .description("description4")
            .integrations(Set.of(
                    DBOrgProduct.Integ.builder()
                        .integrationId(id2)
                        .name(integration2.getName())
                        .type(integration2.getApplication())
                        .filters(Map.of("project", "LFE4"))
                        .build()
                ))
            .build();
        var productId4 = UUID.fromString(productsDatabaseService.insert(company, item4));

        results = productsDatabaseService.filter(company, QueryFilter.builder().strictMatch("integration_type", "application1").build(), 0, 10);
        Assertions.assertThat(results).isNotNull();
        Assertions.assertThat(results.getRecords().size()).isEqualTo(4);

        results = productsDatabaseService.filter(company, QueryFilter.builder().strictMatch("integration_type", "application2").build(), 0, 10);
        Assertions.assertThat(results).isNotNull();
        Assertions.assertThat(results.getRecords().size()).isEqualTo(2);

        results = productsDatabaseService.filter(company, QueryFilter.builder().strictMatch("product_id", productId4).build(), 0, 10);
        Assertions.assertThat(results).isNotNull();
        Assertions.assertThat(results.getRecords().size()).isEqualTo(1);
        Assertions.assertThat(results.getRecords().get(0)).isEqualTo(item4.toBuilder().id(productId4).build());
    }

    @Test
    public void deleteTest() throws NumberFormatException, SQLException{
        var integration1 = Integration.builder()
            .description("description1")
            .name("integ1-to-delete")
            .url("url")
            .application("application1")
            .status("active")
            .build();
        var id1 = Integer.valueOf(integrationService.insert(company, integration1));

        var item = DBOrgProduct.builder()
            .name("product1 to delete")
            .description("description1")
            .integrations(Set.of(
                DBOrgProduct.Integ.builder()
                    .integrationId(id1)
                    .name(integration1.getName())
                    .type(integration1.getApplication())
                    .filters(Map.of("project", "LFE"))
                    .build(),
                DBOrgProduct.Integ.builder()
                    .integrationId(id1)
                    .name(integration1.getName())
                    .type(integration1.getApplication())
                    .filters(Map.of("project", "lev"))
                    .build()
                ))
            .build();
        
        var productId = productsDatabaseService.insert(company, item);
        var results = productsDatabaseService.filter(company, null, 0, 10);

        var deleted = productsDatabaseService.delete(company, productId);

    }
}
