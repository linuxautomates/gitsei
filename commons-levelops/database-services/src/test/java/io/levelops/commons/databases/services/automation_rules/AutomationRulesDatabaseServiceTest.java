package io.levelops.commons.databases.services.automation_rules;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.automation_rules.AutomationRule;
import io.levelops.commons.databases.models.database.automation_rules.ObjectType;
import io.levelops.commons.databases.services.DatabaseSchemaService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.collections4.CollectionUtils;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class AutomationRulesDatabaseServiceTest {
    public static ObjectMapper MAPPER = DefaultObjectMapper.get();

    @Rule
    public SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    private String company = "test";

    private DataSource dataSource;
    private static AutomationRulesDatabaseService automationRulesDatabaseService;

    @Before
    public void setup() throws SQLException {
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        automationRulesDatabaseService = new AutomationRulesDatabaseService(dataSource,MAPPER);
        
        new DatabaseSchemaService(dataSource)
                .ensureSchemaExistence(company);
        automationRulesDatabaseService.ensureTableExistence(company);
    }

    private void verifyRecord(AutomationRule a, AutomationRule e){
//        Assert.assertEquals(e,a);
        Assert.assertEquals(a.getId(), e.getId());
        Assert.assertEquals(a.getName(), e.getName());
        Assert.assertEquals(a.getDescription(), e.getDescription());
        Assert.assertEquals(a.getSource(), e.getSource());
        Assert.assertEquals(a.getOwner(), e.getOwner());
        Assert.assertEquals(a.getObjectType(), e.getObjectType());
        Assert.assertEquals(a.getCritereas(), e.getCritereas());
        Assert.assertNotNull(a.getCreatedAt());
        Assert.assertNotNull(a.getUpdatedAt());
    }
    private void verifyRecords(List<AutomationRule> a, List<AutomationRule> e){
        Assert.assertEquals(CollectionUtils.isEmpty(a), CollectionUtils.isEmpty(e));
        if(CollectionUtils.isEmpty(a)){
            return;
        }
        Assert.assertEquals(a.size(), e.size());
        Map<UUID, AutomationRule> actualMap = a.stream().collect(Collectors.toMap(AutomationRule::getId, x -> x));
        Map<UUID, AutomationRule> expectedMap = e.stream().collect(Collectors.toMap(AutomationRule::getId, x -> x));

        for(UUID key : actualMap.keySet()){
            verifyRecord(actualMap.get(key), expectedMap.get(key));
        }
    }

    private void testGet(AutomationRule automationRule) throws SQLException {
        Optional<AutomationRule> optional = automationRulesDatabaseService.get(company, automationRule.getId().toString());
        Assert.assertTrue(optional.isPresent());
        verifyRecord(optional.get(), automationRule);
    }

    private void testGets(List<AutomationRule> automationRules) throws SQLException {
        for(AutomationRule automationRule : automationRules) {
            testGet(automationRule);
        }
    }

    private List<AutomationRule> testInserts(int n) throws SQLException {
        List<AutomationRule> retVal = new ArrayList<>();
        for(int i=0; i< n; i++) {
            AutomationRule automationRule = AutomationRuleTestUtils.buildAutomationRule(company, i);
            String id = automationRulesDatabaseService.insert(company, automationRule);
            Assert.assertNotNull(id);
            automationRule = automationRule.toBuilder().id(UUID.fromString(id)).build();
            testGet(automationRule);
            retVal.add(automationRule);
        }
        return retVal;
    }

    private void testList(List<AutomationRule> expected) throws SQLException {
        DbListResponse<AutomationRule> dbListResponse = automationRulesDatabaseService.list(company, 0, 300);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(expected.size(), dbListResponse.getCount().intValue());
        Assert.assertEquals(expected.size(), dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(expected.size(), dbListResponse.getRecords().size());
        verifyRecords(dbListResponse.getRecords(), expected);
    }

    private void testListByFilterObjectTypes(List<AutomationRule> allExpected) throws SQLException {
        Map<ObjectType, List<AutomationRule>> map = allExpected.stream().collect(Collectors.groupingBy(AutomationRule::getObjectType));
        for (ObjectType objectType : map.keySet()) {
            List<AutomationRule> expected = map.get(objectType);
            DbListResponse<AutomationRule> result = automationRulesDatabaseService.listByFilter(company, 0, 100, null, List.of(objectType), null);
            Assert.assertNotNull(result);
            Assert.assertEquals(expected.size(), result.getTotalCount().intValue());
            Assert.assertEquals(expected.size(), result.getCount().intValue());
            verifyRecords(result.getRecords(), expected);
        }
        List<ObjectType> allObjectTypes = allExpected.stream().map(AutomationRule::getObjectType).distinct().collect(Collectors.toList());
        DbListResponse<AutomationRule> result = automationRulesDatabaseService.listByFilter(company, 0, 100, null, allObjectTypes, null);
        Assert.assertNotNull(result);
        Assert.assertEquals(allExpected.size(), result.getTotalCount().intValue());
        Assert.assertEquals(allExpected.size(), result.getCount().intValue());
        verifyRecords(result.getRecords(), allExpected);
    }
    private void testListByFilterNamePartial(List<AutomationRule> allExpected) throws SQLException {
        DbListResponse<AutomationRule> result = automationRulesDatabaseService.listByFilter(company, 0, 100, null, null, "AmE");
        Assert.assertNotNull(result);
        Assert.assertEquals(allExpected.size(), result.getTotalCount().intValue());
        Assert.assertEquals(allExpected.size(), result.getCount().intValue());
        verifyRecords(result.getRecords(), allExpected);

        result = automationRulesDatabaseService.listByFilter(company, 0, 100, null, null, "wrong");
        Assert.assertNotNull(result);
        Assert.assertEquals(0, result.getTotalCount().intValue());
        Assert.assertEquals(0, result.getCount().intValue());
        Assert.assertTrue(CollectionUtils.isEmpty(result.getRecords()));
    }

    private void testListByFilter(List<AutomationRule> expected) throws SQLException {
        testListByFilterObjectTypes(expected);
        testListByFilterNamePartial(expected);
    }

    private void testAllLists(List<AutomationRule> expected) throws SQLException {
        testList(expected);
        testListByFilter(expected);
    }

    private List<AutomationRule> testUpdate(List<AutomationRule> expected) throws SQLException {
        List<AutomationRule> updated = new ArrayList<>();
        for(int i=0; i< expected.size(); i++) {
            AutomationRule current = expected.get(i);
            current = current.toBuilder()
                    .name("name - " + i + i)
                    .description("description - " + i + i)
                    .source("source - " + i + i)
                    .owner("owner - " + i + i)
                    .objectType(ObjectType.SCM_PULL_REQUEST).build();
            Boolean success = automationRulesDatabaseService.update(company, current);
            Assert.assertTrue(success);
            testGet(current);
            updated.add(current);
        }
        return updated;
    }

    private void testValidDelete(List<AutomationRule> expected) throws SQLException {
        for(int i=0; i< expected.size(); i++){
            AutomationRule current = expected.get(0);
            Boolean success = automationRulesDatabaseService.delete(company, current.getId().toString());
            Assert.assertTrue(success);
            expected.remove(0);
            testAllLists(expected);
        }
        testAllLists(expected);
    }

    private void testInvalidDelete() throws SQLException {
        Boolean success = automationRulesDatabaseService.delete(company, UUID.randomUUID().toString());
        Assert.assertFalse(success);
    }

    private void testDelete(List<AutomationRule> expected) throws SQLException {
        testInvalidDelete();
        testValidDelete(expected);
    }

    @Test
    public void test() throws SQLException {
        int n = 5;
        List<AutomationRule> expected = testInserts(n);
        testGets(expected);
        testAllLists(expected);

        expected = testUpdate(expected);
        testGets(expected);
        testAllLists(expected);

        testDelete(expected);
    }

    @Test
    public void testBulkDelete() throws SQLException {
        List<String> ids = testInserts(3).stream().map(AutomationRule::getId).collect(Collectors.toList())
                .stream().map(UUID::toString).collect(Collectors.toList());
        automationRulesDatabaseService.bulkDelete(company, ids);
        for (String id : ids) {
            Assertions.assertThat(automationRulesDatabaseService.get(company, id)).isEmpty();
        }
    }
}