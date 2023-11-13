package io.levelops.etl.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.aggregations_shared.database.JobDefinitionDatabaseService;
import io.levelops.aggregations_shared.database.JobInstanceDatabaseService;
import io.levelops.aggregations_shared.database.models.DbJobDefinition;
import io.levelops.aggregations_shared.database.models.DbJobDefinitionUpdate;
import io.levelops.aggregations_shared.database.models.DbJobInstanceUpdate;
import io.levelops.commons.databases.services.DatabaseSchemaService;
import io.levelops.commons.etl.models.DbJobInstance;
import io.levelops.commons.etl.models.JobPriority;
import io.levelops.commons.etl.models.JobStatus;
import io.levelops.commons.etl.models.JobType;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.util.UUID;

import static io.levelops.aggregations_shared.database.EtlDatabaseConstants.ETL_SCHEMA;
import static org.assertj.core.api.Assertions.assertThat;

public class JobMonitorServiceTest {
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    private static DataSource dataSource;
    private static ObjectMapper objectMapper;
    private static JobDefinitionDatabaseService jobDefinitionDatabaseService;
    private static JobInstanceDatabaseService jobInstanceDatabaseService;

    MeterRegistry meterRegistry;
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
        jobInstanceDatabaseService = new JobInstanceDatabaseService(objectMapper, dataSource, jobDefinitionDatabaseService);
        jobInstanceDatabaseService.ensureTableExistence();
        meterRegistry = new SimpleMeterRegistry();
    }


    @Test
    public void testDisabledJobDefinitions() throws IOException {
        JobMonitorService jobMonitorService = new JobMonitorService(
                jobInstanceDatabaseService,
                jobDefinitionDatabaseService,
                meterRegistry,
                0,
                1
        );
        DbJobDefinition jobDefinition = DbJobDefinition.builder()
                .tenantId("sid")
                .integrationId("1")
                .integrationType("jira")
                .ingestionTriggerId(UUID.randomUUID().toString())
                .jobType(JobType.INGESTION_RESULT_PROCESSING_JOB)
                .isActive(false)
                .defaultPriority(JobPriority.HIGH)
                .attemptMax(10)
                .retryWaitTimeInMinutes(11)
                .timeoutInMinutes(12L)
                .frequencyInMinutes(13)
                .fullFrequencyInMinutes(14)
                .aggProcessorName("jira")
                .lastIterationTs(null)
                .metadata(null)
                .build();



        var jobDefintionId = jobDefinitionDatabaseService.insert(jobDefinition);
        DbJobInstance jobInstance = DbJobInstance.builder()
                .jobDefinitionId(jobDefintionId)
                .priority(JobPriority.HIGH)
                .id(UUID.randomUUID())
                .aggProcessorName("TestAggProcessor")
                .status(JobStatus.SCHEDULED)
                .build();
        var jobInstanceId = jobInstanceDatabaseService.insert(jobInstance);
        jobMonitorService.monitorDisabledJobDefinitions();

        var updatedJobInstance = jobInstanceDatabaseService.get(jobInstanceId).get();
        assertThat(updatedJobInstance.getStatus()).isEqualTo(JobStatus.CANCELED);


        // Set isActive to true
        jobDefinitionDatabaseService.update(DbJobDefinitionUpdate.builder()
                .whereClause(DbJobDefinitionUpdate.WhereClause.builder()
                        .tenantId(jobDefinition.getTenantId())
                        .integrationId(jobDefinition.getIntegrationId())
                        .build())
                .isActive(true)
                .build());

        // Set jobInstance status to SCHEDULED
        jobInstanceDatabaseService.update(jobInstanceId, DbJobInstanceUpdate.builder()
                .jobStatus(JobStatus.SCHEDULED)
                .build());

        // Run the monitor again
        jobMonitorService.monitorDisabledJobDefinitions();

        updatedJobInstance = jobInstanceDatabaseService.get(jobInstanceId).get();
        assertThat(updatedJobInstance.getStatus()).isEqualTo(JobStatus.SCHEDULED);
    }
}