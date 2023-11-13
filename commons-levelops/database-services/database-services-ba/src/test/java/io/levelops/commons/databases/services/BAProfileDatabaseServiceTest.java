package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.CICDJobConfigChange;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.report_models.ba.BAProfile;
import io.levelops.web.exceptions.NotFoundException;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class BAProfileDatabaseServiceTest {
    private static final Boolean DEFAULT_PROFILE = true;
    private static final Boolean NON_DEFAULT_PROFILE = false;
    private static final ObjectMapper mapper = DefaultObjectMapper.get();
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;
    String company = "test";
    JdbcTemplate template;
    private static BAProfileDatabaseService baProfileDatabaseService;

    @Before
    public void setUp() throws Exception {
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        template = new JdbcTemplate(dataSource);
        List.of(
                "CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";",
                "DROP SCHEMA IF EXISTS test CASCADE; ",
                "CREATE SCHEMA test; "
        ).forEach(template::execute);
        System.out.println(template.queryForObject("SELECT current_database();", String.class));

        baProfileDatabaseService = new BAProfileDatabaseService(dataSource, mapper);
        baProfileDatabaseService.ensureTableExistence(company);
    }

    //region Verify
    private void verifyRecord(BAProfile e, BAProfile a) {
        Assert.assertEquals(e.getId(), a.getId());
        Assert.assertEquals(e.getName(), a.getName());
        Assert.assertEquals(e.getDescription(), a.getDescription());
        Assert.assertEquals(e.getDefaultProfile(), a.getDefaultProfile());
        Assert.assertEquals(e.getCategories(), a.getCategories());
        Assert.assertNotNull(a.getCreatedAt());
        Assert.assertNotNull(a.getUpdatedAt());
    }
    private void verifyRecords(List<BAProfile> e, List<BAProfile> a) {
        Assert.assertEquals(CollectionUtils.isEmpty(e), CollectionUtils.isEmpty(a));
        if (CollectionUtils.isEmpty(e)) {
            return;
        }
        Assert.assertEquals(e.size(), a.size());
        Map<UUID, BAProfile> expectedMap = e.stream().collect(Collectors.toMap(BAProfile::getId, x -> x));
        Map<UUID, BAProfile> actualMap = a.stream().collect(Collectors.toMap(BAProfile::getId, x -> x));

        for (UUID key : expectedMap.keySet()) {
            verifyRecord(expectedMap.get(key), actualMap.get(key));
        }
    }
    //endregion

    //region Test ListByFilters
    private void testListByFiltersDefaultProfile(List<BAProfile> allExpected) throws SQLException {
        Map<Boolean, List<BAProfile>> map = allExpected.stream().collect(Collectors.groupingBy(BAProfile::getDefaultProfile));
        for (Boolean key : map.keySet()) {
            List<BAProfile> expected = map.get(key);
            DbListResponse<BAProfile> result = baProfileDatabaseService.listByFilter(company, 0, 100, null, null, key);
            Assert.assertNotNull(result);
            Assert.assertEquals(expected.size(), result.getTotalCount().intValue());
            Assert.assertEquals(expected.size(), result.getCount().intValue());
            verifyRecords(expected, result.getRecords());
        }
        DbListResponse<BAProfile> result = baProfileDatabaseService.listByFilter(company, 0, 100, null, null, null);
        Assert.assertNotNull(result);
        Assert.assertEquals(allExpected.size(), result.getTotalCount().intValue());
        Assert.assertEquals(allExpected.size(), result.getCount().intValue());
        verifyRecords(allExpected, result.getRecords());
    }

    private void testListByFiltersNames(List<BAProfile> allExpected) throws SQLException {
        Map<String, List<BAProfile>> map = allExpected.stream().collect(Collectors.groupingBy(BAProfile::getName));
        for (String name : map.keySet()) {
            List<BAProfile> expected = map.get(name);
            DbListResponse<BAProfile> result = baProfileDatabaseService.listByFilter(company, 0, 100, null, List.of(name), null);
            Assert.assertNotNull(result);
            Assert.assertEquals(expected.size(), result.getTotalCount().intValue());
            Assert.assertEquals(expected.size(), result.getCount().intValue());
            verifyRecords(expected, result.getRecords());
        }
        List<UUID> allIds = allExpected.stream().map(BAProfile::getId).distinct().collect(Collectors.toList());
        DbListResponse<BAProfile> result = baProfileDatabaseService.listByFilter(company, 0, 100, allIds, null, null);
        Assert.assertNotNull(result);
        Assert.assertEquals(allExpected.size(), result.getTotalCount().intValue());
        Assert.assertEquals(allExpected.size(), result.getCount().intValue());
        verifyRecords(allExpected, result.getRecords());

        result = baProfileDatabaseService.listByFilter(company, 0, 100, List.of(UUID.randomUUID()), null, null);
        Assert.assertNotNull(result);
        Assert.assertEquals(0, result.getTotalCount().intValue());
        Assert.assertTrue(CollectionUtils.isEmpty(result.getRecords()));
    }
    private void testListByFiltersIds(List<BAProfile> allExpected) throws SQLException {
        Map<UUID, List<BAProfile>> map = allExpected.stream().collect(Collectors.groupingBy(BAProfile::getId));
        for (UUID id : map.keySet()) {
            List<BAProfile> expected = map.get(id);
            DbListResponse<BAProfile> result = baProfileDatabaseService.listByFilter(company, 0, 100, List.of(id), null, null);
            Assert.assertNotNull(result);
            Assert.assertEquals(expected.size(), result.getTotalCount().intValue());
            Assert.assertEquals(expected.size(), result.getCount().intValue());
            verifyRecords(expected, result.getRecords());
        }
        List<UUID> allIds = allExpected.stream().map(BAProfile::getId).distinct().collect(Collectors.toList());
        DbListResponse<BAProfile> result = baProfileDatabaseService.listByFilter(company, 0, 100, allIds, null, null);
        Assert.assertNotNull(result);
        Assert.assertEquals(allExpected.size(), result.getTotalCount().intValue());
        Assert.assertEquals(allExpected.size(), result.getCount().intValue());
        verifyRecords(allExpected, result.getRecords());

        result = baProfileDatabaseService.listByFilter(company, 0, 100, List.of(UUID.randomUUID()), null, null);
        Assert.assertNotNull(result);
        Assert.assertEquals(0, result.getTotalCount().intValue());
        Assert.assertTrue(CollectionUtils.isEmpty(result.getRecords()));
    }
    private void testAllListByFilters(List<BAProfile> allExpected) throws SQLException {
        testListByFiltersIds(allExpected);
        testListByFiltersNames(allExpected);
        testListByFiltersDefaultProfile(allExpected);
    }
    //endregion

    //region Test Get
    private void testGet(BAProfile expected) throws SQLException {
        BAProfile actual = baProfileDatabaseService.get(company, expected.getId().toString()).get();
        verifyRecord(expected, actual);
    }
    //endregion

    //region Test Insert
    private BAProfile testDBInsert(BAProfile baProfile) throws SQLException {
        String id = baProfileDatabaseService.insert(company, baProfile);
        Assert.assertNotNull(id);
        BAProfile expected = baProfile.toBuilder().id(UUID.fromString(id)).build();
        testGet(expected);
        return expected;
    }
    private BAProfile testInsert(int i, boolean defaultProfile) throws SQLException {
        BAProfile expected = BAProfileTestUtils.buildProfile(i, defaultProfile);
        return testDBInsert(expected);
    }
    //endregion

    //region Test Insert With Errors
    private void testInsertWithErrors(int i, boolean defaultProfile) {
        try {
            BAProfile temp = testInsert(i, defaultProfile);
            Assert.fail("Exception expected!");
        } catch (Exception e) {
            System.out.println(e);
        }
    }
    //endregion

    //region Test Update
    private List<BAProfile> testUpdate(List<BAProfile> allExpected) throws SQLException {
        List<BAProfile> allUpdated = new ArrayList<>();
        for(int i=0; i< allExpected.size(); i++){
            BAProfile current = allExpected.get(i);
            BAProfile updated = current.toBuilder()
                    .name("Work Items Updated " + i).description("BA Profile for Work Items Updated " + i)
                    .build();
            Boolean success = baProfileDatabaseService.update(company, updated);
            Assert.assertTrue(success);
            testGet(updated);
            allUpdated.add(updated);
        }
        testAllListByFilters(allUpdated);
        return allUpdated;
    }
    //endregion

    //region Test Set Default
    private List<BAProfile> generateExpected(List<BAProfile> allExpected, UUID newDefaultProfileId) {
        return allExpected.stream().map(p -> p.toBuilder().defaultProfile(p.getId().equals(newDefaultProfileId)).build()).collect(Collectors.toList()) ;
    }
    private void testSetDefaultProfileInvalid() {
        try {
            baProfileDatabaseService.updateDefault(company, UUID.randomUUID());
            Assert.fail("NotFoundException expected");
        } catch (NotFoundException e) {
            System.out.println("All good");
        } catch (SQLException ex) {
            Assert.fail("NotFoundException expected");
        }
    }
    private List<BAProfile> testSetDefaultProfileValid(List<BAProfile> allExpected) throws SQLException, NotFoundException {
        List<BAProfile> allExpectedSorted = allExpected.stream().sorted(Comparator.comparing(BAProfile::getDefaultProfile, Comparator.reverseOrder())).collect(Collectors.toList());
        UUID defaultProfileId = allExpected.stream().filter(a -> BooleanUtils.isTrue(a.getDefaultProfile())).map(BAProfile::getId).collect(Collectors.toList()).get(0);

        List<BAProfile> allUpdated = new ArrayList<>();
        for(int i=0; i< allExpectedSorted.size(); i++){
            BAProfile current = allExpectedSorted.get(i);
            UUID newDefaultProfileId = current.getId();
            ImmutablePair<Integer, Integer> result = baProfileDatabaseService.updateDefault(company, newDefaultProfileId);
            if(defaultProfileId.equals(newDefaultProfileId)) {
                Assert.assertEquals(0, result.getLeft().intValue());
                Assert.assertEquals(0, result.getRight().intValue());
            } else {
                Assert.assertEquals(1, result.getLeft().intValue());
                Assert.assertEquals(1, result.getRight().intValue());
            }
            allUpdated = generateExpected(allExpected, newDefaultProfileId);
            testAllListByFilters(allUpdated);
        }
        return allUpdated;
    }
    private List<BAProfile> testSetDefaultProfile(List<BAProfile> allExpected) throws SQLException, NotFoundException {
        testSetDefaultProfileInvalid();
        return testSetDefaultProfileValid(allExpected);
    }
    //endregion

    //region Test List With Pagination
    private void testListWithPagination(List<BAProfile> allExpected) throws SQLException {
        int totalSize = allExpected.size();
        int pageSize = 2;
        int pages = (totalSize % pageSize == 0) ? (totalSize/pageSize) : (totalSize/pageSize) + 1;
        Set<UUID> seen = new HashSet<>();
        int i = 0;
        while (i < pages) {
            DbListResponse<BAProfile> currentPage = baProfileDatabaseService.list(company, i, pageSize);
            Assert.assertNotNull(currentPage);
            Assert.assertEquals(allExpected.size(), currentPage.getTotalCount().intValue());
            if (i < pages-1) {
                Assert.assertEquals(pageSize, currentPage.getCount().intValue());
            } else {
                Assert.assertTrue(currentPage.getCount().intValue() <= pageSize);
            }
            for(BAProfile p : currentPage.getRecords()) {
                Assert.assertTrue(seen.add(p.getId()));
            }
            i++;
        }
        DbListResponse<BAProfile> currentPage = baProfileDatabaseService.list(company, i, pageSize);
        Assert.assertNotNull(currentPage);
        Assert.assertTrue(CollectionUtils.isEmpty(currentPage.getRecords()));
    }
    //endregion

    //region Test Delete
    private void testValidDelete(List<BAProfile> expected) throws SQLException {
        int size = expected.size();
        for(int i=0; i< size; i++){
            BAProfile current = expected.get(0);
            Boolean success = baProfileDatabaseService.delete(company, current.getId().toString());
            Assert.assertTrue(success);
            expected.remove(0);
            testAllListByFilters(expected);
        }
        testAllListByFilters(expected);
    }

    private void testInvalidDelete() throws SQLException {
        Boolean success = baProfileDatabaseService.delete(company, UUID.randomUUID().toString());
        Assert.assertFalse(success);
    }
    private void testDelete(List<BAProfile> allExpected) throws SQLException {
        testInvalidDelete();
        testValidDelete(allExpected);
    }
    //endregion

    @Test
    public void test() throws SQLException, NotFoundException {
        List<BAProfile> allExpected = new ArrayList<>();
        BAProfile expected = testInsert(0, DEFAULT_PROFILE);
        allExpected.add(expected);
        testAllListByFilters(allExpected);

        //Test multiple default profiles
        testInsertWithErrors(1, DEFAULT_PROFILE);

        //Test multiple profiles with same names
        testInsertWithErrors(0, NON_DEFAULT_PROFILE);

        //Add more non default profiles
        for (int i = 1; i<5; i++) {
            allExpected.add(testInsert(i, NON_DEFAULT_PROFILE));
            testAllListByFilters(allExpected);
        }

        //Test update
        allExpected = testUpdate(allExpected);

        //Test set default update
        allExpected = testSetDefaultProfile(allExpected);

        //Test List with Pagination
        testListWithPagination(allExpected);

        //Test Delete
        testDelete(allExpected);

        System.out.println(allExpected.size());
    }
}