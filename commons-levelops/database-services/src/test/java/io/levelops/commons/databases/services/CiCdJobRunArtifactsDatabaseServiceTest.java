package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.CICDJob;
import io.levelops.commons.databases.models.database.CICDJobRun;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.cicd.CiCdJobRunArtifact;
import io.levelops.commons.databases.services.CiCdJobRunArtifactsDatabaseService.CiCdJobRunArtifactFilter;
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
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class CiCdJobRunArtifactsDatabaseServiceTest {

    private static final String COMPANY = "test";

    @Rule
    public SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    CiCdJobRunArtifactsDatabaseService artifactsDatabaseService;
    String jobRunId;

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
        jobRunId = ciCdJobRunsDatabaseService.insert(COMPANY, CICDJobRun.builder()
                .cicdJobId(UUID.fromString(jobId))
                .jobRunNumber(123L)
                .build());

        artifactsDatabaseService = new CiCdJobRunArtifactsDatabaseService(dataSource, DefaultObjectMapper.get());
        artifactsDatabaseService.ensureTableExistence(COMPANY);
    }

    @Test
    public void test() throws SQLException {
        CiCdJobRunArtifact artifact1 = CiCdJobRunArtifact.builder()
                .cicdJobRunId(UUID.fromString(jobRunId))
                .name("test1")
                .input(true)
                .output(true)
                .type("type")
                .location("loc")
                .hash("hash")
                .qualifier("v1")
                .metadata(Map.of("a", "b"))
                .build();

        // -- insert and get
        String id1 = artifactsDatabaseService.insert(COMPANY, artifact1);
        assertThat(id1).isNotNull();

        CiCdJobRunArtifact output = artifactsDatabaseService.get(COMPANY, id1).orElse(null);
        assertThat(output)
                .usingRecursiveComparison()
                .ignoringFields("createdAt")
                .isEqualTo(artifact1.toBuilder().id(id1).build());

        artifact1 = artifact1.toBuilder()
                .id(id1)
                .input(false)
                .output(true)
                .metadata(Map.of("b", "c"))
                .build();
        String updatedId = artifactsDatabaseService.insert(COMPANY, artifact1);
        assertThat(updatedId).isEqualTo(id1);
        output = artifactsDatabaseService.get(COMPANY, id1).orElse(null);
        assertThat(output)
                .usingRecursiveComparison()
                .ignoringFields("createdAt")
                .isEqualTo(artifact1);


        CiCdJobRunArtifact artifact2 = artifact1.toBuilder()
                .name("test2")
                .input(true)
                .output(true)
                .build();
        String id2 = artifactsDatabaseService.insert(COMPANY, artifact2);
        assertThat(artifactsDatabaseService.get(COMPANY, id2)).isPresent();
        assertThat(id2).isNotEqualTo(id1);

        CiCdJobRunArtifact artifact3 = artifact1.toBuilder()
                .name("test1")
                .qualifier("v2")
                .input(true)
                .output(false)
                .build();
        String id3 = artifactsDatabaseService.insert(COMPANY, artifact3);
        assertThat(artifactsDatabaseService.get(COMPANY, id3)).isPresent();
        assertThat(id3).isNotEqualTo(id1);

        assertThat(artifactsDatabaseService.get(COMPANY, "dummy")).isEmpty();

        // -- get by keys
        assertThat(artifactsDatabaseService.get(COMPANY, jobRunId, "type", "loc", "test1", "v1", "hash").orElse(null))
                .usingRecursiveComparison()
                .ignoringFields("createdAt")
                .isEqualTo(artifact1);
        assertThat(artifactsDatabaseService.get(COMPANY, jobRunId, "type", "loc", "test1", "v2", "hash").map(CiCdJobRunArtifact::getId).orElse(null)).isEqualTo(id3);
        assertThat(artifactsDatabaseService.get(COMPANY, jobRunId, "-", "loc", "test1", "v2", "hash")).isEmpty();
        assertThat(artifactsDatabaseService.get(COMPANY, jobRunId, "type", "-", "test1", "v2", "hash")).isEmpty();
        assertThat(artifactsDatabaseService.get(COMPANY, jobRunId, "type", "loc", "-", "v2", "hash")).isEmpty();
        assertThat(artifactsDatabaseService.get(COMPANY, jobRunId, "type", "loc", "test1", "-", "hash")).isEmpty();
        assertThat(artifactsDatabaseService.get(COMPANY, jobRunId, "type", "loc", "test1", "v2", "-")).isEmpty();


        // -- filter
        assertThat(artifactsDatabaseService.list(COMPANY, 0, 10).getRecords().stream().map(CiCdJobRunArtifact::getId))
                .containsExactlyInAnyOrder(id1, id2, id3);

        checkFilters(null, id1, id2, id3);
        checkFilters(CiCdJobRunArtifactFilter.builder().build(), id1, id2, id3);

        checkFilters(CiCdJobRunArtifactFilter.builder().ids(List.of(id1, id3)).build(), id1, id3);
        checkFilters(CiCdJobRunArtifactFilter.builder().ids(List.of("dummy")).build());

        checkFilters(CiCdJobRunArtifactFilter.builder().excludeIds(List.of(id1, id3)).build(), id2);
        checkFilters(CiCdJobRunArtifactFilter.builder().excludeIds(List.of("dummy")).build(), id1, id2, id3);

        checkFilters(CiCdJobRunArtifactFilter.builder().cicdJobRunIds(List.of(jobRunId)).build(), id1, id2, id3);
        checkFilters(CiCdJobRunArtifactFilter.builder().cicdJobRunIds(List.of(UUID.randomUUID().toString())).build());

        checkFilters(CiCdJobRunArtifactFilter.builder().types(List.of("type")).build(), id1, id2, id3);
        checkFilters(CiCdJobRunArtifactFilter.builder().types(List.of("dummy")).build());

        checkFilters(CiCdJobRunArtifactFilter.builder().locations(List.of("loc")).build(), id1, id2, id3);
        checkFilters(CiCdJobRunArtifactFilter.builder().locations(List.of("dummy")).build());

        checkFilters(CiCdJobRunArtifactFilter.builder().names(List.of("test1")).build(), id1, id3);
        checkFilters(CiCdJobRunArtifactFilter.builder().names(List.of("test2")).build(), id2);
        checkFilters(CiCdJobRunArtifactFilter.builder().names(List.of("dummy")).build());
        checkFilters(CiCdJobRunArtifactFilter.builder().partialName("est").build(), id1, id2, id3);
        checkFilters(CiCdJobRunArtifactFilter.builder().partialName("dummy").build());

        checkFilters(CiCdJobRunArtifactFilter.builder().qualifiers(List.of("v1")).build(), id1, id2);
        checkFilters(CiCdJobRunArtifactFilter.builder().qualifiers(List.of("v2")).build(), id3);
        checkFilters(CiCdJobRunArtifactFilter.builder().qualifiers(List.of("dummy")).build());

        checkFilters(CiCdJobRunArtifactFilter.builder().hashes(List.of("hash")).build(), id1, id2, id3);
        checkFilters(CiCdJobRunArtifactFilter.builder().hashes(List.of("dummy")).build());

        checkFilters(CiCdJobRunArtifactFilter.builder().hashes(List.of("hash")).build(), id1, id2, id3);
        checkFilters(CiCdJobRunArtifactFilter.builder().hashes(List.of("dummy")).build());

        checkFilters(CiCdJobRunArtifactFilter.builder().input(true).build(), id2, id3);
        checkFilters(CiCdJobRunArtifactFilter.builder().input(false).build(), id1);

        checkFilters(CiCdJobRunArtifactFilter.builder().output(true).build(), id1, id2);
        checkFilters(CiCdJobRunArtifactFilter.builder().output(false).build(), id3);

        // -- delete / replace

        artifactsDatabaseService.delete(COMPANY, id3);
        checkFilters(null, id1, id2);

        List<String> newIds = artifactsDatabaseService.replace(COMPANY, jobRunId, List.of(artifact1, artifact3));
        assertThat(newIds).hasSize(2);
        assertThat(newIds).contains(id1);
        assertThat(newIds).doesNotContain(id2);
        assertThat(newIds).doesNotContain(id3); // it's a new id
        assertThat(artifactsDatabaseService.list(COMPANY, 0, 10).getRecords()).hasSize(2);

        List<String> newIds2 = artifactsDatabaseService.replace(COMPANY, jobRunId, List.of());
        assertThat(newIds2).isEmpty();
        assertThat(artifactsDatabaseService.list(COMPANY, 0, 10).getRecords()).isEmpty();
    }

    private void checkFilters(CiCdJobRunArtifactFilter filter, String... expected) throws SQLException {
        assertThat(artifactsDatabaseService.filter(COMPANY, filter, 0, 10).getRecords().stream().map(CiCdJobRunArtifact::getId))
                .containsExactlyInAnyOrder(expected);
    }
}