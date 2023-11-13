package io.levelops.aggregations_shared.database;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.aggregation_shared.test_utils.JobInstanceTestUtils;
import io.levelops.aggregations_shared.database.models.DbJobDefinition;
import io.levelops.aggregations_shared.database.models.DbJobInstanceDelete;
import io.levelops.aggregations_shared.database.models.DbJobInstanceFilter;
import io.levelops.aggregations_shared.database.models.DbJobInstanceUpdate;
import io.levelops.commons.databases.services.DatabaseSchemaService;
import io.levelops.commons.etl.models.DbJobInstance;
import io.levelops.commons.etl.models.JobInstanceId;
import io.levelops.commons.etl.models.JobMetadata;
import io.levelops.commons.etl.models.JobPriority;
import io.levelops.commons.etl.models.JobStatus;
import io.levelops.commons.etl.models.job_progress.EntityProgressDetail;
import io.levelops.commons.etl.models.job_progress.FileProgressDetail;
import io.levelops.commons.etl.models.job_progress.StageProgressDetail;
import io.levelops.commons.functional.IterableUtils;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.MapUtils;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.lang3.BooleanUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static io.levelops.aggregations_shared.database.EtlDatabaseConstants.ETL_SCHEMA;
import static io.levelops.aggregations_shared.database.JobDefinitionDatabaseServiceTest.createJobDefinition;
import static io.levelops.aggregations_shared.database.JobInstanceDatabaseService.JOB_INSTANCE_TABLE;
import static org.assertj.core.api.Assertions.assertThat;

public class JobInstanceDatabaseServiceTest {
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    private static DataSource dataSource;
    private static ObjectMapper objectMapper;
    private static JobDefinitionDatabaseService jobDefinitionDatabaseService;
    private static JobInstanceDatabaseService jobInstanceDatabaseService;

    private static DbJobDefinition jobDefinition;
    private static DbJobDefinition jobDefinition2;
    private static UUID jobDefinitionId;
    private static UUID jobDefinitionId2;

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

