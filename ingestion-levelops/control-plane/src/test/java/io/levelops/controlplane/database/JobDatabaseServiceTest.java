package io.levelops.controlplane.database;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.controlplane.database.JobDatabaseService.JobFilter;
import io.levelops.controlplane.models.DbJob;
import io.levelops.controlplane.models.DbJobUpdate;
import io.levelops.ingestion.models.controlplane.JobStatus;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class JobDatabaseServiceTest {

    @Rule
    public SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    JobDatabaseService jobDatabaseService;

    @Before
    public void setUp() throws Exception {
        DataSource dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        NamedParameterJdbcTemplate template = new NamedParameterJdbcTemplate(dataSource);
        jobDatabaseService = new JobDatabaseService(template, DefaultObjectMapper.get());
        List.of(
                "CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";",
                "DROP SCHEMA IF EXISTS control_plane CASCADE; ",
                "CREATE SCHEMA control_plane; "
        ).forEach(template.getJdbcTemplate()::execute);

        jobDatabaseService.ensureTableExistence();
    }

    @Test
    public void createJob() {
        String id = "faa9cca1-600a-4262-9c2c-519fa6d9e66f";
        jobDatabaseService.createJob(id, "ctrl", "{\"some\":\"query\"}", null, null, null, null, null);

        DbJob job = jobDatabaseService.getJobById(id).orElse(null);
        assertThat(job.getId()).isEqualTo(id);
        assertThat(job.getStatus()).isEqualTo(JobStatus.UNASSIGNED);
        assertThat(job.getTenantId()).isEqualTo("");
        assertThat(job.getIntegrationId()).isEqualTo("");
        assertThat(job.getReserved()).isEqualTo(false);
        assertThat(job.getLevel()).isEqualTo(0);
        assertThat(job.getQuery()).isEqualTo(Map.of("some", "query"));
        assertThat(job.getCallbackUrl()).isEqualTo("");
        assertThat(job.getControllerName()).isEqualTo("ctrl");
        assertThat(job.getCreatedAt()).isGreaterThan(0);
        assertThat(job.getResult()).isNull();

        DefaultObjectMapper.prettyPrint(job);
    }


    @Test
    public void createJob2() {
        String id = "faa9cca1-600a-4262-9c2c-519fa6d9e66f";
        jobDatabaseService.createJob(id, "ctrl", "{\"some\":\"query\"}", "coke", "123", true, "url", Set.of("a", "b", "c"));

        DbJob job = jobDatabaseService.getJobById(id).orElse(null);
        assertThat(job.getId()).isEqualTo(id);
        assertThat(job.getStatus()).isEqualTo(JobStatus.UNASSIGNED);
        assertThat(job.getTenantId()).isEqualTo("coke");
        assertThat(job.getIntegrationId()).isEqualTo("123");
        assertThat(job.getReserved()).isEqualTo(true);
        assertThat(job.getLevel()).isEqualTo(0);
        assertThat(job.getQuery()).isEqualTo(Map.of("some", "query"));
        assertThat(job.getCallbackUrl()).isEqualTo("url");
        assertThat(job.getControllerName()).isEqualTo("ctrl");
        assertThat(job.getCreatedAt()).isGreaterThan(0);
        assertThat(job.getTags()).containsExactlyInAnyOrder("a", "b", "c");

        DefaultObjectMapper.prettyPrint(job);
    }

    @Test
    public void testGet() throws JsonProcessingException {
        String id = "faa9cca1-600a-4262-9c2c-519fa6d9e66f";
        jobDatabaseService.createJob(id, "ctrl", "{\"some\":\"query\"}", "coke", "123", true, "url", Set.of("a", "b", "c"));
        jobDatabaseService.updateJob(id, DbJobUpdate.builder()
                .status(JobStatus.FAILURE)
                .agentId("288450bc-b319-45ea-9cad-864e00f7c070")
                .result(Map.of("data", true))
                .ingestionFailures(List.of())
                .build());
        DbJob job = jobDatabaseService.getJobById(id).orElse(null);
        DefaultObjectMapper.prettyPrint(job);

        assertThat(job.getId()).isEqualTo(id);
        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILURE);
        assertThat(job.getTenantId()).isEqualTo("coke");
        assertThat(job.getIntegrationId()).isEqualTo("123");
        assertThat(job.getReserved()).isEqualTo(true);
        assertThat(job.getLevel()).isEqualTo(0);
        assertThat(job.getAttemptCount()).isEqualTo(0);
        assertThat(job.getAttemptMax()).isEqualTo(0);
        assertThat(job.getQuery()).isEqualTo(Map.of("some", "query"));
        assertThat(job.getCallbackUrl()).isEqualTo("url");
        assertThat(job.getControllerName()).isEqualTo("ctrl");
        assertThat(job.getCreatedAt()).isGreaterThan(0);
        assertThat(job.getStatusChangedAt()).isGreaterThan(0);
        assertThat(DefaultObjectMapper.get().writeValueAsString(job.getResult())).isEqualTo("{\"data\":true}");
        assertThat(job.getTags()).containsExactlyInAnyOrder("a", "b", "c");
    }

    @Test
    public void updateJobStatus() {
        String id = "faa9cca1-600a-4262-9c2c-519fa6d9e66f";
        jobDatabaseService.createJob(id, "ctrl", "{\"some\":\"query\"}", "coke", "123", false, "url", null);
        assertThat(jobDatabaseService.getJobById(id).orElseThrow().getStatus()).isEqualTo(JobStatus.UNASSIGNED);

        jobDatabaseService.updateJob(id, DbJobUpdate.builder()
                .status(JobStatus.SCHEDULED).build());
        assertThat(jobDatabaseService.getJobById(id).orElseThrow().getStatus()).isEqualTo(JobStatus.SCHEDULED);

        jobDatabaseService.updateJob(id, DbJobUpdate.builder()
                .status(JobStatus.PENDING)
                .agentId("5dea389c-c161-4833-8ff7-fefc7bc8c001").build());
        assertThat(jobDatabaseService.getJobById(id).orElseThrow().getStatus()).isEqualTo(JobStatus.PENDING);
        assertThat(jobDatabaseService.getJobById(id).orElseThrow().getAgentId()).isEqualTo("5dea389c-c161-4833-8ff7-fefc7bc8c001");

        jobDatabaseService.updateJob(id, DbJobUpdate.builder()
                .status(JobStatus.SUCCESS)
                .agentId("5dea389c-c161-4833-8ff7-fefc7bc8c001")
                .result(Map.of("data", true))
                .ingestionFailures(List.of()).build());
        assertThat(jobDatabaseService.getJobById(id).orElseThrow().getStatus()).isEqualTo(JobStatus.SUCCESS);
        assertThat(jobDatabaseService.getJobById(id).orElseThrow().getAgentId()).isEqualTo("5dea389c-c161-4833-8ff7-fefc7bc8c001");
        assertThat(jobDatabaseService.getJobById(id).orElseThrow().getResult()).isEqualTo(Map.of("data", true));

        jobDatabaseService.updateJob(id, DbJobUpdate.builder()
                .status(JobStatus.FAILURE)
                .agentId("5dea389c-c161-4833-8ff7-fefc7bc8c001")
                .incrementAttemptCount(true)
                .statusCondition(JobStatus.SUCCESS)
                .build());
        assertThat(jobDatabaseService.getJobById(id).orElseThrow().getStatus()).isEqualTo(JobStatus.FAILURE);
        assertThat(jobDatabaseService.getJobById(id).orElseThrow().getAgentId()).isEqualTo("5dea389c-c161-4833-8ff7-fefc7bc8c001");
        assertThat(jobDatabaseService.getJobById(id).orElseThrow().getAttemptCount()).isEqualTo(1);

        jobDatabaseService.updateJob(id, DbJobUpdate.builder()
                .status(JobStatus.UNASSIGNED)
                .agentId("5dea389c-c161-4833-8ff7-fefc7bc8c001")
                .incrementAttemptCount(true)
                .statusCondition(JobStatus.FAILURE)
                .build());
        assertThat(jobDatabaseService.getJobById(id).orElseThrow().getStatus()).isEqualTo(JobStatus.UNASSIGNED);
        assertThat(jobDatabaseService.getJobById(id).orElseThrow().getAgentId()).isEqualTo("5dea389c-c161-4833-8ff7-fefc7bc8c001");
        assertThat(jobDatabaseService.getJobById(id).orElseThrow().getAttemptCount()).isEqualTo(2);

        jobDatabaseService.updateJob(id, DbJobUpdate.builder()
                .status(JobStatus.FAILURE)
                .agentId("5dea389c-c161-4833-8ff7-fefc7bc8c001")
                .incrementAttemptCount(true)
                .statusCondition(JobStatus.SUCCESS)
                .build());
        assertThat(jobDatabaseService.getJobById(id).orElseThrow().getStatus()).isEqualTo(JobStatus.UNASSIGNED);
        assertThat(jobDatabaseService.getJobById(id).orElseThrow().getAgentId()).isEqualTo("5dea389c-c161-4833-8ff7-fefc7bc8c001");
        assertThat(jobDatabaseService.getJobById(id).orElseThrow().getAttemptCount()).isEqualTo(2);

        jobDatabaseService.updateJob(id, DbJobUpdate.builder()
                .intermediateState(Map.of("test", "state"))
                .build());
        assertThat(jobDatabaseService.getJobById(id).orElseThrow().getIntermediateState()).isEqualTo(Map.of("test", "state"));
    }

    @Test
    public void filterJobs() {
        String id1 = "10000000-0000-0000-0000-000000000000";
        String id2 = "20000000-0000-0000-0000-000000000000";
        String id3 = "30000000-0000-0000-0000-000000000000";
        String id4 = "40000000-0000-0000-0000-000000000000";
        String id5 = "50000000-0000-0000-0000-000000000000";
        jobDatabaseService.createJob(id1, "ctrl1", "{\"some\":\"query\"}", null, null, true, null, null);
        jobDatabaseService.updateJob(id1, DbJobUpdate.builder()
                .status(JobStatus.UNASSIGNED)
                .statusCondition(JobStatus.UNASSIGNED)
                .incrementAttemptCount(true).build());

        jobDatabaseService.createJob(id2, "ctrl2", "{\"some\":\"query\"}", null, null, null, null, Set.of("a"));
        jobDatabaseService.updateJob(id2, DbJobUpdate.builder().status(JobStatus.PENDING).build());

        jobDatabaseService.createJob(id3, "ctrl3", "{\"some\":\"query\"}", null, null, null, null, Set.of("b"));
        jobDatabaseService.updateJob(id3, DbJobUpdate.builder().status(JobStatus.SUCCESS).build());

        jobDatabaseService.createJob(id4, "ctrl4", "{\"some\":\"query\"}", "a", "b", true, null, Set.of("a", "b"));
        jobDatabaseService.updateJob(id4, DbJobUpdate.builder().status(JobStatus.SUCCESS).build());

        jobDatabaseService.createJob(id5, "ctrl5", "{\"some\":\"query\"}", "a", "c", null, null, null);
        jobDatabaseService.updateJob(id5, DbJobUpdate.builder().status(JobStatus.SUCCESS).build());

        // no filters
        assertThat(jobDatabaseService.streamJobs(10, null).map(DbJob::getId)).containsExactly(id1, id2, id3, id4, id5);

        // test status filter
        assertThat(jobDatabaseService.filterJobs(0, 10, JobFilter.builder().statuses(List.of(JobStatus.UNASSIGNED)).build())).hasSize(1);
        assertThat(jobDatabaseService.filterJobs(0, 10, JobFilter.builder().statuses(List.of(JobStatus.PENDING)).build())).hasSize(1);
        assertThat(jobDatabaseService.filterJobs(0, 10, JobFilter.builder().statuses(List.of(JobStatus.SUCCESS)).build())).hasSize(3);
        assertThat(jobDatabaseService.filterJobs(0, 10, JobFilter.builder().statuses(List.of(JobStatus.SUCCESS, JobStatus.PENDING)).build())).hasSize(4);

        // test reserved filter
        assertThat(jobDatabaseService.filterJobs(0, 10, null)).hasSize(5);
        assertThat(jobDatabaseService.filterJobs(0, 10, JobFilter.builder().reserved(false).build())).hasSize(3);
        assertThat(jobDatabaseService.filterJobs(0, 10, JobFilter.builder().reserved(true).build())).hasSize(2);

        // test before filter
        assertThat(jobDatabaseService.filterJobs(0, 10, JobFilter.builder().before(Instant.MIN).build())).hasSize(0);
        assertThat(jobDatabaseService.filterJobs(0, 10, JobFilter.builder().before(Instant.MAX).build())).hasSize(5);

        // test tenant integration filter
        assertThat(jobDatabaseService.filterJobs(0, 10, JobFilter.builder().tenantId("a").build())).hasSize(2);
        assertThat(jobDatabaseService.filterJobs(0, 10, JobFilter.builder().integrationIds(List.of("b")).build())).hasSize(5);
        assertThat(jobDatabaseService.filterJobs(0, 10, JobFilter.builder().tenantId("a").integrationIds(List.of("b")).build())).hasSize(1);
        assertThat(jobDatabaseService.filterJobs(0, 10, JobFilter.builder().tenantId("a").integrationIds(List.of("c")).build())).hasSize(1);
        assertThat(jobDatabaseService.filterJobs(0, 10, JobFilter.builder().tenantId("a").integrationIds(List.of("b", "c")).build())).hasSize(2);

        // test attempts filter
        assertThat(jobDatabaseService.streamJobs(10, JobFilter.builder().belowMaxAttemptsOrDefaultValue(0).build())).isEmpty();
        assertThat(jobDatabaseService.streamJobs(10, JobFilter.builder().belowMaxAttemptsOrDefaultValue(1).build()).map(DbJob::getId)).containsExactly(id2, id3, id4, id5);
        assertThat(jobDatabaseService.streamJobs(10, JobFilter.builder().belowMaxAttemptsOrDefaultValue(2).build()).map(DbJob::getId)).containsExactly(id1, id2, id3, id4, id5);

        // test tags filter
        assertThat(jobDatabaseService.filterJobs(0, 10, JobFilter.builder().tags(Set.of("a")).build())).hasSize(2);
        assertThat(jobDatabaseService.filterJobs(0, 10, JobFilter.builder().tags(Set.of("b")).build())).hasSize(2);
        assertThat(jobDatabaseService.filterJobs(0, 10, JobFilter.builder().tags(Set.of("a", "b")).build())).hasSize(1);
        assertThat(jobDatabaseService.filterJobs(0, 10, JobFilter.builder().tags(Set.of("a", "c")).build())).hasSize(0);

        // test controller name
        assertThat(jobDatabaseService.streamJobs( 10, JobFilter.builder().controllerNames(List.of("ctrl1")).build()).map(DbJob::getId)).containsExactly(id1);
        assertThat(jobDatabaseService.streamJobs( 10, JobFilter.builder().controllerNames(List.of("ctrl2", "ctrl5")).build()).map(DbJob::getId)).containsExactly(id2, id5);
        assertThat(jobDatabaseService.streamJobs( 10, JobFilter.builder().excludeControllerNames(List.of("ctrl2","ctrl3")).build()).map(DbJob::getId)).containsExactly(id1, id4, id5);

        // test pagination
        assertThat(jobDatabaseService.filterJobs(0, 3, null)).hasSize(3);
        assertThat(jobDatabaseService.filterJobs(3, 3, null)).hasSize(2);
    }

}