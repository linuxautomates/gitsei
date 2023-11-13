package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.CICDInstance;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.filters.CICDInstanceFilter;
import io.levelops.commons.databases.models.filters.CICD_TYPE;
import io.levelops.commons.models.DbListResponse;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Log4j2
public class CiCdInstancesDatabaseServiceListTest {

    private final static String company = "test";
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;
    private static CiCdInstancesDatabaseService dbService;
    private static IntegrationService integrationService;
    private static List<CICDInstance> cicdInstanceList;
    private static List<String> integrationIds;

    @BeforeClass
    public static void setup() throws SQLException {
        if (dataSource!=null) return;
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        dbService = new CiCdInstancesDatabaseService(dataSource);
        integrationService = new IntegrationService(dataSource);
        new DatabaseSchemaService(dataSource)
                .ensureSchemaExistence(company);
        integrationService.ensureTableExistence(company);
        dbService.ensureTableExistence(company);
        integrationIds = new ArrayList<>();
        integrationIds.add(insertIntegration("jira_cicd_test"));
        integrationIds.add(insertIntegration("zendesk_cicd_test"));
        List<UUID> jenkinsInstanceGuids = generateCiCdInstanceGuids(5);
        cicdInstanceList = testInserts(jenkinsInstanceGuids, integrationIds.get(0));
        jenkinsInstanceGuids = generateCiCdInstanceGuids(3);
        cicdInstanceList.addAll(testInserts(jenkinsInstanceGuids, integrationIds.get(1)));
        jenkinsInstanceGuids = generateCiCdInstanceGuids(2);
        cicdInstanceList.addAll(testInserts(jenkinsInstanceGuids, null));
    }

