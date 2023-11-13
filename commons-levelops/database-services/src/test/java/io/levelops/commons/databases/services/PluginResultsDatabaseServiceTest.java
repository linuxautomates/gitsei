package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.Plugin;
import io.levelops.commons.databases.models.database.Plugin.PluginClass;
import io.levelops.commons.databases.models.database.Product;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.plugins.DbPluginResult;
import io.levelops.commons.databases.models.database.plugins.DbPluginResultLabel;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

public class PluginResultsDatabaseServiceTest {

    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    private static NamedParameterJdbcTemplate template;

    private static PluginResultsDatabaseService resultsDatabaseService;

    private static PluginDatabaseService pluginDatabaseService;

    private static ComponentProductMappingService componentProductMappingService;

    private static ProductService productsService;

    private static UserService userService;

    private static IntegrationService integrationService;

    private static final String company = "test";

    private static String productId10;
    private static String productId2020;

    @BeforeClass
    public static void setUp() throws Exception {
        DataSource dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        template = new NamedParameterJdbcTemplate(dataSource);
        componentProductMappingService = new ComponentProductMappingService(DefaultObjectMapper.get(), dataSource);
        pluginDatabaseService = new PluginDatabaseService(dataSource, DefaultObjectMapper.get());
        resultsDatabaseService = new PluginResultsDatabaseService(dataSource, DefaultObjectMapper.get(), componentProductMappingService);
        userService = new UserService(dataSource, DefaultObjectMapper.get());
        productsService = new ProductService(dataSource);
        List.of(
                "CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";",
                "CREATE SCHEMA IF NOT EXISTS " + company
        ).forEach(template.getJdbcTemplate()::execute);
        System.out.println(template.getJdbcTemplate().queryForObject("SELECT current_database();", String.class));
        userService.ensureTableExistence(company);
        integrationService = new IntegrationService(dataSource);
        integrationService.ensureTableExistence(company);
        productsService.ensureTableExistence(company);
        pluginDatabaseService.ensureTableExistence(company);
        resultsDatabaseService.ensureTableExistence(company);
        pluginDatabaseService.insert(company, PLUGIN_1);
        pluginDatabaseService.insert(company, PLUGIN_2);
        pluginDatabaseService.insert(company, PLUGIN_3);
        pluginDatabaseService.insert(company, PLUGIN_4);
        productId10 = productsService.insert(company, Product.builder().name("P10").description("P10").key("P10").build());
        productId2020 = productsService.insert(company, Product.builder().name("P2020").description("P2020").key("P2020").build());
        RESULT_WITH_1_LABEL_2_VALUES = getResultWith1Labels2Values();
        RESULT_WITH_3_LABELS_A = getResultWith3LabelsA();
        RESULT_WITH_3_LABELS_B = getResultWith3LabelsB();
        RESULT_WITH_NO_LABEL = getResultWithNoLabels();
    }

    @Before
    public void cleanup() {
        template.getJdbcTemplate().execute("DELETE FROM " + company + "." + PluginResultsDatabaseService.RESULTS_TABLE);
    }

    private void setTs(String id, String ts) {
        int updateR = resultsDatabaseService.template.getJdbcTemplate().update("update test.plugin_results set created_at = '" + ts + "'::BIGINT where id = '" + id + "'::uuid");
        int updateLabels = resultsDatabaseService.template.getJdbcTemplate().update("update test.plugin_result_labels set created_at = '" + ts + "'::BIGINT where result_id = '" + id + "'::uuid");
        System.out.println("Set ts of " + id + " to " + ts + "; matched rows: " + updateR + " + " + updateLabels);
    }

