package io.levelops.commons.databases.services;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

import javax.sql.DataSource;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.assertj.core.api.Assertions;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import io.levelops.commons.databases.models.database.Product;
import io.levelops.commons.databases.models.database.QueryFilter;
import io.levelops.commons.databases.models.database.User;
import io.levelops.commons.databases.models.database.access.RoleType;
import io.levelops.commons.databases.models.database.mappings.ComponentProductMapping;
import io.levelops.commons.databases.models.database.mappings.TagItemMapping;
import io.levelops.commons.databases.models.database.mappings.TagItemMapping.TagItemType;
import io.levelops.commons.databases.models.database.plugins.DbPluginResult;
import io.levelops.commons.databases.models.database.plugins.MsTmtVulnerability;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.jackson.ParsingUtils;
import io.levelops.commons.models.ComponentType;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;

@SuppressWarnings("unused")
public class MsTMTDatabaseServiceTest {
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    private static DataSource dataSource;
    private final static String company = "test";

    private static MsTMTDatabaseService dbService;
    private static PluginResultsDatabaseService pluginResultsService;
    private static UserService userService;
    private static ProductService productService;
    private static ComponentProductMappingService componentProductMappingService;
    private static TagsService tagsService;
    private static TagItemDBService tagitemService;
    private static IntegrationService integrationService;

    private static ObjectMapper mapper = DefaultObjectMapper.get();

    @BeforeClass
    public static void setup() throws SQLException {
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        var template = new NamedParameterJdbcTemplate(dataSource);
        List.of(
                "CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";",
                "CREATE SCHEMA IF NOT EXISTS " + company
        ).forEach(template.getJdbcTemplate()::execute);

        userService = new UserService(dataSource, mapper);
        userService.ensureTableExistence(company);
        integrationService = new IntegrationService(dataSource);
        integrationService.ensureTableExistence(company);
        productService = new ProductService(dataSource);
        productService.ensureTableExistence(company);
        componentProductMappingService = new ComponentProductMappingService(mapper, dataSource);
        componentProductMappingService.ensureTableExistence(company);
        tagsService = new TagsService(dataSource);
        tagsService.ensureTableExistence(company);
        tagitemService = new TagItemDBService(dataSource);
        tagitemService.ensureTableExistence(company);
        pluginResultsService = new PluginResultsDatabaseService(dataSource, mapper, componentProductMappingService);
        pluginResultsService.ensureTableExistence(company);

        dbService = new MsTMTDatabaseService(dataSource, mapper);
        dbService.ensureTableExistence(company);
    }

    @Test
    public void test() throws SQLException {
        var array = dataSource.getConnection().createArrayOf("text", new String[]{"a", "b"});
        var set = ParsingUtils.parseSet("test", Map.class, array);
        System.out.println("SET: " + set);
    }

