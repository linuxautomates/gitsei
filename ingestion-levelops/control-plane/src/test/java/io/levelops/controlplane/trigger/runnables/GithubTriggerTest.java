package io.levelops.controlplane.trigger.runnables;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.controlplane.database.DefaultTriggerDatabaseService;
import io.levelops.controlplane.database.DefaultTriggeredJobDatabaseService;
import io.levelops.controlplane.database.JobDatabaseService;
import io.levelops.controlplane.database.TriggerDatabaseService;
import io.levelops.controlplane.discovery.AgentRegistryService;
import io.levelops.controlplane.models.DbTrigger;
import io.levelops.controlplane.services.DefaultTriggeredJobService;
import io.levelops.controlplane.services.JobTrackingService;
import io.levelops.controlplane.services.TriggeredJobService;
import io.levelops.ingestion.integrations.github.models.GithubIterativeScanQuery;
import io.levelops.ingestion.merging.IngestionResultMergingService;
import io.levelops.ingestion.models.CreateJobRequest;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class GithubTriggerTest {
    @Rule
    public SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    private TriggerDatabaseService triggerDatabaseService;
    private JobTrackingService jobTrackingService;
    private JobDatabaseService jobDatabaseService;

    private ObjectMapper mapper;

    @Mock
    private AgentRegistryService agentRegistryService;
    @Mock
    private TriggeredJobService mockTriggeredJobService;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        DataSource dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        NamedParameterJdbcTemplate template = new NamedParameterJdbcTemplate(dataSource);
        mapper = DefaultObjectMapper.get();
        jobDatabaseService = new JobDatabaseService(template, mapper);
        var ingestionMergingService = new IngestionResultMergingService();
        jobTrackingService = new JobTrackingService(mapper, jobDatabaseService, agentRegistryService, ingestionMergingService);
        var triggeredJobDatabaseService = new DefaultTriggeredJobDatabaseService(template, mapper);

        TriggeredJobService triggeredJobService = new DefaultTriggeredJobService(jobTrackingService, triggeredJobDatabaseService);
        triggerDatabaseService = new DefaultTriggerDatabaseService(template, mapper);

        List.of(
                "CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";",
                "DROP SCHEMA IF EXISTS control_plane CASCADE; ",
                "CREATE SCHEMA control_plane; "
        ).forEach(template.getJdbcTemplate()::execute);

        triggerDatabaseService.ensureTableExistence();
        triggeredJobDatabaseService.ensureTableExistence();
        jobDatabaseService.ensureTableExistence();
    }

    @Test
    public void test() throws Exception {
        long DEFAULT_FULL_SCAN_FREQ_IN_MIN = TimeUnit.DAYS.toMinutes(14);
        long DEFAULT_ONBOARDING_SPAN_IN_DAYS = 14;
        long DEFAULT_BACKWARD_SCAN_SUB_JOB_SPAN_IN_MIN = TimeUnit.DAYS.toMinutes(7);
        long REPO_SCAN_FREQ_IN_MIN = TimeUnit.DAYS.toMinutes(1);
        long HISTORICAL_SPAN_IN_DAYS = 90;
        long HISTORICAL_SUB_JOB_SPAN_IN_MIN = TimeUnit.DAYS.toMinutes(60); // Make this greater than the historical span so we're done in 2 iteration
        int HISTORICAL_SUCCESSIVE_BACKWARD_SCAN_COUNT = 2;
        GithubTrigger trigger = new GithubTrigger(
                mapper,
                mockTriggeredJobService,
                triggerDatabaseService,
                DEFAULT_FULL_SCAN_FREQ_IN_MIN,
                DEFAULT_FULL_SCAN_FREQ_IN_MIN,
                DEFAULT_ONBOARDING_SPAN_IN_DAYS,
                DEFAULT_BACKWARD_SCAN_SUB_JOB_SPAN_IN_MIN,
                REPO_SCAN_FREQ_IN_MIN,
                HISTORICAL_SPAN_IN_DAYS,
                HISTORICAL_SUB_JOB_SPAN_IN_MIN,
                HISTORICAL_SUCCESSIVE_BACKWARD_SCAN_COUNT
        );

        UUID triggerId = UUID.randomUUID();
        triggerDatabaseService.createTrigger(triggerId, "test", "1", false, "test_type", 10, "{}", null, null);

        UUID iterationId = UUID.randomUUID();
        triggerDatabaseService.updateTriggerWithIteration(triggerId.toString(), iterationId.toString(), Instant.now());

        Date now = new Date();

        // Regular backward scan
        DbTrigger dbTrigger = triggerDatabaseService.getTriggerById(triggerId.toString()).get();
        trigger.run(dbTrigger);
        ArgumentCaptor<CreateJobRequest> argument = ArgumentCaptor.forClass(CreateJobRequest.class);
        verify(mockTriggeredJobService).createTriggeredJob(any(), eq(false), argument.capture());
        GithubIterativeScanQuery query = (GithubIterativeScanQuery) argument.getValue().getQuery();
        assertIsCloseTo(query.getFrom().toInstant(), now.toInstant().minus(Duration.ofDays(DEFAULT_ONBOARDING_SPAN_IN_DAYS)));
        assertIsCloseTo(query.getTo().toInstant(), now.toInstant().minus(Duration.ofDays(DEFAULT_ONBOARDING_SPAN_IN_DAYS-7)));

        // Regular forward scan
        dbTrigger = triggerDatabaseService.getTriggerById(triggerId.toString()).get();
        trigger.run(dbTrigger);
        verify(mockTriggeredJobService, times(1)).createTriggeredJob(any(), eq(true), argument.capture());
        query = (GithubIterativeScanQuery) argument.getValue().getQuery();
        assertIsCloseTo(query.getFrom().toInstant(), now.toInstant());
        assertIsCloseTo(query.getTo().toInstant(), now.toInstant());

        // Continue with backward scan
        dbTrigger = triggerDatabaseService.getTriggerById(triggerId.toString()).get();
        trigger.run(dbTrigger);
        verify(mockTriggeredJobService, times(2)).createTriggeredJob(any(), eq(true), argument.capture());
        query = (GithubIterativeScanQuery) argument.getValue().getQuery();
        assertIsCloseTo(query.getFrom().toInstant(), now.toInstant().minus(Duration.ofDays(DEFAULT_ONBOARDING_SPAN_IN_DAYS-7)));
        assertIsCloseTo(query.getTo().toInstant(), now.toInstant());

        // Set fetch history to true. Let the fun begin!
        // Should be done in 2 succesive backward iterations
        dbTrigger = triggerDatabaseService.getTriggerById(triggerId.toString()).get();
        HashMap<String, Object> map = (HashMap<String, Object>) dbTrigger.getMetadata();
        map.put("should_fetch_history", "true");
        map.put("should_start_fetching_history", "true");
        triggerDatabaseService.updateTriggerMetadata(triggerId.toString(), map);
        trigger.run(dbTrigger);
        verify(mockTriggeredJobService, times(2)).createTriggeredJob(any(), eq(false), argument.capture());
        query = (GithubIterativeScanQuery) argument.getValue().getQuery();
        assertIsCloseTo(query.getFrom().toInstant(), now.toInstant().minus(Duration.ofDays(HISTORICAL_SPAN_IN_DAYS)));
        assertIsCloseTo(query.getTo().toInstant(), now.toInstant().minus(Duration.ofDays(HISTORICAL_SPAN_IN_DAYS-60)));

        // 2nd successive backward scan of the historical strategy
        dbTrigger = triggerDatabaseService.getTriggerById(triggerId.toString()).get();
        map = (HashMap<String, Object>) dbTrigger.getMetadata();
        assertThat(map.get("should_start_fetching_history")).isEqualTo(false);
        assertThat(map.get("should_fetch_history")).isEqualTo(true);
        trigger.run(dbTrigger);
        verify(mockTriggeredJobService, times(3)).createTriggeredJob(any(), eq(true), argument.capture());
        query = (GithubIterativeScanQuery) argument.getValue().getQuery();
        assertIsCloseTo(query.getFrom().toInstant(), now.toInstant().minus(Duration.ofDays(HISTORICAL_SPAN_IN_DAYS-60)));
        assertIsCloseTo(query.getTo().toInstant(), now.toInstant());

        // Should go back to the normal strategy and continue with the forward scan
        dbTrigger = triggerDatabaseService.getTriggerById(triggerId.toString()).get();
        map = (HashMap<String, Object>) dbTrigger.getMetadata();
        assertThat(map.get("should_start_fetching_history")).isEqualTo(false);
        assertThat(map.get("should_fetch_history")).isEqualTo(false);
        trigger.run(dbTrigger);
        verify(mockTriggeredJobService, times(4)).createTriggeredJob(any(), eq(true), argument.capture());
        query = (GithubIterativeScanQuery) argument.getValue().getQuery();
        assertIsCloseTo(query.getFrom().toInstant(), now.toInstant());
        assertIsCloseTo(query.getTo().toInstant(), now.toInstant());
    }

    // Checks if the 2 instants are within 2 minutes of each other
    private void assertIsCloseTo(Instant d1, Instant d2) {
        var duration = Duration.between(d1, d2);
        assertThat(duration.abs().toMinutes()).isLessThanOrEqualTo(2);
    }
}