    @Test
    public void testInsert() throws SQLException {
        componentProductMappingService.ensureTableExistence("test");
        resultsDatabaseService.ensureTableExistence("test");
        pluginDatabaseService.ensureTableExistence("test");

        Optional<DbPluginResult> test = resultsDatabaseService.get("test", UUID.randomUUID().toString());
        assertThat(test.isEmpty()).isTrue();
        
        String id1 = resultsDatabaseService.insert("test", RESULT_WITH_3_LABELS_A);
        String id2 = resultsDatabaseService.insert("test", RESULT_WITH_1_LABEL_2_VALUES);
        String id3 = resultsDatabaseService.insert("test", RESULT_WITH_NO_LABEL);
        setTs(id1, "100");
        setTs(id2, "200");
        setTs(id3, "300");
        System.out.println(id1);
        System.out.println(id2);
        System.out.println(id3);

        DbPluginResult get1 = resultsDatabaseService.get("test", id1).orElse(null);
        DbPluginResult get2 = resultsDatabaseService.get("test", id2).orElse(null);
        DbPluginResult get3 = resultsDatabaseService.get("test", id3).orElse(null);
        DefaultObjectMapper.prettyPrint(get1);
        DefaultObjectMapper.prettyPrint(get2);
        DefaultObjectMapper.prettyPrint(get3);
        assertThat(get1).isNotNull();
        assertThat(get1.getLabels()).hasSize(3);
        assertThat(get1.getId()).isEqualTo("2f4fa3e3-5f13-43f1-a366-4bb21c3bf78c");
        assertThat(get1.getPluginName()).isEqualTo("My Tool");
        assertThat(get1.getTool()).isEqualTo("mytool");
        assertThat(get1.getVersion()).isEqualTo("1.0");
        assertThat(get1.getSuccessful()).isEqualTo(true);
        assertThat(get1.getMetadata().get("elapsedms")).isEqualTo(10);
        assertThat(get1.getGcsPath()).isEqualTo("bucket/path/asdasd.txt");
        assertThat(get2).isNotNull();
        assertThat(get2.getLabels()).hasSize(2);
        assertThat(get3).isNotNull();
        assertThat(get3.getLabels()).hasSize(0);

        DbListResponse<DbPluginResult> list = resultsDatabaseService.list("test", 0, 25);
        DefaultObjectMapper.prettyPrint(list);
        assertThat(list.getRecords()).hasSize(3);
        assertThat(list.getRecords().get(0).getId()).isEqualTo(id3);
        assertThat(list.getRecords().get(0).getLabels()).hasSize(0);
        assertThat(list.getRecords().get(1).getId()).isEqualTo(id2);
        assertThat(list.getRecords().get(1).getLabels()).hasSize(2);
        assertThat(list.getRecords().get(2).getId()).isEqualTo(id1);
        assertThat(list.getRecords().get(2).getLabels()).hasSize(3);

        assertThat(resultsDatabaseService.delete("test", id1)).isTrue();
        assertThat(resultsDatabaseService.get("test", id1)).isEmpty();
    }

    private List<String> setupResultsForListTests() throws SQLException {
        componentProductMappingService.ensureTableExistence("test");
        resultsDatabaseService.ensureTableExistence("test");
        pluginDatabaseService.ensureTableExistence("test");

        // try{
        //     productsService.insert(company, Product.builder().name("P10").description("P10").build());
        // }
        // catch(DuplicateKeyException e){
        //     log.error("PRODUCTS!!!", e);
        // }
        // try{ 
        //     productsService.insert(company, Product.builder().name("P2020").description("P2020").build());
        // }
        // catch(DuplicateKeyException e){
        //     // ignore
        // } 
        try{
            pluginDatabaseService.insert("test", PLUGIN_1);
        }
        catch(DuplicateKeyException e){
            // ignore
        } 
        try{
            pluginDatabaseService.insert("test", PLUGIN_2);
        }
        catch(DuplicateKeyException e){
            // ignore
        } 
        try{
            pluginDatabaseService.insert("test", PLUGIN_3);
        }
        catch(DuplicateKeyException e){
            // ignore
        } 
        try{
            pluginDatabaseService.insert("test", PLUGIN_4);
        }
        catch(DuplicateKeyException e){
            // ignore
        } 
        // ComponentProductMapping mappingsA = ComponentProductMapping.builder().componentId(RESULT_WITH_3_LABELS_A.getPluginId()).componentType("plugin").productIds(RESULT_WITH_3_LABELS_A.getProductIds()).build();
        // componentProductMappingService.insert("test", mappingsA);
        // ComponentProductMapping mappingsB = ComponentProductMapping.builder().componentId(RESULT_WITH_3_LABELS_B.getPluginId()).componentType("plugin").productIds(RESULT_WITH_3_LABELS_B.getProductIds()).build();
        // componentProductMappingService.insert("test", mappingsB);
        String idA = resultsDatabaseService.insert("test", RESULT_WITH_3_LABELS_A);
        String idB = resultsDatabaseService.insert("test", RESULT_WITH_3_LABELS_B);

        // set the timestamps to enforce the order: A older than B
        setTs(idA, "100");
        setTs(idB, "200");
        return List.of(idA, idB);
    }

    @Test
    public void testList() throws SQLException {
        List<String> ids = setupResultsForListTests();
        String idA = ids.get(0);
        String idB = ids.get(1);

        List<DbPluginResult> results = resultsDatabaseService.list("test", 0, 10).getRecords();
        assertThat(results).hasSize(2);

        assertThat(results).hasSize(2);
        assertThat(results.get(0).getId()).isEqualTo(idB);
        assertThat(results.get(1).getId()).isEqualTo(idA);
    }

