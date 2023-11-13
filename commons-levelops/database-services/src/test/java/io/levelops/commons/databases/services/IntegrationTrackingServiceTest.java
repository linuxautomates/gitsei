package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.IntegrationTracker;
import io.levelops.commons.databases.models.database.IntegrationTrackerEx;
import io.levelops.ingestion.models.IntegrationType;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class IntegrationTrackingServiceTest {
    private static final String company = "test";
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;
    private static IntegrationService integrationService;
    private static IntegrationTrackingService integrationTrackingService;

    @BeforeClass
    public static void setup() throws Exception {
        if (dataSource != null) {
            return;
        }
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();

        integrationService = new IntegrationService(dataSource);
        integrationTrackingService = new IntegrationTrackingService(dataSource);

        new DatabaseSchemaService(dataSource).ensureSchemaExistence(company);
        integrationService.ensureTableExistence(company);
        integrationTrackingService.ensureTableExistence(company);
    }

    @Test
    public void testUpsertAndUpdateLatestAggsStartTime() throws SQLException {
        Integration integration = IntegrationUtils.createIntegration(integrationService, company, 1, IntegrationType.AZURE_DEVOPS.toString());
        String integrationId = integration.getId();

        //Long ingestedAtT0 = Long.valueOf(1632096000);
        Long ingestedAtT0 = Long.valueOf(1632182400);
        Long ingestedAtT1 = Long.valueOf(1632268800);

        integrationTrackingService.upsert(company, IntegrationTracker.builder().integrationId(integrationId).latestIngestedAt(ingestedAtT0).lastAggStartedAt(ingestedAtT0).build());
        IntegrationTracker integrationTracker = integrationTrackingService.get(company,integrationId).orElse(null);
        Assert.assertNotNull(integrationTracker);
        Assert.assertEquals(integrationId, integrationTracker.getIntegrationId());
        Assert.assertEquals(ingestedAtT0, integrationTracker.getLatestIngestedAt());
        Assert.assertEquals(ingestedAtT0, integrationTracker.getLastAggStartedAt());
        Assert.assertEquals(0L, integrationTracker.getLastAggEndedAt().longValue());

        //Old way to update Agg Started At - Only Aggs Start set to ingestedAtT1, IngestAt should be ingestedAtT0
        integrationTrackingService.upsert(company, IntegrationTracker.builder().integrationId(integrationId).lastAggStartedAt(ingestedAtT1).build());
        integrationTracker = integrationTrackingService.get(company,integrationId).orElse(null);
        Assert.assertNotNull(integrationTracker);
        Assert.assertEquals(integrationId, integrationTracker.getIntegrationId());
        Assert.assertEquals(0L, integrationTracker.getLatestIngestedAt().longValue()); //This is the bug - LEV-3886
        Assert.assertEquals(ingestedAtT1, integrationTracker.getLastAggStartedAt());
        Assert.assertEquals(0L, integrationTracker.getLastAggEndedAt().longValue());

        //Reset test data
        integrationTrackingService.upsert(company, IntegrationTracker.builder().integrationId(integrationId).latestIngestedAt(ingestedAtT0).lastAggStartedAt(ingestedAtT0).build());
        integrationTracker = integrationTrackingService.get(company,integrationId).orElse(null);
        Assert.assertNotNull(integrationTracker);
        Assert.assertEquals(integrationId, integrationTracker.getIntegrationId());
        Assert.assertEquals(ingestedAtT0, integrationTracker.getLatestIngestedAt());
        Assert.assertEquals(ingestedAtT0, integrationTracker.getLastAggStartedAt());
        Assert.assertEquals(0L, integrationTracker.getLastAggEndedAt().longValue());

        //Update only Agg Started At
        Assert.assertEquals(1, integrationTrackingService.updateLastAggStarted(company, Integer.parseInt(integrationId), ingestedAtT1));
        integrationTracker = integrationTrackingService.get(company,integrationId).orElse(null);
        Assert.assertNotNull(integrationTracker);
        Assert.assertEquals(integrationId, integrationTracker.getIntegrationId());
        Assert.assertEquals(ingestedAtT0, integrationTracker.getLatestIngestedAt());
        Assert.assertEquals(ingestedAtT1, integrationTracker.getLastAggStartedAt());
        Assert.assertEquals(0L, integrationTracker.getLastAggEndedAt().longValue());
    }

    @Test
    public void testLatestIngestedAt() throws SQLException {
        Integration integration = IntegrationUtils.createIntegration(integrationService, company, 0, IntegrationType.AZURE_DEVOPS.toString());
        String integrationId = integration.getId();

        Long ingestedAt1 = 1332096000L;
        Long ingestedAt2 = 1432182400L;
        Long ingestedAt3 = 1532268800L;
        Long ingestedAt4 = 1632268800L;
        Long ingestedAt5 = 1732268800L;

        integrationTrackingService.upsert(company, IntegrationTracker.builder().integrationId(integrationId).latestIngestedAt(ingestedAt1).lastAggStartedAt(ingestedAt1).build());
        integrationTrackingService.upsert(company, IntegrationTracker.builder().integrationId(integrationId).latestIngestedAt(ingestedAt2).lastAggStartedAt(ingestedAt2).build());
        integrationTrackingService.upsert(company, IntegrationTracker.builder().integrationId(integrationId).latestIngestedAt(ingestedAt3).lastAggStartedAt(ingestedAt3).build());
        integrationTrackingService.upsert(company, IntegrationTracker.builder().integrationId(integrationId).latestIngestedAt(ingestedAt4).lastAggStartedAt(ingestedAt4).build());
        integrationTrackingService.upsert(company, IntegrationTracker.builder().integrationId(integrationId).latestIngestedAt(ingestedAt5).lastAggStartedAt(ingestedAt5).build());

        IntegrationTracker integrationTracker = integrationTrackingService.get(company,integrationId).orElse(null);
        Assert.assertNotNull(integrationTracker);
        Assert.assertEquals(ingestedAt5, integrationTracker.getLatestIngestedAt());
    }

    private void verify(IntegrationTracker it, Long latestIngestedAt, Long latestAggregatedAt, Long latestESIndexedAt) {
        Assert.assertNotNull(it);
        Assert.assertEquals(latestIngestedAt, it.getLatestIngestedAt());
        Assert.assertEquals(latestAggregatedAt, it.getLatestAggregatedAt());
        Assert.assertEquals(latestESIndexedAt, it.getLatestESIndexedAt());
    }

    @Test
    public void test() throws SQLException {
        Integration integration1 = IntegrationUtils.createIntegration(integrationService, company, 2, IntegrationType.AZURE_DEVOPS.toString());
        Integer integrationId1 = Integer.parseInt(integration1.getId());

        Integration integration2 = IntegrationUtils.createIntegration(integrationService, company, 3, IntegrationType.JIRA.toString());
        Integer integrationId2 = Integer.parseInt(integration2.getId());

        Integration integration3 = IntegrationUtils.createIntegration(integrationService, company, 4, IntegrationType.GITHUB.toString());
        Integer integrationId3 = Integer.parseInt(integration3.getId());

        Long ingestedAt1 = 1332096000L;
        Long ingestedAt2 = 1432182400L;
        Long ingestedAt3 = 1532268800L;
        Long ingestedAt4 = 1632268800L;
        Long ingestedAt5 = 1732268800L;

        integrationTrackingService.upsert(company, IntegrationTracker.builder().integrationId(integrationId1.toString()).latestIngestedAt(ingestedAt1).build());
        integrationTrackingService.upsert(company, IntegrationTracker.builder().integrationId(integrationId2.toString()).latestIngestedAt(ingestedAt1).build());
        integrationTrackingService.upsert(company, IntegrationTracker.builder().integrationId(integrationId3.toString()).latestIngestedAt(ingestedAt1).build());

        IntegrationTracker integrationTracker1 = integrationTrackingService.get(company,integrationId1.toString()).orElse(null);
        verify(integrationTracker1, ingestedAt1, 0l, 0l);
        IntegrationTracker integrationTracker2 = integrationTrackingService.get(company,integrationId2.toString()).orElse(null);
        verify(integrationTracker2, ingestedAt1, 0l, 0l);
        IntegrationTracker integrationTracker3 = integrationTrackingService.get(company,integrationId3.toString()).orElse(null);
        verify(integrationTracker3, ingestedAt1, 0l, 0l);

        integrationTrackingService.upsertJiraWIDBAggregatedAt(company, integrationId1, ingestedAt2);
        integrationTrackingService.upsertJiraWIDBAggregatedAt(company, integrationId2, ingestedAt2);

        integrationTracker1 = integrationTrackingService.get(company,integrationId1.toString()).orElse(null);
        verify(integrationTracker1, ingestedAt1, ingestedAt2, 0l);
        integrationTracker2 = integrationTrackingService.get(company,integrationId2.toString()).orElse(null);
        verify(integrationTracker2, ingestedAt1, ingestedAt2, 0l);
        integrationTracker3 = integrationTrackingService.get(company,integrationId3.toString()).orElse(null);
        verify(integrationTracker3, ingestedAt1, 0l, 0l);

        integrationTrackingService.upsertJiraWIDBAggregatedAt(company, integrationId2, ingestedAt3);

        integrationTracker1 = integrationTrackingService.get(company,integrationId1.toString()).orElse(null);
        verify(integrationTracker1, ingestedAt1, ingestedAt2, 0l);
        integrationTracker2 = integrationTrackingService.get(company,integrationId2.toString()).orElse(null);
        verify(integrationTracker2, ingestedAt1, ingestedAt3, 0l);
        integrationTracker3 = integrationTrackingService.get(company,integrationId3.toString()).orElse(null);
        verify(integrationTracker3, ingestedAt1, 0l, 0l);

        Instant oldestIngestedAt = Instant.ofEpochSecond(ingestedAt1);
        List<IntegrationTrackerEx> mismatchedTrackers = integrationTrackingService.getDBESMismatchedTrackers(company, oldestIngestedAt);
        Assert.assertEquals(2, mismatchedTrackers.size());
        Assert.assertEquals(Set.of("jira", "azure_devops"), mismatchedTrackers.stream().map(IntegrationTrackerEx::getApplication).collect(Collectors.toSet()));
        Assert.assertEquals(Set.of(1,2), mismatchedTrackers.stream().map(IntegrationTrackerEx::getIntegrationId).collect(Collectors.toSet()));

        Assert.assertFalse(integrationTrackingService.updateESIndexedAt(company, ingestedAt1));

        integrationTracker1 = integrationTrackingService.get(company,integrationId1.toString()).orElse(null);
        verify(integrationTracker1, ingestedAt1, ingestedAt2, 0l);
        integrationTracker2 = integrationTrackingService.get(company,integrationId2.toString()).orElse(null);
        verify(integrationTracker2, ingestedAt1, ingestedAt3, 0l);
        integrationTracker3 = integrationTrackingService.get(company,integrationId3.toString()).orElse(null);
        verify(integrationTracker3, ingestedAt1, 0l, 0l);
        mismatchedTrackers = integrationTrackingService.getDBESMismatchedTrackers(company, oldestIngestedAt);
        Assert.assertEquals(2, mismatchedTrackers.size());
        Assert.assertEquals(Set.of("jira", "azure_devops"), mismatchedTrackers.stream().map(IntegrationTrackerEx::getApplication).collect(Collectors.toSet()));
        Assert.assertEquals(Set.of(1,2), mismatchedTrackers.stream().map(IntegrationTrackerEx::getIntegrationId).collect(Collectors.toSet()));

        Assert.assertFalse(integrationTrackingService.updateESIndexedAt(company, ingestedAt4));

        integrationTracker1 = integrationTrackingService.get(company,integrationId1.toString()).orElse(null);
        verify(integrationTracker1, ingestedAt1, ingestedAt2, 0l);
        integrationTracker2 = integrationTrackingService.get(company,integrationId2.toString()).orElse(null);
        verify(integrationTracker2, ingestedAt1, ingestedAt3, 0l);
        integrationTracker3 = integrationTrackingService.get(company,integrationId3.toString()).orElse(null);
        verify(integrationTracker3, ingestedAt1, 0l, 0l);
        mismatchedTrackers = integrationTrackingService.getDBESMismatchedTrackers(company, oldestIngestedAt);
        Assert.assertEquals(2, mismatchedTrackers.size());
        Assert.assertEquals(Set.of("jira", "azure_devops"), mismatchedTrackers.stream().map(IntegrationTrackerEx::getApplication).collect(Collectors.toSet()));
        Assert.assertEquals(Set.of(1,2), mismatchedTrackers.stream().map(IntegrationTrackerEx::getIntegrationId).collect(Collectors.toSet()));


        Assert.assertTrue(integrationTrackingService.updateESIndexedAt(company, ingestedAt2));

        integrationTracker1 = integrationTrackingService.get(company,integrationId1.toString()).orElse(null);
        verify(integrationTracker1, ingestedAt2, ingestedAt2, ingestedAt2);
        integrationTracker2 = integrationTrackingService.get(company,integrationId2.toString()).orElse(null);
        verify(integrationTracker2, ingestedAt1, ingestedAt3, 0l);
        integrationTracker3 = integrationTrackingService.get(company,integrationId3.toString()).orElse(null);
        verify(integrationTracker3, ingestedAt1, 0l, 0l);
        mismatchedTrackers = integrationTrackingService.getDBESMismatchedTrackers(company, oldestIngestedAt);
        Assert.assertTrue(CollectionUtils.isNotEmpty(mismatchedTrackers));
        Assert.assertEquals(1, mismatchedTrackers.size());
        Assert.assertEquals(Set.of("jira"), mismatchedTrackers.stream().map(IntegrationTrackerEx::getApplication).collect(Collectors.toSet()));
        Assert.assertEquals(Set.of(2), mismatchedTrackers.stream().map(IntegrationTrackerEx::getIntegrationId).collect(Collectors.toSet()));

        Assert.assertTrue(integrationTrackingService.updateESIndexedAt(company, ingestedAt3));

        integrationTracker1 = integrationTrackingService.get(company,integrationId1.toString()).orElse(null);
        verify(integrationTracker1, ingestedAt2, ingestedAt2, ingestedAt2);
        integrationTracker2 = integrationTrackingService.get(company,integrationId2.toString()).orElse(null);
        verify(integrationTracker2, ingestedAt3, ingestedAt3, ingestedAt3);
        integrationTracker3 = integrationTrackingService.get(company,integrationId3.toString()).orElse(null);
        verify(integrationTracker3, ingestedAt1, 0l, 0l);
    }
}