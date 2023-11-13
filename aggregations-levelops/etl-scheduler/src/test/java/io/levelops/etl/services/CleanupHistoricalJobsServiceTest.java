package io.levelops.etl.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.aggregation_shared.test_utils.JobInstanceTestUtils;
import io.levelops.aggregations_shared.database.JobDefinitionDatabaseService;
import io.levelops.aggregations_shared.database.JobInstanceDatabaseService;
import io.levelops.aggregations_shared.database.models.DbJobDefinition;
import io.levelops.commons.etl.models.DbJobInstance;
import io.levelops.aggregations_shared.database.models.DbJobInstanceFilter;
import io.levelops.commons.databases.services.DatabaseSchemaService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static io.levelops.aggregation_shared.test_utils.JobInstanceTestUtils.createJobDefinition;
import static io.levelops.aggregations_shared.database.EtlDatabaseConstants.ETL_SCHEMA;
import static io.levelops.aggregations_shared.database.JobInstanceDatabaseService.JOB_INSTANCE_TABLE;
import static org.assertj.core.api.Assertions.assertThat;

public class CleanupHistoricalJobsServiceTest {
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    private static DataSource dataSource;
    private static ObjectMapper objectMapper;
    private static JobDefinitionDatabaseService jobDefinitionDatabaseService;
    private static JobInstanceDatabaseService jobInstanceDatabaseService;

    private static DbJobDefinition jobDefinition;
    private static UUID jobDefinitionId;

    @BeforeClass
    public static void setup() throws SQLException, JsonProcessingException {
        objectMapper = DefaultObjectMapper.get();
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        DatabaseSchemaService schemaService = new DatabaseSchemaService(dataSource);
        schemaService.ensureSchemaExistence(ETL_SCHEMA);
        jobDefinitionDatabaseService = new JobDefinitionDatabaseService(objectMapper, dataSource);
        jobInstanceDatabaseService = new JobInstanceDatabaseService(objectMapper, dataSource, jobDefinitionDatabaseService);
        jobDefinitionDatabaseService.ensureTableExistence();
        jobInstanceDatabaseService.ensureTableExistence();

        jobDefinition = createJobDefinition(null, null, null, null);
        jobDefinitionId = jobDefinitionDatabaseService.insert(jobDefinition);
    }

    @Before
    public void resetDb() throws SQLException {
        String sql = "DELETE FROM " + JOB_INSTANCE_TABLE + ";";
        dataSource.getConnection().prepareStatement(sql).execute();
    }

    private DbJobInstance createInstance(DbJobInstance instance) {
        return JobInstanceTestUtils.createInstance(instance, jobDefinitionId);
    }

    private void assertEquals(DbJobInstance j1, DbJobInstance expected) {
        JobInstanceTestUtils.assertEquals(j1, expected);
    }

    // Creates the instances and adds in the job instance id to the data structure
    // for easy validations
    private List<DbJobInstance> insert(List<DbJobInstance> instances) {
        return JobInstanceTestUtils.insert(instances, jobInstanceDatabaseService);
    }

    @Test
    public void testDeleteOldJobs() {
        Instant t0 = Instant.now().minus(10, ChronoUnit.DAYS);
        Instant t1 = t0.plus(1, ChronoUnit.DAYS);
        Instant t2 = t1.plus(1, ChronoUnit.DAYS);
        Instant t6 = t0.plus(6, ChronoUnit.DAYS);
        Instant t7 = t0.plus(7, ChronoUnit.DAYS);


        var insertedInstances = insert(List.of(
                createInstance(DbJobInstance.builder().jobDefinitionId(jobDefinitionId).createdAt(t0).build()),
                createInstance(DbJobInstance.builder().jobDefinitionId(jobDefinitionId).createdAt(t1).build()),
                createInstance(DbJobInstance.builder().jobDefinitionId(jobDefinitionId).createdAt(t2).build()),
                createInstance(DbJobInstance.builder().jobDefinitionId(jobDefinitionId).createdAt(t6).build()),
                createInstance(DbJobInstance.builder().jobDefinitionId(jobDefinitionId).createdAt(t7).build())));

        CleanupHistoricalJobsService cleanupService = new CleanupHistoricalJobsService(
                jobInstanceDatabaseService,
                0,
                5
        );
        int deleted = cleanupService.deleteOldJobInstances();
        assertThat(deleted).isEqualTo(3);
        var retrievedInstances = jobInstanceDatabaseService.stream(DbJobInstanceFilter.builder()
                .orderBy(List.of(DbJobInstanceFilter.JobInstanceOrderByField.builder()
                        .orderByColumn(DbJobInstanceFilter.JobInstanceOrderBy.CREATED_AT)
                        .isAscending(true)
                        .build()))
                .build()).toList();
        assertEquals(retrievedInstances.get(0), insertedInstances.get(3));
        assertEquals(retrievedInstances.get(1), insertedInstances.get(4));
    }
}