    @Test
    public void testFilter() throws SQLException {
        List<String> ids = setupResultsForListTests();
        String idA = ids.get(0);
        String idB = ids.get(1);

        DbListResponse<DbPluginResult> response;

        // test tool filter
        response = resultsDatabaseService.filter("test", null, Set.of(UUID.fromString("4f4fa3e3-5f13-43f1-a366-4bb21c3bf78c")), null, null, null, null, null, 0, 10, null);
        assertThat(response.getRecords().stream().map(DbPluginResult::getId)).containsExactly(idB);
        assertThat(response.getTotalCount()).isEqualTo(1);

        response = resultsDatabaseService.filter("test", null, Set.of(UUID.fromString("1f4fa3e3-5f13-43f1-a366-4bb21c3bf78c")), null, null, null, null, null, 0, 10, null);
        assertThat(response.getRecords().stream().map(DbPluginResult::getId)).containsExactly(idA);
        assertThat(response.getTotalCount()).isEqualTo(1);

        response = resultsDatabaseService.filter("test", null, Set.of(UUID.randomUUID()), null, null, null, null, null, 0, 10, null);
        assertThat(response.getRecords().stream().map(DbPluginResult::getId)).isEmpty();
        assertThat(response.getTotalCount()).isEqualTo(0);

        response = resultsDatabaseService.filter("test", null, null, null, null, null, null, null, 0, 10, null);
        assertThat(response.getRecords().stream().map(DbPluginResult::getId)).containsExactly(idB, idA);
        assertThat(response.getTotalCount()).isEqualTo(2);

        response = resultsDatabaseService.filter("test", null, null, null, null, null, null, null, 10, 1, null);
        assertThat(response.getRecords().stream().map(DbPluginResult::getId)).isEmpty();
        assertThat(response.getTotalCount()).isEqualTo(2);

        // test sort

        response = resultsDatabaseService.filter("test", null, null, null, null, null, null, null, 0, 10, null);
        assertThat(response.getRecords().stream().map(DbPluginResult::getId)).containsExactly(idB, idA);
        assertThat(response.getTotalCount()).isEqualTo(2);

        response = resultsDatabaseService.filter("test", null, null, null, null, null, null, null, 0, 10, Map.of("created_at", SortingOrder.DESC));
        assertThat(response.getRecords().stream().map(DbPluginResult::getId)).containsExactly(idB, idA);
        assertThat(response.getTotalCount()).isEqualTo(2);

        response = resultsDatabaseService.filter("test", null, null, null, null, null, null, null, 0, 10, Map.of("created_at", SortingOrder.ASC));
        assertThat(response.getRecords().stream().map(DbPluginResult::getId)).containsExactly(idA, idB);
        assertThat(response.getTotalCount()).isEqualTo(2);

        // test version filter

        response = resultsDatabaseService.filter("test", null, null, Set.of("2.0"), null, null, null, null, 0, 10, null);
        assertThat(response.getRecords().stream().map(DbPluginResult::getId)).containsExactly(idB);
        assertThat(response.getTotalCount()).isEqualTo(1);

        response = resultsDatabaseService.filter("test", null, null, Set.of("1.0"), null, null, null, null, 0, 10, null);
        assertThat(response.getRecords().stream().map(DbPluginResult::getId)).containsExactly(idA);
        assertThat(response.getTotalCount()).isEqualTo(1);

        response = resultsDatabaseService.filter("test", null, null, Set.of("-"), null, null, null, null, 0, 10, null);
        assertThat(response.getRecords().stream().map(DbPluginResult::getId)).isEmpty();
        assertThat(response.getTotalCount()).isEqualTo(0);

        // test products filter

        response = resultsDatabaseService.filter("test", null, null, null, Set.of(productId2020, "0"), null, null, null, 0, 10, null);
        assertThat(response.getRecords().stream().map(DbPluginResult::getId)).containsExactly(idB);
        assertThat(response.getTotalCount()).isEqualTo(1);

        response = resultsDatabaseService.filter("test", null, null, null, Set.of(productId2020), null, null, null, 0, 10, null);
        assertThat(response.getRecords().stream().map(DbPluginResult::getId)).containsExactly(idB);
        assertThat(response.getTotalCount()).isEqualTo(1);

        response = resultsDatabaseService.filter("test", null, null, null, Set.of("0"), null, null, null, 0, 10, null);
        assertThat(response.getRecords().stream().map(DbPluginResult::getId)).isEmpty();
        assertThat(response.getTotalCount()).isEqualTo(0);

        // test successful filter

        response = resultsDatabaseService.filter("test", null, null, null, null, true, null, null, 0, 10, null);
        assertThat(response.getRecords().stream().map(DbPluginResult::getId)).containsExactly(idA);
        assertThat(response.getTotalCount()).isEqualTo(1);

        response = resultsDatabaseService.filter("test", null, null, null, null, false, null, null, 0, 10, null);
        assertThat(response.getRecords().stream().map(DbPluginResult::getId)).containsExactly(idB);
        assertThat(response.getTotalCount()).isEqualTo(1);

        response = resultsDatabaseService.filter("test", null, null, null, null, null, null, null, 0, 10, null);
        assertThat(response.getRecords().stream().map(DbPluginResult::getId)).containsExactly(idB, idA);
        assertThat(response.getTotalCount()).isEqualTo(2);

        // combination

        response = resultsDatabaseService.filter("test", null, Set.of(RESULT_WITH_3_LABELS_B.getPluginId()), Set.of("2.0"), Set.of(productId2020), false, null, null, 0, 10, null);
        assertThat(response.getRecords().stream().map(DbPluginResult::getId)).containsExactly(idB);
        assertThat(response.getTotalCount()).isEqualTo(1);

        response = resultsDatabaseService.filter("test", null, Set.of(UUID.randomUUID()), Set.of("2.0"), Set.of(productId2020), false, null, null, 0, 10, null);
        assertThat(response.getRecords().stream().map(DbPluginResult::getId)).isEmpty();
        assertThat(response.getTotalCount()).isEqualTo(0);

        response = resultsDatabaseService.filter("test", null, Set.of(UUID.fromString("2f4fa3e3-5f13-43f1-a366-4bb21c3bf78c")), Set.of("no"), Set.of(productId2020), false, null, null, 0, 10, null);
        assertThat(response.getRecords().stream().map(DbPluginResult::getId)).isEmpty();
        assertThat(response.getTotalCount()).isEqualTo(0);

        // test from to

        response = resultsDatabaseService.filter("test", null, null, null, null, null, DateUtils.fromEpochSecondToDate(100L), DateUtils.fromEpochSecondToDate(200L), 0, 10, null);
        assertThat(response.getRecords().stream().map(DbPluginResult::getId)).containsExactly(idB, idA);
        assertThat(response.getTotalCount()).isEqualTo(2);

        response = resultsDatabaseService.filter("test", null, null, null, null, null, DateUtils.fromEpochSecondToDate(150L), DateUtils.fromEpochSecondToDate(200L), 0, 10, null);
        assertThat(response.getRecords().stream().map(DbPluginResult::getId)).containsExactly(idB);
        assertThat(response.getTotalCount()).isEqualTo(1);

        response = resultsDatabaseService.filter("test", null, null, null, null, null, DateUtils.fromEpochSecondToDate(100L), DateUtils.fromEpochSecondToDate(150L), 0, 10, null);
        assertThat(response.getRecords().stream().map(DbPluginResult::getId)).containsExactly(idA);
        assertThat(response.getTotalCount()).isEqualTo(1);

        response = resultsDatabaseService.filter("test", Set.of(UUID.fromString("2f4fa3e3-5f13-43f1-a366-4bb21c3bf78c")), null, null, null, null, DateUtils.fromEpochSecondToDate(100L), DateUtils.fromEpochSecondToDate(150L), 0, 10, null);
        assertThat(response.getRecords().stream().map(DbPluginResult::getId)).containsExactly(idA);
        assertThat(response.getTotalCount()).isEqualTo(1);

        response = resultsDatabaseService.filter("test", Set.of(UUID.randomUUID()), null, null, null, null, DateUtils.fromEpochSecondToDate(100L), DateUtils.fromEpochSecondToDate(150L), 0, 10, null);
        assertThat(response.getRecords()).isEmpty();
        assertThat(response.getTotalCount()).isEqualTo(0);
    }

