package io.levelops.controlplane.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.controlplane.database.DefaultTriggeredJobDatabaseService;
import io.levelops.controlplane.database.JobDatabaseService;
import io.levelops.controlplane.database.TriggeredJobDatabaseService;
import io.levelops.controlplane.discovery.AgentRegistryService;
import io.levelops.controlplane.models.DbTriggeredJob;
import io.levelops.ingestion.merging.IngestionResultMergingService;
import io.levelops.ingestion.models.controlplane.JobStatus;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultTriggeredJobServiceTest {

    @Rule
    public SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    private TriggeredJobDatabaseService triggeredJobDatabaseService;
    private JobDatabaseService jobDatabaseService;
    private JobTrackingService jobTrackingService;
    private ObjectMapper objectMapper;
    private DefaultTriggeredJobService defaultTriggeredJobService;

    @Mock
    AgentRegistryService agentRegistryService;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        DataSource dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        NamedParameterJdbcTemplate template = new NamedParameterJdbcTemplate(dataSource);
        objectMapper = DefaultObjectMapper.get();
        triggeredJobDatabaseService = new DefaultTriggeredJobDatabaseService(template, DefaultObjectMapper.get());
        jobDatabaseService = new JobDatabaseService(template, DefaultObjectMapper.get());
        IngestionResultMergingService ingestionResultMergingService = new IngestionResultMergingService();
        jobTrackingService = new JobTrackingService(objectMapper, jobDatabaseService, agentRegistryService, ingestionResultMergingService);
        defaultTriggeredJobService = new DefaultTriggeredJobService(jobTrackingService, triggeredJobDatabaseService);

        List.of(
                "CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";",
                "DROP SCHEMA IF EXISTS control_plane CASCADE; ",
                "CREATE SCHEMA control_plane; "
        ).forEach(template.getJdbcTemplate()::execute);

        triggeredJobDatabaseService.ensureTableExistence();
        jobDatabaseService.ensureTableExistence();
    }

    private void insertJob(String jobId, JobStatus status) {
        triggeredJobDatabaseService.getTemplate().getJdbcTemplate().execute(String.format("INSERT INTO control_plane.jobs (id, status) VALUES ('%s'::uuid, '%s'::job_status_t);", jobId, status));
    }

    private void deleteJob(String jobId) {
        triggeredJobDatabaseService.getTemplate().getJdbcTemplate().execute(String.format("DELETE from control_plane.jobs where id = '%s'::uuid", jobId));
    }

    @Test
    public void testFailedReonboardingJob() {
        String jobId1 = "10000000-1fdd-439a-bd99-aa515bda1bf9";
        String jobId2 = "20000000-1fdd-439a-bd99-aa515bda1bf9";
        String jobId3 = "30000000-1fdd-439a-bd99-aa515bda1bf9";
        String triggerId = "f0240868-b1ff-4ce1-b00a-a2a48cb9d51a";
        String iterationId1 = "10000000-252b-41a9-925a-3faf8da4d7cb";

        // Sanity test with empty jobs
        var results = defaultTriggeredJobService.retrieveLatestSuccessfulTriggeredJobs(triggerId, false, true);
        assertThat(results.map(DbTriggeredJob::getJobId)).isEmpty();

        triggeredJobDatabaseService.createTriggeredJob(jobId1, triggerId, iterationId1, 10L, false);
        insertJob(jobId1, JobStatus.FAILURE);
        triggeredJobDatabaseService.createTriggeredJob(jobId2, triggerId, iterationId1, 20L, true);
        insertJob(jobId2, JobStatus.FAILURE);
        triggeredJobDatabaseService.createTriggeredJob(jobId3, triggerId, iterationId1, 30L, true);
        insertJob(jobId3, JobStatus.SUCCESS);

        // Initial onboarding job has failed, but there is a subsequent successful job
        results = defaultTriggeredJobService.retrieveLatestSuccessfulTriggeredJobs(triggerId, false, true);
        assertThat(results.map(DbTriggeredJob::getJobId)).containsExactly(jobId3);

        // All jobs are failures
        deleteJob(jobId3);
        insertJob(jobId3, JobStatus.FAILURE);
        results = defaultTriggeredJobService.retrieveLatestSuccessfulTriggeredJobs(triggerId, false, true);
        assertThat(results.map(DbTriggeredJob::getJobId)).isEmpty();

        // test onlySuccessfulResults = false
        results = defaultTriggeredJobService.retrieveLatestSuccessfulTriggeredJobs(triggerId, false, false);
        assertThat(results.map(DbTriggeredJob::getJobId)).containsExactly(jobId3, jobId2, jobId1);
    }
}
