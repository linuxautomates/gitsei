package io.levelops.aggregations_shared.database;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import io.levelops.aggregations_shared.database.models.DbJobDefinition;
import io.levelops.aggregations_shared.database.models.DbJobDefinitionFilter;
import io.levelops.aggregations_shared.database.models.DbJobDefinitionUpdate;
import io.levelops.commons.databases.services.DatabaseSchemaService;
import io.levelops.commons.etl.models.JobPriority;
import io.levelops.commons.etl.models.JobType;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.levelops.aggregations_shared.database.EtlDatabaseConstants.ETL_SCHEMA;
import static io.levelops.aggregations_shared.database.JobDefinitionDatabaseService.JOB_DEFINITION_TABLE;
import static org.assertj.core.api.Assertions.assertThat;


public class JobDefinitionDatabaseServiceTest {
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    private static DataSource dataSource;
    private static ObjectMapper objectMapper;
    private static JobDefinitionDatabaseService jobDefinitionDatabaseService;

    @BeforeClass
    public static void setup() throws SQLException {
        objectMapper = DefaultObjectMapper.get();
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        DatabaseSchemaService schemaService = new DatabaseSchemaService(dataSource);
        schemaService.ensureSchemaExistence(ETL_SCHEMA);
        jobDefinitionDatabaseService = new JobDefinitionDatabaseService(objectMapper, dataSource);
        jobDefinitionDatabaseService.ensureTableExistence();
    }

    @Before
    public void resetDb() throws SQLException {
        String sql = "DELETE FROM " + JOB_DEFINITION_TABLE + ";";
        dataSource.getConnection().prepareStatement(sql).execute();
    }

    private void assertEquals(DbJobDefinition j1, DbJobDefinition expected) {
        assertThat(j1.getTenantId()).isEqualTo(expected.getTenantId());
        assertThat(j1.getIntegrationId()).isEqualTo(expected.getIntegrationId());
        assertThat(j1.getIntegrationType()).isEqualTo(expected.getIntegrationType());
        assertThat(j1.getIngestionTriggerId()).isEqualTo(expected.getIngestionTriggerId());
        assertThat(j1.getIsActive()).isEqualTo(expected.getIsActive());
        assertThat(j1.getDefaultPriority()).isEqualTo(expected.getDefaultPriority());
        assertThat(j1.getAttemptMax()).isEqualTo(expected.getAttemptMax());
        assertThat(j1.getRetryWaitTimeInMinutes()).isEqualTo(expected.getRetryWaitTimeInMinutes());
        assertThat(j1.getTimeoutInMinutes()).isEqualTo(expected.getTimeoutInMinutes());
        assertThat(j1.getFrequencyInMinutes()).isEqualTo(expected.getFrequencyInMinutes());
        assertThat(j1.getFullFrequencyInMinutes()).isEqualTo(expected.getFullFrequencyInMinutes());
        assertThat(j1.getAggProcessorName()).isEqualTo(expected.getAggProcessorName());
        assertThat(j1.getLastIterationTs()).isEqualTo(expected.getLastIterationTs());
        assertThat(j1.getMetadata()).isEqualTo(expected.getMetadata() == null ? Collections.emptyMap() : expected.getMetadata());
    }

    public static DbJobDefinition createJobDefinition(
            Instant lastIterationTs, String tenantId, String integrationId, Boolean isActive, JobType jobType) {
        tenantId = MoreObjects.firstNonNull(tenantId, "sid");
        integrationId = MoreObjects.firstNonNull(integrationId, "1");
        lastIterationTs = MoreObjects.firstNonNull(lastIterationTs, Instant.now());
        isActive = MoreObjects.firstNonNull(isActive, true);
        jobType = MoreObjects.firstNonNull(jobType, JobType.INGESTION_RESULT_PROCESSING_JOB);
        return DbJobDefinition.builder()
                .tenantId(tenantId)
                .integrationId(integrationId)
                .integrationType("jira")
                .ingestionTriggerId(UUID.randomUUID().toString())
                .jobType(jobType)
                .isActive(isActive)
                .defaultPriority(JobPriority.HIGH)
                .attemptMax(10)
                .retryWaitTimeInMinutes(11)
                .timeoutInMinutes(12L)
                .frequencyInMinutes(13)
                .fullFrequencyInMinutes(14)
                .aggProcessorName("jira")
                .lastIterationTs(lastIterationTs)
                .metadata(null)
                .build();
    }