    @Test
    public void testFilterResultIdsByLabels() throws SQLException {
        componentProductMappingService.ensureTableExistence("test");
        List<String> ids = setupResultsForListTests();
        String idA = ids.get(0);
        String idB = ids.get(1);

        DbPluginResultLabel notFound = DbPluginResultLabel.builder().key("not").value("found").build();
        DbPluginResultLabel envProdLabel = DbPluginResultLabel.builder().key("env").value("prod").build();

        DbListResponse<String> response;
        response = resultsDatabaseService.filterResultIdsByLabels("test", null, null, null, null, null, null, null, null, 0, 10, null);
        assertThat(response.getRecords()).containsExactly(idB, idA);
        assertThat(response.getTotalCount()).isEqualTo(2);

        response = resultsDatabaseService.filterResultIdsByLabels("test", null, null, null, null, null, null, null, null, 2, 1, null);
        assertThat(response.getRecords()).isEmpty();
        assertThat(response.getTotalCount()).isEqualTo(2);

        response = resultsDatabaseService.filterResultIdsByLabels("test", null, null, null, null, null, List.of(), null, null, 0, 10, null);
        assertThat(response.getRecords()).containsExactly(idB, idA);
        assertThat(response.getTotalCount()).isEqualTo(2);

        response = resultsDatabaseService.filterResultIdsByLabels("test", null, null, null, null, null, List.of(notFound), null, null, 0, 10, null);
        assertThat(response.getRecords()).isEmpty();
        assertThat(response.getTotalCount()).isEqualTo(0);

        response = resultsDatabaseService.filterResultIdsByLabels("test", null, null, null, null, null, List.of(envProdLabel, notFound), null, null, 0, 10, null);
        assertThat(response.getRecords()).isEmpty();
        assertThat(response.getTotalCount()).isEqualTo(0);


        List<DbPluginResultLabel> filterLabels = FILTER_BY_LABELS_ENV_PROD_AND_STAGE_OPS;

        // test tool filter

        response = resultsDatabaseService.filterResultIdsByLabels("test", null, Set.of(RESULT_WITH_3_LABELS_B.getPluginId()), null, null, null, filterLabels, null, null, 0, 10, null);
        assertThat(response.getRecords()).containsExactly(idB);
        assertThat(response.getTotalCount()).isEqualTo(1);

        response = resultsDatabaseService.filterResultIdsByLabels("test", null, Set.of(RESULT_WITH_3_LABELS_A.getPluginId()), null, null, null, filterLabels, null, null, 0, 10, null);
        assertThat(response.getRecords()).containsExactly(idA);
        assertThat(response.getTotalCount()).isEqualTo(1);

        response = resultsDatabaseService.filterResultIdsByLabels("test", null, Set.of(UUID.randomUUID()), null, null, null, filterLabels, null, null, 0, 10, null);
        assertThat(response.getRecords()).isEmpty();
        assertThat(response.getTotalCount()).isEqualTo(0);

        response = resultsDatabaseService.filterResultIdsByLabels("test", null, null, null, null, null, filterLabels, null, null, 0, 10, null);
        assertThat(response.getRecords()).containsExactly(idB, idA);
        assertThat(response.getTotalCount()).isEqualTo(2);

        // test sort

        response = resultsDatabaseService.filterResultIdsByLabels("test", null, null, null, null, null, filterLabels, null, null, 0, 10, null);
        assertThat(response.getRecords()).containsExactly(idB, idA);
        assertThat(response.getTotalCount()).isEqualTo(2);

        response = resultsDatabaseService.filterResultIdsByLabels("test", null, null, null, null, null, filterLabels, null, null, 0, 10, Map.of("created_at", SortingOrder.DESC));
        assertThat(response.getRecords()).containsExactly(idB, idA);
        assertThat(response.getTotalCount()).isEqualTo(2);

        response = resultsDatabaseService.filterResultIdsByLabels("test", null, null, null, null, null, filterLabels, null, null, 0, 10,Map.of("created_at", SortingOrder.ASC));
        assertThat(response.getRecords()).containsExactly(idA, idB);
        assertThat(response.getTotalCount()).isEqualTo(2);


        // test version filter

        response = resultsDatabaseService.filterResultIdsByLabels("test", null, null, Set.of("2.0"), null, null, filterLabels, null, null, 0, 10, null);
        assertThat(response.getRecords()).containsExactly(idB);
        assertThat(response.getTotalCount()).isEqualTo(1);

        response = resultsDatabaseService.filterResultIdsByLabels("test", null, null, Set.of("1.0"), null, null, filterLabels, null, null, 0, 10, null);
        assertThat(response.getRecords()).containsExactly(idA);
        assertThat(response.getTotalCount()).isEqualTo(1);

        response = resultsDatabaseService.filterResultIdsByLabels("test", null, null, Set.of("-"), null, null, filterLabels, null, null, 0, 10, null);
        assertThat(response.getRecords()).isEmpty();
        assertThat(response.getTotalCount()).isEqualTo(0);

        // test products filter

        response = resultsDatabaseService.filterResultIdsByLabels("test", null, null, null, Set.of(productId2020, "0"), null, filterLabels, null, null, 0, 10, null);
        assertThat(response.getRecords()).containsExactly(idB);
        assertThat(response.getTotalCount()).isEqualTo(1);

        response = resultsDatabaseService.filterResultIdsByLabels("test", null, null, null, Set.of(productId2020), null, filterLabels, null, null, 0, 10, null);
        assertThat(response.getRecords()).containsExactly(idB);
        assertThat(response.getTotalCount()).isEqualTo(1);

        response = resultsDatabaseService.filterResultIdsByLabels("test", null, null, null, Set.of("0"), null, filterLabels, null, null, 0, 10, null);
        assertThat(response.getRecords()).isEmpty();
        assertThat(response.getTotalCount()).isEqualTo(0);

        // test successful filter

        response = resultsDatabaseService.filterResultIdsByLabels("test", null, null, null, null, true, filterLabels, null, null, 0, 10, null);
        assertThat(response.getRecords()).containsExactly(idA);
        assertThat(response.getTotalCount()).isEqualTo(1);

        response = resultsDatabaseService.filterResultIdsByLabels("test", null, null, null, null, false, filterLabels, null, null, 0, 10, null);
        assertThat(response.getRecords()).containsExactly(idB);
        assertThat(response.getTotalCount()).isEqualTo(1);

        response = resultsDatabaseService.filterResultIdsByLabels("test", null, null, null, null, null, filterLabels, null, null, 0, 10, null);
        assertThat(response.getRecords()).containsExactly(idB, idA);
        assertThat(response.getTotalCount()).isEqualTo(2);

        // combination

        response = resultsDatabaseService.filterResultIdsByLabels("test", null, Set.of(RESULT_WITH_3_LABELS_B.getPluginId()), Set.of("2.0"), Set.of(productId2020), false, filterLabels, null, null, 0, 10, null);
        assertThat(response.getRecords()).containsExactly(idB);
        assertThat(response.getTotalCount()).isEqualTo(1);

        response = resultsDatabaseService.filterResultIdsByLabels("test", Set.of(UUID.randomUUID()), Set.of(RESULT_WITH_3_LABELS_B.getPluginId()), Set.of("2.0"), Set.of(productId2020), false, filterLabels, null, null, 0, 10, null);
        assertThat(response.getRecords()).isEmpty();
        assertThat(response.getTotalCount()).isEqualTo(0);

        response = resultsDatabaseService.filterResultIdsByLabels("test", null, Set.of(UUID.randomUUID()), Set.of("2.0"), Set.of(productId2020), false, filterLabels, null, null, 0, 10, null);
        assertThat(response.getRecords()).isEmpty();
        assertThat(response.getTotalCount()).isEqualTo(0);

        response = resultsDatabaseService.filterResultIdsByLabels("test", null, Set.of(UUID.fromString("2f4fa3e3-5f13-43f1-a366-4bb21c3bf78c")), Set.of("no"), Set.of(productId2020), false, filterLabels, null, null, 0, 10, null);
        assertThat(response.getRecords()).isEmpty();
        assertThat(response.getTotalCount()).isEqualTo(0);

        response = resultsDatabaseService.filterResultIdsByLabels("test", Set.of(UUID.randomUUID()), Set.of(UUID.fromString("2f4fa3e3-5f13-43f1-a366-4bb21c3bf78c")), Set.of("no"), Set.of(productId2020), false, filterLabels, null, null, 0, 10, null);
        assertThat(response.getRecords()).isEmpty();
        assertThat(response.getTotalCount()).isEqualTo(0);

    }

