package io.levelops.aggregations.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.storage.Storage;
import io.levelops.aggregations.models.messages.AppAggMessage;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.pagerduty.DbPagerDutyService;
import io.levelops.commons.databases.services.AggregationsDatabaseService;
import io.levelops.commons.databases.services.IntegrationAggService;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.ServicesDatabaseService;
import io.levelops.commons.databases.services.pagerduty.PagerDutyAlertsDatabaseService;
import io.levelops.commons.databases.services.pagerduty.PagerDutyIncidentsDatabaseService;
import io.levelops.commons.databases.services.pagerduty.PagerDutyServicesDatabaseService;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.ingestion.exceptions.IngestionServiceException;
import io.levelops.ingestion.models.controlplane.MultipleTriggerResults;
import io.levelops.ingestion.services.ControlPlaneService;
import io.levelops.integrations.pagerduty.models.PagerDutyEntity;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.postgresql.ds.PGSimpleDataSource;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PagerDutyAggregationControllerTest {
    @Rule
    public SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    private ObjectMapper mapper = DefaultObjectMapper.get();
    @Mock
    private Storage storage;
    @Mock
    private IntegrationService integrationService;
    @Mock
    private ControlPlaneService controlPlaneService;
    @Mock
    private IntegrationAggService aggService;
    @Mock
    private AggregationsDatabaseService aggregationsDatabaseService;
    @Mock
    private PGSimpleDataSource dataSource;
    @Mock
    private AggregationHelper<AppAggMessage> aggregationHelper;
    @Mock
    private PagerDutyAlertsDatabaseService alertsService;
    @Mock
    private PagerDutyIncidentsDatabaseService incidentsService;
    @Mock
    private PagerDutyServicesDatabaseService pdServices;
    @Mock
    private ServicesDatabaseService services;
    private String company = "test";
    private String policySub = "test";
    private String projectName = "test";
    private int batchSize = 10;

    @Test
    public void test() throws IngestionServiceException, SQLException, IOException {
        MockitoAnnotations.initMocks(this);
        pdServices = Mockito.mock(PagerDutyServicesDatabaseService.class);

        MultipleTriggerResults multiResults = mapper.readValue(ResourceUtils.getResourceAsString(
            "models/pd_trigger_results.json"), 
            MultipleTriggerResults.class);
        // when(dataSource.getConnection()).thenReturn(pg.getEmbeddedPostgres().getPostgresDatabase().getConnection());
        when(integrationService.get(anyString(), anyString())).thenReturn(Optional.of(
            Integration.builder()
                .application("application")
                .createdAt(0L)
                .description("description")
                .id("1")
                .name("name")
                .status("status")
                .updatedAt(0L)
                .url("url")
                .build()));
//        when(storage.readAllBytes(eq(com.google.cloud.storage.BlobId.of(
//            "dev-ingestion-levelops",
//            "data/tenant-foo/integration-30/2020/02/26/job-c511b5d8-ab88-4b61-895f-b40decd1bdeb/alerts/alerts.0.json",
//            1582685180417131L))))
//            .thenReturn(ResourceUtils.getResourceAsString("models/pd_alerts_blob.json").getBytes());
        when(storage.readAllBytes(eq(com.google.cloud.storage.BlobId.of(
            "dev-ingestion-levelops",
            "data/tenant-foo/integration-30/2020/02/26/job-c511b5d8-ab88-4b61-895f-b40decd1bdeb/alerts/alerts.0.json",
            1582685180417131L))))
            .thenReturn(ResourceUtils.getResourceAsString("models/pd_alerts_blob.json").getBytes());
        
        when(storage.readAllBytes(eq(com.google.cloud.storage.BlobId.of(
            "dev-ingestion-levelops", 
            "data/tenant-foo/integration-30/2020/02/26/job-c511b5d8-ab88-4b61-895f-b40decd1bdeb/incidents/incidents.0.json", 
            1582685179811986L))))
            .thenReturn(ResourceUtils.getResourceAsString("models/pd_incidents_blob.json").getBytes());
        
        when(storage.readAllBytes(eq(com.google.cloud.storage.BlobId.of(
            "dev-ingestion-levelops", 
            "data/tenant-foo/integration-30/2020/02/26/job-c511b5d8-ab88-4b61-895f-b40decd1bdeb/services/services.0.json", 
            1582685179811987L)))
            )
            .thenReturn(ResourceUtils.getResourceAsString("models/pd_services_blob.json").getBytes());
        
        // when(storage.create(any(BlobInfo.class), any(byte[].class))).thenReturn(null);
        when(controlPlaneService.getAllTriggerResults(any(IntegrationKey.class), anyBoolean(), anyBoolean(), anyBoolean()))
            .thenReturn(multiResults);
        // when(aggService.listByFilter(anyString(), anyString(), any(), any(), anyBoolean(), anyInt(), anyInt())).thenReturn(
        //         DbListResponse.of(List.of(), 0)
        // );
        when(services.insert(eq(company), any())).thenReturn(UUID.randomUUID().toString());

        UUID serviceId = UUID.randomUUID();
        when(pdServices.getByPagerDutyId(eq(company), anyInt(), anyString())).thenReturn(Optional.of(DbPagerDutyService.builder().id(serviceId).build()));
        // when(pdServices.insert(eq(company), any())).thenReturn(UUID.randomUUID().toString());
        // when(incidentsService.insert(eq(company), any())).thenReturn(UUID.randomUUID().toString());
        // when(alertsService.insert(eq(company), any())).thenReturn(UUID.randomUUID().toString());

        AppAggMessage message = AppAggMessage.builder()
                                    .customer("test")
                                    .integrationId("1")
                                    .integrationType("integrationType")
                                    .outputBucket("outputBucket")
                                    .productId("productId")
                                    .build();
        
        PagerDutyAggregationController controller = new PagerDutyAggregationControllerForTesting(
            mapper, 
            storage, 
            policySub, 
            projectName, 
            integrationService, 
            controlPlaneService, 
            aggService,
            aggregationsDatabaseService,
            batchSize, 
            dataSource,
            aggregationHelper,
            alertsService,
            incidentsService,
            pdServices,
            services);
        
        Stream<PagerDutyEntity> stream = PagerDutyAggregationController.getPagerDutyEntitiesStream(
            mapper, 
            storage,
            multiResults.getTriggerResults().stream().flatMap(result -> result.getJobs().stream()));
        List<PagerDutyEntity> entities = stream.collect(Collectors.toList());
        Assert.assertEquals(14, entities.size());
        // controller.doTask(message);

        controller.doTask(message);
    }

    class PagerDutyAggregationControllerForTesting extends PagerDutyAggregationController {

        public PagerDutyAggregationControllerForTesting(ObjectMapper mapper, Storage storage, String policySub,
                                                        String projectName, IntegrationService integrationService, ControlPlaneService controlPlaneService,
                                                        IntegrationAggService integrationAggService, AggregationsDatabaseService aggregationsDatabaseService,
                                                        int batchSize, PGSimpleDataSource dataSource, AggregationHelper<AppAggMessage> aggregationHelper,
                                                        PagerDutyAlertsDatabaseService alertsService,
                                                        PagerDutyIncidentsDatabaseService incidentsService,
                                                        PagerDutyServicesDatabaseService pdServices,
                                                        ServicesDatabaseService services) {
            super(mapper, storage, policySub, projectName, integrationService, controlPlaneService, integrationAggService,
                    aggregationsDatabaseService, batchSize, dataSource, 30, aggregationHelper, alertsService, incidentsService, pdServices, services);
            
        }
    }
}