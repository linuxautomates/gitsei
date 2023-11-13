package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.CICDJob;
import io.levelops.commons.databases.models.database.CICDJobRun;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.cicd.CiCdJobRunArtifact;
import io.levelops.commons.databases.services.CicdJobRunArtifactCorrelationService.CicdArtifactCorrelationSettings;
import io.levelops.commons.databases.services.CicdJobRunArtifactCorrelationService.IntermediateMapping;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class CicdJobRunArtifactCorrelationServiceTest {


    private static final String COMPANY = "test";

    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    static DataSource dataSource;
    static CiCdJobRunArtifactsDatabaseService artifactsDatabaseService;
    static CiCdJobRunArtifactMappingDatabaseService mappingDatabaseService;
    static CiCdJobRunArtifactsDatabaseService ciCdJobRunArtifactsDatabaseService;
    ;
    static CiCdJobRunsDatabaseService ciCdJobRunsDatabaseService;
    static UUID jobRunId1;
    static UUID jobRunId2;
    static UUID jobRunId3;

    @BeforeClass
    public static void setUp() throws Exception {
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
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

        ciCdJobRunsDatabaseService = new CiCdJobRunsDatabaseService(DefaultObjectMapper.get(), dataSource);
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

        ciCdJobRunArtifactsDatabaseService = new CiCdJobRunArtifactsDatabaseService(dataSource, DefaultObjectMapper.get());
        ciCdJobRunArtifactsDatabaseService.ensureTableExistence(COMPANY);
    }

    @Before
    public void beforeMethod() throws SQLException {
        dataSource.getConnection().prepareStatement("DELETE FROM test.cicd_job_run_artifacts;").execute();
    }

    @Test
    public void intermediateMappingComparator() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        IntermediateMapping m1 = IntermediateMapping.builder().runId1(a).runIds(Set.of(a, b)).build();
        IntermediateMapping m2 = IntermediateMapping.builder().runId1(a).runIds(Set.of(b, a)).build();
        assertThat(m1).isEqualTo(m2);
    }

    private CICDJobRun getJobRun(UUID id) throws SQLException {
        return ciCdJobRunsDatabaseService.get(COMPANY, id.toString()).get();
    }

    @Test
    public void testNoCorrelation() throws SQLException {
        CiCdJobRunArtifact artifact1 = CiCdJobRunArtifact.builder()
                .cicdJobRunId(jobRunId1)
                .name("test1")
                .input(true)
                .output(true)
                .type("container")
                .location("loc")
                .hash("hash")
                .qualifier("v1")
                .build();

        artifactsDatabaseService.insert(COMPANY, artifact1);

        CicdArtifactCorrelationSettings settings = CicdArtifactCorrelationSettings.builder()
                .build();
        var correlationService = new CicdJobRunArtifactCorrelationService(dataSource, mappingDatabaseService, settings, ciCdJobRunArtifactsDatabaseService);

        List<IntermediateMapping> correlatedArtifacts = correlationService.getCorrelatedArtifacts(COMPANY, 0, 10);

        CICDJobRun jobRun = getJobRun(jobRunId1);
        List<IntermediateMapping> individualCorrelatedArtifacts = correlationService.getIndividualCorrelatedArtifacts(COMPANY, jobRun, List.of());
        assertThat(correlatedArtifacts).isEmpty();
        assertThat(individualCorrelatedArtifacts).isEmpty();
    }

    @Test
    public void testIdentityCorrelation() throws SQLException {
        CicdArtifactCorrelationSettings settings = CicdArtifactCorrelationSettings.builder()
                .correlationIdentityDefault(true)
                .build();
        var correlationService = new CicdJobRunArtifactCorrelationService(dataSource, mappingDatabaseService, settings, ciCdJobRunArtifactsDatabaseService);

        List<IntermediateMapping> correlatedArtifacts = correlationService.getCorrelatedArtifacts(COMPANY, 0, 10);
        List<IntermediateMapping> individualCorrelatedArtifacts = correlationService.getIndividualCorrelatedArtifacts(COMPANY, getJobRun(jobRunId1), List.of());

        DefaultObjectMapper.prettyPrint(correlatedArtifacts);

        // no artifact but each job run is still mapped to itself
        assertThat(correlatedArtifacts).containsExactlyInAnyOrder(IntermediateMapping.builder()
                        .runId1(jobRunId1)
                        .runIds(Set.of(jobRunId1))
                        .build(),
                IntermediateMapping.builder()
                        .runId1(jobRunId2)
                        .runIds(Set.of(jobRunId2))
                        .build(),
                IntermediateMapping.builder()
                        .runId1(jobRunId3)
                        .runIds(Set.of(jobRunId3))
                        .build());
        assertThat(individualCorrelatedArtifacts).containsExactly(IntermediateMapping.builder()
                .runId1(jobRunId1)
                .runIds(Set.of(jobRunId1))
                .build());
    }

    @Test
    public void testHashCorrelation() throws SQLException {
        CiCdJobRunArtifact artifact1 = CiCdJobRunArtifact.builder()
                .cicdJobRunId(jobRunId1)
                .name("test1")
                .input(true)
                .output(true)
                .type("container")
                .location("loc1")
                .hash("hashA")
                .qualifier("v1")
                .metadata(Map.of("a", "b"))
                .build();
        CiCdJobRunArtifact artifact2 = CiCdJobRunArtifact.builder()
                .cicdJobRunId(jobRunId2)
                .name("test2")
                .input(true)
                .output(true)
                .type("container")
                .location("loc2")
                .hash("hashA")
                .qualifier("v2")
                .build();
        CiCdJobRunArtifact artifact3 = CiCdJobRunArtifact.builder()
                .cicdJobRunId(jobRunId3)
                .name("test3")
                .input(true)
                .output(true)
                .type("container")
                .location("loc2")
                .hash("hashB")
                .qualifier("v2")
                .build();

        String a1 = artifactsDatabaseService.insert(COMPANY, artifact1);
        String a2 = artifactsDatabaseService.insert(COMPANY, artifact2);
        String a3 = artifactsDatabaseService.insert(COMPANY, artifact3);

        CicdArtifactCorrelationSettings settings = CicdArtifactCorrelationSettings.builder()
                .correlationHashDefault(true)
                .build();
        var correlationService = new CicdJobRunArtifactCorrelationService(dataSource, mappingDatabaseService, settings, ciCdJobRunArtifactsDatabaseService);

        List<IntermediateMapping> correlatedArtifacts = correlationService.getCorrelatedArtifacts(COMPANY, 0, 10);
        List<IntermediateMapping> individualCorrelatedArtifacts = correlationService.getIndividualCorrelatedArtifacts(COMPANY, getJobRun(jobRunId1), List.of(a1));

        System.out.println("a1=" + a1);
        System.out.println("a2=" + a2);
        System.out.println("a3=" + a3);
        System.out.println("jobRun1=" + jobRunId1);
        System.out.println("jobRun2=" + jobRunId2);
        System.out.println("jobRun3=" + jobRunId3);
        DefaultObjectMapper.prettyPrint(correlatedArtifacts);

        assertThat(correlatedArtifacts).containsExactlyInAnyOrder(
                IntermediateMapping.builder()
                        .runId1(jobRunId1)
                        .runIds(Set.of(jobRunId1, jobRunId2))
                        .build(),
                IntermediateMapping.builder()
                        .runId1(jobRunId2)
                        .runIds(Set.of(jobRunId1, jobRunId2))
                        .build(),
                IntermediateMapping.builder()
                        .runId1(jobRunId3)
                        .runIds(Set.of(jobRunId3))
                        .build());
        assertThat(individualCorrelatedArtifacts).containsExactlyInAnyOrder(
                IntermediateMapping.builder()
                        .runId1(jobRunId1)
                        .runIds(Set.of(jobRunId2))
                        .build(),
                IntermediateMapping.builder()
                        .runId1(jobRunId2)
                        .runIds(Set.of(jobRunId1))
                        .build()
        );
    }

    @Test
    public void testHashAndIdentityCorrelation() throws SQLException {
        CiCdJobRunArtifact artifact1 = CiCdJobRunArtifact.builder()
                .cicdJobRunId(jobRunId1)
                .name("test1")
                .input(true)
                .output(true)
                .type("container")
                .location("loc1")
                .hash("hashA")
                .qualifier("v1")
                .build();
        CiCdJobRunArtifact artifact2 = CiCdJobRunArtifact.builder()
                .cicdJobRunId(jobRunId2)
                .name("test2")
                .input(true)
                .output(true)
                .type("container")
                .location("loc2")
                .hash("hashA")
                .qualifier("v2")
                .build();

        String a1 = artifactsDatabaseService.insert(COMPANY, artifact1);
        String a2 = artifactsDatabaseService.insert(COMPANY, artifact2);

        CicdArtifactCorrelationSettings settings = CicdArtifactCorrelationSettings.builder()
                .correlationHashDefault(true)
                .correlationIdentityDefault(true)
                .build();
        var correlationService = new CicdJobRunArtifactCorrelationService(dataSource, mappingDatabaseService, settings, ciCdJobRunArtifactsDatabaseService);

        List<IntermediateMapping> correlatedArtifacts = correlationService.getCorrelatedArtifacts(COMPANY, 0, 10);
        List<IntermediateMapping> individualCorrelatedArtifacts1 = correlationService.getIndividualCorrelatedArtifacts(COMPANY, getJobRun(jobRunId1), List.of(a1));
        List<IntermediateMapping> individualCorrelatedArtifacts3 = correlationService.getIndividualCorrelatedArtifacts(COMPANY, getJobRun(jobRunId3), List.of());

        System.out.println("a1=" + a1);
        System.out.println("a2=" + a2);
        System.out.println("jobRun1=" + jobRunId1);
        System.out.println("jobRun2=" + jobRunId2);
        System.out.println("jobRun3=" + jobRunId3);
        DefaultObjectMapper.prettyPrint(correlatedArtifacts);

        assertThat(correlatedArtifacts).containsExactlyInAnyOrder(
                IntermediateMapping.builder()
                        .runId1(jobRunId1)
                        .runIds(Set.of(jobRunId1, jobRunId2))
                        .build(),
                IntermediateMapping.builder()
                        .runId1(jobRunId2)
                        .runIds(Set.of(jobRunId1, jobRunId2))
                        .build(),
                IntermediateMapping.builder()
                        .runId1(jobRunId3)
                        .runIds(Set.of(jobRunId3))
                        .build());
        assertThat(individualCorrelatedArtifacts1).containsExactlyInAnyOrder(
                IntermediateMapping.builder()
                        .runId1(jobRunId1)
                        .runIds(Set.of(jobRunId2))
                        .build(),
                IntermediateMapping.builder()
                        .runId1(jobRunId1)
                        .runIds(Set.of(jobRunId1))
                        .build(),
                IntermediateMapping.builder()
                        .runId1(jobRunId2)
                        .runIds(Set.of(jobRunId1))
                        .build()
        );
        assertThat(individualCorrelatedArtifacts3).containsExactlyInAnyOrder(
                IntermediateMapping.builder()
                        .runId1(jobRunId3)
                        .runIds(Set.of(jobRunId3))
                        .build()
        );
    }

    @Test
    public void testNameTagCorrelation() throws SQLException {
        CiCdJobRunArtifact artifact1 = CiCdJobRunArtifact.builder()
                .cicdJobRunId(jobRunId1)
                .name("test")
                .input(true)
                .output(true)
                .type("container")
                .location("loc1")
                .hash("hashA")
                .qualifier("v1")
                .build();
        CiCdJobRunArtifact artifact2 = CiCdJobRunArtifact.builder()
                .cicdJobRunId(jobRunId2)
                .name("test")
                .input(true)
                .output(true)
                .type("container")
                .location("loc2")
                .hash("hashA")
                .qualifier("v2")
                .build();
        CiCdJobRunArtifact artifact3 = CiCdJobRunArtifact.builder()
                .cicdJobRunId(jobRunId3)
                .name("test")
                .input(true)
                .output(true)
                .type("container")
                .location("loc3")
                .hash("hashB")
                .qualifier("v2")
                .build();

        String a1 = artifactsDatabaseService.insert(COMPANY, artifact1);
        String a2 = artifactsDatabaseService.insert(COMPANY, artifact2);
        String a3 = artifactsDatabaseService.insert(COMPANY, artifact3);

        CicdArtifactCorrelationSettings settings = CicdArtifactCorrelationSettings.builder()
                .correlationNameQualifierDefault(true)
                .build();
        var correlationService = new CicdJobRunArtifactCorrelationService(dataSource, mappingDatabaseService, settings, ciCdJobRunArtifactsDatabaseService);

        List<IntermediateMapping> correlatedArtifacts = correlationService.getCorrelatedArtifacts(COMPANY, 0, 10);
        List<IntermediateMapping> individualCorrelatedArtifacts1 = correlationService.getIndividualCorrelatedArtifacts(COMPANY, getJobRun(jobRunId1), List.of(a1));
        List<IntermediateMapping> individualCorrelatedArtifacts3 = correlationService.getIndividualCorrelatedArtifacts(COMPANY, getJobRun(jobRunId3), List.of(a3));

        System.out.println("a1=" + a1);
        System.out.println("a2=" + a2);
        System.out.println("a3=" + a3);
        System.out.println("jobRun1=" + jobRunId1);
        System.out.println("jobRun2=" + jobRunId2);
        System.out.println("jobRun3=" + jobRunId3);
        DefaultObjectMapper.prettyPrint(correlatedArtifacts);

        assertThat(correlatedArtifacts).containsExactlyInAnyOrder(
                IntermediateMapping.builder()
                        .runId1(jobRunId1)
                        .runIds(Set.of(jobRunId1))
                        .build(),
                IntermediateMapping.builder()
                        .runId1(jobRunId2)
                        .runIds(Set.of(jobRunId2, jobRunId3))
                        .build(),
                IntermediateMapping.builder()
                        .runId1(jobRunId3)
                        .runIds(Set.of(jobRunId2, jobRunId3))
                        .build());

        assertThat(individualCorrelatedArtifacts1).isEmpty();
        assertThat(individualCorrelatedArtifacts3).containsExactlyInAnyOrder(
                IntermediateMapping.builder()
                        .runId1(jobRunId3)
                        .runIds(Set.of(jobRunId2))
                        .build(),
                IntermediateMapping.builder()
                        .runId1(jobRunId2)
                        .runIds(Set.of(jobRunId3))
                        .build()
        );
    }

    @Test
    public void testNameTagLocCorrelationNoMatch() throws SQLException {
        CiCdJobRunArtifact artifact1 = CiCdJobRunArtifact.builder()
                .cicdJobRunId(jobRunId1)
                .name("test")
                .input(true)
                .output(true)
                .type("container")
                .location("loc1")
                .hash("hashA")
                .qualifier("v1")
                .build();
        CiCdJobRunArtifact artifact2 = CiCdJobRunArtifact.builder()
                .cicdJobRunId(jobRunId2)
                .name("test")
                .input(true)
                .output(true)
                .type("container")
                .location("loc2")
                .hash("hashA")
                .qualifier("v2")
                .build();
        CiCdJobRunArtifact artifact3 = CiCdJobRunArtifact.builder()
                .cicdJobRunId(jobRunId3)
                .name("test")
                .input(true)
                .output(true)
                .type("container")
                .location("loc3")
                .hash("hashB")
                .qualifier("v2")
                .build();

        String a1 = artifactsDatabaseService.insert(COMPANY, artifact1);
        String a2 = artifactsDatabaseService.insert(COMPANY, artifact2);
        String a3 = artifactsDatabaseService.insert(COMPANY, artifact3);

        CicdArtifactCorrelationSettings settings = CicdArtifactCorrelationSettings.builder()
                .correlationNameQualifierLocationDefault(true)
                .build();
        var correlationService = new CicdJobRunArtifactCorrelationService(dataSource, mappingDatabaseService, settings, ciCdJobRunArtifactsDatabaseService);

        List<IntermediateMapping> correlatedArtifacts = correlationService.getCorrelatedArtifacts(COMPANY, 0, 10);
        List<IntermediateMapping> individualCorrelatedArtifacts1 = correlationService.getIndividualCorrelatedArtifacts(COMPANY, getJobRun(jobRunId1), List.of(a1));
        List<IntermediateMapping> individualCorrelatedArtifacts2 = correlationService.getIndividualCorrelatedArtifacts(COMPANY, getJobRun(jobRunId2), List.of(a2));
        List<IntermediateMapping> individualCorrelatedArtifacts3 = correlationService.getIndividualCorrelatedArtifacts(COMPANY, getJobRun(jobRunId3), List.of(a3));

        System.out.println("a1=" + a1);
        System.out.println("a2=" + a2);
        System.out.println("a3=" + a3);
        System.out.println("jobRun1=" + jobRunId1);
        System.out.println("jobRun2=" + jobRunId2);
        System.out.println("jobRun3=" + jobRunId3);
        DefaultObjectMapper.prettyPrint(correlatedArtifacts);

        assertThat(correlatedArtifacts).containsExactlyInAnyOrder(
                IntermediateMapping.builder()
                        .runId1(jobRunId1)
                        .runIds(Set.of(jobRunId1))
                        .build(),
                IntermediateMapping.builder()
                        .runId1(jobRunId2)
                        .runIds(Set.of(jobRunId2))
                        .build(),
                IntermediateMapping.builder()
                        .runId1(jobRunId3)
                        .runIds(Set.of(jobRunId3))
                        .build());
        assertThat(individualCorrelatedArtifacts1).isEmpty();
        assertThat(individualCorrelatedArtifacts2).isEmpty();
        assertThat(individualCorrelatedArtifacts3).isEmpty();
    }

    @Test
    public void testNameTagLocCorrelationMatch() throws SQLException {
        CiCdJobRunArtifact artifact1 = CiCdJobRunArtifact.builder()
                .cicdJobRunId(jobRunId1)
                .name("test")
                .input(true)
                .output(true)
                .type("container")
                .location("loc1")
                .hash("hashA")
                .qualifier("v1")
                .build();
        CiCdJobRunArtifact artifact2 = CiCdJobRunArtifact.builder()
                .cicdJobRunId(jobRunId2)
                .name("test")
                .input(true)
                .output(true)
                .type("container")
                .location("loc2")
                .hash("hashA")
                .qualifier("v2")
                .build();
        CiCdJobRunArtifact artifact3 = CiCdJobRunArtifact.builder()
                .cicdJobRunId(jobRunId3)
                .name("test")
                .input(true)
                .output(true)
                .type("container")
                .location("loc2")
                .hash("hashB")
                .qualifier("v2")
                .build();

        String a1 = artifactsDatabaseService.insert(COMPANY, artifact1);
        String a2 = artifactsDatabaseService.insert(COMPANY, artifact2);
        String a3 = artifactsDatabaseService.insert(COMPANY, artifact3);

        CicdArtifactCorrelationSettings settings = CicdArtifactCorrelationSettings.builder()
                .correlationNameQualifierLocationDefault(true)
                .build();
        var correlationService = new CicdJobRunArtifactCorrelationService(dataSource, mappingDatabaseService, settings, ciCdJobRunArtifactsDatabaseService);

        List<IntermediateMapping> correlatedArtifacts = correlationService.getCorrelatedArtifacts(COMPANY, 0, 10);
        List<IntermediateMapping> individualCorrelatedArtifacts1 = correlationService.getIndividualCorrelatedArtifacts(COMPANY, getJobRun(jobRunId1), List.of(a1));
        List<IntermediateMapping> individualCorrelatedArtifacts2 = correlationService.getIndividualCorrelatedArtifacts(COMPANY, getJobRun(jobRunId2), List.of(a2));
        List<IntermediateMapping> individualCorrelatedArtifacts3 = correlationService.getIndividualCorrelatedArtifacts(COMPANY, getJobRun(jobRunId3), List.of(a3));


        System.out.println("a1=" + a1);
        System.out.println("a2=" + a2);
        System.out.println("a3=" + a3);
        System.out.println("jobRun1=" + jobRunId1);
        System.out.println("jobRun2=" + jobRunId2);
        System.out.println("jobRun3=" + jobRunId3);
        DefaultObjectMapper.prettyPrint(correlatedArtifacts);

        assertThat(correlatedArtifacts).containsExactlyInAnyOrder(
                IntermediateMapping.builder()
                        .runId1(jobRunId1)
                        .runIds(Set.of(jobRunId1))
                        .build(),
                IntermediateMapping.builder()
                        .runId1(jobRunId2)
                        .runIds(Set.of(jobRunId2, jobRunId3))
                        .build(),
                IntermediateMapping.builder()
                        .runId1(jobRunId3)
                        .runIds(Set.of(jobRunId2, jobRunId3))
                        .build());
        assertThat(individualCorrelatedArtifacts1).isEmpty();
        assertThat(individualCorrelatedArtifacts3).containsExactlyInAnyOrder(
                IntermediateMapping.builder()
                        .runId1(jobRunId3)
                        .runIds(Set.of(jobRunId2))
                        .build(),
                IntermediateMapping.builder()
                        .runId1(jobRunId2)
                        .runIds(Set.of(jobRunId3))
                        .build()
        );
        assertThat(individualCorrelatedArtifacts2).containsExactlyInAnyOrder(
                IntermediateMapping.builder()
                        .runId1(jobRunId2)
                        .runIds(Set.of(jobRunId3))
                        .build(),
                IntermediateMapping.builder()
                        .runId1(jobRunId3)
                        .runIds(Set.of(jobRunId2))
                        .build()
        );
    }

    @Test
    public void testAllCorrelations() throws SQLException {
        CiCdJobRunArtifact artifact1 = CiCdJobRunArtifact.builder()
                .cicdJobRunId(jobRunId1)
                .name("test")
                .input(true)
                .output(true)
                .type("container")
                .location("loc1")
                .hash("hashA")
                .qualifier("v1")
                .build();
        CiCdJobRunArtifact artifact2 = CiCdJobRunArtifact.builder()
                .cicdJobRunId(jobRunId2)
                .name("test")
                .input(true)
                .output(true)
                .type("container")
                .location("loc2")
                .hash("hashB")
                .qualifier("v2")
                .build();
        CiCdJobRunArtifact artifact3 = CiCdJobRunArtifact.builder()
                .cicdJobRunId(jobRunId3)
                .name("test")
                .input(true)
                .output(true)
                .type("container")
                .location("loc2")
                .hash("hashA")
                .qualifier("v2")
                .build();

        String a1 = artifactsDatabaseService.insert(COMPANY, artifact1);
        String a2 = artifactsDatabaseService.insert(COMPANY, artifact2);
        String a3 = artifactsDatabaseService.insert(COMPANY, artifact3);

        CicdArtifactCorrelationSettings settings = CicdArtifactCorrelationSettings.builder()
                .correlationNameQualifierLocationDefault(true)
                .correlationHashDefault(true)
                .correlationIdentityDefault(true)
                .build();
        var correlationService = new CicdJobRunArtifactCorrelationService(dataSource, mappingDatabaseService, settings, ciCdJobRunArtifactsDatabaseService);

        List<IntermediateMapping> correlatedArtifacts = correlationService.getCorrelatedArtifacts(COMPANY, 0, 10);
        List<IntermediateMapping> individualCorrelatedArtifacts1 = correlationService.getIndividualCorrelatedArtifacts(COMPANY, getJobRun(jobRunId1), List.of(a1));
        List<IntermediateMapping> individualCorrelatedArtifacts2 = correlationService.getIndividualCorrelatedArtifacts(COMPANY, getJobRun(jobRunId2), List.of(a2));
        List<IntermediateMapping> individualCorrelatedArtifacts3 = correlationService.getIndividualCorrelatedArtifacts(COMPANY, getJobRun(jobRunId3), List.of(a3));


        System.out.println("a1=" + a1);
        System.out.println("a2=" + a2);
        System.out.println("a3=" + a3);
        System.out.println("jobRun1=" + jobRunId1);
        System.out.println("jobRun2=" + jobRunId2);
        System.out.println("jobRun3=" + jobRunId3);
        DefaultObjectMapper.prettyPrint(correlatedArtifacts);

        assertThat(correlatedArtifacts).containsExactlyInAnyOrder(
                IntermediateMapping.builder()
                        .runId1(jobRunId1)
                        .runIds(Set.of(jobRunId1, jobRunId3))
                        .build(),
                IntermediateMapping.builder()
                        .runId1(jobRunId2)
                        .runIds(Set.of(jobRunId2, jobRunId3))
                        .build(),
                IntermediateMapping.builder()
                        .runId1(jobRunId3)
                        .runIds(Set.of(jobRunId1, jobRunId2, jobRunId3))
                        .build());
        assertThat(individualCorrelatedArtifacts1).containsExactlyInAnyOrder(
                IntermediateMapping.builder()
                        .runId1(jobRunId1)
                        .runIds(Set.of(jobRunId3))
                        .build(),
                IntermediateMapping.builder()
                        .runId1(jobRunId1)
                        .runIds(Set.of(jobRunId1))
                        .build(),
                IntermediateMapping.builder()
                        .runId1(jobRunId3)
                        .runIds(Set.of(jobRunId1))
                        .build()
        );
        assertThat(individualCorrelatedArtifacts2).containsExactlyInAnyOrder(
                IntermediateMapping.builder()
                        .runId1(jobRunId2)
                        .runIds(Set.of(jobRunId3))
                        .build(),
                IntermediateMapping.builder()
                        .runId1(jobRunId2)
                        .runIds(Set.of(jobRunId2))
                        .build(),
                IntermediateMapping.builder()
                        .runId1(jobRunId3)
                        .runIds(Set.of(jobRunId2))
                        .build()
        );
        assertThat(individualCorrelatedArtifacts3).containsExactlyInAnyOrder(
                IntermediateMapping.builder()
                        .runId1(jobRunId3)
                        .runIds(Set.of(jobRunId1, jobRunId2))
                        .build(),
                IntermediateMapping.builder()
                        .runId1(jobRunId3)
                        .runIds(Set.of(jobRunId3))
                        .build(),
                IntermediateMapping.builder()
                        .runId1(jobRunId1)
                        .runIds(Set.of(jobRunId3))
                        .build(),
                IntermediateMapping.builder()
                        .runId1(jobRunId2)
                        .runIds(Set.of(jobRunId3))
                        .build()
        );
    }
}