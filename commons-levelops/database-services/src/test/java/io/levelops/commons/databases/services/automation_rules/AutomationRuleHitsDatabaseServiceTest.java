package io.levelops.commons.databases.services.automation_rules;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.automation_rules.AutomationRule;
import io.levelops.commons.databases.models.database.automation_rules.AutomationRuleHit;
import io.levelops.commons.databases.models.database.automation_rules.ObjectType;
import io.levelops.commons.databases.services.DatabaseSchemaService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
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

public class AutomationRuleHitsDatabaseServiceTest  {
    public static ObjectMapper MAPPER = DefaultObjectMapper.get();

    @Rule
    public SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    private String company = "test";

    private DataSource dataSource;
    private static AutomationRulesDatabaseService automationRulesDatabaseService;
    private static AutomationRuleHitsDatabaseService automationRuleHitsDatabaseService;

    @Before
    public void setup() throws SQLException {
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        automationRulesDatabaseService = new AutomationRulesDatabaseService(dataSource,MAPPER);
        automationRuleHitsDatabaseService = new AutomationRuleHitsDatabaseService(dataSource, MAPPER);

        new DatabaseSchemaService(dataSource)
                .ensureSchemaExistence(company);
        automationRulesDatabaseService.ensureTableExistence(company);
        automationRuleHitsDatabaseService.ensureTableExistence(company);
    }

    private void verifyRecord(AutomationRuleHit a, AutomationRuleHit e){
        Assert.assertEquals(a.getId(), e.getId());
        Assert.assertEquals(a.getObjectId(), e.getObjectId());
        Assert.assertEquals(a.getObjectType(), e.getObjectType());
        Assert.assertEquals(a.getRuleId(), e.getRuleId());
        Assert.assertEquals(a.getRuleName(), e.getRuleName());
        Assert.assertEquals(a.getCount(), e.getCount());
        Assert.assertEquals(a.getHitContent(), e.getHitContent());
        Assert.assertEquals(a.getContext(), e.getContext());
        Assert.assertNotNull(a.getCreatedAt());
        Assert.assertNotNull(a.getUpdatedAt());
    }
    private void verifyRecords(List<AutomationRuleHit> a, List<AutomationRuleHit> e){
        Assert.assertEquals(CollectionUtils.isEmpty(a), CollectionUtils.isEmpty(e));
        if(CollectionUtils.isEmpty(a)){
            return;
        }
        Assert.assertEquals(a.size(), e.size());
        Map<UUID, AutomationRuleHit> actualMap = a.stream().collect(Collectors.toMap(AutomationRuleHit::getId, x -> x));
        Map<UUID, AutomationRuleHit> expectedMap = e.stream().collect(Collectors.toMap(AutomationRuleHit::getId, x -> x));

        for(UUID key : actualMap.keySet()){
            verifyRecord(actualMap.get(key), expectedMap.get(key));
        }
    }

    private void testGet(AutomationRuleHit automationRuleHit) throws SQLException {
        Optional<AutomationRuleHit> optional = automationRuleHitsDatabaseService.get(company, automationRuleHit.getId().toString());
        Assert.assertTrue(optional.isPresent());
        verifyRecord(optional.get(), automationRuleHit);
    }

    private void testGets(List<AutomationRuleHit> automationRules) throws SQLException {
        for(AutomationRuleHit automationRule : automationRules) {
            testGet(automationRule);
        }
    }

    private List<AutomationRuleHit> testInserts(List<AutomationRule> rules) throws SQLException {
        List<AutomationRuleHit> retVal = new ArrayList<>();
        for(int i=0; i< rules.size(); i++) {
            AutomationRule automationRule = rules.get(i);
            AutomationRuleHit automationRuleHit = AutomationRuleHitTestUtils.buildAutomationRule(company, i, automationRule);
            String id = automationRuleHitsDatabaseService.insert(company, automationRuleHit);
            Assert.assertNotNull(id);
            automationRuleHit = automationRuleHit.toBuilder().id(UUID.fromString(id)).ruleName(automationRule.getName()).build();
            testGet(automationRuleHit);
            retVal.add(automationRuleHit);
        }
        return retVal;
    }

