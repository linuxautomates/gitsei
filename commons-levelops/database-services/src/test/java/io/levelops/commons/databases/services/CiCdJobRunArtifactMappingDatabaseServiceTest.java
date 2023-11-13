package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.CICDJob;
import io.levelops.commons.databases.models.database.CICDJobRun;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.cicd.CiCdJobRunArtifactMapping;
import io.levelops.commons.databases.services.CiCdJobRunArtifactMappingDatabaseService.CicdJobRunArtifactMappingFilter;
import io.levelops.commons.databases.services.CicdJobRunArtifactCorrelationService.IntermediateMapping;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class CiCdJobRunArtifactMappingDatabaseServiceTest {

    private static final String COMPANY = "test";

    @Rule
    public SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    CiCdJobRunArtifactsDatabaseService artifactsDatabaseService;
    CiCdJobRunArtifactMappingDatabaseService mappingDatabaseService;
    UUID jobRunId1;
    UUID jobRunId2;
    UUID jobRunId3;

    @Before
    public void setUp() throws Exception {
        DataSource dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        JdbcTemplate template = new JdbcTemplate(dataSource);
        List.of(
                "CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";",
                "DROP SCHEMA IF EXISTS test CASCADE; ",
                "CREATE SCHEMA test; "
        ).forEach(template::execute);

        var integrationService = new IntegrationService(dataSource);
        integrationService.ensureTableExistence(COMPANY);
        var integration = Integration.builder()
                .id("1")
                .name("name")
                .url("http")
                .status("good")
                .application("jira")
                .description("desc")
                .satellite(true)
                .build();
        integrationService.insert(COMPANY, integration);

        CiCdInstancesDatabaseService ciCdInstancesDatabaseService = new CiCdInstancesDatabaseService(dataSource);
        ciCdInstancesDatabaseService.ensureTableExistence(COMPANY);

        CiCdJobsDatabaseService ciCdJobsDatabaseService = new CiCdJobsDatabaseService(dataSource);
        ciCdJobsDatabaseService.ensureTableExistence(COMPANY);
        String jobId = ciCdJobsDatabaseService.insert(COMPANY, CICDJob.builder()
                .jobName("jobName1").jobFullName("jobFullName1")
                .build());

        CiCdJobRunsDatabaseService ciCdJobRunsDatabaseService = new CiCdJobRunsDatabaseService(DefaultObjectMapper.get(), dataSource);
        ciCdJobRunsDatabaseService.ensureTableExistence(COMPANY);
        jobRunId1 = UUID.fromString(ciCdJobRunsDatabaseService.insert(COMPANY, CICDJobRun.builder()
                .cicdJobId(UUID.fromString(jobId))
                .jobRunNumber(1L)
                .build()));
        jobRunId2 = UUID.fromString(ciCdJobRunsDatabaseService.insert(COMPANY, CICDJobRun.builder()
                .cicdJobId(UUID.fromString(jobId))
                .jobRunNumber(2L)
                .build()));
        jobRunId3 = UUID.fromString(ciCdJobRunsDatabaseService.insert(COMPANY, CICDJobRun.builder()
                .cicdJobId(UUID.fromString(jobId))
                .jobRunNumber(3L)
                .build()));

        artifactsDatabaseService = new CiCdJobRunArtifactsDatabaseService(dataSource, DefaultObjectMapper.get());
        artifactsDatabaseService.ensureTableExistence(COMPANY);

        mappingDatabaseService = new CiCdJobRunArtifactMappingDatabaseService(dataSource, DefaultObjectMapper.get());
        mappingDatabaseService.ensureTableExistence(COMPANY);
    }

    @Test
    public void test() throws SQLException {
        // -- insert m1
        CiCdJobRunArtifactMapping m1 = CiCdJobRunArtifactMapping.builder()
                .cicdJobRunId1(jobRunId1)
                .cicdJobRunId2(jobRunId2)
                .build();
        String mid1 = mappingDatabaseService.insert(COMPANY, m1);
        assertThat(mid1).isNotEmpty();

        // -- get
        CiCdJobRunArtifactMapping m1get = mappingDatabaseService.get(COMPANY, mid1).orElseThrow();
        assertThat(m1get)
                .usingRecursiveComparison()
                .ignoringFields("createdAt", "id")
                .isEqualTo(m1);
        // -- get by key
        assertThat(mappingDatabaseService.get(COMPANY, jobRunId1, jobRunId2).orElseThrow())
                .usingRecursiveComparison()
                .ignoringFields("createdAt", "id")
                .isEqualTo(m1);

        // -- insert dedupe
        assertThat(mappingDatabaseService.insert(COMPANY, m1)).isEqualTo(mid1);
        assertThat(mappingDatabaseService.get(COMPANY, mid1).orElseThrow().getCreatedAt()).isEqualTo(m1get.getCreatedAt());

        // -- insert m2 and m3
        CiCdJobRunArtifactMapping m2 = CiCdJobRunArtifactMapping.builder()
                .cicdJobRunId1(jobRunId2)
                .cicdJobRunId2(jobRunId3)
                .build();
        CiCdJobRunArtifactMapping m3 = CiCdJobRunArtifactMapping.builder()
                .cicdJobRunId1(jobRunId1)
                .cicdJobRunId2(jobRunId3)
                .build();
        mappingDatabaseService.insert(COMPANY, m2);
        mappingDatabaseService.insert(COMPANY, m3);

        // -- stream & filter
        assertThat(mappingDatabaseService.stream(COMPANY, null).map(this::removeVariableFields)).containsExactlyInAnyOrder(m1, m2, m3);
        checkStream(null, null, m1, m2, m3);
        checkStream(List.of(jobRunId1.toString()), null, m1, m3);
        checkStream(null, List.of(jobRunId3.toString()), m2, m3);
        checkStream(List.of(jobRunId1.toString()), List.of(jobRunId2.toString()), m1);
        checkStream(List.of(jobRunId2.toString()), List.of(jobRunId3.toString()), m2);
        checkStream(List.of(jobRunId1.toString()), List.of(jobRunId3.toString()), m3);
        checkStream(List.of(jobRunId3.toString()), List.of(jobRunId1.toString()));

        // -- replace (no op)
        // a1 -> a2, a3 (m1 + m3)
        mappingDatabaseService.replace(COMPANY,
                IntermediateMapping.builder()
                        .runId1(jobRunId1)
                        .runIds(Set.of(jobRunId2, jobRunId3))
                        .build());
        checkStream(List.of(jobRunId1.toString()), null, m1, m3);
        assertThat(mappingDatabaseService.get(COMPANY, mid1).orElseThrow().getCreatedAt()).isEqualTo(m1get.getCreatedAt());

        // -- replace: a1 -> a1 (m4)
        mappingDatabaseService.replace(COMPANY,
                IntermediateMapping.builder()
                        .runId1(jobRunId1)
                        .runIds(Set.of(jobRunId1))
                        .build());
        CiCdJobRunArtifactMapping m4 = CiCdJobRunArtifactMapping.builder()
                .cicdJobRunId1(jobRunId1)
                .cicdJobRunId2(jobRunId1)
                .build();
        checkStream(List.of(jobRunId1.toString()), null, m4);
        checkStream(null, null, m2, m4);

        // -- replace no op
        mappingDatabaseService.replace(COMPANY,
                IntermediateMapping.builder()
                        .runId1(jobRunId1)
                        .runIds(null)
                        .build());
        checkStream(null, null, m2, m4);

        // -- delete
        mappingDatabaseService.bulkDeleteByRunId1(COMPANY, Stream.of(jobRunId1.toString()));
        checkStream(null, null, m2);
        mappingDatabaseService.delete(COMPANY, jobRunId2.toString(), jobRunId3.toString());
        checkStream(null, null);
    }

    public void checkStream(List<String> runId1, List<String> runId2, CiCdJobRunArtifactMapping... expected) {
        assertThat(mappingDatabaseService.stream(COMPANY,
                        CicdJobRunArtifactMappingFilter.builder()
                                .cicdJobRunId1List(runId1)
                                .cicdJobRunId2List(runId2)
                                .build())
                .map(this::removeVariableFields)
        ).containsExactlyInAnyOrder(expected);
    }

    public CiCdJobRunArtifactMapping removeVariableFields(CiCdJobRunArtifactMapping m) {
        return m.toBuilder()
                .id(null)
                .createdAt(null)
                .build();
    }

    // PROP-3631
    @Test
    public void testBulkReplacePROP3631() throws SQLException {

        mappingDatabaseService.bulkReplace(COMPANY, List.of(
                IntermediateMapping.builder()
                        .runId1(jobRunId1)
                        .runIds(Set.of(jobRunId1))
                        .build(),
                IntermediateMapping.builder()
                        .runId1(jobRunId1)
                        .runIds(Set.of(jobRunId1, jobRunId2))
                        .build()));

        // (just checking if this doesn't throw exceptions)

    }

    @Test
    public void testConsolidateMappings() {
        var mappings = List.of(
                IntermediateMapping.builder()
                        .runId1(jobRunId1)
                        .runIds(Set.of(jobRunId1))
                        .build(),
                IntermediateMapping.builder()
                        .runId1(jobRunId1)
                        .runIds(Set.of(jobRunId2))
                        .build(),
                IntermediateMapping.builder()
                        .runId1(jobRunId2)
                        .runIds(Set.of(jobRunId1, jobRunId2))
                        .build());

        var consolidatedMapping = mappingDatabaseService.consolidateMappings(mappings);
        assertThat(consolidatedMapping).containsExactlyInAnyOrder(
                IntermediateMapping.builder()
                        .runId1(jobRunId1)
                        .runIds(Set.of(jobRunId1, jobRunId2))
                        .build(),
                IntermediateMapping.builder()
                        .runId1(jobRunId2)
                        .runIds(Set.of(jobRunId1, jobRunId2))
                        .build());

        var consolidatedEmptyMapping = mappingDatabaseService.consolidateMappings(List.of());
        assertThat(consolidatedEmptyMapping).isEmpty();
    }

    @Test
    public void testBulkReplace() {
        mappingDatabaseService.bulkReplace(COMPANY, List.of(
                IntermediateMapping.builder()
                        .runId1(jobRunId1)
                        .runIds(Set.of(jobRunId1))
                        .build(),
                IntermediateMapping.builder()
                        .runId1(jobRunId1)
                        .runIds(Set.of(jobRunId2))
                        .build()));

        var allMappings = mappingDatabaseService.filter(
                0, 100, COMPANY, CicdJobRunArtifactMappingFilter.builder().build());
        assertThat(allMappings.getRecords()).hasSize(2);
    }

}