    @Test
    public void insertTest() throws SQLException {
        var pluginResult1 = DbPluginResult.builder()
            .createdAt(0L)
            .gcsPath("gcsPath")
            .metadata(Map.of())
            .pluginClass("pluginClass")
            .pluginName("pluginName")
            .pluginId(UUID.randomUUID())
            .productIds(List.of())
            .successful(true)
            .version("version")
            .labels(List.of())
            .build();
        var pluginResultId1 = UUID.fromString(pluginResultsService.insert(company, pluginResult1));
        
        var tagIds1 = tagsService.insert(company, List.of("tag1", "tag2"));

        tagitemService.batchInsert(company, List.of(
            TagItemMapping.builder()
                .itemId(pluginResultId1.toString())
                .tagId(tagIds1.get(0))
                .tagItemType(TagItemType.PLUGIN_RESULT)
                .build(),
            TagItemMapping.builder()
                .itemId(pluginResultId1.toString())
                .tagId(tagIds1.get(1))
                .tagItemType(TagItemType.PLUGIN_RESULT)
                .build()
            ));

        var pluginResult2 = DbPluginResult.builder()
            .createdAt(0L)
            .gcsPath("gcsPath")
            .metadata(Map.of())
            .pluginClass("pluginClass")
            .pluginName("pluginName")
            .pluginId(UUID.randomUUID())
            .productIds(List.of())
            .successful(true)
            .version("version")
            .labels(List.of())
            .build();
        var pluginResultId2 = UUID.fromString(pluginResultsService.insert(company, pluginResult2));
        
        var tagIds2 = tagsService.insert(company, List.of("test", "lol"));

        tagitemService.batchInsert(company, List.of(
            TagItemMapping.builder()
                .itemId(pluginResultId2.toString())
                .tagId(tagIds2.get(0))
                .tagItemType(TagItemType.PLUGIN_RESULT)
                .build(),
            TagItemMapping.builder()
                .itemId(pluginResultId2.toString())
                .tagId(tagIds2.get(1))
                .tagItemType(TagItemType.PLUGIN_RESULT)
                .build()
            ));

        var user = User.builder()
            .email("email")
            .firstName("firstName")
            .lastName("lastName")
            .passwordAuthEnabled(false)
            .bcryptPassword("bcryptPassword")
            .samlAuthEnabled(false)
            .userType(RoleType.ADMIN)
            .build();
        var ownerId = userService.insert(company, user);

        var product1 = Product.builder()
            .description("description")
            .immutable(false)
            .key("key")
            .name("Product 1")
            .ownerId(ownerId)
            .build();
        var productId1 = Integer.valueOf(productService.insert(company, product1));

        var product2 = Product.builder()
            .description("description")
            .immutable(false)
            .key("key2")
            .name("name2")
            .ownerId(ownerId)
            .build();
        var productId2 = Integer.valueOf(productService.insert(company, product2));

        componentProductMappingService.insert(company, ComponentProductMapping.builder()
            .componentId(pluginResultId1)
            .componentType(ComponentType.PLUGIN_RESULT.toString())
            .productIds(List.of(productId1))
            .build());

        componentProductMappingService.batchInsert(company, ComponentProductMapping.builder()
            .componentId(pluginResultId2)
            .componentType(ComponentType.PLUGIN_RESULT.toString())
            .productIds(List.of(productId2))
            .build());

        var item1 = MsTmtVulnerability.builder()
            .category("bitcoin")
            .createdAt("4/3/2020 11:12:42 AM")
            .description("description")
            .extraData(Map.of())
            .ingestedAt(1611646854)
            .model("model 1")
            .name("name 1")
            .owner("owner@company.com")
            .pluginResultId(pluginResultId1)
            .priority("high")
            .projects(List.of())
            .state("open")
            .tags(List.of())
            .build();
        var vulnId1 = dbService.insert(company, item1);

        var item2 = MsTmtVulnerability.builder()
            .category("injection")
            .createdAt("4/3/2020 11:12:42 AM")
            .description("description")
            .extraData(Map.of())
            .ingestedAt(1614325254)
            .model("model 2")
            .name("name 2")
            .owner("owner@company.com")
            .pluginResultId(pluginResultId1)
            .priority("low")
            .projects(List.of())
            .state("open")
            .tags(List.of())
            .build();
        var vulnId2 = dbService.insert(company, item2);

        var item3 = MsTmtVulnerability.builder()
            .category("sql")
            .createdAt("4/3/2020 11:12:42 AM")
            .description("description")
            .extraData(Map.of())
            .ingestedAt(1616744454)
            .model("model 3")
            .name("name 3")
            .owner("owner@company.com")
            .pluginResultId(pluginResultId2)
            .priority("medium")
            .projects(List.of())
            .state("fixed")
            .tags(List.of())
            .build();
        var vulnId3 = dbService.insert(company, item3);

        var results = dbService.list(company, 0, 10);

        Assertions.assertThat(results.getRecords()).isNotEmpty();
        Assertions.assertThat(results.getRecords().size()).isEqualTo(3);

        var filters = QueryFilter.builder()
            .strictMatch("tag", List.of(tagIds1.get(0)))
            .build();
        results = dbService.filter(company, filters, 0, 10);
        Assertions.assertThat(results.getRecords()).isNotEmpty();
        Assertions.assertThat(results.getRecords().size()).isEqualTo(2);

        var filters2 = QueryFilter.builder()
            .strictMatch("tag", List.of(tagIds2.get(0)))
            // .strictMatch("", List.of(""))
            .build();
        results = dbService.filter(company, filters2, 0, 10);
        Assertions.assertThat(results.getRecords()).isNotEmpty();
        Assertions.assertThat(results.getRecords().size()).isEqualTo(1);

        var values = dbService.getValues(company, "priority", filters, 0, 10);
        Assertions.assertThat(values).isNotNull();
        Assertions.assertThat(values.getTotalCount()).isEqualTo(2);
        Assertions.assertThat(values.getRecords()).containsExactlyInAnyOrderElementsOf(List.of(Map.of("key", "low"),Map.of("key", "high")));

        values = dbService.getValues(company, "priority", filters2, 0, 10);
        Assertions.assertThat(values).isNotNull();
        Assertions.assertThat(values.getTotalCount()).isEqualTo(1);
        Assertions.assertThat(values.getRecords()).containsExactlyInAnyOrderElementsOf(List.of(Map.of("key", "medium")));

        values = dbService.getValues(company, "category", filters, 0, 10);
        Assertions.assertThat(values).isNotNull();

        values = dbService.getValues(company, "model", filters, 0, 10);
        Assertions.assertThat(values).isNotNull();
        Assertions.assertThat(values.getTotalCount()).isEqualTo(2);
        Assertions.assertThat(values.getRecords()).containsExactlyInAnyOrderElementsOf(List.of(Map.of("key", "model 1"), Map.of("key", "model 2")));

        values = dbService.getValues(company, "project", filters, 0, 10);
        Assertions.assertThat(values).isNotNull();
        Assertions.assertThat(values.getTotalCount()).isEqualTo(1);
        Assertions.assertThat(values.getRecords()).containsExactlyInAnyOrderElementsOf(List.of(Map.of("key", productId1, "value", "Product 1")));

        values = dbService.getValues(company, "tag", filters, 0, 10);
        Assertions.assertThat(values).isNotNull();
        Assertions.assertThat(values.getTotalCount()).isEqualTo(2);
        Assertions.assertThat(values.getRecords()).containsExactlyInAnyOrderElementsOf(List.of(Map.of("key", Integer.valueOf(tagIds1.get(0)), "value", "tag1"), Map.of("key", Integer.valueOf(tagIds1.get(1)), "value", "tag2")));

        var aggs = dbService.aggregate(company, "category", filters, 0, 10);
        Assertions.assertThat(aggs).isNotNull();

        aggs = dbService.aggregate(company, "priority", filters, 0, 10);
        Assertions.assertThat(aggs).isNotNull();

        aggs = dbService.aggregate(company, "project", filters, 0, 10);
        Assertions.assertThat(aggs).isNotNull();

        aggs = dbService.aggregate(company, "tag", filters, 0, 10);
        Assertions.assertThat(aggs).isNotNull();

        filters = QueryFilter.builder().build();
        aggs = dbService.aggregate(company, "category", filters, 0, 100);
        Assertions.assertThat(aggs).isNotNull();
        Assertions.assertThat(aggs.getTotalCount()).isEqualTo(3);

        filters = QueryFilter.builder()
                .strictMatch("ingested_at", Map.of("$gt", "1611646860", "$lt", "1616744460"))
                .build();
        aggs = dbService.aggregate(company, "category", filters, 0, 100);
        Assertions.assertThat(aggs).isNotNull();
        Assertions.assertThat(aggs.getTotalCount()).isEqualTo(2);

        // test last N Reports
        var pluginResult3 = DbPluginResult.builder()
                .createdAt(0L)
                .gcsPath("gcsPath")
                .metadata(Map.of())
                .pluginClass("pluginClass")
                .pluginName("pluginName")
                .pluginId(UUID.randomUUID())
                .productIds(List.of())
                .successful(true)
                .version("version")
                .labels(List.of())
                .build();
        var pluginResultId3 = UUID.fromString(pluginResultsService.insert(company, pluginResult3));
        System.out.println("pluginResultId3 = " + pluginResultId3);
        componentProductMappingService.batchInsert(company, ComponentProductMapping.builder()
                .componentId(pluginResultId3)
                .componentType(ComponentType.PLUGIN_RESULT.toString())
                .productIds(List.of(productId2))
                .build());
        tagitemService.batchInsert(company, List.of(
                TagItemMapping.builder()
                        .itemId(pluginResultId3.toString())
                        .tagId(tagIds2.get(0))
                        .tagItemType(TagItemType.PLUGIN_RESULT)
                        .build(),
                TagItemMapping.builder()
                        .itemId(pluginResultId3.toString())
                        .tagId(tagIds2.get(1))
                        .tagItemType(TagItemType.PLUGIN_RESULT)
                        .build()
        ));
        var item4 = MsTmtVulnerability.builder()
                .category("sql")
                .createdAt("4/3/2020 11:12:42 AM")
                .description("description")
                .extraData(Map.of())
                .ingestedAt(1616744999)
                .model("model 3")
                .name("name 3")
                .owner("owner@company.com")
                .pluginResultId(pluginResultId3)
                .priority("critical")
                .projects(List.of())
                .state("pending")
                .tags(List.of())
                .build();
        var vulnId4 = dbService.insert(company, item4);

        filters = QueryFilter.builder()
                .strictMatch("project", List.of(productId2))
                .strictMatch("tag", tagIds2)
                .build();
        aggs = dbService.aggregate(company, "category", filters, 0, 100);
        Assertions.assertThat(aggs).isNotNull();
        Assertions.assertThat(aggs.getTotalCount()).isEqualTo(1);
        Assertions.assertThat(aggs.getRecords()).containsExactlyInAnyOrderElementsOf(List.of(Map.of("key", "sql", "count", 2)));

        filters = QueryFilter.builder()
                .strictMatch("project", List.of(productId2))
                .strictMatch("tag", tagIds2)
                .strictMatch("n_last_reports", 1)
                .build();
        aggs = dbService.aggregate(company, "category", filters, 0, 100);
        Assertions.assertThat(aggs).isNotNull();
        Assertions.assertThat(aggs.getTotalCount()).isEqualTo(1);
        Assertions.assertThat(aggs.getRecords()).containsExactlyInAnyOrderElementsOf(List.of(Map.of("key", "sql", "count", 1)));
    }
}
