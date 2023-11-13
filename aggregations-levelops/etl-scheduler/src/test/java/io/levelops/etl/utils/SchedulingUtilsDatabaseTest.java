package io.levelops.etl.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.aggregation_shared.test_utils.JobInstanceTestUtils;
import io.levelops.aggregations_shared.database.JobDefinitionDatabaseService;
import io.levelops.aggregations_shared.database.JobInstanceDatabaseService;
import io.levelops.aggregations_shared.database.models.DbJobDefinition;
import io.levelops.aggregations_shared.database.models.DbJobInstanceFilter;
import io.levelops.aggregations_shared.utils.IngestionResultPayloadUtils;
import io.levelops.commons.databases.services.DatabaseSchemaService;
import io.levelops.commons.etl.models.DbJobInstance;
import io.levelops.commons.etl.models.JobInstancePayload;
import io.levelops.commons.etl.models.JobStatus;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.etl.parameter_suppliers.JobDefinitionParameterSupplierRegistry;
import io.levelops.ingestion.services.ControlPlaneService;
import io.levelops.services.GcsStorageService;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import static io.levelops.aggregation_shared.test_utils.JobInstanceTestUtils.createJobDefinition;
import static io.levelops.aggregations_shared.database.EtlDatabaseConstants.ETL_SCHEMA;
import static io.levelops.aggregations_shared.database.JobInstanceDatabaseService.JOB_INSTANCE_TABLE;
import static org.assertj.core.api.Assertions.assertThat;

public class SchedulingUtilsDatabaseTest {
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    private static DataSource dataSource;
    private static ObjectMapper objectMapper;
    private static JobDefinitionDatabaseService jobDefinitionDatabaseService;
    private static JobInstanceDatabaseService jobInstanceDatabaseService;

    @Mock
    private static ControlPlaneService controlPlaneService;

    @Mock
    private static JobDefinitionParameterSupplierRegistry registry;

    @Mock
    private static GcsStorageService gcsStorageService;

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
        jobDefinition = jobDefinition.toBuilder().id(jobDefinitionId).build();
    }

    @Before
    public void setupBefore() {
        MockitoAnnotations.openMocks(this);
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

    private JobInstancePayload createPayload(List<String> jobIntanceIds) {
        return JobInstanceTestUtils.createPayloadFromIngestionJobIds(jobIntanceIds);
    }


    // Creates the instances and adds in the job instance id to the data structure
    // for easy validations
    private List<DbJobInstance> insert(List<DbJobInstance> instances) {
        return JobInstanceTestUtils.insert(instances, jobInstanceDatabaseService);
    }

    private void assertEqualsJobInstances(List<DbJobInstance> retrievedInstances, List<DbJobInstance> expectedInstances) {
        assertThat(retrievedInstances.size()).isEqualTo(expectedInstances.size());
        for (int i = 0; i < retrievedInstances.size(); i++) {
            assertEquals(retrievedInstances.get(i), expectedInstances.get(i));
        }
    }

    public void testStreamJobInstancesSinceLastFullInternal(List<Boolean> fullList, List<Integer> expectedIndices) throws SQLException {
        var insertedInstances = insert(fullList.stream().map(full ->
                createInstance(DbJobInstance.builder().jobDefinitionId(jobDefinitionId).isFull(full).build())).toList());

        IngestionResultPayloadUtils ingestionResultPayloadUtils = new IngestionResultPayloadUtils(
                objectMapper,
                controlPlaneService,
                jobInstanceDatabaseService,
                jobDefinitionDatabaseService,
                gcsStorageService);
        SchedulingUtils schedulingUtils = new SchedulingUtils(jobInstanceDatabaseService, registry, ingestionResultPayloadUtils);
        var stream = jobInstanceDatabaseService.stream(DbJobInstanceFilter.builder()
                .jobDefinitionIds(List.of(jobDefinitionId))
                .jobStatuses(List.of(JobStatus.PENDING))
                .orderBy(List.of(DbJobInstanceFilter.JobInstanceOrderByField.builder()
                        .orderByColumn(DbJobInstanceFilter.JobInstanceOrderBy.INSTANCE_ID)
                        .isAscending(false)
                        .build()))
                .build()
        );
        var retrievedInstances = ingestionResultPayloadUtils.streamJobInstancesFromLastFull(stream).toList();
        var expectedInstances = expectedIndices.stream().map(insertedInstances::get).toList();
        assertThat(retrievedInstances.size()).isEqualTo(expectedInstances.size());
        assertEqualsJobInstances(retrievedInstances, expectedInstances);
        resetDb();
    }

    @Test
    public void testStreamJobInstancesSinceLastFull() throws SQLException {
        testStreamJobInstancesSinceLastFullInternal(List.of(false, false, true, false, false, false, false), List.of(6, 5, 4, 3, 2));
        testStreamJobInstancesSinceLastFullInternal(List.of(true, false, false, false), List.of(3, 2, 1, 0));
        testStreamJobInstancesSinceLastFullInternal(List.of(true, true, false, false, false), List.of(4, 3, 2, 1));
        testStreamJobInstancesSinceLastFullInternal(List.of(false, true, false, true, true, false, false, false), List.of(7, 6, 5, 4));
        testStreamJobInstancesSinceLastFullInternal(List.of(false, false, false), List.of(2, 1, 0));
        testStreamJobInstancesSinceLastFullInternal(List.of(false, false, true), List.of(2));
        testStreamJobInstancesSinceLastFullInternal(List.of(false), List.of(0));
        testStreamJobInstancesSinceLastFullInternal(List.of(true), List.of(0));
        testStreamJobInstancesSinceLastFullInternal(List.of(), List.of());
    }
}
