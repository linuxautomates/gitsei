package io.levelops.controlplane.database;

import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.controlplane.database.TriggeredJobDatabaseService.TriggeredJobFilter;
import io.levelops.controlplane.models.DbIteration;
import io.levelops.controlplane.models.DbTriggeredJob;
import io.levelops.ingestion.models.controlplane.JobStatus;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultTriggeredJobDatabaseServiceTest {

    @Rule
    public SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    private TriggeredJobDatabaseService triggeredJobDatabaseService;
    private JobDatabaseService jobDatabaseService;

    @Before
    public void setUp() throws Exception {
        DataSource dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        NamedParameterJdbcTemplate template = new NamedParameterJdbcTemplate(dataSource);
        triggeredJobDatabaseService = new DefaultTriggeredJobDatabaseService(template, DefaultObjectMapper.get());
        jobDatabaseService = new JobDatabaseService(template, DefaultObjectMapper.get());

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

    @Test
    public void test() {
        String jobId1 = "10000000-1fdd-439a-bd99-aa515bda1bf9";
        String jobId2 = "20000000-1fdd-439a-bd99-aa515bda1bf9";
        String triggerId = "f0240868-b1ff-4ce1-b00a-a2a48cb9d51a";
        String iterationId1 = "10000000-252b-41a9-925a-3faf8da4d7cb";
        String iterationId2 = "20000000-252b-41a9-925a-3faf8da4d7cb";
        Long iterationTs1 = 1583368231L;
        Long iterationTs2 = 2583368231L;
        boolean partial = false;

        DbTriggeredJob triggeredJob1 = DbTriggeredJob.builder()
                .jobId(jobId1)
                .triggerId(triggerId)
                .iterationId(iterationId1)
                .iterationTs(iterationTs1)
                .partial(partial)
                .build();
        DbTriggeredJob triggeredJob2 = DbTriggeredJob.builder()
                .jobId(jobId2)
                .triggerId(triggerId)
                .iterationId(iterationId2)
                .iterationTs(iterationTs2)
                .partial(partial)
                .build();

        /*
        getLatestPartialTriggeredJobsSince
        getPartialTriggeredJobsBetween
        filterTriggeredJobs
        getLatestSuccessfulTriggeredJob
        getLastSuccessfulTriggeredJobBeforeIteration
        getLatestSuccessfulTriggeredJob
         */

        // --- create and get latest
        triggeredJobDatabaseService.createTriggeredJob(jobId1, triggerId, iterationId1, iterationTs1, partial);
        insertJob(jobId1, JobStatus.SUCCESS);

        TriggeredJobFilter filter = TriggeredJobFilter.builder()
                .partial(partial)
                .build();
        assertThat(triggeredJobDatabaseService.getTriggeredJob(triggerId, filter).map(DbTriggeredJob::getJobId).orElse(null))
                .isEqualTo(jobId1);

        triggeredJobDatabaseService.createTriggeredJob(jobId2, triggerId, iterationId2, iterationTs2, partial);
        insertJob(jobId2, JobStatus.FAILURE);

        assertThat(triggeredJobDatabaseService.getTriggeredJob(triggerId, filter).map(DbTriggeredJob::getJobId).orElse(null))
                .isEqualTo(jobId2);

        // --- get by iteration id
        assertThat(triggeredJobDatabaseService.getTriggeredJobsByIterationId(iterationId1))
                .usingElementComparatorIgnoringFields("createdAt")
                .containsExactly(triggeredJob1);
        assertThat(triggeredJobDatabaseService.getTriggeredJobsByIterationId(iterationId2))
                .usingElementComparatorIgnoringFields("createdAt")
                .containsExactly(triggeredJob2);

        // --- get by trigger id
        assertThat(triggeredJobDatabaseService.streamTriggeredJobs(triggerId, null, null))
                .usingElementComparatorIgnoringFields("createdAt")
                .containsExactly(triggeredJob2, triggeredJob1);

        assertThat(triggeredJobDatabaseService.streamTriggeredJobs(triggerId, null, 1))
                .usingElementComparatorIgnoringFields("createdAt")
                .containsExactly(triggeredJob2);

        assertThat(triggeredJobDatabaseService.getIterationsByTriggerId(triggerId, 0, 10))
                .containsExactly(
                        DbIteration.builder()
                                .iterationId(iterationId2)
                                .iterationTs(iterationTs2)
                                .build(),
                        DbIteration.builder()
                                .iterationId(iterationId1)
                                .iterationTs(iterationTs1)
                                .build());

        // --- get by job id
        assertThat(triggeredJobDatabaseService.getTriggeredJobByJobId(jobId1).map(DbTriggeredJob::getIterationId).orElse(null)).isEqualTo(iterationId1);
        assertThat(triggeredJobDatabaseService.getTriggeredJobByJobId(jobId2).map(DbTriggeredJob::getIterationId).orElse(null)).isEqualTo(iterationId2);

        // --- delete
        triggeredJobDatabaseService.deleteTriggeredJobs(triggerId);
        assertThat(triggeredJobDatabaseService.streamTriggeredJobs(triggerId, null, null)).isEmpty();
    }

    @Test
    public void test2() {
        String jobId1 = "10000000-1fdd-439a-bd99-aa515bda1bf9";
        String jobId2 = "20000000-1fdd-439a-bd99-aa515bda1bf9";
        String jobId3 = "30000000-1fdd-439a-bd99-aa515bda1bf9";
        String jobId4 = "40000000-1fdd-439a-bd99-aa515bda1bf9";
        String jobId5 = "50000000-1fdd-439a-bd99-aa515bda1bf9";
        String triggerId = "f0240868-b1ff-4ce1-b00a-a2a48cb9d51a";
        String iterationId1 = "10000000-252b-41a9-925a-3faf8da4d7cb";

        TriggeredJobFilter filter = TriggeredJobFilter.builder()
                .statuses(List.of(JobStatus.SUCCESS))
                .build();
        assertThat(triggeredJobDatabaseService.getTriggeredJob(triggerId, filter)).isNotPresent();

        // 1) insert successful full job
        triggeredJobDatabaseService.createTriggeredJob(jobId1, triggerId, iterationId1, 1001L, false);
        insertJob(jobId1, JobStatus.SUCCESS);

        filter = TriggeredJobFilter.builder()
                .statuses(List.of(JobStatus.SUCCESS))
                .partial(false)
                .build();
        assertThat(triggeredJobDatabaseService.getTriggeredJob(triggerId, filter).map(DbTriggeredJob::getJobId).orElse(null)).isEqualTo(jobId1);
        filter = TriggeredJobFilter.builder()
                .statuses(List.of(JobStatus.SUCCESS))
                .build();
        assertThat(triggeredJobDatabaseService.getTriggeredJob(triggerId, filter).map(DbTriggeredJob::getJobId).orElse(null)).isEqualTo(jobId1);
        filter = TriggeredJobFilter.builder()
                .statuses(List.of(JobStatus.SUCCESS))
                .partial(true)
                .build();
        assertThat(triggeredJobDatabaseService.getTriggeredJob(triggerId, filter).map(DbTriggeredJob::getJobId)).isNotPresent();

        // 2) insert failed -> should still return job 1
        triggeredJobDatabaseService.createTriggeredJob(jobId2, triggerId, iterationId1, 1002L, false);
        insertJob(jobId2, JobStatus.FAILURE);

        filter = TriggeredJobFilter.builder()
                .statuses(List.of(JobStatus.SUCCESS))
                .partial(false)
                .build();
        assertThat(triggeredJobDatabaseService.getTriggeredJob(triggerId, filter).map(DbTriggeredJob::getJobId).orElse(null)).isEqualTo(jobId1);

        // 3) insert successful but partial -> should still return job1
        triggeredJobDatabaseService.createTriggeredJob(jobId3, triggerId, iterationId1, 1003L, true);
        insertJob(jobId3, JobStatus.SUCCESS);

        filter = TriggeredJobFilter.builder()
                .statuses(List.of(JobStatus.SUCCESS))
                .partial(false)
                .build();
        assertThat(triggeredJobDatabaseService.getTriggeredJob(triggerId, filter).map(DbTriggeredJob::getJobId).orElse(null)).isEqualTo(jobId1);

        // job 3 returned if ignoring partial field
        filter = TriggeredJobFilter.builder()
                .statuses(List.of(JobStatus.SUCCESS))
                .partial(true)
                .build();
        assertThat(triggeredJobDatabaseService.getTriggeredJob(triggerId, filter).map(DbTriggeredJob::getJobId).orElse(null)).isEqualTo(jobId3);
        filter = TriggeredJobFilter.builder()
                .statuses(List.of(JobStatus.SUCCESS))
                .build();
        assertThat(triggeredJobDatabaseService.getTriggeredJob(triggerId, filter).map(DbTriggeredJob::getJobId).orElse(null)).isEqualTo(jobId3);

        // 4) insert successful full job -> should be returned
        triggeredJobDatabaseService.createTriggeredJob(jobId4, triggerId, iterationId1, 1004L, false);
        insertJob(jobId4, JobStatus.SUCCESS);

        filter = TriggeredJobFilter.builder()
                .statuses(List.of(JobStatus.SUCCESS))
                .partial(false)
                .build();
        assertThat(triggeredJobDatabaseService.getTriggeredJob(triggerId, filter).map(DbTriggeredJob::getJobId).orElse(null)).isEqualTo(jobId4);

        // 5) insert another successful partial job to test ts
        triggeredJobDatabaseService.createTriggeredJob(jobId5, triggerId, iterationId1, 1005L, true);
        insertJob(jobId5, JobStatus.SUCCESS);

        // test iteration ts

        filter = TriggeredJobFilter.builder()
                .statuses(List.of(JobStatus.SUCCESS))
                .partial(false)
                .beforeInclusive(2000L)
                .build();
        triggeredJobDatabaseService.getTriggeredJob(triggerId, filter);
        filter = TriggeredJobFilter.builder()
                .statuses(List.of(JobStatus.SUCCESS))
                .partial(false)
                .beforeInclusive(2000L)
                .build();
        assertThat(triggeredJobDatabaseService.getTriggeredJob(triggerId, filter).map(DbTriggeredJob::getJobId).orElse(null)).isEqualTo(jobId4);
        filter = TriggeredJobFilter.builder()
                .statuses(List.of(JobStatus.SUCCESS))
                .partial(false)
                .beforeInclusive(1100L)
                .build();
        assertThat(triggeredJobDatabaseService.getTriggeredJob(triggerId, filter).map(DbTriggeredJob::getJobId).orElse(null)).isEqualTo(jobId4);
        filter = TriggeredJobFilter.builder()
                .statuses(List.of(JobStatus.SUCCESS))
                .partial(false)
                .beforeInclusive(0L)
                .build();
        assertThat(triggeredJobDatabaseService.getTriggeredJob(triggerId, filter)).isNotPresent();
        filter = TriggeredJobFilter.builder()
                .statuses(List.of(JobStatus.SUCCESS))
                .partial(true)
                .beforeInclusive(2000L)
                .build();
        assertThat(triggeredJobDatabaseService.getTriggeredJob(triggerId, filter).map(DbTriggeredJob::getJobId).orElse(null)).isEqualTo(jobId5);
        filter = TriggeredJobFilter.builder()
                .statuses(List.of(JobStatus.SUCCESS))
                .partial(true)
                .beforeInclusive(1100L)
                .build();
        assertThat(triggeredJobDatabaseService.getTriggeredJob(triggerId, filter).map(DbTriggeredJob::getJobId).orElse(null)).isEqualTo(jobId5);
        filter = TriggeredJobFilter.builder()
                .statuses(List.of(JobStatus.SUCCESS))
                .partial(true)
                .beforeInclusive(0L)
                .build();
        assertThat(triggeredJobDatabaseService.getTriggeredJob(triggerId, filter)).isNotPresent();

        filter = TriggeredJobFilter.builder()
                .partial(true)
                .afterExclusive(0L)
                .build();
        assertThat(triggeredJobDatabaseService.streamTriggeredJobs(triggerId, filter, null).map(DbTriggeredJob::getJobId)).containsExactly(jobId5, jobId3);
        filter = TriggeredJobFilter.builder()
                .partial(true)
                .afterExclusive(1005L)
                .build();
        assertThat(triggeredJobDatabaseService.streamTriggeredJobs(triggerId, filter, null).map(DbTriggeredJob::getJobId)).isEmpty();

        filter = TriggeredJobFilter.builder()
                .partial(true)
                .afterExclusive(0L)
                .beforeInclusive(2000L)
                .build();
        assertThat(triggeredJobDatabaseService.streamTriggeredJobs(triggerId, filter, null).map(DbTriggeredJob::getJobId)).containsExactly(jobId5, jobId3);
        filter = TriggeredJobFilter.builder()
                .partial(true)
                .afterExclusive(0L)
                .beforeInclusive(1005L)
                .build();
        assertThat(triggeredJobDatabaseService.streamTriggeredJobs(triggerId, filter, null).map(DbTriggeredJob::getJobId)).containsExactly(jobId5, jobId3);
        filter = TriggeredJobFilter.builder()
                .partial(true)
                .afterExclusive(1005L)
                .beforeInclusive(2000L)
                .build();
        assertThat(triggeredJobDatabaseService.streamTriggeredJobs(triggerId, filter, null).map(DbTriggeredJob::getJobId)).isEmpty();

    }

    @Test
    public void testFilter() {
        String jobId1 = "10000000-1fdd-439a-bd99-aa515bda1bf9";
        String jobId2 = "20000000-1fdd-439a-bd99-aa515bda1bf9";
        String jobId3 = "30000000-1fdd-439a-bd99-aa515bda1bf9";
        String triggerId = "f0240868-b1ff-4ce1-b00a-a2a48cb9d51a";
        String iterationId1 = "10000000-252b-41a9-925a-3faf8da4d7cb";

        triggeredJobDatabaseService.createTriggeredJob(jobId1, triggerId, iterationId1, 10L, false);
        insertJob(jobId1, JobStatus.SUCCESS);
        triggeredJobDatabaseService.createTriggeredJob(jobId2, triggerId, iterationId1, 20L, true);
        insertJob(jobId2, JobStatus.FAILURE);
        triggeredJobDatabaseService.createTriggeredJob(jobId3, triggerId, iterationId1, 30L, true);
        insertJob(jobId3, JobStatus.SUCCESS);

        // pagination
        assertThat(triggeredJobDatabaseService.filterTriggeredJobs(0, 10, triggerId, null, null).getRecords().stream().map(DbTriggeredJob::getJobId)).containsExactly(jobId3, jobId2, jobId1);
        assertThat(triggeredJobDatabaseService.filterTriggeredJobs(0, 2, triggerId, null, null).getRecords().stream().map(DbTriggeredJob::getJobId)).containsExactly(jobId3, jobId2);
        assertThat(triggeredJobDatabaseService.filterTriggeredJobs(2, 2, triggerId, null, null).getRecords().stream().map(DbTriggeredJob::getJobId)).containsExactly(jobId1);
        assertThat(triggeredJobDatabaseService.streamTriggeredJobs(triggerId, null, null).map(DbTriggeredJob::getJobId)).containsExactly(jobId3, jobId2, jobId1);

        // status
        assertThat(triggeredJobDatabaseService.filterTriggeredJobs(0, 10, triggerId, TriggeredJobFilter.builder().statuses(List.of()).build(), null).getRecords().stream().map(DbTriggeredJob::getJobId)).containsExactly(jobId3, jobId2, jobId1);
        assertThat(triggeredJobDatabaseService.filterTriggeredJobs(0, 10, triggerId, TriggeredJobFilter.builder().statuses(List.of(JobStatus.SUCCESS)).build(), null).getRecords().stream().map(DbTriggeredJob::getJobId)).containsExactly(jobId3, jobId1);
        assertThat(triggeredJobDatabaseService.filterTriggeredJobs(0, 10, triggerId, TriggeredJobFilter.builder().statuses(List.of(JobStatus.FAILURE, JobStatus.SCHEDULED)).build(), null).getRecords().stream().map(DbTriggeredJob::getJobId)).containsExactly(jobId2);
        assertThat(triggeredJobDatabaseService.filterTriggeredJobs(0, 10, triggerId, TriggeredJobFilter.builder().statuses(List.of(JobStatus.SUCCESS, JobStatus.FAILURE)).build(), null).getRecords().stream().map(DbTriggeredJob::getJobId)).containsExactly(jobId3, jobId2, jobId1);

        // partial
        assertThat(triggeredJobDatabaseService.filterTriggeredJobs(0, 10, triggerId, TriggeredJobFilter.builder().partial(true).build(), null).getRecords().stream().map(DbTriggeredJob::getJobId)).containsExactly(jobId3, jobId2);
        assertThat(triggeredJobDatabaseService.filterTriggeredJobs(0, 10, triggerId, TriggeredJobFilter.builder().partial(false).build(), null).getRecords().stream().map(DbTriggeredJob::getJobId)).containsExactly(jobId1);

        // afterExclusive
        assertThat(triggeredJobDatabaseService.filterTriggeredJobs(0, 10, triggerId, TriggeredJobFilter.builder().afterExclusive(0L).build(), null).getRecords().stream().map(DbTriggeredJob::getJobId)).containsExactly(jobId3, jobId2, jobId1);
        assertThat(triggeredJobDatabaseService.filterTriggeredJobs(0, 10, triggerId, TriggeredJobFilter.builder().afterExclusive(10L).build(), null).getRecords().stream().map(DbTriggeredJob::getJobId)).containsExactly(jobId3, jobId2);
        assertThat(triggeredJobDatabaseService.filterTriggeredJobs(0, 10, triggerId, TriggeredJobFilter.builder().afterExclusive(20L).build(), null).getRecords().stream().map(DbTriggeredJob::getJobId)).containsExactly(jobId3);
        assertThat(triggeredJobDatabaseService.filterTriggeredJobs(0, 10, triggerId, TriggeredJobFilter.builder().afterExclusive(30L).build(), null).getRecords().stream().map(DbTriggeredJob::getJobId)).isEmpty();

        // beforeInclusive
        assertThat(triggeredJobDatabaseService.filterTriggeredJobs(0, 10, triggerId, TriggeredJobFilter.builder().beforeInclusive(30L).build(), null).getRecords().stream().map(DbTriggeredJob::getJobId)).containsExactly(jobId3, jobId2, jobId1);
        assertThat(triggeredJobDatabaseService.filterTriggeredJobs(0, 10, triggerId, TriggeredJobFilter.builder().beforeInclusive(20L).build(), null).getRecords().stream().map(DbTriggeredJob::getJobId)).containsExactly(jobId2, jobId1);
        assertThat(triggeredJobDatabaseService.filterTriggeredJobs(0, 10, triggerId, TriggeredJobFilter.builder().beforeInclusive(10L).build(), null).getRecords().stream().map(DbTriggeredJob::getJobId)).containsExactly(jobId1);
        assertThat(triggeredJobDatabaseService.filterTriggeredJobs(0, 10, triggerId, TriggeredJobFilter.builder().beforeInclusive(0L).build(), null).getRecords().stream().map(DbTriggeredJob::getJobId)).isEmpty();

        // before and after
        assertThat(triggeredJobDatabaseService.filterTriggeredJobs(0, 10, triggerId, TriggeredJobFilter.builder().afterExclusive(0L).beforeInclusive(30L).build(), null).getRecords().stream().map(DbTriggeredJob::getJobId)).containsExactly(jobId3, jobId2, jobId1);
        assertThat(triggeredJobDatabaseService.filterTriggeredJobs(0, 10, triggerId, TriggeredJobFilter.builder().afterExclusive(10L).beforeInclusive(20L).build(), null).getRecords().stream().map(DbTriggeredJob::getJobId)).containsExactly(jobId2);
        assertThat(triggeredJobDatabaseService.filterTriggeredJobs(0, 10, triggerId, TriggeredJobFilter.builder().afterExclusive(10L).beforeInclusive(10L).build(), null).getRecords().stream().map(DbTriggeredJob::getJobId)).isEmpty();

        // combination
        assertThat(triggeredJobDatabaseService.filterTriggeredJobs(0, 10, triggerId, TriggeredJobFilter.builder().statuses(List.of(JobStatus.SUCCESS)).partial(false).afterExclusive(0L).beforeInclusive(10L).build(), null).getRecords().stream().map(DbTriggeredJob::getJobId)).containsExactly(jobId1);
        assertThat(triggeredJobDatabaseService.filterTriggeredJobs(0, 10, triggerId, TriggeredJobFilter.builder().statuses(List.of(JobStatus.FAILURE)).partial(true).afterExclusive(10L).beforeInclusive(20L).build(), null).getRecords().stream().map(DbTriggeredJob::getJobId)).containsExactly(jobId2);

        // max attempt
        assertThat(triggeredJobDatabaseService.filterTriggeredJobs(0, 2, triggerId, TriggeredJobFilter.builder().belowMaxAttemptsOrDefaultValue(10).build(), null).getRecords().stream().map(DbTriggeredJob::getJobId)).containsExactly(jobId3, jobId2);
    }
}