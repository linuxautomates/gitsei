package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.AggregationRecord;
import io.levelops.commons.databases.models.database.Product;
import io.levelops.commons.databases.models.database.User;
import io.levelops.commons.databases.models.database.access.RoleType;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.sql.DataSource;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class AggregationsDatabaseServiceTest {
    @Rule
    public SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    private DataSource dataSource;
    private UserService userService;
    private ProductService productService;
    private AggregationsDatabaseService dbService;
    private IntegrationService integrationService;
    private String company = "test";

    @Before
    public void setup() throws SQLException {
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        userService = new UserService(dataSource, DefaultObjectMapper.get());
        productService = new ProductService(dataSource);
        dbService = new AggregationsDatabaseService(dataSource);
        new DatabaseSchemaService(dataSource)
                .ensureSchemaExistence(company);
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

        integrationService = new IntegrationService(dataSource);
        integrationService.ensureTableExistence(company);
        productService.ensureTableExistence(company);
        dbService.ensureTableExistence(company);
    }

    private void verifyAggregationRecord(AggregationRecord a, AggregationRecord e){
        Assert.assertEquals(a.getId(), e.getId());
        Assert.assertEquals(a.getVersion(), e.getVersion());
        Assert.assertEquals(a.getSuccessful(), e.getSuccessful());
        Assert.assertEquals(a.getType(), e.getType());
        Assert.assertEquals(a.getToolType(), e.getToolType());
        Assert.assertEquals(a.getGcsPath(), e.getGcsPath());
        Assert.assertNotNull(a.getCreatedAt());
        Assert.assertNotNull(a.getUpdatedAt());
        Assert.assertEquals(CollectionUtils.isEmpty(a.getProductIds()),CollectionUtils.isEmpty(e.getProductIds()));
        if(CollectionUtils.isEmpty(e.getProductIds())){
            return;
        }
        Assert.assertEquals(a.getProductIds().stream().collect(Collectors.toSet()),e.getProductIds().stream().collect(Collectors.toSet()));
    }
    private void verifyAggregationRecords(List<AggregationRecord> a, List<AggregationRecord> e){
        Assert.assertEquals(CollectionUtils.isEmpty(a), CollectionUtils.isEmpty(e));
        if(CollectionUtils.isEmpty(a)){
            return;
        }
        Assert.assertEquals(a.size(), e.size());
        Map<String, AggregationRecord> actualMap = a.stream().collect(Collectors.toMap(AggregationRecord::getId, x -> x));
        Map<String, AggregationRecord> expectedMap = e.stream().collect(Collectors.toMap(AggregationRecord::getId, x -> x));

        for(String key : actualMap.keySet()){
            verifyAggregationRecord(actualMap.get(key), expectedMap.get(key));
        }
    }

    private List<AggregationRecord> testInsert(int i, List<Integer> productIds) throws SQLException {
        List<AggregationRecord> records = new ArrayList<>();
        for(int j=0; j<2; j++){
            AggregationRecord expected = AggregationRecord.builder()
                    .id(UUID.randomUUID().toString())
                    .version("v" + i + j)
                    .successful((i %2 == 0) ? true : false)
                    .type(AggregationRecord.Type.PLUGIN_AGGREGATION)
                    .toolType("jenkins_config"+i)
                    .gcsPath("gcspath" + i + +j)
                    .productIds(productIds)
                    .build();
            String id = dbService.insert(company, expected);
            Assert.assertNotNull(id);
            expected = expected.toBuilder().id(id).build();
            AggregationRecord actual = dbService.get(company, id).get();
            verifyAggregationRecord(actual, expected);
            records.add(expected);
        }
        return records;
    }

    private List<AggregationRecord> testInserts(List<Integer> productIds) throws SQLException {
        List<AggregationRecord> aggregationRecords = new ArrayList<>();
        for(int i=0; i<5; i++){
            List<AggregationRecord> a = testInsert(i, productIds.subList(0, i));
            aggregationRecords.addAll(a);
        }
        return aggregationRecords;
    }

    private List<Integer> trimProductIds (List<Integer> productIds) {
        if(productIds.size() <=1){
            return productIds;
        }
        return IntStream.range(0, productIds.size())
                .filter(i -> (i != 0))
                .mapToObj(i -> productIds.get(i))
                .collect(Collectors.toList());
    }
    private AggregationRecord testUpdate(AggregationRecord t) throws SQLException {
        List<Integer> productIds = trimProductIds(t.getProductIds());
        AggregationRecord updated = t.toBuilder()
                .version(t.getVersion() + "u")
                .successful(!t.getSuccessful())
                .productIds(productIds)
                .build();
        boolean successful = dbService.update(company, updated);
        Assert.assertTrue(successful);
        AggregationRecord actual = dbService.get(company, t.getId()).get();
        verifyAggregationRecord(actual, updated);
        return updated;
    }

    private List<AggregationRecord> testUpdates(List<AggregationRecord> expected) throws SQLException {
        List<AggregationRecord> updated = new ArrayList<>();
        for(AggregationRecord t : expected){
            updated.add(testUpdate(t));
        }
        return updated;
    }

    private void testList(List<AggregationRecord> expected) throws SQLException {
        DbListResponse<AggregationRecord> result = dbService.list(company, 0, 100);
        Assert.assertNotNull(result);
        Assert.assertEquals(expected.size(), result.getTotalCount().intValue());
        Assert.assertEquals(expected.size(), result.getCount().intValue());
        verifyAggregationRecords(result.getRecords(), expected);
    }

    private void testListByFilterType(List<AggregationRecord> expected) throws SQLException {
        DbListResponse<AggregationRecord> result = dbService.listByFilter(company, 0, 100, null, List.of(AggregationRecord.Type.PLUGIN_AGGREGATION), null);
        Assert.assertNotNull(result);
        Assert.assertEquals(expected.size(), result.getTotalCount().intValue());
        Assert.assertEquals(expected.size(), result.getCount().intValue());
        verifyAggregationRecords(result.getRecords(), expected);

        result = dbService.listByFilter(company, 0, 1, null, List.of(AggregationRecord.Type.PLUGIN_AGGREGATION), null);
        Assert.assertNotNull(result);
        Assert.assertEquals(expected.size(), result.getTotalCount().intValue());
        Assert.assertEquals(1, result.getCount().intValue());
        verifyAggregationRecord(result.getRecords().get(0), expected.get(expected.size()-1));

        result = dbService.listByFilter(company, 0, 100, null, List.of(AggregationRecord.Type.COMBINATION_AGGREGATION), null);
        Assert.assertNotNull(result);
        Assert.assertEquals(0, result.getTotalCount().intValue());
        Assert.assertEquals(0, result.getCount().intValue());
        verifyAggregationRecords(result.getRecords(), null);
    }

    private void testListByFilterToolType(List<AggregationRecord> allExpected) throws SQLException {
        Map<String, List<AggregationRecord>> map = allExpected.stream().collect(Collectors.groupingBy(AggregationRecord::getToolType));
        for(String toolType : map.keySet()){
            List<AggregationRecord> expected = map.get(toolType);
            DbListResponse<AggregationRecord> result = dbService.listByFilter(company, 0, 100, null, null, List.of(toolType));
            Assert.assertNotNull(result);
            Assert.assertEquals(expected.size(), result.getTotalCount().intValue());
            Assert.assertEquals(expected.size(), result.getCount().intValue());
            verifyAggregationRecords(result.getRecords(), expected);
        }

        Map<String, AggregationRecord> lastRecordByToolType = allExpected.stream().collect(Collectors.toMap(AggregationRecord::getToolType, x -> x, (o1,o2) -> o2));
        for(String currentToolType : lastRecordByToolType.keySet()){
            DbListResponse<AggregationRecord> result = dbService.listByFilter(company, 0, 1, null, null, List.of(currentToolType));
            Assert.assertNotNull(result);
            Assert.assertEquals(2, result.getTotalCount().intValue());
            Assert.assertEquals(1, result.getCount().intValue());
            verifyAggregationRecord(result.getRecords().get(0), lastRecordByToolType.get(currentToolType));
        }
    }

    private void testListByFilter(List<AggregationRecord> expected) throws SQLException {
        testListByFilterType(expected);
        testListByFilterToolType(expected);
    }

    private void testDelete(List<AggregationRecord> expected) throws SQLException {
        for(int i=0; i< expected.size(); i++){
            AggregationRecord current = expected.get(0);
            Boolean success = dbService.delete(company, current.getId());
            Assert.assertTrue(success);
            expected.remove(0);
            testList(expected);
        }
        testList(expected);
    }

    private Product createProduct(int i) throws SQLException {
        Product.ProductBuilder bldr = Product.builder()
                .name("name-" +i)
                .key("key-"+i)
                .description("desc-" +i)
                .ownerId("1");
        if(i==0){
            bldr.bootstrapped(true).immutable(true);
        }
        Product p = bldr.build();
        String productId = productService.insert(company, p);
        return p.toBuilder().id(productId).key(p.getKey().toUpperCase()).build();
    }
    private List<Product> createProducts() throws SQLException {
        List<Product> products = new ArrayList<>();
        for(int i=0; i<5; i++){
            products.add(createProduct(i));
        }
        return products;
    }

    @Test
    public void test() throws SQLException {
        List<Product> products = createProducts();
        List<Integer> productIds = products.stream().map(p -> Integer.parseInt(p.getId())).collect(Collectors.toList());
        List<AggregationRecord> expected = testInserts(productIds);
        testList(expected);
        testListByFilter(expected);
        expected = testUpdates(expected);
        testList(expected);
        testListByFilter(expected);
        testDelete(expected);
    }
}