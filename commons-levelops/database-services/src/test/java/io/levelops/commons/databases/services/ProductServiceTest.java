package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.Product;
import io.levelops.commons.databases.models.database.State;
import io.levelops.commons.databases.models.database.Tag;
import io.levelops.commons.databases.models.database.User;
import io.levelops.commons.databases.models.database.access.RoleType;
import io.levelops.commons.databases.models.database.mappings.TagItemMapping;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.collections4.CollectionUtils;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class ProductServiceTest {
    public static ObjectMapper MAPPER = DefaultObjectMapper.get();
    private static final String company = "test";

    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;
    private static ProductService productService;
    private static UserService userService;
    private static JdbcTemplate template;
    private static IntegrationService integrationService;
    private static CiCdInstancesDatabaseService ciCdInstancesDatabaseService;
    private static CiCdJobsDatabaseService ciCdJobsDatabaseService;
    private static CiCdJobRunsDatabaseService ciCdJobRunsDatabaseService;
    private static CiCdJobRunStageDatabaseService ciCdJobRunStageDatabaseService;
    private static StateDBService stateDBService;
    private static WorkItemDBService workItemDBService;
    private static TicketTemplateDBService ticketTemplateDBService;
    private static QuestionnaireTemplateDBService questionnaireTemplateDBService;
    private static BestPracticesService bestPracticesService;
    private static TagsService tagsService;
    private static TagItemDBService tagItemDBService;
    private static String integrationId;

    private static final Pattern PATTERN = Pattern.compile("^.*violates foreign key constraint.*on table \\\"(?<tblname>.*)\\\".*$", Pattern.MULTILINE);
    private List<Product> products;

    @BeforeClass
    public static void setup() throws SQLException {
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        template = new JdbcTemplate(dataSource);
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        userService = new UserService(dataSource, DefaultObjectMapper.get());
        productService = new ProductService(dataSource);
        stateDBService = new StateDBService(dataSource);
        integrationService = new IntegrationService(dataSource);
        ciCdInstancesDatabaseService = new CiCdInstancesDatabaseService(dataSource);
        ciCdJobsDatabaseService = new CiCdJobsDatabaseService(dataSource);
        ciCdJobRunsDatabaseService = new CiCdJobRunsDatabaseService(MAPPER, dataSource);
        ciCdJobRunStageDatabaseService = new CiCdJobRunStageDatabaseService(dataSource, MAPPER);
        bestPracticesService = new BestPracticesService(dataSource);
        questionnaireTemplateDBService = new QuestionnaireTemplateDBService(dataSource);
        ticketTemplateDBService = new TicketTemplateDBService(dataSource, MAPPER);
        workItemDBService = new WorkItemDBService(dataSource, MAPPER, productService, stateDBService, false, 1000);
        tagsService = new TagsService(dataSource);
        tagItemDBService = new TagItemDBService(dataSource);
        new DatabaseSchemaService(dataSource).ensureSchemaExistence(company);
        userService.ensureTableExistence(company);
        userService.insert(company, User.builder()
                .userType(RoleType.LIMITED_USER)
                .bcryptPassword("asd")
                .email("asd@asd.com")
                .passwordAuthEnabled(Boolean.FALSE)
                .samlAuthEnabled(false)
                .firstName("asd")
                .lastName("asd")
                .build());
        integrationService.ensureTableExistence(company);
        productService.ensureTableExistence(company);
        stateDBService.ensureTableExistence(company);
        ciCdInstancesDatabaseService.ensureTableExistence(company);
        ciCdJobsDatabaseService.ensureTableExistence(company);
        ciCdJobRunsDatabaseService.ensureTableExistence(company);
        ciCdJobRunStageDatabaseService.ensureTableExistence(company);
        bestPracticesService.ensureTableExistence(company);
        questionnaireTemplateDBService.ensureTableExistence(company);
        ticketTemplateDBService.ensureTableExistence(company);
        workItemDBService.ensureTableExistence(company);
        tagsService.ensureTableExistence(company);
        tagItemDBService.ensureTableExistence(company);

        template.execute("DELETE FROM " + company + ".products WHERE bootstrapped = false AND immutable = false");
        Integration integration = Integration.builder()
                .id("1")
                .name("name")
                .url("http")
                .status("good")
                .application("jira")
                .description("desc")
                .satellite(true)
                .build();
        integrationId = integrationService.insert(company, integration);
    }

    public Product testInsert(int i, Set<Integer> integrationIds) throws SQLException {
        Product.ProductBuilder bldr = Product.builder()
                .name("NAme-" + i)
                .key("key-" + i)
                .orgIdentifier("org-"+i)
                .description("desc-" + i)
                .ownerId("1")
                .integrationIds(integrationIds);
        if (i == 0) {
            bldr.bootstrapped(true).immutable(true);
        }
        Product p = bldr.build();
        String productId = productService.insert(company, p);
        return p.toBuilder().id(productId).key(p.getKey().toUpperCase()).build();
    }

    public Product testDemoInsert(int i, Set<Integer> integrationIds) throws SQLException {
        Product.ProductBuilder bldr = Product.builder()
                .name("DEMO-" + i)
                .key("DEMO-" + i)
                .description("desc-" + i)
                .ownerId("1")
                .demo(true)
                .integrationIds(integrationIds);
        if (i == 0) {
            bldr.bootstrapped(true).immutable(true);
        }
        Product p = bldr.build();
        String productId = productService.insert(company, p);
        return p.toBuilder().id(productId).key(p.getKey().toUpperCase()).build();
    }

    private List<Product> testInserts(int n) throws SQLException {
        var integration1 = Integration.builder()
                .name("ap1")
                .application("jira")
                .description("description")
                .status("ok")
                .url("url")
                .satellite(false)
                .build();

        var integration2 = Integration.builder()
                .name("ap2")
                .application("github")
                .description("description")
                .status("ok")
                .url("url")
                .satellite(false)
                .build();

        var integrationId1 = Integer.parseInt(integrationService.insert(company, integration1));
        var integrationId2 = Integer.parseInt(integrationService.insert(company, integration2));
        List<Product> products = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            products.add(testInsert(i, Set.of(integrationId1, integrationId2)));
        }
        return products;
    }

    private void testListByFilter(List<Product> products) throws SQLException {
        DbListResponse<Product> dbListResponse = productService.listByFilter(company, "ame", null, null, null, 0, 100);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(products.size(), dbListResponse.getRecords().size());
        verifyRecords(dbListResponse.getRecords(), products);

        dbListResponse = productService.listByFilter(company, "null", null, null, null, 0, 100);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(0, dbListResponse.getRecords().size());
        Assert.assertTrue(CollectionUtils.isEmpty(dbListResponse.getRecords()));
    }

    private void verifyRecord(Product a, Product e) {
        Assert.assertEquals(a.getId(), e.getId());
        Assert.assertEquals(a.getName(), e.getName());
        Assert.assertEquals(a.getDescription(), e.getDescription());
        Assert.assertEquals(a.getKey(), e.getKey());
        Assert.assertEquals(a.getOwnerId(), e.getOwnerId());
        Assert.assertEquals(Boolean.TRUE.equals(a.getBootstrapped()), Boolean.TRUE.equals(e.getBootstrapped()));
        Assert.assertEquals(Boolean.TRUE.equals(a.getImmutable()), Boolean.TRUE.equals(e.getImmutable()));
    }

    private void verifyRecords(List<Product> a, List<Product> e) {
        Assert.assertEquals(CollectionUtils.isEmpty(a), CollectionUtils.isEmpty(e));
        if (CollectionUtils.isEmpty(a)) {
            return;
        }
        Assert.assertEquals(a.size(), e.size());
        Map<String, Product> actualMap = a.stream().collect(Collectors.toMap(Product::getId, x -> x));
        Map<String, Product> expectedMap = e.stream().collect(Collectors.toMap(Product::getId, x -> x));

        for (String key : actualMap.keySet()) {
            verifyRecord(actualMap.get(key), expectedMap.get(key));
        }
    }

    private void testGetSystemImmutableProducts(List<Product> products) throws SQLException {
        List<Product> systemProducts = productService.getSystemImmutableProducts(company);
        Assert.assertNotNull(systemProducts);
        Assert.assertTrue(CollectionUtils.isNotEmpty(systemProducts));
        Assert.assertEquals(1, systemProducts.size()); // default + test
        verifyRecords(systemProducts.stream().filter(x -> !"Default Workspace".equals(x.getName())).collect(Collectors.toList()),
                products.stream().filter(x -> Boolean.TRUE.equals(x.getBootstrapped()) && Boolean.TRUE.equals(x.getImmutable())).collect(Collectors.toList()));
    }

    private void testDeleteProductInUse() throws SQLException {
        User user = UserUtils.createUser(userService, company, 0);
        Product product = ProductUtils.createProduct(productService, company, 0, user);
        State state = StateUtils.createOpenState(stateDBService, company);
        int n = 5;
        WorkItemTestUtils.createWorkItem(workItemDBService, company, n, product.getId(), state, user.getId());
        try {
            boolean success = productService.delete(company, product.getId());
            Assert.fail("Expected SQLException");
        } catch (SQLException e) {
            String msg = e.getMessage();
            Assert.assertTrue(msg.contains("violates foreign key constraint"));
            Matcher matcher = PATTERN.matcher(msg);
            Assert.assertTrue(matcher.find());
            String tblName = matcher.group("tblname");
            Assert.assertEquals("workitems", tblName);
        }
    }

    public void testBulkDelete() throws SQLException {
        String id1 = testInsert(6, Set.of()).getId();
        String id2 = testInsert(7, Set.of()).getId();
        productService.bulkDelete(company, List.of(id1, id2));
        assertThat(productService.get(company, id1)).isEmpty();
        assertThat(productService.get(company, id2)).isEmpty();
    }

    public void testUpdate(Product product) throws SQLException {
        Product productReq = product.toBuilder().name("test").build();
        productService.update(company, productReq);
        var product1 = productService.get(company, product.getId());
        Assertions.assertThat(product1.get().getName()).isEqualTo("test");
    }

    public void testListIntegrationsByFilter() throws SQLException {
        String integ1 = "integration-1.0";
        String integ2 = "integration-2.0";
        Integration integration1 = Integration.builder()
                .name(integ1)
                .url("http")
                .status("good")
                .application("jira")
                .description("desc")
                .satellite(true)
                .build();
        Integration integration2 = Integration.builder()
                .name(integ2)
                .url("http")
                .status("good")
                .application("github")
                .description("desc")
                .satellite(false)
                .build();
        var integrationId1 = integrationService.insert(company, integration1);
        var integrationId2 = integrationService.insert(company, integration2);
        String tagId1 = tagsService.insert(company, Tag.builder().name("database1").build());
        tagItemDBService.batchInsert(company, List.of(TagItemMapping.builder()
                .itemId(integrationId1)
                .tagId(tagId1)
                .tagItemType(TagItemMapping.TagItemType.INTEGRATION)
                .build()));
        String tagId2 = tagsService.insert(company, Tag.builder().name("database2").build());
        tagItemDBService.batchInsert(company, List.of(TagItemMapping.builder()
                .itemId(integrationId2)
                .tagId(tagId2)
                .tagItemType(TagItemMapping.TagItemType.INTEGRATION)
                .build()));

        Product product = testInsert(productService.list(company, 0, 1000).getCount() + 1,
                Set.of(Integer.parseInt(integrationId1), Integer.parseInt(integrationId2)));

        //test by name filter
        DbListResponse<Integration> integrations = productService.listIntegrationsByFilter(company,
                "integration-", false, null, null, null,
                Integer.parseInt(product.getId()), 0, 100);
        Assert.assertEquals(2, integrations.getRecords().size());
        Assert.assertEquals(integ1, integrations.getRecords().get(0).getName());
        Assert.assertNotEquals(integ1, integrations.getRecords().get(1).getName());
        //test by satellite filter
        integrations = productService.listIntegrationsByFilter(company, "integ", false, null,
                false, null, Integer.parseInt(product.getId()), 0, 100);
        Assert.assertEquals(integrations.getRecords().size(), 1);
        Assert.assertEquals(integ2, integrations.getRecords().get(0).getName());
        //test by applications filter
        integrations = productService.listIntegrationsByFilter(company, "integ", false,
                List.of("jira"), null, null, Integer.parseInt(product.getId()), 0, 100);
        Assert.assertEquals(integrations.getRecords().size(), 1);
        Assert.assertEquals(integ1, integrations.getRecords().get(0).getName());
        //test by tags filter
        integrations = productService.listIntegrationsByFilter(company, null, false,
                List.of("jira"), null, List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10),
                Integer.parseInt(product.getId()), 0, 10);
        Assert.assertEquals("integration-1.0", integrations.getRecords().get(0).getName());
        //test by filter (negative condition)
        integrations = productService.listIntegrationsByFilter(company, "not present", false,
                List.of("jira"), null, List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10),
                Integer.parseInt(product.getId()), 0, 10);
        Assert.assertEquals(0, integrations.getRecords().size());
    }

    @Test
    public void test() throws SQLException {
        products = testInserts(5);

        var product1 = productService.get(company, products.get(1).getId());
        Assertions.assertThat(product1).isNotEmpty();
        Assertions.assertThat(product1.get().getIntegrations()).isNotEmpty();

        testListByFilter(products);

        String name = null;
        var productIds = Set.<Integer>of();
        var integrationIds = Set.<Integer>of();
        var integrationType = Set.<String>of("jira");
        var category = Set.<String>of();
        var key = Set.<String>of();
        var ownerId = Set.<String>of();
        var orgIdentifiers = Set.<String>of();
        var results = productService.listByFilter(company, name, productIds, integrationIds, integrationType, category, key, orgIdentifiers, ownerId, false, false, null, null, null, null, 0, 10);
        Assertions.assertThat(results.getTotalCount()).isEqualTo(5);

        name = null;
        productIds = Set.<Integer>of(Integer.parseInt(product1.get().getId()));
        integrationIds = Set.<Integer>of();
        integrationType = Set.<String>of("jira");
        category = Set.<String>of();
        key = Set.<String>of();
        ownerId = Set.<String>of();
        orgIdentifiers = Set.<String>of();
        results = productService.listByFilter(company, name, productIds, integrationIds, integrationType, category, key, orgIdentifiers, ownerId, false, false, null, null, null, null, 0, 10);
        Assertions.assertThat(results.getTotalCount()).isEqualTo(1);

        name = null;
        productIds = Set.<Integer>of();
        integrationIds = Set.<Integer>of();
        integrationType = Set.<String>of("jira");
        category = Set.<String>of();
        key = Set.<String>of();
        ownerId = Set.<String>of();
        orgIdentifiers = Set.<String>of("org-1");
        results = productService.listByFilter(company, name, productIds, integrationIds, integrationType, category, key, orgIdentifiers, ownerId, false, false, null, null, null, null, 0, 10);
        Assertions.assertThat(results.getTotalCount()).isEqualTo(1);
        Assertions.assertThat(results.getRecords().get(0).getName()).isEqualTo("NAme-1");

        Product product = productService.getProduct(company, "org-0", "key-0").get();
        Assertions.assertThat(product).isNotNull();

        testGetSystemImmutableProducts(products);
        testDeleteProductInUse();
        testBulkDelete();
        testUpdate(product1.get());
        testListIntegrationsByFilter();
    }

    @Test
    public void testDisable() throws SQLException {
        for (int i = 10; i < 13; i++) {
            testInsert(i, Set.of(Integer.parseInt(integrationId)));
        }
        var productList = productService.list(company, 0, 100);
        var totalWorkspaceCount = productList.getCount();
        productList.getRecords().forEach(p -> assertThat(p.getDisabled()).isFalse());

        var modifiedP = productList.getRecords().get(2).toBuilder().disabled(true).build();
        productService.update(company, modifiedP);
        var p2 = productService.get(company, modifiedP.getId());
        assertThat(p2.get().getDisabled()).isTrue();

        // Disabled product should not be returned anymore
        productList = productService.list(company, 0, 100);
        assertThat(productList.getRecords().size()).isEqualTo(totalWorkspaceCount - 1);
        productList.getRecords().forEach(p -> assertThat(p.getId()).isNotEqualTo(p2.get().getId()));

        var disabledProductList = productService.listByFilter(
                company, null, null, null, null, null, null,  null,
                null, null, null, null, null, true, null,
                0, 100);
        assertThat(disabledProductList.getRecords().size()).isEqualTo(1);
        assertThat(disabledProductList.getRecords().get(0).getId()).isEqualTo(p2.get().getId());
    }

    @Test
    public void testDemo() throws SQLException {
        var productList = productService.list(company, 0, 100);
        productList.getRecords().forEach(p -> assertThat(p.getDemo()).isFalse());
        for (int i = 13; i < 14; i++) {
            testDemoInsert(i, Set.of(Integer.parseInt(integrationId)));
        }

        var demoProductList = productService.listByFilter(
                company, null, null, null, null, null, null, null,
                null, null, null, null, null, null, true,
                0, 100);
        assertThat(demoProductList.getRecords().size()).isEqualTo(1);
    }
}