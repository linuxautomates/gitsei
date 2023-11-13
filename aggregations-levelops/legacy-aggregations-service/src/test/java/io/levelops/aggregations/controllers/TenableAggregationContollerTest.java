package io.levelops.aggregations.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import io.levelops.aggregations.exceptions.AggregationFailedException;
import io.levelops.aggregations.helpers.TenableAggHelper;
import io.levelops.aggregations.helpers.TenableAggregatorService;
import io.levelops.aggregations.models.TenableAggData;
import io.levelops.aggregations.models.messages.AppAggMessage;
import io.levelops.aggregations.parsers.JobDtoParser;
import io.levelops.commons.databases.models.database.IntegrationAgg;
import io.levelops.commons.databases.models.database.IntegrationAgg.AnalyticType;
import io.levelops.commons.databases.services.AggregationsDatabaseService;
import io.levelops.commons.databases.services.IntegrationAggService;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.ingestion.models.controlplane.MultipleTriggerResults;
import io.levelops.ingestion.services.ControlPlaneService;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.postgresql.ds.PGSimpleDataSource;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TenableAggregationContollerTest {
    @Rule
    public SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    private final ObjectMapper mapper = DefaultObjectMapper.get();
    @Mock
    private Storage storage;
    @Mock
    private Storage jobDtoStorage;
    @Mock
    private IntegrationService integrationService;
    @Mock
    private ControlPlaneService controlPlaneService;
    @Mock
    private IntegrationAggService aggService;
    @Mock
    private PGSimpleDataSource dataSource;
    @Mock
    private AggregationsDatabaseService aggregationsDatabaseService;
    // @Mock
    private AggregationHelper<AppAggMessage> aggregationHelper;

    @Test
    public void test() throws SQLException, IOException, AggregationFailedException {
        MockitoAnnotations.initMocks(this);

        when(storage.readAllBytes(BlobId.of("outputBucket",
                "gcsPath")))
                .thenReturn(ResourceUtils.getResourceAsString("models/tenable-agg-yesterday.json").getBytes());
        when(jobDtoStorage.readAllBytes(BlobId.of("tenable-dev-ingestion-levelops",
                "data/tenant-tenable/integration-1/2020/05/22/job-3293026a-f8de-45cc-8f55-e501f54f61d8/vulnerability/vulnerability.0.json",
                1590122517031254L)))
                .thenReturn(ResourceUtils.getResourceAsString("models/tenable-vulnerability.json").getBytes());

        TenableAggHelper tenableAggHelper = new TenableAggHelper(new JobDtoParser(jobDtoStorage, mapper));
        TenableAggregatorService tenableAggregatorService = new TenableAggregatorService();
        MultipleTriggerResults multiResults = mapper.readValue(ResourceUtils.getResourceAsString(
                        "models/tenable-trigger-result.json"),
            MultipleTriggerResults.class);
        when(dataSource.getConnection()).thenReturn(pg.getEmbeddedPostgres().getPostgresDatabase().getConnection());

        when(aggService.listByFilter(anyString(), any(), any(), anyBoolean(), anyInt(), anyInt())).thenReturn(
            DbListResponse.of(List.of(IntegrationAgg.builder()
                .id("id")
                .createdAt(0L)
                .gcsPath("gcsPath")
                .version("version")
                .successful(true).createdAt(ZonedDateTime.ofInstant(Instant.now(), ZoneOffset.UTC).minusDays(1).toEpochSecond())
                .type(AnalyticType.TENABLE)
                .integrationIds(List.of("integ1"))
                .build()),
            1));
        AppAggMessage message = AppAggMessage.builder()
                                    .customer("test")
                                    .integrationId("1")
                                    .integrationType("integrationType")
                                    .outputBucket("outputBucket")
                                    .productId("productId")
                                    .build();
        
        aggregationHelper = new DefaultAggregationHelper<>(mapper, storage, aggService, aggregationsDatabaseService);
        
        TenableAggregationsController controller = new TenableAggregationsControllerForTesting(
                storage, integrationService, "test", aggService, aggregationsDatabaseService, mapper, controlPlaneService,
                dataSource, tenableAggHelper, tenableAggregatorService, aggregationHelper);

        TenableAggData tenableAggData = controller.runTenableAgg(message, multiResults);
        tenableAggData.setResults(multiResults);
        controller.updateTimeSeries("test", "integ1", "outputBucket", tenableAggData);
    }

    class TenableAggregationsControllerForTesting extends TenableAggregationsController {

        public TenableAggregationsControllerForTesting(Storage storage, IntegrationService integrationService,
                                                       String subscriptionName, IntegrationAggService aggService,
                                                       AggregationsDatabaseService aggregationsDatabaseService,
                                                       ObjectMapper mapper, ControlPlaneService controlPlaneService,
                                                       PGSimpleDataSource dataSource, TenableAggHelper tenableAggHelper,
                                                       TenableAggregatorService tenableAggregatorService,
                                                       AggregationHelper<AppAggMessage> aggregationHelper) {
            super(storage, integrationService, subscriptionName, aggService, aggregationsDatabaseService, mapper,
                    controlPlaneService, dataSource, tenableAggHelper, tenableAggregatorService, aggregationHelper);
        }
    }
}