        jobDefinition = createJobDefinition(null, null, null, null, null);
        jobDefinitionId = jobDefinitionDatabaseService.insert(jobDefinition);
        jobDefinition2 = createJobDefinition(null, null, null, null, null);
        jobDefinitionId2 = jobDefinitionDatabaseService.insert(jobDefinition);
    }

    @Before
    public void resetDb() throws SQLException {
        String sql = "DELETE FROM " + JOB_INSTANCE_TABLE + ";";
        dataSource.getConnection().prepareStatement(sql).execute();
    }

    //    private DbJobInstance createInstance(UUID definitionId, Instant scheduledStartTime, JobStatus jobStatus) {
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
    public void testInsert() {
        var insertedInstances = insert(List.of(
                createInstance(DbJobInstance.builder().jobDefinitionId(jobDefinitionId).build()),
                createInstance(DbJobInstance.builder().jobDefinitionId(jobDefinitionId).build()),
                createInstance(DbJobInstance.builder().jobDefinitionId(jobDefinitionId).build())));

        var insertedInstances2 = insert(List.of(
                createInstance(DbJobInstance.builder().jobDefinitionId(jobDefinitionId2).build()),
                createInstance(DbJobInstance.builder().jobDefinitionId(jobDefinitionId2).build()),
                createInstance(DbJobInstance.builder().jobDefinitionId(jobDefinitionId2).build())));

        insertedInstances.forEach(insertedInstance -> {
            var retrieved = jobInstanceDatabaseService.get(insertedInstance.getJobInstanceId());
            assertEquals(insertedInstance, retrieved.get());
        });

        insertedInstances2.forEach(insertedInstance -> {
            var retrieved = jobInstanceDatabaseService.get(insertedInstance.getJobInstanceId());
            assertEquals(insertedInstance, retrieved.get());
        });
        // Ensure that the instance id is autoincrementing
        assertThat(insertedInstances.stream().map(DbJobInstance::getInstanceId).collect(Collectors.toList()))
                .containsExactly(1, 2, 3);
        assertThat(insertedInstances2.stream().map(DbJobInstance::getInstanceId).collect(Collectors.toList()))
                .containsExactly(1, 2, 3);
    }

    private void assertResultContainsIndices(List<DbJobInstance> inserted, List<DbJobInstance> result, List<Integer> expectedIndices) {
        assertThat(result.stream().map(DbJobInstance::getJobInstanceId)).containsAll(
                expectedIndices.stream().map(i -> inserted.get(i).getJobInstanceId()).collect(Collectors.toList())
        );
    }

    @Test
    public void testFilter() {
        Instant now = Instant.now();
        Instant nowPlus10 = now.plus(10, ChronoUnit.MINUTES);
        var insertedInstances = insert(List.of(
                createInstance(DbJobInstance.builder().scheduledStartTime(nowPlus10).attemptCount(11).timeoutInMinutes(0L).priority(JobPriority.HIGH).build()),                     // 0
                createInstance(DbJobInstance.builder().jobDefinitionId(jobDefinitionId2).lastHeartbeat(nowPlus10).priority(JobPriority.LOW).build()),                              // 1
                createInstance(DbJobInstance.builder().tags(Set.of("sid")).isFull(false).priority(JobPriority.HIGH).build()),                                                       // 2
                createInstance(DbJobInstance.builder().jobDefinitionId(jobDefinitionId2).status(JobStatus.SUCCESS).lastHeartbeat(nowPlus10).priority(JobPriority.MEDIUM).build()),    // 3
                createInstance(DbJobInstance.builder().tags(Set.of("sid", "test")).lastHeartbeat(now).priority(JobPriority.HIGH).build())                                           // 4
        ));
        // 0, 2, 4, 3, 1

        // -- jobInstanceId filter
        var result = jobInstanceDatabaseService.filter(0, 100, DbJobInstanceFilter.builder()
                        .jobInstanceIds(List.of(insertedInstances.get(0).getJobInstanceId(), insertedInstances.get(2).getJobInstanceId()))
                        .build())
                .getRecords();
        assertResultContainsIndices(insertedInstances, result, List.of(0, 2));


        // -- jobDefinitionId filter
        result = jobInstanceDatabaseService.filter(0, 100, DbJobInstanceFilter.builder()
                        .jobDefinitionIds(List.of(jobDefinitionId2))
                        .build())
                .getRecords();
        assertResultContainsIndices(insertedInstances, result, List.of(1, 3));

        // -- job status filter
        result = jobInstanceDatabaseService.filter(0, 100, DbJobInstanceFilter.builder()
                        .jobStatuses(List.of(JobStatus.SUCCESS))
                        .build())
                .getRecords();
        assertResultContainsIndices(insertedInstances, result, List.of(3));

        result = jobInstanceDatabaseService.filter(0, 100, DbJobInstanceFilter.builder()
                        .jobStatuses(List.of(JobStatus.PENDING))
                        .build())
                .getRecords();
        assertResultContainsIndices(insertedInstances, result, List.of(0, 1, 2, 4));

        // -- tag filter
        result = jobInstanceDatabaseService.filter(0, 100, DbJobInstanceFilter.builder()
                        .tags(List.of("sid"))
                        .build())
                .getRecords();
        assertResultContainsIndices(insertedInstances, result, List.of(2, 4));

        result = jobInstanceDatabaseService.filter(0, 100, DbJobInstanceFilter.builder()
                        .tags(List.of("sid", "test"))
                        .build())
                .getRecords();
        assertResultContainsIndices(insertedInstances, result, List.of(4));

        // -- getScheduledTimeAtOrBefore filter
        result = jobInstanceDatabaseService.filter(0, 100, DbJobInstanceFilter.builder()
                        .scheduledTimeAtOrBefore(Instant.now())
                        .build())
                .getRecords();
        assertResultContainsIndices(insertedInstances, result, List.of(1, 2, 3, 4));

        result = jobInstanceDatabaseService.filter(0, 100, DbJobInstanceFilter.builder()
                        .scheduledTimeAtOrBefore(Instant.now().plus(30, ChronoUnit.MINUTES))
                        .build())
                .getRecords();
        assertResultContainsIndices(insertedInstances, result, List.of(0, 1, 2, 3, 4));


        // -- lastHeartbeatBefore
        result = jobInstanceDatabaseService.filter(0, 100, DbJobInstanceFilter.builder()
                        .lastHeartbeatBefore(now.plus(1, ChronoUnit.MINUTES))
                        .build())
                .getRecords();
        assertResultContainsIndices(insertedInstances, result, List.of(4));

        result = jobInstanceDatabaseService.filter(0, 100, DbJobInstanceFilter.builder()
                        .lastHeartbeatBefore(Instant.now().plus(100, ChronoUnit.MINUTES))
                        .build())
                .getRecords();
        assertResultContainsIndices(insertedInstances, result, List.of(4, 1, 3));

        result = jobInstanceDatabaseService.filter(0, 100, DbJobInstanceFilter.builder()
                        .lastHeartbeatBefore(Instant.now().minus(10, ChronoUnit.MINUTES))
                        .build())
                .getRecords();
        assertResultContainsIndices(insertedInstances, result, List.of());


        // -- isFull filter
        result = jobInstanceDatabaseService.filter(0, 100, DbJobInstanceFilter.builder()
                        .isFull(false)
                        .build())
                .getRecords();
        assertResultContainsIndices(insertedInstances, result, List.of(2));

        // -- getBelowMaxAttempts
        result = jobInstanceDatabaseService.filter(0, 100, DbJobInstanceFilter.builder()
                        .belowMaxAttempts(true)
                        .build())
                .getRecords();
        assertResultContainsIndices(insertedInstances, result, List.of(1, 2, 3, 4));

        // -- timedout filer
        result = jobInstanceDatabaseService.filter(0, 100, DbJobInstanceFilter.builder()
                        .timedOut(true)
                        .build())
                .getRecords();
        assertResultContainsIndices(insertedInstances, result, List.of(0));

        // -- order by created at asc
        result = jobInstanceDatabaseService.filter(0, 100, DbJobInstanceFilter.builder()
                        .orderBy(List.of(DbJobInstanceFilter.JobInstanceOrderByField.builder()
                                .orderByColumn(DbJobInstanceFilter.JobInstanceOrderBy.CREATED_AT)
                                .isAscending(true)
                                .build()))
                        .build())
                .getRecords();
        assertResultContainsIndices(insertedInstances, result, List.of(0, 1, 2, 3, 4));

        // -- order by created at desc
        result = jobInstanceDatabaseService.filter(0, 100, DbJobInstanceFilter.builder()
                        .orderBy(List.of(DbJobInstanceFilter.JobInstanceOrderByField.builder()
                                .orderByColumn(DbJobInstanceFilter.JobInstanceOrderBy.CREATED_AT)
                                .isAscending(false)
                                .build()))
                        .build())
                .getRecords();
        assertResultContainsIndices(insertedInstances, result, List.of(4, 3, 2, 1, 0));

        // -- order by instance id
        result = jobInstanceDatabaseService.filter(0, 100, DbJobInstanceFilter.builder()
                        .orderBy(List.of(DbJobInstanceFilter.JobInstanceOrderByField.builder()
                                .orderByColumn(DbJobInstanceFilter.JobInstanceOrderBy.INSTANCE_ID)
                                .isAscending(true)
                                .build()))
                        .build())
                .getRecords();
        assertResultContainsIndices(insertedInstances, result, List.of(0, 1, 2, 3, 4));

        // -- order by priority + instance id
        result = jobInstanceDatabaseService.filter(0, 100, DbJobInstanceFilter.builder()
                        .orderBy(List.of(
                                DbJobInstanceFilter.JobInstanceOrderByField.builder()
                                        .orderByColumn(DbJobInstanceFilter.JobInstanceOrderBy.PRIORITY)
                                        .isAscending(true)
                                        .build(),
                                DbJobInstanceFilter.JobInstanceOrderByField.builder()
                                        .orderByColumn(DbJobInstanceFilter.JobInstanceOrderBy.INSTANCE_ID)
                                        .isAscending(true)
                                        .build()
                        )).build())
                .getRecords();
        assertResultContainsIndices(insertedInstances, result, List.of(0, 2, 4, 3, 1));
    }

    @Test
    public void testStreamAll() {
        var insertedInstances = insert(List.of(
                createInstance(DbJobInstance.builder().jobDefinitionId(jobDefinitionId).build()),
                createInstance(DbJobInstance.builder().jobDefinitionId(jobDefinitionId).build()),
                createInstance(DbJobInstance.builder().jobDefinitionId(jobDefinitionId).build()),
                createInstance(DbJobInstance.builder().jobDefinitionId(jobDefinitionId).build()),
                createInstance(DbJobInstance.builder().jobDefinitionId(jobDefinitionId).build()),
                createInstance(DbJobInstance.builder().jobDefinitionId(jobDefinitionId).build()),
                createInstance(DbJobInstance.builder().jobDefinitionId(jobDefinitionId).build()),
                createInstance(DbJobInstance.builder().jobDefinitionId(jobDefinitionId).build())));

        var result = jobInstanceDatabaseService.stream(DbJobInstanceFilter.builder().build());
        assertResultContainsIndices(insertedInstances, result.collect(Collectors.toList()), List.of(0, 1, 2, 3, 4, 5, 6, 7));
    }

    private DbJobInstance updateAndCompare(DbJobInstance insertedInstance, DbJobInstanceUpdate update) throws JsonProcessingException {
        var updated = jobInstanceDatabaseService.update(insertedInstance.getJobInstanceId(), update);
        assertThat(updated).isTrue();
        var updatedInstanceBuilder = insertedInstance.toBuilder();
        if (update.getJobStatus() != null) {
            updatedInstanceBuilder.status(update.getJobStatus());
        }
        if (update.getWorkerId() != null) {
            updatedInstanceBuilder.workerId(update.getWorkerId());
        }
        if (BooleanUtils.isTrue(update.getIncrementAttemptCount())) {
            updatedInstanceBuilder.attemptCount(insertedInstance.getAttemptCount() + 1);
        }
        if (update.getHeartbeat() != null) {
            updatedInstanceBuilder.lastHeartbeat(update.getHeartbeat());
        }
        if (update.getProgress() != null) {
            updatedInstanceBuilder.progress(update.getProgress());
        }
        if (update.getStartTime() != null) {
            updatedInstanceBuilder.startTime(update.getStartTime());
        }


        var updatedInstance = updatedInstanceBuilder.build();
        return getAndCompare(updatedInstance);
    }

    private DbJobInstance getAndCompare(DbJobInstance expected) {
        var retrieved = jobInstanceDatabaseService.get(expected.getJobInstanceId()).get();
        assertEquals(retrieved, expected);
        return retrieved;
    }

    @Test
    public void testUpdate() throws JsonProcessingException {
        var insertedInstances = insert(List.of(
                createInstance(DbJobInstance.builder().jobDefinitionId(jobDefinitionId).build())
        ));
        var insertedInstance = insertedInstances.get(0);

        // job status
        insertedInstance = updateAndCompare(insertedInstance, DbJobInstanceUpdate.builder()
                .jobStatus(JobStatus.FAILURE)
                .build());

        // worker id
        insertedInstance = updateAndCompare(insertedInstance, DbJobInstanceUpdate.builder()
                .workerId("abcd")
                .build());

        // increment attempt count
        insertedInstance = updateAndCompare(insertedInstance, DbJobInstanceUpdate.builder()
                .incrementAttemptCount(true)
                .build());

        // heartbeat
        insertedInstance = updateAndCompare(insertedInstance, DbJobInstanceUpdate.builder()
                .heartbeat(Instant.now())
                .build());

        // progress
        insertedInstance = updateAndCompare(insertedInstance, DbJobInstanceUpdate.builder()
                .progress(Map.of("hi", 1))
                .build());

        // start time
        insertedInstance = updateAndCompare(insertedInstance, DbJobInstanceUpdate.builder()
                .startTime(Instant.now())
                .build());
    }

    @Test
    public void testUpdateConditions() throws JsonProcessingException {
        var insertedInstances = insert(List.of(
                createInstance(DbJobInstance.builder()
                        .jobDefinitionId(jobDefinitionId)
                        .status(JobStatus.PENDING)
                        .workerId("abcd")
                        .build())
        ));
        var insertedInstance = insertedInstances.get(0);

        // This should not update because status condition is not met
        var result = jobInstanceDatabaseService.update(insertedInstance.getJobInstanceId(), DbJobInstanceUpdate.builder()
                .jobStatus(JobStatus.FAILURE)
                .statusCondition(JobStatus.FAILURE)
                .build());
        assertThat(result).isFalse();

        result = jobInstanceDatabaseService.update(insertedInstance.getJobInstanceId(), DbJobInstanceUpdate.builder()
                .statusCondition(JobStatus.PENDING)
                .jobStatus(JobStatus.FAILURE)
                .build());
        assertThat(result).isTrue();
        insertedInstance = insertedInstance.toBuilder()
                .status(JobStatus.FAILURE)
                .metadata(JobMetadata.builder().build())
                .build();
        getAndCompare(insertedInstance);

        // This should not update because status condition is not met
        result = jobInstanceDatabaseService.update(insertedInstance.getJobInstanceId(), DbJobInstanceUpdate.builder()
                .workerId("workerId")
                .workerIdCondition("workerId")
                .build());
        assertThat(result).isFalse();

        result = jobInstanceDatabaseService.update(insertedInstance.getJobInstanceId(), DbJobInstanceUpdate.builder()
                .workerIdCondition("abcd")
                .workerId("workerId")
                .build());
        assertThat(result).isTrue();
        insertedInstance = insertedInstance.toBuilder().workerId("workerId").build();
        getAndCompare(insertedInstance);
    }

    @Test
    public void testStatusUpdateTimeTrigger() throws JsonProcessingException, InterruptedException {
        var insertedInstances = insert(List.of(
                createInstance(DbJobInstance.builder()
                        .jobDefinitionId(jobDefinitionId)
                        .status(JobStatus.PENDING)
                        .build())
        ));
        var insertedInstance = insertedInstances.get(0);

        var retrievedOld = jobInstanceDatabaseService.get(insertedInstance.getJobInstanceId()).get();

        // Wait for 2 seconds before updating the status
        TimeUnit.SECONDS.sleep(2);
        var result = jobInstanceDatabaseService.update(insertedInstance.getJobInstanceId(), DbJobInstanceUpdate.builder()
                .jobStatus(JobStatus.FAILURE)
                .build());
        assertThat(result).isTrue();

        // Wait for another 2 seconds before retrieving to give the trigger time to work
        TimeUnit.SECONDS.sleep(2);
        var retrievedNew = jobInstanceDatabaseService.get(insertedInstance.getJobInstanceId()).get();
        assertThat(retrievedNew.getStatusChangedAt()).isAfter(retrievedOld.getStatusChangedAt());

        // Update something other than the status and ensure that the statusChangedAt field is not updated
        result = jobInstanceDatabaseService.update(insertedInstance.getJobInstanceId(), DbJobInstanceUpdate.builder()
                .heartbeat(Instant.now())
                .build());
        assertThat(result).isTrue();
        var retrievedNew2 = jobInstanceDatabaseService.get(insertedInstance.getJobInstanceId()).get();
        assertThat(retrievedNew2.getStatusChangedAt()).isEqualTo(retrievedNew.getStatusChangedAt());
    }

    @Test
    public void testExcludePayloadFilter() {
        var insertedInstances = insert(List.of(
                createInstance(DbJobInstance.builder()
                        .jobDefinitionId(jobDefinitionId)
                        .status(JobStatus.PENDING)
                        .build())
        ));

        var result = jobInstanceDatabaseService.filter(0, 1, DbJobInstanceFilter.builder()
                .jobInstanceIds(List.of(insertedInstances.get(0).getJobInstanceId()))
                .excludePayload(true)
                .build());

        assertThat(result.getTotalCount()).isEqualTo(1);
        assertThat(result.getRecords().get(0).getJobInstanceId())
                .isEqualTo(insertedInstances.get(0).getJobInstanceId());
        assertThat(result.getRecords().get(0).getPayload()).isEqualTo(null);
    }

    @Test
    public void testProgressDetails() throws JsonProcessingException {
        Map<String, StageProgressDetail> progressDetailMap = new HashMap<>();
        progressDetailMap.put("stage1", StageProgressDetail.builder()
                .fileProgressMap(Map.of(
                        0, FileProgressDetail.builder()
                                .entityProgressDetail(EntityProgressDetail.builder()
                                        .totalEntities(10)
                                        .successful(8)
                                        .failed(2)
                                        .build())
                                .failures(List.of())
                                .durationMilliSeconds(100L)
                                .build(),
                        1, FileProgressDetail.builder()
                                .entityProgressDetail(EntityProgressDetail.builder()
                                        .totalEntities(20)
                                        .successful(18)
                                        .failed(2)
                                        .build())
                                .failures(List.of())
                                .durationMilliSeconds(100L)
                                .build()))
                .build());

        var inserted = insert(List.of(
                createInstance(DbJobInstance.builder().progressDetails(progressDetailMap).build())
        ));

        var retrieved = jobInstanceDatabaseService.get(inserted.get(0).getJobInstanceId());
        assertThat(retrieved.get().getProgressDetails()).isEqualTo(progressDetailMap);

        // Update the progress map and ensure that it gets persisted correctly
        progressDetailMap.put("stage2", StageProgressDetail.builder()
                .fileProgressMap(Map.of(
                        0, FileProgressDetail.builder()
                                .entityProgressDetail(EntityProgressDetail.builder()
                                        .totalEntities(10)
                                        .successful(8)
                                        .failed(2)
                                        .build())
                                .failures(List.of())
                                .durationMilliSeconds(100L)
                                .build()))
                .build());
        var updated = jobInstanceDatabaseService.update(inserted.get(0).getJobInstanceId(), DbJobInstanceUpdate.builder()
                .progressDetails(progressDetailMap)
                .build());
        assertThat(updated).isTrue();

        retrieved = jobInstanceDatabaseService.get(inserted.get(0).getJobInstanceId());
        assertThat(retrieved.get().getProgressDetails()).isEqualTo(progressDetailMap);
    }

    public void testDeleteInternal(Instant t0, Instant t1, Instant t2, Instant createdAtBefore, List<Integer> expectedInstances) throws SQLException {
        var insertedInstances = insert(List.of(
                createInstance(DbJobInstance.builder().jobDefinitionId(jobDefinitionId).createdAt(t0).build()),
                createInstance(DbJobInstance.builder().jobDefinitionId(jobDefinitionId).createdAt(t1).build()),
                createInstance(DbJobInstance.builder().jobDefinitionId(jobDefinitionId).createdAt(t2).build())));

        var deletedCount = jobInstanceDatabaseService.delete(DbJobInstanceDelete.builder()
                .createdAtBefore(createdAtBefore)
                .build());
        var allInstances = jobInstanceDatabaseService.stream(
                DbJobInstanceFilter.builder()
                        .orderBy(List.of(DbJobInstanceFilter.JobInstanceOrderByField.builder()
                                .orderByColumn(DbJobInstanceFilter.JobInstanceOrderBy.CREATED_AT)
                                .isAscending(true)
                                .build())).build()).toList();
        assertThat(allInstances.size()).isEqualTo(expectedInstances.size());
        for (int i = 0; i < expectedInstances.size(); i++) {
            assertEquals(allInstances.get(0), insertedInstances.get(expectedInstances.get(0)));
        }
        resetDb();
    }

    @Test
    public void testDelete() throws SQLException {
        Instant t0 = Instant.now().minus(10, ChronoUnit.HOURS);
        Instant t1 = t0.plus(1, ChronoUnit.HOURS);
        Instant t2 = t1.plus(1, ChronoUnit.HOURS);

        testDeleteInternal(t0, t1, t2, t1.plus(1, ChronoUnit.MINUTES), List.of(2));
        testDeleteInternal(t0, t1, t2, t2.plus(1, ChronoUnit.MINUTES), List.of());
        testDeleteInternal(t0, t1, t2, t0.plus(1, ChronoUnit.MINUTES), List.of(1, 2));
        testDeleteInternal(t0, t1, t2, t0.minus(1, ChronoUnit.MINUTES), List.of(0, 1, 2));
    }

    @Test
    public void testMetadata() throws JsonProcessingException {
        String metadataJson = "{\"checkpoint\": {\"a\": \"b\"}, \"extra\": {\"c\": \"d\"}, \"field\": 123 }";

        // test deserialization
        JobMetadata jobMetadata = DefaultObjectMapper.get().readValue(metadataJson, JobMetadata.class);
        DefaultObjectMapper.prettyPrint(jobMetadata);
        assertThat(jobMetadata.getCheckpoint()).containsEntry("a", "b");
        assertThat(jobMetadata.getDynamicFields().get("extra")).isEqualTo(Map.of("c", "d"));
        assertThat(jobMetadata.getDynamicFields().get("field")).isEqualTo(123);

        // test insert
        DbJobInstance jobInstance = JobInstanceTestUtils.createInstance(DbJobInstance.builder()
                        .metadata(jobMetadata)
                        .build(),
                jobDefinitionId);
        DbJobInstance inserted = IterableUtils.getFirst(JobInstanceTestUtils.insert(jobInstanceDatabaseService, jobInstance)).orElseThrow();
        getAndCompare(inserted);

        // test update
        JobMetadata updatedMetadata = inserted.getMetadata().toBuilder()
                .checkpoint(MapUtils.append(inserted.getMetadata().getCheckpoint(), "updated", "value"))
                .build();
        DefaultObjectMapper.prettyPrint(updatedMetadata);

        // check that toBuilder preserves dynamic fields
        assertThat(jobMetadata.getDynamicFields().get("extra")).isEqualTo(Map.of("c", "d"));

        jobInstanceDatabaseService.update(inserted.getJobInstanceId(), DbJobInstanceUpdate.builder()
                .metadata(updatedMetadata)
                .build());
        getAndCompare(inserted.toBuilder()
                .metadata(updatedMetadata)
                .build());
    }

    @Test
    public void testPayloadUrl() throws JsonProcessingException {
        DbJobInstance jobInstance = JobInstanceTestUtils.createInstance(DbJobInstance.builder()
                        .payloadGcsFilename("http://example.com")
                        .build(),
                jobDefinitionId);
        DbJobInstance inserted = IterableUtils.getFirst(JobInstanceTestUtils.insert(jobInstanceDatabaseService, jobInstance)).orElseThrow();
        getAndCompare(inserted);

        // test update
        jobInstanceDatabaseService.update(inserted.getJobInstanceId(), DbJobInstanceUpdate.builder()
                .payloadGcsFileName("http://example.com/updated")
                .build());
        getAndCompare(inserted.toBuilder()
                .payloadGcsFilename("http://example.com/updated")
                .build());
    }

    @Test
    public void testIsReprocessing() throws IOException {
        DbJobInstance jobInstance = JobInstanceTestUtils.createInstance(DbJobInstance.builder()
                        .isReprocessing(true)
                        .build(),
                jobDefinitionId);
        JobInstanceId id = jobInstanceDatabaseService.insert(jobInstance);
        DbJobInstance output = jobInstanceDatabaseService.get(id).orElseThrow();
        assertThat(output.getIsReprocessing()).isTrue();
    }

    @Test
    public void insertAndUpdateJobDefinition() throws IOException {
        DbJobInstance jobInstance = JobInstanceTestUtils.createInstance(DbJobInstance.builder().build(), jobDefinitionId);
        jobInstanceDatabaseService.insertAndUpdateJobDefinition(jobInstance, null, Instant.ofEpochSecond(0));
        DbJobDefinition dbJobDefinition = jobDefinitionDatabaseService.get(jobDefinitionId).orElseThrow();
        assertThat(dbJobDefinition.getLastIterationTs().getEpochSecond()).isEqualTo(0);
        assertThat(dbJobDefinition.getMetadata()).isEmpty();

        jobInstanceDatabaseService.insertAndUpdateJobDefinition(jobInstance, Map.of("a", "b"), Instant.ofEpochSecond(1));
        dbJobDefinition = jobDefinitionDatabaseService.get(jobDefinitionId).orElseThrow();
        assertThat(dbJobDefinition.getLastIterationTs().getEpochSecond()).isEqualTo(1);
        assertThat(dbJobDefinition.getMetadata()).containsExactlyEntriesOf(Map.of("a", "b"));


        jobInstanceDatabaseService.insertAndUpdateJobDefinition(jobInstance, Map.of("c", "d"), Instant.ofEpochSecond(2));
        dbJobDefinition = jobDefinitionDatabaseService.get(jobDefinitionId).orElseThrow();
        assertThat(dbJobDefinition.getLastIterationTs().getEpochSecond()).isEqualTo(2);
        assertThat(dbJobDefinition.getMetadata()).containsExactlyInAnyOrderEntriesOf(Map.of("a", "b", "c", "d"));

    }
}