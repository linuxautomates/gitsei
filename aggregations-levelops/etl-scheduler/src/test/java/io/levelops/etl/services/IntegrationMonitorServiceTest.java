package io.levelops.etl.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.aggregations_shared.database.JobDefinitionDatabaseService;
import io.levelops.aggregations_shared.database.JobInstanceDatabaseService;
import io.levelops.aggregations_shared.database.models.DbJobDefinition;
import io.levelops.aggregations_shared.database.models.DbJobDefinitionFilter;
import io.levelops.aggregations_shared.models.SnapshottingSettings;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.Tenant;
import io.levelops.commons.databases.services.DatabaseSchemaService;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.IntegrationTrackingService;
import io.levelops.commons.etl.models.JobType;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.exceptions.InventoryException;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.etl.parameter_suppliers.JiraParameterSupplier;
import io.levelops.etl.parameter_suppliers.JobDefinitionParameterSupplierRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import lombok.NonNull;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;

import static io.levelops.aggregations_shared.database.EtlDatabaseConstants.ETL_SCHEMA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class IntegrationMonitorServiceTest {
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    private static DataSource dataSource;
    private static ObjectMapper objectMapper;
    private static JobDefinitionDatabaseService jobDefinitionDatabaseService;

    MeterRegistry meterRegistry;

    @Mock
    InventoryService inventoryService;

    @Mock
    JobDefinitionParameterSupplierRegistry registry;

    @Mock
    IntegrationService integrationService;
    @Mock
    IntegrationTrackingService integrationTrackingService;
    @Mock
    JobInstanceDatabaseService jobInstanceDatabaseService;

    @Before
    public void setup() throws SQLException {
        MockitoAnnotations.initMocks(this);
        objectMapper = DefaultObjectMapper.get();
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        DatabaseSchemaService schemaService = new DatabaseSchemaService(dataSource);
        schemaService.ensureSchemaExistence(ETL_SCHEMA);
        jobDefinitionDatabaseService = new JobDefinitionDatabaseService(objectMapper, dataSource);
        jobDefinitionDatabaseService.ensureTableExistence();
        meterRegistry = new SimpleMeterRegistry();
    }


    @Test
    public void testSyncJobDefinitions() throws InventoryException, JsonProcessingException {
        // Setup:
        // 1. Two tenants: warriors and lakers
        // 2. Warriors has two integrations: jira1 and jira2
        // 3. Lakers has no integrations
        // 4. 2 tenant level jobs configured
        // 5. 2 integration level jobs configured
        when(registry.getGenericTenantJobSuppliers(any())).thenReturn(List.of(
                new JiraParameterSupplier(integrationService, integrationTrackingService, jobInstanceDatabaseService, SnapshottingSettings.builder().build()) {
                    @Override
                    public JobType getJobType() {
                        return JobType.GENERIC_TENANT_JOB;
                    }

                    @Override
                    public @NonNull String getEtlProcessorName() {
                        return "jira1";
                    }
                },
                new JiraParameterSupplier(integrationService, integrationTrackingService, jobInstanceDatabaseService, SnapshottingSettings.builder().build()) {
                    @Override
                    public JobType getJobType() {
                        return JobType.GENERIC_TENANT_JOB;
                    }

                    @Override
                    public @NonNull String getEtlProcessorName() {
                        return "jira2";
                    }
                }
        ));
        when(registry.getGenericIntegrationJobParameterSupplier(any(), any(), any())).thenReturn(List.of(
                new JiraParameterSupplier(integrationService, integrationTrackingService, jobInstanceDatabaseService, SnapshottingSettings.builder().build()) {
                    @Override
                    public JobType getJobType() {
                        return JobType.GENERIC_INTEGRATION_JOB;
                    }

                    @Override
                    public @NonNull String getEtlProcessorName() {
                        return "jira3";
                    }
                },
                new JiraParameterSupplier(integrationService, integrationTrackingService, jobInstanceDatabaseService, SnapshottingSettings.builder().build()) {
                    @Override
                    public JobType getJobType() {
                        return JobType.GENERIC_INTEGRATION_JOB;
                    }

                    @Override
                    public @NonNull String getEtlProcessorName() {
                        return "jira4";
                    }
                }
        ));

        when(inventoryService.listTenants()).thenReturn(List.of(
                Tenant.builder()
                        .id("warriors")
                        .tenantName("warriors")
                        .build(),
                Tenant.builder()
                        .id("lakers")
                        .tenantName("lakersSuck")
                        .build()));

        when(inventoryService.listIntegrations("warriors")).thenReturn(
                DbListResponse.<Integration>builder()
                        .records(List.of(
                                Integration.builder()
                                        .id("jira1")
                                        .name("jira1")
                                        .build(),
                                Integration.builder()
                                        .id("jira2")
                                        .name("jira2")
                                        .build()
                        ))
                        .build()
        );
        when(inventoryService.listIntegrations("lakers")).thenReturn(
                DbListResponse.<Integration>builder()
                        .records(List.of())
                        .build()
        );
        IntegrationMonitorService integrationMonitorService = new IntegrationMonitorService(
                jobDefinitionDatabaseService,
                registry,
                inventoryService,
                meterRegistry,
                0,
                0
        );
        integrationMonitorService.syncJobs();
        var warriorsJobs = jobDefinitionDatabaseService.stream(DbJobDefinitionFilter.builder()
                        .tenantIds(List.of("warriors"))
                        .build())
                .toList();
        var lakersJobs = jobDefinitionDatabaseService.stream(DbJobDefinitionFilter.builder()
                        .tenantIds(List.of("lakers"))
                        .build())
                .toList();

        // 2 tenant level jobs + 2 * 2 integration level jobs
        assertThat(warriorsJobs.size()).isEqualTo(6);
        var warriorsTenantJobs = filterTenantJob(warriorsJobs);
        var warriorsIntegrationJobs = filterIntegrationJob(warriorsJobs);
        assertThat(warriorsTenantJobs.size()).isEqualTo(2);
        assertThat(warriorsIntegrationJobs.size()).isEqualTo(4);
        assertThat(warriorsIntegrationJobs.stream().map(DbJobDefinition::getIntegrationId).toList())
                .containsExactlyInAnyOrder("jira1", "jira1", "jira2", "jira2");

        // 2 tenant level jobs
        assertThat(lakersJobs.size()).isEqualTo(2);
        var lakersTenantJobs = filterTenantJob(lakersJobs);
        var lakersIntegrationJobs = filterIntegrationJob(lakersJobs);
        assertThat(lakersTenantJobs.size()).isEqualTo(2);
        assertThat(lakersIntegrationJobs.size()).isEqualTo(0);

        var totalJobs = jobDefinitionDatabaseService.stream(DbJobDefinitionFilter.builder().build()).toList();

        // Run syncjobs again and nothing should change this time around
        integrationMonitorService.syncJobs();
        var totalJobsAfter = jobDefinitionDatabaseService.stream(DbJobDefinitionFilter.builder().build()).toList();
        assertThat(totalJobsAfter).containsExactlyInAnyOrderElementsOf(totalJobs);

        // Now let's say one of the tenants is deleted
        when(inventoryService.listTenants()).thenReturn(List.of(
                Tenant.builder()
                        .id("warriors")
                        .tenantName("warriors")
                        .build()));
        integrationMonitorService.syncJobs();
        // Check that the lakers jobs are now disabled
        var lakersJobsAfter = jobDefinitionDatabaseService.stream(DbJobDefinitionFilter.builder()
                        .tenantIds(List.of("lakers"))
                        .build())
                .toList();
        assertThat(lakersJobsAfter.stream().map(DbJobDefinition::getIsActive).toList())
                .containsExactlyInAnyOrder(false, false);


        // Now let's delete one of the integrations for the warriors tenant
        when(inventoryService.listIntegrations("warriors")).thenReturn(
                DbListResponse.<Integration>builder()
                        .records(List.of(
                                Integration.builder()
                                        .id("jira1")
                                        .name("jira1")
                                        .build()
                        ))
                        .build()
        );
        // Check that the jira2 jobs are now disabled
        integrationMonitorService.syncJobs();
        var warriorsJobsAfter = jobDefinitionDatabaseService.stream(DbJobDefinitionFilter.builder()
                        .tenantIds(List.of("warriors"))
                        .build())
                .toList();
        assertThat(warriorsJobsAfter.stream()
                .filter(j -> j.getJobType().equals(JobType.GENERIC_INTEGRATION_JOB))
                .filter(j -> j.getIntegrationId().equals("jira2"))
                .map(DbJobDefinition::getIsActive).toList()).containsExactly(false, false);
        assertThat(warriorsJobsAfter.stream()
                .filter(j -> j.getJobType().equals(JobType.GENERIC_INTEGRATION_JOB))
                .filter(j -> j.getIntegrationId().equals("jira1"))
                .map(DbJobDefinition::getIsActive).toList()).containsExactly(true, true);


        // Now let's delete one of the suppliers
        when(registry.getGenericTenantJobSuppliers(any())).thenReturn(List.of(
                new JiraParameterSupplier(integrationService, integrationTrackingService, jobInstanceDatabaseService, SnapshottingSettings.builder().build()) {
                    @Override
                    public JobType getJobType() {
                        return JobType.GENERIC_TENANT_JOB;
                    }

                    @Override
                    public @NonNull String getEtlProcessorName() {
                        return "jira1";
                    }
                }
        ));
        // Check the jira2 tenant level job is now disabled
        integrationMonitorService.syncJobs();
        var warriorsJobsAfter2 = jobDefinitionDatabaseService.stream(DbJobDefinitionFilter.builder()
                        .tenantIds(List.of("warriors"))
                        .build())
                .toList();
        assertThat(warriorsJobsAfter2.stream().filter(j -> j.getJobType().equals(JobType.GENERIC_TENANT_JOB))
                .filter(j -> j.getAggProcessorName().equals("jira2"))
                .map(DbJobDefinition::getIsActive).toList()).containsExactly(false);
    }

    private List<DbJobDefinition> filterTenantJob(List<DbJobDefinition> jobs) {
        return jobs.stream().filter(job -> job.getJobType() == JobType.GENERIC_TENANT_JOB).toList();
    }

    private List<DbJobDefinition> filterIntegrationJob(List<DbJobDefinition> jobs) {
        return jobs.stream().filter(job -> job.getJobType() == JobType.GENERIC_INTEGRATION_JOB).toList();
    }
}