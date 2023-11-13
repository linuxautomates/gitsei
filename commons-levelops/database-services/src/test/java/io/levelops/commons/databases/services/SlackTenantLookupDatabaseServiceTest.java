package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.SlackTenantLookup;
import io.levelops.commons.jackson.DefaultObjectMapper;
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

import static io.levelops.commons.databases.services.DatabaseService.LEVELOPS_INVENTORY_SCHEMA;

public class SlackTenantLookupDatabaseServiceTest {
    @Rule
    public SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    private String schema = LEVELOPS_INVENTORY_SCHEMA;

    private DataSource dataSource;
    private SlackTenantLookupDatabaseService slackTenantLookupDatabaseService;

    @Before
    public void setup() throws SQLException {
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        slackTenantLookupDatabaseService = new SlackTenantLookupDatabaseService(dataSource, DefaultObjectMapper.get());

        new DatabaseSchemaService(dataSource)
                .ensureSchemaExistence(schema);
        slackTenantLookupDatabaseService.ensureTableExistence(schema);
    }

    private SlackTenantLookup testSingleInsert(int tenantName, int teamId) throws SQLException {
        SlackTenantLookup expected = SlackTenantLookup.builder()
        .teamId("team-" + teamId)
        .tenantName("tenant-name-" + tenantName)
        .build();
        String id = slackTenantLookupDatabaseService.upsert(expected);
        Assert.assertNotNull(id);
        return expected.toBuilder().id(UUID.fromString(id)).build();
    }
    private SlackTenantLookup testMultipleUpserts(int tenantName, int teamId) throws SQLException {
        List<SlackTenantLookup> slackTenantLookups = new ArrayList<>();
        for(int j=0; j< 5; j++) {
            slackTenantLookups.add(testSingleInsert(tenantName,teamId));
        }
        Assert.assertEquals(5, slackTenantLookups.size());
        Assert.assertEquals(1, slackTenantLookups.stream().map(SlackTenantLookup::getId).collect(Collectors.toSet()).size());
        return slackTenantLookups.get(0);
    }

    private List<SlackTenantLookup> testInserts(int n) throws SQLException {
        List<SlackTenantLookup> expected = new ArrayList<>();
        for(int i=1; i<=n; i++) {
            expected.add(testMultipleUpserts(i, i));
            expected.add(testMultipleUpserts(i, n+1));
        }
        Assert.assertEquals(n*2, expected.size());
        Assert.assertEquals(n*2, expected.stream().map(SlackTenantLookup::getId).collect(Collectors.toSet()).size());
        return expected;
    }

    private void verifyRecord(SlackTenantLookup e, SlackTenantLookup a) {
        Assert.assertEquals(a.getId(), e.getId());
        Assert.assertEquals(a.getTeamId(), e.getTeamId());
        Assert.assertEquals(a.getTenantName(), e.getTenantName());
        Assert.assertNotNull(a.getCreatedAt());
    }

    private void testLookUp(List<SlackTenantLookup> expected) throws SQLException {
        Map<String, List<SlackTenantLookup>> teamIdMap = expected.stream()
                .collect(Collectors.groupingBy(x -> x.getTeamId(),
                        Collectors.mapping(x -> x, Collectors.toList())));

        for(String teamId : teamIdMap.keySet()) {
            List<SlackTenantLookup> lookups = slackTenantLookupDatabaseService.lookup(teamId);
            verifyRecords(teamIdMap.get(teamId), lookups);
        }
    }
    private void verifyRecords(List<SlackTenantLookup> a, List<SlackTenantLookup> e) {
        Assert.assertEquals(CollectionUtils.isEmpty(a), CollectionUtils.isEmpty(e));
        if (CollectionUtils.isEmpty(a)) {
            return;
        }
        Assert.assertEquals(a.size(), e.size());
        Map<UUID, SlackTenantLookup> actualMap = a.stream().collect(Collectors.toMap(SlackTenantLookup::getId, x -> x));
        Map<UUID, SlackTenantLookup> expectedMap = e.stream().collect(Collectors.toMap(SlackTenantLookup::getId, x -> x));

        for (UUID key : actualMap.keySet()) {
            verifyRecord(actualMap.get(key), expectedMap.get(key));
        }
    }

    @Test
    public void test() throws SQLException {
        List<SlackTenantLookup> expected = testInserts(5);
        testLookUp(expected);
    }
}