    private List<UUID> insertJobDefinitions(List<DbJobDefinition> jobDefinitions) {
        return jobDefinitions.stream()
                .map(jobDefinition -> {
                    try {
                        return jobDefinitionDatabaseService.insert(jobDefinition);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                }).toList();
    }

    @Test
    public void testInsertAndRetrieval() throws SQLException, JsonProcessingException {
        Instant now = Instant.now();
        DbJobDefinition jobDefinition = DbJobDefinition.builder()
                .tenantId("sid")
                .integrationId("1")
                .integrationType("jira")
                .ingestionTriggerId(UUID.randomUUID().toString())
                .jobType(JobType.INGESTION_RESULT_PROCESSING_JOB)
                .isActive(true)
                .defaultPriority(JobPriority.HIGH)
                .attemptMax(10)
                .retryWaitTimeInMinutes(11)
                .timeoutInMinutes(12L)
                .frequencyInMinutes(13)
                .fullFrequencyInMinutes(14)
                .aggProcessorName("jira")
                .lastIterationTs(now)
                .metadata(null)
                .build();
        UUID id = jobDefinitionDatabaseService.insert(jobDefinition);
        var o = jobDefinitionDatabaseService.get(id).get();
        assertEquals(o, jobDefinition);
        assertThat(o.getId()).isEqualTo(id);
        assertThat(o.getCreatedAt()).isStrictlyBetween(now, Instant.now());

        Instant now2 = Instant.now();
        // Change the lastiterationTs and metadata
        DbJobDefinition jobDefinition2 = jobDefinition.toBuilder()
                .lastIterationTs(now2)
                .metadata(Map.of("key", "value", "key2", 1, "key3", false))
                .build();
        UUID id2 = jobDefinitionDatabaseService.insert(jobDefinition2);
        var o2 = jobDefinitionDatabaseService.get(id2).get();
        assertEquals(o2, jobDefinition2);
    }

    @Test
    public void testIdsFilter() {
        var jobDefinitions = List.of(
                createJobDefinition(null, null, "1", false, null),
                createJobDefinition(null, null, "2", true, null),
                createJobDefinition(null, null, "3", false, null),
                createJobDefinition(null, null, "4", true, null),
                createJobDefinition(null, null, "5", true, null),
                createJobDefinition(null, null, "6", false, null)
        );

        List<UUID> uuidLIst = jobDefinitions.stream()
                .map(jobDefinition -> {
                    try {
                        return jobDefinitionDatabaseService.insert(jobDefinition);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                }).collect(Collectors.toList());

        // Ids filter
        var filteredResult = jobDefinitionDatabaseService.filter(0, 100, DbJobDefinitionFilter.builder()
                .ids(uuidLIst.subList(0, 3))
                .build());
        assertThat(filteredResult.getTotalCount()).isEqualTo(3);
        assertThat(filteredResult.getRecords().stream().map(DbJobDefinition::getId).collect(Collectors.toList()))
                .containsExactlyInAnyOrderElementsOf(uuidLIst.subList(0, 3));
    }

    @Test
    public void isActiveFilterTest() {
        var jobDefinitions = List.of(
                createJobDefinition(null, null, "1", false, null),
                createJobDefinition(null, null, "2", true, null),
                createJobDefinition(null, null, "3", false, null),
                createJobDefinition(null, null, "4", true, null),
                createJobDefinition(null, null, "5", true, null),
                createJobDefinition(null, null, "6", false, null)
        );

        List<UUID> uuidLIst = jobDefinitions.stream()
                .map(jobDefinition -> {
                    try {
                        return jobDefinitionDatabaseService.insert(jobDefinition);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                }).collect(Collectors.toList());
        // is active filter
        var filteredResult = jobDefinitionDatabaseService.filter(0, 100, DbJobDefinitionFilter.builder()
                .isActive(true)
                .build());
        assertThat(filteredResult.getTotalCount()).isEqualTo(3);
        assertThat(filteredResult.getRecords().stream().map(DbJobDefinition::getId).collect(Collectors.toList()))
                .containsExactlyInAnyOrderElementsOf(List.of(uuidLIst.get(1), uuidLIst.get(3), uuidLIst.get(4)));


        filteredResult = jobDefinitionDatabaseService.filter(0, 100, DbJobDefinitionFilter.builder()
                .isActive(false)
                .build());
        assertThat(filteredResult.getTotalCount()).isEqualTo(3);
        assertThat(filteredResult.getRecords().stream().map(DbJobDefinition::getId).collect(Collectors.toList()))
                .containsExactlyInAnyOrderElementsOf(List.of(uuidLIst.get(0), uuidLIst.get(2), uuidLIst.get(5)));
    }

    @Test
    public void testTenantIdAndIntegrationIdFilter() {
        var jobDefinitions = List.of(
                createJobDefinition(null, "stephenCurry", "30", false, null),
                createJobDefinition(null, "stephenCurry", "31", false, null),
                createJobDefinition(null, "klayBae", "1", false, null),
                createJobDefinition(null, "klayBae", "2", false, null)
        );
        var ids = insertJobDefinitions(jobDefinitions);

        var filteredResult = jobDefinitionDatabaseService.filter(0, 1, DbJobDefinitionFilter.builder()
                .tenantIdIntegrationIdPair(Pair.of("stephenCurry", "30"))
                .build()).getRecords();
        assertThat(filteredResult.size()).isEqualTo(1);
        assertThat(filteredResult.get(0).getTenantId()).isEqualTo("stephenCurry");
        assertThat(filteredResult.get(0).getIntegrationId()).isEqualTo("30");

        filteredResult = jobDefinitionDatabaseService.filter(0, 1, DbJobDefinitionFilter.builder()
                .tenantIdIntegrationIdPair(Pair.of("klayBae", "2"))
                .build()).getRecords();
        assertThat(filteredResult.size()).isEqualTo(1);
        assertThat(filteredResult.get(0).getTenantId()).isEqualTo("klayBae");
        assertThat(filteredResult.get(0).getIntegrationId()).isEqualTo("2");
    }

    @Test
    public void testTenantIdFilter() {
        var jobDefinitions = List.of(
                createJobDefinition(null, "stephenCurry", "30", false, null),
                createJobDefinition(null, "stephenCurry", "31", false, null),
                createJobDefinition(null, "klayBae", "1", false, null),
                createJobDefinition(null, "klayBae", "2", false, null)
        );
        var ids = insertJobDefinitions(jobDefinitions);
        var results = jobDefinitionDatabaseService.filter(0, 100, DbJobDefinitionFilter.builder()
                .tenantIds(List.of("stephenCurry"))
                .build());
        assertThat(results.getRecords().size()).isEqualTo(2);
    }

    @Test
    public void testStreamAllDefinitions() {
        var jobDefinitions = List.of(
                createJobDefinition(null, null, "1", false, null),
                createJobDefinition(null, null, "2", true, null),
                createJobDefinition(null, null, "3", false, null),
                createJobDefinition(null, null, "4", true, null),
                createJobDefinition(null, null, "5", true, null),
                createJobDefinition(null, null, "6", false, null)
        );

        List<UUID> uuidLIst = jobDefinitions.stream()
                .map(jobDefinition -> {
                    try {
                        return jobDefinitionDatabaseService.insert(jobDefinition);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                }).collect(Collectors.toList());

        var results = jobDefinitionDatabaseService.stream(DbJobDefinitionFilter.builder().build()).collect(Collectors.toList());
        assertThat(results.stream().map(DbJobDefinition::getId).collect(Collectors.toList()))
                .containsExactlyInAnyOrderElementsOf(uuidLIst);
    }

    @Test
    public void testFilterPages() {
        var jobDefinitions = List.of(
                createJobDefinition(null, null, "1", false, null),
                createJobDefinition(null, null, "2", true, null),
                createJobDefinition(null, null, "3", false, null),
                createJobDefinition(null, null, "4", true, null),
                createJobDefinition(null, null, "5", true, null),
                createJobDefinition(null, null, "6", false, null)
        );

        List<UUID> uuidLIst = jobDefinitions.stream()
                .map(jobDefinition -> {
                    try {
                        return jobDefinitionDatabaseService.insert(jobDefinition);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                }).collect(Collectors.toList());

        // Test page number and size
        var filteredResult = jobDefinitionDatabaseService.filter(0, 1, DbJobDefinitionFilter.builder()
                .ids(uuidLIst.subList(0, 3))
                .build());
        assertThat(filteredResult.getCount()).isEqualTo(1);
        assertThat(filteredResult.getTotalCount()).isEqualTo(3);

        filteredResult = jobDefinitionDatabaseService.filter(1, 1, DbJobDefinitionFilter.builder()
                .ids(uuidLIst.subList(0, 3))
                .build());
        assertThat(filteredResult.getCount()).isEqualTo(1);
        assertThat(filteredResult.getTotalCount()).isEqualTo(3);

        filteredResult = jobDefinitionDatabaseService.filter(2, 1, DbJobDefinitionFilter.builder()
                .ids(uuidLIst.subList(0, 3))
                .build());
        assertThat(filteredResult.getCount()).isEqualTo(1);
        assertThat(filteredResult.getTotalCount()).isEqualTo(3);
    }

    @Test
    public void testUpdates() throws JsonProcessingException {
        var j1 = createJobDefinition(null, null, "1", true, null);
        var j2 = createJobDefinition(null, null, "2", true, null);
        var id1 = jobDefinitionDatabaseService.insert(j1);
        var id2 = jobDefinitionDatabaseService.insert(j2);

        var j1db = jobDefinitionDatabaseService.get(id1).get();
        assertEquals(j1db, j1);

        var updated = jobDefinitionDatabaseService.update(DbJobDefinitionUpdate.builder()
                .isActive(false)
                .whereClause(DbJobDefinitionUpdate.WhereClause.builder()
                        .id(id1)
                        .build())
                .build());
        j1 = j1.toBuilder().isActive(false).build();
        assertThat(updated).isTrue();
        j1db = jobDefinitionDatabaseService.get(id1).get();
        assertEquals(j1db, j1);

        Instant now = Instant.now();
        updated = jobDefinitionDatabaseService.update(DbJobDefinitionUpdate.builder()
                .lastIterationTs(now)
                .whereClause(DbJobDefinitionUpdate.WhereClause.builder()
                        .id(id1)
                        .build())
                .build());
        j1 = j1.toBuilder().lastIterationTs(now).build();
        assertThat(updated).isTrue();
        j1db = jobDefinitionDatabaseService.get(id1).get();
        assertEquals(j1db, j1);


        Instant now2 = Instant.now();
        updated = jobDefinitionDatabaseService.update(DbJobDefinitionUpdate.builder()
                .lastIterationTs(now2)
                .isActive(true)
                .metadata(Map.of("key", "value"))
                .whereClause(DbJobDefinitionUpdate.WhereClause.builder()
                        .id(id1)
                        .build())
                .build());
        j1 = j1.toBuilder()
                .metadata(Map.of("key", "value"))
                .lastIterationTs(now2)
                .isActive(true)
                .build();
        assertThat(updated).isTrue();
        j1db = jobDefinitionDatabaseService.get(id1).get();
        assertEquals(j1db, j1);

        var tenantUpdated = jobDefinitionDatabaseService.update(DbJobDefinitionUpdate.builder()
                .isActive(false)
                .metadata(Map.of("key2", "value2"))
                .whereClause(DbJobDefinitionUpdate.WhereClause.builder()
                        .tenantId(j1.getTenantId())
                        .build())
                .build());
        assertThat(tenantUpdated).isTrue();
        var jobs = jobDefinitionDatabaseService.stream(DbJobDefinitionFilter.builder()
                .tenantIds(List.of(j1.getTenantId()))
                .build());
        assertThat(jobs).allMatch(jobDefinition -> !jobDefinition.getIsActive());
    }

    @Test
    public void testFilterJobType() {
        var jobDefinitions = List.of(
                createJobDefinition(null, null, "1", false, JobType.INGESTION_RESULT_PROCESSING_JOB),
                createJobDefinition(null, null, "2", true, JobType.INGESTION_RESULT_PROCESSING_JOB),
                createJobDefinition(null, null, "2", true, JobType.GENERIC_INTEGRATION_JOB),
                createJobDefinition(null, null, "2", true, JobType.GENERIC_INTEGRATION_JOB),
                createJobDefinition(null, null, "2", true, JobType.GENERIC_INTEGRATION_JOB),
                createJobDefinition(null, null, "3", true, JobType.GENERIC_TENANT_JOB)
        );

        List<UUID> uuids = jobDefinitions.stream()
                .map(jobDefinition -> {
                    try {
                        return jobDefinitionDatabaseService.insert(jobDefinition);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                }).toList();

        var filteredGenericIntegration = jobDefinitionDatabaseService.stream(
                DbJobDefinitionFilter.builder()
                        .jobTypes(List.of(JobType.GENERIC_INTEGRATION_JOB))
                        .build()
        ).toList();
        assertThat(filteredGenericIntegration.size()).isEqualTo(3);
        assertThat(filteredGenericIntegration.stream().map(DbJobDefinition::getId).collect(Collectors.toList()))
                .containsExactlyInAnyOrderElementsOf(uuids.subList(2, 5));

        var filteredGenericTenant = jobDefinitionDatabaseService.stream(
                DbJobDefinitionFilter.builder()
                        .jobTypes(List.of(JobType.GENERIC_TENANT_JOB))
                        .build()
        ).toList();
        assertThat(filteredGenericTenant.size()).isEqualTo(1);
        assertThat(filteredGenericTenant.stream().map(DbJobDefinition::getId).collect(Collectors.toList()))
                .containsExactlyInAnyOrderElementsOf(uuids.subList(5, 6));

        var filteredIngestionResultProcessing = jobDefinitionDatabaseService.stream(
                DbJobDefinitionFilter.builder()
                        .jobTypes(List.of(JobType.INGESTION_RESULT_PROCESSING_JOB))
                        .build()
        ).toList();
        assertThat(filteredIngestionResultProcessing.size()).isEqualTo(2);
        assertThat(filteredIngestionResultProcessing.stream().map(DbJobDefinition::getId).collect(Collectors.toList()))
                .containsExactlyInAnyOrderElementsOf(uuids.subList(0, 2));
    }

}