    private static List<UUID> generateCiCdInstanceGuids(int n) {
        List<UUID> cicdInstanceGuids = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            cicdInstanceGuids.add(UUID.randomUUID());
        }
        return cicdInstanceGuids;
    }

    private static String insertIntegration(String name) throws SQLException {
        Integration integration = Integration.builder()
                .name(name)
                .url("http")
                .status("good")
                .application("jenkins")
                .description("desc")
                .satellite(true)
                .build();
        return integrationService.insert(company, integration);
    }

    private static CICDInstance testInsert(int i, UUID instanceGuid, String integrationId) throws SQLException {
        CICDInstance cicdInstance = CICDInstance.builder()
                .id(instanceGuid)
                .name("name-" + i)
                .url("url-" + i)
                .integrationId(integrationId)
                .type("jenkins")
                .build();
        String id = dbService.insert(company, cicdInstance);
        Assert.assertEquals(id, cicdInstance.getId().toString());
        Assert.assertNotNull(id);
        CICDInstance expected = cicdInstance.toBuilder().id(UUID.fromString(id)).build();
        testGet(expected);
        return expected;
    }

    private static void testGet(CICDInstance expected) throws SQLException {
        CICDInstance actual = dbService.get(company, expected.getId().toString()).get();
        verifyRecord(actual, expected);
    }

    private static List<CICDInstance> testInserts(List<UUID> jenkinsInstanceGuids, String integrationId) throws SQLException {
        List<CICDInstance> cicdInstances = new ArrayList<>();
        for (int i = 0; i < jenkinsInstanceGuids.size(); i++) {
            CICDInstance cicdInstance = testInsert(i, jenkinsInstanceGuids.get(i), integrationId);
            cicdInstances.add(cicdInstance);
        }
        return cicdInstances;
    }

    private static void verifyRecord(CICDInstance a, CICDInstance e) {
        Assert.assertEquals(a.getId(), e.getId());
        Assert.assertEquals(a.getName(), e.getName());
        Assert.assertEquals(a.getUrl(), e.getUrl());
        Assert.assertEquals(a.getIntegrationId(), e.getIntegrationId());
        Assert.assertNotNull(a.getCreatedAt());
        Assert.assertNotNull(a.getUpdatedAt());
    }

    private static void verifyRecords(List<CICDInstance> a, List<CICDInstance> e) {
        Assert.assertEquals(CollectionUtils.isEmpty(a), CollectionUtils.isEmpty(e));
        if (CollectionUtils.isEmpty(a)) {
            return;
        }
        Assert.assertEquals(a.size(), e.size());
        Map<UUID, CICDInstance> actualMap = a.stream().collect(Collectors.toMap(CICDInstance::getId, x -> x));
        Map<UUID, CICDInstance> expectedMap = e.stream().collect(Collectors.toMap(CICDInstance::getId, x -> x));

        for (UUID key : actualMap.keySet()) {
            verifyRecord(actualMap.get(key), expectedMap.get(key));
        }
    }

    @Test
    public void testPartialMatch() throws SQLException {
        CICDInstanceFilter filter
                = CICDInstanceFilter.builder()
                .partialMatch(Map.of("name", Map.of("$begins", "na")))
                .build();
        DbListResponse<CICDInstance> result = dbService.list(company, filter, null, 0, 100);
        Assert.assertNotNull(result);
        Assert.assertEquals(cicdInstanceList.size(), result.getTotalCount().intValue());
        Assert.assertEquals(cicdInstanceList.size(), result.getCount().intValue());
        verifyRecords(result.getRecords(), cicdInstanceList);

        filter = CICDInstanceFilter.builder()
                .partialMatch(Map.of("name", Map.of("$begins", "name-")))
                .build();
        result = dbService.list(company, filter, null, 0, 100);
        Assert.assertNotNull(result);
        Assert.assertEquals(cicdInstanceList.size(), result.getTotalCount().intValue());
        Assert.assertEquals(cicdInstanceList.size(), result.getCount().intValue());
        verifyRecords(result.getRecords(), cicdInstanceList);

        filter = CICDInstanceFilter.builder()
                .partialMatch(Map.of("name", Map.of("$begins", "ins")))
                .build();
        result = dbService.list(company, filter, null, 0, 100);
        Assert.assertNotNull(result);
        Assert.assertEquals(0, result.getTotalCount().intValue());
        Assert.assertEquals(0, result.getCount().intValue());

        filter = CICDInstanceFilter.builder()
                .partialMatch(Map.of("name", Map.of("$contains", "ame-")))
                .build();
        result = dbService.list(company, filter, null, 0, 100);
        Assert.assertNotNull(result);
        Assert.assertEquals(cicdInstanceList.size(), result.getTotalCount().intValue());
        Assert.assertEquals(cicdInstanceList.size(), result.getCount().intValue());
        verifyRecords(result.getRecords(), cicdInstanceList);

        filter = CICDInstanceFilter.builder()
                .partialMatch(Map.of("name", Map.of("$ends", "-0")))
                .build();
        result = dbService.list(company, filter, null, 0, 100);
        Assert.assertNotNull(result);
        Assert.assertEquals(3, result.getTotalCount().intValue());
        Assert.assertEquals(3, result.getCount().intValue());
        verifyRecords(result.getRecords(), List.of(cicdInstanceList.get(0), cicdInstanceList.get(5), cicdInstanceList.get(8)));

        filter = CICDInstanceFilter.builder()
                .partialMatch(Map.of("name", Map.of("$ends", "-0", "$begins", "name")))
                .build();
        result = dbService.list(company, filter, null, 0, 100);
        Assert.assertNotNull(result);
        Assert.assertEquals(3, result.getTotalCount().intValue());
        Assert.assertEquals(3, result.getCount().intValue());
        verifyRecords(result.getRecords(), List.of(cicdInstanceList.get(0), cicdInstanceList.get(5), cicdInstanceList.get(8)));

        filter = CICDInstanceFilter.builder()
                .partialMatch(Map.of("name", Map.of("$ends", "-0", "$begins", "inst")))
                .build();
        result = dbService.list(company, filter, null, 0, 100);
        Assert.assertNotNull(result);
        Assert.assertEquals(0, result.getTotalCount().intValue());
        Assert.assertEquals(0, result.getCount().intValue());

        filter = CICDInstanceFilter.builder()
                .partialMatch(Map.of("name", Map.of("$ends", "-0", "$begins", "name", "$contains", "name")))
                .build();
        result = dbService.list(company, filter, null, 0, 100);
        Assert.assertNotNull(result);
        Assert.assertEquals(3, result.getTotalCount().intValue());
        Assert.assertEquals(3, result.getCount().intValue());
        verifyRecords(result.getRecords(), List.of(cicdInstanceList.get(0), cicdInstanceList.get(5), cicdInstanceList.get(8)));
    }

    @Test
    public void testCreatedRangeFilter() throws SQLException {
        CICDInstanceFilter filter = CICDInstanceFilter.builder()
                .instanceCreatedRange((ImmutablePair.of(1584469711L, 1584469714L)))
                .build();
        DbListResponse<CICDInstance> result = dbService.list(company, filter, null, 0, 100);
        Assert.assertNotNull(result);
        Assert.assertEquals(0, result.getTotalCount().intValue());
        Assert.assertEquals(0, result.getCount().intValue());

        filter = CICDInstanceFilter.builder()
                .instanceCreatedRange((ImmutablePair.of(1584469711L, 1933134892L)))
                .build();
        result = dbService.list(company, filter, null, 0, 100);
        Assert.assertNotNull(result);
        Assert.assertEquals(cicdInstanceList.size(), result.getTotalCount().intValue());
        Assert.assertEquals(cicdInstanceList.size(), result.getCount().intValue());
        verifyRecords(result.getRecords(), cicdInstanceList);
    }

    @Test
    public void testUpdatedRangeFilter() throws SQLException {
        CICDInstanceFilter filter = CICDInstanceFilter.builder()
                .instanceUpdatedRange((ImmutablePair.of(1584469711L, 1584469714L)))
                .build();
        DbListResponse<CICDInstance> result = dbService.list(company, filter, null, 0, 100);
        Assert.assertNotNull(result);
        Assert.assertEquals(0, result.getTotalCount().intValue());
        Assert.assertEquals(0, result.getCount().intValue());

        filter = CICDInstanceFilter.builder()
                .instanceUpdatedRange((ImmutablePair.of(1584469711L, 1933134892L)))
                .build();
        result = dbService.list(company, filter, null, 0, 100);
        Assert.assertNotNull(result);
        Assert.assertEquals(cicdInstanceList.size(), result.getTotalCount().intValue());
        Assert.assertEquals(cicdInstanceList.size(), result.getCount().intValue());
        verifyRecords(result.getRecords(), cicdInstanceList);
    }

    @Test
    public void testNamesFilter() throws SQLException {
        CICDInstanceFilter filter = CICDInstanceFilter.builder()
                .names(List.of("name-0"))
                .build();
        DbListResponse<CICDInstance> result = dbService.list(company, filter, null, 0, 100);
        Assert.assertNotNull(result);
        Assert.assertEquals(3, result.getTotalCount().intValue());
        Assert.assertEquals(3, result.getCount().intValue());
        verifyRecords(result.getRecords(), List.of(cicdInstanceList.get(0), cicdInstanceList.get(5), cicdInstanceList.get(8)));

        filter = CICDInstanceFilter.builder()
                .names(List.of("name-0", "name-1"))
                .build();
        result = dbService.list(company, filter, null, 0, 100);
        Assert.assertNotNull(result);
        Assert.assertEquals(6, result.getTotalCount().intValue());
        Assert.assertEquals(6, result.getCount().intValue());
        verifyRecords(result.getRecords(), List.of(
                cicdInstanceList.get(0), cicdInstanceList.get(1),
                cicdInstanceList.get(5), cicdInstanceList.get(6),
                cicdInstanceList.get(8), cicdInstanceList.get(9)));

        filter = CICDInstanceFilter.builder()
                .names(List.of("name-0", "name-1", "name-7"))
                .build();
        result = dbService.list(company, filter, null, 0, 100);
        Assert.assertNotNull(result);
        Assert.assertEquals(6, result.getTotalCount().intValue());
        Assert.assertEquals(6, result.getCount().intValue());
        verifyRecords(result.getRecords(), List.of(
                cicdInstanceList.get(0), cicdInstanceList.get(1),
                cicdInstanceList.get(5), cicdInstanceList.get(6),
                cicdInstanceList.get(8), cicdInstanceList.get(9)));

        filter = CICDInstanceFilter.builder()
                .names(List.of("name-7", "name-8"))
                .build();
        result = dbService.list(company, filter, null, 0, 100);
        Assert.assertNotNull(result);
        Assert.assertEquals(0, result.getTotalCount().intValue());
        Assert.assertEquals(0, result.getCount().intValue());

        filter = CICDInstanceFilter.builder()
                .names(List.of())
                .build();
        result = dbService.list(company, filter, null, 0, 100);
        Assert.assertNotNull(result);
        Assert.assertEquals(cicdInstanceList.size(), result.getTotalCount().intValue());
        Assert.assertEquals(cicdInstanceList.size(), result.getCount().intValue());
    }

    @Test
    public void testIdsFilter() throws SQLException {
        CICDInstanceFilter filter = CICDInstanceFilter.builder()
                .ids(cicdInstanceList.stream()
                        .map(CICDInstance::getId)
                        .map(String::valueOf)
                        .collect(Collectors.toList()))
                .build();
        DbListResponse<CICDInstance> filteredResult = dbService.list(company, filter, null, 0, 100);
        Assert.assertNotNull(filteredResult);
        Assert.assertEquals(cicdInstanceList.size(), filteredResult.getTotalCount().intValue());
        Assert.assertEquals(cicdInstanceList.size(), filteredResult.getCount().intValue());
        List<UUID> actualIdsList = filteredResult.getRecords().stream()
                .filter(Objects::nonNull).map(CICDInstance::getId)
                .sorted().collect(Collectors.toList());
        List<UUID> expectedIdsList = cicdInstanceList.stream()
                .map(CICDInstance::getId)
                .sorted().collect(Collectors.toList());
        Assert.assertEquals(expectedIdsList, actualIdsList);

        List<String> inputIds = cicdInstanceList.subList(0, 5)
                .stream()
                .map(CICDInstance::getId)
                .map(String::valueOf)
                .collect(Collectors.toList());
        filter = CICDInstanceFilter.builder()
                .ids(inputIds)
                .build();
        filteredResult = dbService.list(company, filter, null, 0, 100);
        Assert.assertNotNull(filteredResult);
        Assert.assertEquals(cicdInstanceList.subList(0, 5).size(), filteredResult.getTotalCount().intValue());
        Assert.assertEquals(cicdInstanceList.subList(0, 5).size(), filteredResult.getCount().intValue());
        actualIdsList = filteredResult.getRecords().stream()
                .filter(Objects::nonNull).map(CICDInstance::getId)
                .sorted().collect(Collectors.toList());
        expectedIdsList = cicdInstanceList.subList(0, 5).stream()
                .map(CICDInstance::getId)
                .sorted().collect(Collectors.toList());
        Assert.assertEquals(expectedIdsList, actualIdsList);

        inputIds = List.of(cicdInstanceList.get(0), cicdInstanceList.get(3), cicdInstanceList.get(5))
                .stream()
                .map(CICDInstance::getId)
                .map(String::valueOf)
                .collect(Collectors.toList());
        filter = CICDInstanceFilter.builder()
                .ids(inputIds)
                .build();
        filteredResult = dbService.list(company, filter, null, 0, 100);
        Assert.assertNotNull(filteredResult);
        Assert.assertEquals(List.of(cicdInstanceList.get(0), cicdInstanceList.get(3), cicdInstanceList.get(5)).size(), filteredResult.getTotalCount().intValue());
        Assert.assertEquals(List.of(cicdInstanceList.get(0), cicdInstanceList.get(3), cicdInstanceList.get(5)).size(), filteredResult.getCount().intValue());
        actualIdsList = filteredResult.getRecords().stream()
                .filter(Objects::nonNull).map(CICDInstance::getId)
                .sorted().collect(Collectors.toList());
        expectedIdsList = List.of(cicdInstanceList.get(0), cicdInstanceList.get(3), cicdInstanceList.get(5)).stream()
                .map(CICDInstance::getId)
                .sorted().collect(Collectors.toList());
        Assert.assertEquals(expectedIdsList, actualIdsList);

        filter = CICDInstanceFilter.builder()
                .ids(List.of())
                .build();
        filteredResult = dbService.list(company, filter, null, 0, 100);
        Assert.assertNotNull(filteredResult);
        Assert.assertEquals(cicdInstanceList.size(), filteredResult.getTotalCount().intValue());
        Assert.assertEquals(cicdInstanceList.size(), filteredResult.getCount().intValue());
        actualIdsList = filteredResult.getRecords().stream()
                .filter(Objects::nonNull).map(CICDInstance::getId)
                .sorted().collect(Collectors.toList());
        expectedIdsList = cicdInstanceList.stream()
                .map(CICDInstance::getId)
                .sorted().collect(Collectors.toList());
        Assert.assertEquals(expectedIdsList, actualIdsList);
    }

    @Test
    public void testIntegrationIdsFilter() throws SQLException {
        CICDInstanceFilter filter = CICDInstanceFilter.builder()
                .integrationIds(List.of())
                .build();
        DbListResponse<CICDInstance> result = dbService.list(company, filter, null, 0, 100);
        Assert.assertNotNull(result);
        Assert.assertEquals(cicdInstanceList.size(), result.getTotalCount().intValue());
        Assert.assertEquals(cicdInstanceList.size(), result.getCount().intValue());

        filter = CICDInstanceFilter.builder()
                .integrationIds(List.of(integrationIds.get(0)))
                .build();
        result = dbService.list(company, filter, null, 0, 100);
        Assert.assertNotNull(result);
        Assert.assertEquals(5, result.getTotalCount().intValue());
        Assert.assertEquals(5, result.getCount().intValue());
        verifyRecords(result.getRecords(), cicdInstanceList.subList(0, 5));

        filter = CICDInstanceFilter.builder()
                .integrationIds(integrationIds)
                .build();
        result = dbService.list(company, filter, null, 0, 100);
        Assert.assertNotNull(result);
        Assert.assertEquals(cicdInstanceList.size() - 2, result.getTotalCount().intValue());
        Assert.assertEquals(cicdInstanceList.size() - 2, result.getCount().intValue());
        verifyRecords(result.getRecords(), cicdInstanceList.subList(0, 8));

        filter = CICDInstanceFilter.builder()
                .integrationIds(List.of("10", "11"))
                .build();
        result = dbService.list(company, filter, null, 0, 100);
        Assert.assertNotNull(result);
        Assert.assertEquals(0, result.getTotalCount().intValue());
        Assert.assertEquals(0, result.getCount().intValue());
    }

    @Test
    public void testMissingIntegrationIdsFilter() throws SQLException {
        CICDInstanceFilter filter = CICDInstanceFilter.builder()
                .integrationIds(List.of())
                .missingFields(Map.of("integration_id", true))
                .build();
        DbListResponse<CICDInstance> result = dbService.list(company, filter, null, 0, 100);
        Assert.assertNotNull(result);
        Assert.assertEquals(2, result.getTotalCount().intValue());
        Assert.assertEquals(2, result.getCount().intValue());

        filter = CICDInstanceFilter.builder()
                .missingFields(Map.of("integration_id", true))
                .build();
        result = dbService.list(company, filter, null, 0, 100);
        Assert.assertNotNull(result);
        Assert.assertEquals(2, result.getTotalCount().intValue());
        Assert.assertEquals(2, result.getCount().intValue());

        filter = CICDInstanceFilter.builder()
                .integrationIds(List.of(integrationIds.get(0)))
                .missingFields(Map.of("integration_id", false))
                .build();
        result = dbService.list(company, filter, null, 0, 100);
        Assert.assertNotNull(result);
        Assert.assertEquals(5, result.getTotalCount().intValue());
        Assert.assertEquals(5, result.getCount().intValue());
        verifyRecords(result.getRecords(), cicdInstanceList.subList(0, 5));

        filter = CICDInstanceFilter.builder()
                .integrationIds(List.of(integrationIds.get(0)))
                .missingFields(Map.of("integration_id", true))
                .build();
        result = dbService.list(company, filter, null, 0, 100);
        Assert.assertNotNull(result);
        Assert.assertEquals(0, result.getTotalCount().intValue());
        Assert.assertEquals(0, result.getCount().intValue());

        filter = CICDInstanceFilter.builder()
                .integrationIds(integrationIds)
                .missingFields(Map.of("integration_id", false))
                .build();
        result = dbService.list(company, filter, null, 0, 100);
        Assert.assertNotNull(result);
        Assert.assertEquals(cicdInstanceList.size() - 2, result.getTotalCount().intValue());
        Assert.assertEquals(cicdInstanceList.size() - 2, result.getCount().intValue());
        verifyRecords(result.getRecords(), cicdInstanceList.subList(0, 8));

        filter = CICDInstanceFilter.builder()
                .integrationIds(integrationIds)
                .missingFields(Map.of("integration_id", true))
                .build();
        result = dbService.list(company, filter, null, 0, 100);
        Assert.assertNotNull(result);
        Assert.assertEquals(0, result.getTotalCount().intValue());
        Assert.assertEquals(0, result.getCount().intValue());

        filter = CICDInstanceFilter.builder()
                .integrationIds(integrationIds)
                .missingFields(Map.of("does-not-exist", true))
                .build();
        result = dbService.list(company, filter, null, 0, 100);
        Assert.assertNotNull(result);
        Assert.assertEquals(cicdInstanceList.size() - 2, result.getTotalCount().intValue());
        Assert.assertEquals(cicdInstanceList.size() - 2, result.getCount().intValue());
    }

    @Test
    public void testTypeFilter() throws SQLException {
        CICDInstanceFilter filter = CICDInstanceFilter.builder()
                .types(List.of(CICD_TYPE.jenkins))
                .build();
        DbListResponse<CICDInstance> result = dbService.list(company, filter, null, 0, 100);
        Assert.assertNotNull(result);
        Assert.assertEquals(cicdInstanceList.size(), result.getTotalCount().intValue());
        Assert.assertEquals(cicdInstanceList.size(), result.getCount().intValue());

        filter = CICDInstanceFilter.builder()
                .types(List.of(CICD_TYPE.azure_devops))
                .build();
        result = dbService.list(company, filter, null, 0, 100);
        Assert.assertNotNull(result);
        Assert.assertEquals(0, result.getTotalCount().intValue());
        Assert.assertEquals(0, result.getCount().intValue());
    }

    @Test
    public void testPartialMatchAndIntegrationIdsFilter() throws SQLException {
        CICDInstanceFilter filter = CICDInstanceFilter.builder()
                .partialMatch(Map.of("name", Map.of("$begins", "name-")))
                .integrationIds(integrationIds)
                .build();
        DbListResponse<CICDInstance> result = dbService.list(company, filter, null, 0, 100);
        Assert.assertNotNull(result);
        Assert.assertEquals(cicdInstanceList.size() - 2, result.getTotalCount().intValue());
        Assert.assertEquals(cicdInstanceList.size() - 2, result.getCount().intValue());
        verifyRecords(result.getRecords(), cicdInstanceList.subList(0, 8));

        filter = CICDInstanceFilter.builder()
                .partialMatch(Map.of("name", Map.of("$begins", "inst")))
                .integrationIds(integrationIds)
                .build();
        result = dbService.list(company, filter, null, 0, 100);
        Assert.assertNotNull(result);
        Assert.assertEquals(0, result.getTotalCount().intValue());
        Assert.assertEquals(0, result.getCount().intValue());

        filter = CICDInstanceFilter.builder()
                .partialMatch(Map.of("name", Map.of("$begins", "name-")))
                .integrationIds(List.of(integrationIds.get(0)))
                .build();
        result = dbService.list(company, filter, null, 0, 100);
        Assert.assertNotNull(result);
        Assert.assertEquals(5, result.getTotalCount().intValue());
        Assert.assertEquals(5, result.getCount().intValue());
        verifyRecords(result.getRecords(), cicdInstanceList.subList(0, 5));

        filter = CICDInstanceFilter.builder()
                .partialMatch(Map.of("name", Map.of("$begins", "name-", "$ends", "-0")))
                .integrationIds(List.of(integrationIds.get(1)))
                .build();
        result = dbService.list(company, filter, null, 0, 100);
        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.getTotalCount().intValue());
        Assert.assertEquals(1, result.getCount().intValue());
        verifyRecords(result.getRecords(), List.of(cicdInstanceList.get(5)));

        filter = CICDInstanceFilter.builder()
                .partialMatch(Map.of("type", Map.of("$contains", "jenkins")))
                .integrationIds(List.of(integrationIds.get(1)))
                .build();
        result = dbService.list(company, filter, null, 0, 100);
        Assert.assertNotNull(result);
        Assert.assertEquals(3, result.getTotalCount().intValue());
        Assert.assertEquals(3, result.getCount().intValue());
        verifyRecords(result.getRecords(), cicdInstanceList.subList(5, 8));
    }

    @Test
    public void testSortByFilter() throws SQLException {
        CICDInstanceFilter filter = CICDInstanceFilter.builder()
                .partialMatch(Map.of("name", Map.of("$begins", "name-")))
                .integrationIds(integrationIds)
                .build();
        Map<String, SortingOrder> sortBy = Map.of("updated_at", SortingOrder.DESC);
        DbListResponse<CICDInstance> filteredResult = dbService.list(company, filter, sortBy, 0, 100);
        Assert.assertNotNull(filteredResult);
        Assert.assertEquals(cicdInstanceList.size() - 2, filteredResult.getTotalCount().intValue());
        Assert.assertEquals(cicdInstanceList.size() - 2, filteredResult.getCount().intValue());
        List<Instant> actualInstantsList = filteredResult.getRecords().stream()
                .filter(Objects::nonNull).map(CICDInstance::getUpdatedAt)
                .collect(Collectors.toList());
        DbListResponse<CICDInstance> result = dbService.list(company, filter, null, 0, 100);
        Assert.assertNotNull(result);
        Assert.assertEquals(cicdInstanceList.size() - 2, result.getTotalCount().intValue());
        Assert.assertEquals(cicdInstanceList.size() - 2, result.getCount().intValue());
        List<Instant> expectedInstantsList
                = result.getRecords().stream()
                .filter(Objects::nonNull)
                .map(CICDInstance::getUpdatedAt)
                .sorted(Collections.reverseOrder())
                .collect(Collectors.toList());
        Assert.assertEquals(expectedInstantsList, actualInstantsList);

        sortBy = Map.of("created_at", SortingOrder.DESC);
        filteredResult = dbService.list(company, filter, sortBy, 0, 100);
        Assert.assertNotNull(filteredResult);
        Assert.assertEquals(cicdInstanceList.size() - 2, filteredResult.getTotalCount().intValue());
        Assert.assertEquals(cicdInstanceList.size() - 2, filteredResult.getCount().intValue());
        actualInstantsList = filteredResult.getRecords().stream()
                .filter(Objects::nonNull).map(CICDInstance::getCreatedAt)
                .collect(Collectors.toList());
        result = dbService.list(company, filter, null, 0, 100);
        Assert.assertNotNull(result);
        Assert.assertEquals(cicdInstanceList.size() - 2, result.getTotalCount().intValue());
        Assert.assertEquals(cicdInstanceList.size() - 2, result.getCount().intValue());
        expectedInstantsList = result.getRecords().stream()
                .filter(Objects::nonNull)
                .map(CICDInstance::getCreatedAt)
                .sorted(Collections.reverseOrder())
                .collect(Collectors.toList());
        Assert.assertEquals(expectedInstantsList, actualInstantsList);

        sortBy = Map.of("name", SortingOrder.DESC);
        filteredResult = dbService.list(company, filter, sortBy, 0, 100);
        Assert.assertNotNull(filteredResult);
        Assert.assertEquals(cicdInstanceList.size() - 2, filteredResult.getTotalCount().intValue());
        Assert.assertEquals(cicdInstanceList.size() - 2, filteredResult.getCount().intValue());
        List<String> actualNamesList = filteredResult.getRecords().stream()
                .filter(Objects::nonNull).map(CICDInstance::getName)
                .collect(Collectors.toList());
        result = dbService.list(company, filter, null, 0, 100);
        Assert.assertNotNull(result);
        Assert.assertEquals(cicdInstanceList.size() - 2, result.getTotalCount().intValue());
        Assert.assertEquals(cicdInstanceList.size() - 2, result.getCount().intValue());
        List<String> expectedNamesList = result.getRecords().stream()
                .filter(Objects::nonNull)
                .map(CICDInstance::getName)
                .sorted(Collections.reverseOrder())
                .collect(Collectors.toList());
        Assert.assertEquals(expectedNamesList, actualNamesList);
    }

    @Test
    public void testCICDInstanceExcludeFilter() throws SQLException {
        CICDInstanceFilter filter = CICDInstanceFilter.builder()
                .excludeNames(List.of("name-0"))
                .build();
        DbListResponse<CICDInstance> result = dbService.list(company, filter, null, 0, 100);
        Assert.assertNotNull(result);
        Assert.assertEquals(7, result.getTotalCount().intValue());
        Assert.assertEquals(7, result.getCount().intValue());
        Assert.assertEquals(7, result.getRecords().size());

        filter = CICDInstanceFilter.builder()
                .excludeNames(List.of("name-0"))
                .names(List.of("name-0"))
                .build();
        result = dbService.list(company, filter, null, 0, 100);
        Assert.assertNotNull(result);
        Assert.assertEquals(0, result.getTotalCount().intValue());
        Assert.assertEquals(0, result.getCount().intValue());
        Assert.assertEquals(0, result.getRecords().size());

        filter = CICDInstanceFilter.builder()
                .excludeTypes(List.of(CICD_TYPE.azure_devops))
                .build();
        result = dbService.list(company, filter, null, 0, 100);
        Assert.assertNotNull(result);
        Assert.assertEquals(10, result.getTotalCount().intValue());
        Assert.assertEquals(10, result.getCount().intValue());
        Assert.assertEquals(10, result.getRecords().size());

        filter = CICDInstanceFilter.builder()
                .types(List.of(CICD_TYPE.azure_devops))
                .excludeTypes(List.of(CICD_TYPE.azure_devops))
                .build();
        result = dbService.list(company, filter, null, 0, 100);
        Assert.assertNotNull(result);
        Assert.assertEquals(0, result.getTotalCount().intValue());
        Assert.assertEquals(0, result.getCount().intValue());
        Assert.assertEquals(0, result.getRecords().size());

        filter = CICDInstanceFilter.builder()
                .excludeIds(List.of("dfa8f6a5-9707-4eea-94a4-0c07b39f2b6c"))
                .build();
        result = dbService.list(company, filter, null, 0, 100);
        Assert.assertNotNull(result);
        Assert.assertEquals(10, result.getTotalCount().intValue());
        Assert.assertEquals(10, result.getCount().intValue());
        Assert.assertEquals(10, result.getRecords().size());

        filter = CICDInstanceFilter.builder()
                .ids(List.of("dfa8f6a5-9707-4eea-94a4-0c07b39f2b6c"))
                .excludeIds(List.of("dfa8f6a5-9707-4eea-94a4-0c07b39f2b6c"))
                .build();
        result = dbService.list(company, filter, null, 0, 100);
        Assert.assertNotNull(result);
        Assert.assertEquals(0, result.getTotalCount().intValue());
        Assert.assertEquals(0, result.getCount().intValue());
        Assert.assertEquals(0, result.getRecords().size());
    }
}