    private void testList(List<AutomationRuleHit> expected) throws SQLException {
        DbListResponse<AutomationRuleHit> dbListResponse = automationRuleHitsDatabaseService.list(company, 0, 300);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(expected.size(), dbListResponse.getCount().intValue());
        Assert.assertEquals(expected.size(), dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(expected.size(), dbListResponse.getRecords().size());
        verifyRecords(dbListResponse.getRecords(), expected);
    }

    private void testAllLists(List<AutomationRuleHit> expected) throws SQLException {
        testList(expected);
    }

    private void testListByFilterObjectTypes(List<AutomationRuleHit> allExpected) throws SQLException {
        Map<ObjectType, List<AutomationRuleHit>> map = allExpected.stream().collect(Collectors.groupingBy(AutomationRuleHit::getObjectType));
        for (ObjectType objectType : map.keySet()) {
            List<AutomationRuleHit> expected = map.get(objectType);
            DbListResponse<AutomationRuleHit> result = automationRuleHitsDatabaseService.listByFilter(company, 0, 100, null,
                    List.of(objectType), null, null, null);
            Assert.assertNotNull(result);
            Assert.assertEquals(expected.size(), result.getTotalCount().intValue());
            Assert.assertEquals(expected.size(), result.getCount().intValue());
            verifyRecords(result.getRecords(), expected);
        }
        List<ObjectType> allObjectTypes = allExpected.stream().map(AutomationRuleHit::getObjectType).distinct().collect(Collectors.toList());
        DbListResponse<AutomationRuleHit> result = automationRuleHitsDatabaseService.listByFilter(company, 0, 100, null,
                allObjectTypes, null, null, null);
        Assert.assertNotNull(result);
        Assert.assertEquals(allExpected.size(), result.getTotalCount().intValue());
        Assert.assertEquals(allExpected.size(), result.getCount().intValue());
        verifyRecords(result.getRecords(), allExpected);
    }
    private void testListByFilterObjectIds(List<AutomationRuleHit> allExpected) throws SQLException {
        Map<String, List<AutomationRuleHit>> map = allExpected.stream().collect(Collectors.groupingBy(AutomationRuleHit::getObjectId));
        for (String objectId : map.keySet()) {
            List<AutomationRuleHit> expected = map.get(objectId);
            DbListResponse<AutomationRuleHit> result = automationRuleHitsDatabaseService.listByFilter(company, 0, 100, null,
                    null, List.of(objectId), null, null);
            Assert.assertNotNull(result);
            Assert.assertEquals(expected.size(), result.getTotalCount().intValue());
            Assert.assertEquals(expected.size(), result.getCount().intValue());
            verifyRecords(result.getRecords(), expected);
        }
        List<String> allObjectIds = allExpected.stream().map(AutomationRuleHit::getObjectId).distinct().collect(Collectors.toList());
        DbListResponse<AutomationRuleHit> result = automationRuleHitsDatabaseService.listByFilter(company, 0, 100, null,
                null, allObjectIds,null, null);
        Assert.assertNotNull(result);
        Assert.assertEquals(allExpected.size(), result.getTotalCount().intValue());
        Assert.assertEquals(allExpected.size(), result.getCount().intValue());
        verifyRecords(result.getRecords(), allExpected);
    }

    private void testListByFilterObjectTypeObjectIdPairs(List<AutomationRuleHit> allExpected) throws SQLException {
        Map<ImmutablePair<ObjectType, String>, List<AutomationRuleHit>> map = allExpected.stream().collect(Collectors.groupingBy(x -> ImmutablePair.of(x.getObjectType(), x.getObjectId())));
        for (ImmutablePair<ObjectType, String> pair : map.keySet()) {
            List<AutomationRuleHit> expected = map.get(pair);
            DbListResponse<AutomationRuleHit> result = automationRuleHitsDatabaseService.listByFilter(company, 0, 100, null,
                    null, null, List.of(pair), null);
            Assert.assertNotNull(result);
            Assert.assertEquals(expected.size(), result.getTotalCount().intValue());
            Assert.assertEquals(expected.size(), result.getCount().intValue());
            verifyRecords(result.getRecords(), expected);
        }
        List<ImmutablePair<ObjectType, String>> allObjectTypeObjectIdPairs = allExpected.stream().map(x -> ImmutablePair.of(x.getObjectType(), x.getObjectId())).collect(Collectors.toList());
        DbListResponse<AutomationRuleHit> result = automationRuleHitsDatabaseService.listByFilter(company, 0, 100, null,
                null, null, allObjectTypeObjectIdPairs, null);
        Assert.assertNotNull(result);
        Assert.assertEquals(allExpected.size(), result.getTotalCount().intValue());
        Assert.assertEquals(allExpected.size(), result.getCount().intValue());
        verifyRecords(result.getRecords(), allExpected);
    }

    private void testListByFilterRuleIds(List<AutomationRuleHit> allExpected) throws SQLException {
        Map<UUID, List<AutomationRuleHit>> map = allExpected.stream().collect(Collectors.groupingBy(AutomationRuleHit::getRuleId));
        for (UUID ruleId : map.keySet()) {
            List<AutomationRuleHit> expected = map.get(ruleId);
            DbListResponse<AutomationRuleHit> result = automationRuleHitsDatabaseService.listByFilter(company, 0, 100, null,
                    null, null, null, List.of(ruleId));
            Assert.assertNotNull(result);
            Assert.assertEquals(expected.size(), result.getTotalCount().intValue());
            Assert.assertEquals(expected.size(), result.getCount().intValue());
            verifyRecords(result.getRecords(), expected);
        }
        List<UUID> allRuleIds = allExpected.stream().map(AutomationRuleHit::getRuleId).distinct().collect(Collectors.toList());
        DbListResponse<AutomationRuleHit> result = automationRuleHitsDatabaseService.listByFilter(company, 0, 100, null,
                null, null, null, allRuleIds);
        Assert.assertNotNull(result);
        Assert.assertEquals(allExpected.size(), result.getTotalCount().intValue());
        Assert.assertEquals(allExpected.size(), result.getCount().intValue());
        verifyRecords(result.getRecords(), allExpected);
    }

    private void testListByFilter(List<AutomationRuleHit> expected) throws SQLException {
        testListByFilterObjectTypes(expected);
        testListByFilterObjectIds(expected);
        testListByFilterObjectTypeObjectIdPairs(expected);
        testListByFilterRuleIds(expected);
    }

    private void testUpsert(List<AutomationRuleHit> expected) throws SQLException {
        for(int i=0; i< expected.size(); i++) {
            AutomationRuleHit current = expected.get(i);
            String id = automationRuleHitsDatabaseService.upsert(company, current);
            Assert.assertNotNull(id);
            Assert.assertEquals(current.getId().toString(), id);
            testGet(current);
        }
    }

    private List<AutomationRuleHit> testUpdate(List<AutomationRuleHit> expected) throws SQLException {
        List<AutomationRuleHit> updated = new ArrayList<>();
        for(int i=0; i< expected.size(); i++) {
            AutomationRuleHit current = expected.get(i);
            current = current.toBuilder()
                    .objectId("object-id" + i + i)
                    .objectType(ObjectType.SCM_PULL_REQUEST)
                    .count(1 + i + i)
                    .hitContent("content-" + i + i)
                    .context(Map.of("criterea_1", String.valueOf(i + i)))
                    .build();
            Boolean success = automationRuleHitsDatabaseService.update(company, current);
            Assert.assertTrue(success);
            testGet(current);
            updated.add(current);
        }
        return updated;
    }

    private void testValidDelete(List<AutomationRuleHit> expected) throws SQLException {
        for(int i=0; i< expected.size(); i++){
            AutomationRuleHit current = expected.get(0);
            Boolean success = automationRuleHitsDatabaseService.delete(company, current.getId().toString());
            Assert.assertTrue(success);
            expected.remove(0);
            testAllLists(expected);
        }
        testAllLists(expected);
    }

    private void testInvalidDelete() throws SQLException {
        Boolean success = automationRuleHitsDatabaseService.delete(company, UUID.randomUUID().toString());
        Assert.assertFalse(success);
    }

    private void testDelete(List<AutomationRuleHit> expected) throws SQLException {
        testInvalidDelete();
        testValidDelete(expected);
    }

    @Test
    public void test() throws SQLException {
        int n = 5;
        List<AutomationRule> rules = AutomationRuleTestUtils.createAutomationRules(automationRulesDatabaseService, company, n);
        List<AutomationRuleHit> expected = testInserts(rules);
        testGets(expected);
        testAllLists(expected);
        testListByFilter(expected);

        testUpsert(expected);
        testGets(expected);
        testAllLists(expected);
        testListByFilter(expected);

        expected = testUpdate(expected);
        testGets(expected);
        testAllLists(expected);
        testListByFilter(expected);

        testDelete(expected);
    }


}