    @Test
    public void testFilterByLabels() throws SQLException {
        componentProductMappingService.ensureTableExistence("test");
        List<String> ids = setupResultsForListTests();
        String idA = ids.get(0);
        String idB = ids.get(1);

        List<DbPluginResult> results = resultsDatabaseService.filterByLabels("test", null, null, null, null, null, null, null, null, 0, 10, null).getRecords();
        assertThat(results.stream().map(DbPluginResult::getId)).containsExactly(idB, idA);

        List<DbPluginResultLabel> filterLabels = FILTER_BY_LABELS_ENV_PROD_AND_STAGE_OPS;

        results = resultsDatabaseService.filterByLabels("test", null, null, null, null, null, filterLabels, null, null, 0, 10, null).getRecords();
        assertThat(results).hasSize(2);
        assertThat(results.get(0).getId()).isEqualTo(idB);
        assertThat(results.get(1).getId()).isEqualTo(idA);

        results = resultsDatabaseService.filterByLabels("test", null, null, null, null, null, filterLabels, null, null, 0, 1, null).getRecords();
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getId()).isEqualTo(idB);
        results = resultsDatabaseService.filterByLabels("test", null, Set.of(UUID.randomUUID()), null, null, null, filterLabels, null, null, 0, 1, null).getRecords();
        assertThat(results).isEmpty();
        results = resultsDatabaseService.filterByLabels("test", null, Set.of(RESULT_WITH_3_LABELS_B.getPluginId(), UUID.randomUUID()), null, null, null, filterLabels, null, null, 0, 1, null).getRecords();
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getId()).isEqualTo(idB);

        results = resultsDatabaseService.filterByLabels("test", null, null, null, null, null, filterLabels, null, null, 1, 1, null).getRecords();
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getId()).isEqualTo(idA);
    }


    @Test
    public void testSearchLabelKeys() throws SQLException {
        componentProductMappingService.ensureTableExistence("test");
        resultsDatabaseService.ensureTableExistence("test");
        String id1 = resultsDatabaseService.insert("test", RESULT_WITH_3_LABELS_A);
        String id2 = resultsDatabaseService.insert("test", RESULT_WITH_1_LABEL_2_VALUES);
        String id3 = resultsDatabaseService.insert("test", RESULT_WITH_NO_LABEL);
        setTs(id1, "100");
        setTs(id2, "200");
        setTs(id3, "300");
        List<String> keys;
        keys = resultsDatabaseService.searchLabelKeys("test", "not found", 0, 10);
        assertThat(keys).isEmpty();
        keys = resultsDatabaseService.searchLabelKeys("test", "en", 0, 10);
        assertThat(keys).containsExactly("env");
        keys = resultsDatabaseService.searchLabelKeys("test", "rele", 0, 10);
        assertThat(keys).containsExactly("release");
        keys = resultsDatabaseService.searchLabelKeys("test", "", 0, 10);
        assertThat(keys).containsExactly("env", "release", "stage");
        keys = resultsDatabaseService.searchLabelKeys("test", "ease", 0, 10);
        assertThat(keys).isEmpty();
        keys = resultsDatabaseService.searchLabelKeys("test", "", 0, 2);
        assertThat(keys).containsExactly("env", "release");
        keys = resultsDatabaseService.searchLabelKeys("test", null, 0, 2);
        assertThat(keys).containsExactly("env", "release");
        keys = resultsDatabaseService.searchLabelKeys("test", "", 1, 2);
        assertThat(keys).containsExactly("stage");
    }

    @Test
    public void testSearchLabelValues() throws SQLException {
        componentProductMappingService.ensureTableExistence("test");
        resultsDatabaseService.ensureTableExistence("test");
        String id1 = resultsDatabaseService.insert("test", RESULT_WITH_3_LABELS_A);
        String id2 = resultsDatabaseService.insert("test", RESULT_WITH_1_LABEL_2_VALUES);
        String id3 = resultsDatabaseService.insert("test", RESULT_WITH_NO_LABEL);
        setTs(id1, "100");
        setTs(id2, "200");
        setTs(id3, "300");
        List<String> keys;
        keys = resultsDatabaseService.searchLabelValues("test", null, null, 0, 1);
        assertThat(keys).isEmpty();
        keys = resultsDatabaseService.searchLabelValues("test", "not found", "prod", 0, 10);
        assertThat(keys).isEmpty();
        keys = resultsDatabaseService.searchLabelValues("test", "env", "not found", 0, 10);
        assertThat(keys).isEmpty();
        keys = resultsDatabaseService.searchLabelValues("test", "env", "pro", 0, 10);
        assertThat(keys).containsExactly("prod");
        keys = resultsDatabaseService.searchLabelValues("test", "env", "us1", 0, 10);
        assertThat(keys).containsExactly("us1");
        keys = resultsDatabaseService.searchLabelValues("test", "env", "", 0, 10);
        assertThat(keys).containsExactly("prod", "us1");
        keys = resultsDatabaseService.searchLabelValues("test", "env", "od", 0, 10);
        assertThat(keys).isEmpty();
        keys = resultsDatabaseService.searchLabelValues("test", "env", "", 0, 1);
        assertThat(keys).containsExactly("prod");
        keys = resultsDatabaseService.searchLabelValues("test", "env", null, 0, 1);
        assertThat(keys).containsExactly("prod");
        keys = resultsDatabaseService.searchLabelValues("test", "env", "", 1, 1);
        assertThat(keys).containsExactly("us1");

        keys = resultsDatabaseService.searchLabelValues("test", "env", "pRo", 0, 10);
        assertThat(keys).containsExactly("prod");
    }

    @Test
    public void testDistinctLabelKeys() throws SQLException {
        componentProductMappingService.ensureTableExistence("test");
        String id1 = resultsDatabaseService.insert(company, RESULT_WITH_3_LABELS_A);
        String id2 = resultsDatabaseService.insert(company, RESULT_WITH_1_LABEL_2_VALUES);
        String id3 = resultsDatabaseService.insert(company, RESULT_WITH_NO_LABEL);
        setTs(id1, "100");
        setTs(id2, "200");
        setTs(id3, "300");
        List<String> keys;
        keys = resultsDatabaseService.distinctLabelKeys(company, 0, 10);
        assertThat(keys).containsExactly("env", "release", "stage");
        keys = resultsDatabaseService.distinctLabelKeys(company, 0, 2);
        assertThat(keys).containsExactly("env", "release");
        keys = resultsDatabaseService.distinctLabelKeys(company, 1, 2);
        assertThat(keys).containsExactly("stage");
    }

    @Test
    public void testDistinctLabelValues() throws SQLException {
        componentProductMappingService.ensureTableExistence("test");
        resultsDatabaseService.ensureTableExistence("test");
        String id1 = resultsDatabaseService.insert("test", RESULT_WITH_3_LABELS_A);
        String id2 = resultsDatabaseService.insert("test", RESULT_WITH_1_LABEL_2_VALUES);
        String id3 = resultsDatabaseService.insert("test", RESULT_WITH_NO_LABEL);
        setTs(id1, "100");
        setTs(id2, "200");
        setTs(id3, "300");
        List<String> values;
        values = resultsDatabaseService.distinctLabelValues("test", "env", 0, 10);
        assertThat(values).containsExactly("prod", "us1");
        values = resultsDatabaseService.distinctLabelValues("test", "env", 0, 1);
        assertThat(values).containsExactly("prod");
        values = resultsDatabaseService.distinctLabelValues("test", "env", 1, 1);
        assertThat(values).containsExactly("us1");

        values = resultsDatabaseService.distinctLabelValues("test", "release", 0, 10);
        assertThat(values).containsExactly("r42");

        values = resultsDatabaseService.distinctLabelValues("test", "stage", 0, 10);
        assertThat(values).containsExactly("ops");
    }

    @Test
    public void testBulkPluginReportDelete() throws SQLException {
        componentProductMappingService.ensureTableExistence("test");
        resultsDatabaseService.ensureTableExistence("test");
        String id1 = resultsDatabaseService.insert("test", RESULT_WITH_3_LABELS_A);
        String id2 = resultsDatabaseService.insert("test", RESULT_WITH_1_LABEL_2_VALUES);
        String id3 = resultsDatabaseService.insert("test", RESULT_WITH_NO_LABEL);
        setTs(id1, "100");
        setTs(id2, "200");
        setTs(id3, "300");
        resultsDatabaseService.deleteBulkPluginResult("test", List.of(id1, id2, id3));
        assertThat(resultsDatabaseService.get(company, id1)).isEmpty();
        assertThat(resultsDatabaseService.get(company, id2)).isEmpty();
        assertThat(resultsDatabaseService.get(company, id2)).isEmpty();
    }

    private static Plugin PLUGIN_1 = Plugin.builder()
            .id("1f4fa3e3-5f13-43f1-a366-4bb21c3bf78c")
            .pluginClass(PluginClass.REPORT_FILE)
            .custom(false)
            .tool("mytool")
            .version("1.0")
            .description("")
            .gcsPath("bucket/path/asdasd.txt")
            .name("My Tool")
            .readme(Map.of())
            .build();

    private static Plugin PLUGIN_2 = PLUGIN_1.toBuilder().id("2f4fa3e3-5f13-43f1-a366-4bb21c3bf78c").tool("mytool2").build();
    private static Plugin PLUGIN_3 = PLUGIN_1.toBuilder().id("3f4fa3e3-5f13-43f1-a366-4bb21c3bf78c").tool("mytool3").build();
    private static Plugin PLUGIN_4 = PLUGIN_1.toBuilder().id("4f4fa3e3-5f13-43f1-a366-4bb21c3bf78c").tool("mytool4").build();

    private static DbPluginResult RESULT_WITH_3_LABELS_A;
    private static DbPluginResult getResultWith3LabelsA() {
    return DbPluginResult.builder()
            .id("2f4fa3e3-5f13-43f1-a366-4bb21c3bf78c")
            .pluginId(UUID.fromString("1f4fa3e3-5f13-43f1-a366-4bb21c3bf78c"))
            .tool("mytool")
            .version("1.0")
            .productIds(List.of(Integer.valueOf(productId10)))
            .successful(true)
            .metadata(Map.of("elapsedms", 10))
            .gcsPath("bucket/path/asdasd.txt")
            .labels(List.of(DbPluginResultLabel.builder()
                            .key("env")
                            .value("prod")
                            .build(),
                    DbPluginResultLabel.builder()
                            .key("stage")
                            .value("ops")
                            .build(),
                    DbPluginResultLabel.builder()
                            .key("release")
                            .value("r42")
                            .build()))
            .build();
    }

    private static DbPluginResult RESULT_WITH_1_LABEL_2_VALUES;
    private static DbPluginResult getResultWith1Labels2Values() {
        return DbPluginResult.builder()
            .tool("mytool")
            .pluginId(UUID.fromString("2f4fa3e3-5f13-43f1-a366-4bb21c3bf78c"))
            .version("1.0")
            .productIds(List.of(Integer.valueOf(productId10)))
            .successful(true)
            .metadata(Map.of("elapsedms", 10))
            .gcsPath("bucket/path/asdasd.txt")
            .labels(List.of(DbPluginResultLabel.builder()
                            .key("env")
                            .value("prod")
                            .build(),
                    DbPluginResultLabel.builder()
                            .key("env")
                            .value("us1")
                            .build()))
            .build();
    }

    private static DbPluginResult RESULT_WITH_NO_LABEL;
    private static DbPluginResult getResultWithNoLabels() {
        return DbPluginResult.builder()
            .tool("mytool")
            .pluginId(UUID.fromString("3f4fa3e3-5f13-43f1-a366-4bb21c3bf78c"))
            .version("1.0")
            .productIds(List.of(Integer.valueOf(productId10)))
            .successful(true)
            .metadata(Map.of("elapsedms", 10))
            .gcsPath("bucket/path/asdasd.txt")
            .build();
    }

    private static DbPluginResult RESULT_WITH_3_LABELS_B;
    private static DbPluginResult getResultWith3LabelsB() {
        return DbPluginResult.builder()
            .id("30cca48a-97fd-47ef-aacf-bf279ae02d69")
            .pluginId(UUID.fromString("4f4fa3e3-5f13-43f1-a366-4bb21c3bf78c"))
            .tool("mytoolB")
            .version("2.0")
            .productIds(List.of(Integer.valueOf(productId2020)))
            .successful(false)
            .metadata(Map.of("elapsedms", 10))
            .gcsPath("bucket/path/asdasdasd.txt")
            .labels(List.of(DbPluginResultLabel.builder()
                            .key("env")
                            .value("prod")
                            .build(),
                    DbPluginResultLabel.builder()
                            .key("stage")
                            .value("ops")
                            .build(),
                    DbPluginResultLabel.builder()
                            .key("release")
                            .value("r42")
                            .build()))
            .build();
    }

    private List<DbPluginResultLabel> FILTER_BY_LABELS_ENV_PROD_AND_STAGE_OPS = List.of(DbPluginResultLabel.builder()
                    .key("env")
                    .value("prod")
                    .build(),
            DbPluginResultLabel.builder()
                    .key("stage")
                    .value("ops")
                    .build());
}