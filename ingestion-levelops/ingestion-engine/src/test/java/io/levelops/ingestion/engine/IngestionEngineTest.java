package io.levelops.ingestion.engine;

import io.levelops.ingestion.controllers.ControllerIngestionResult;
import io.levelops.ingestion.controllers.DataController;
import io.levelops.ingestion.controllers.IntermediateStateUpdater;
import io.levelops.ingestion.controllers.generic.ControllerIngestionResultList;
import io.levelops.ingestion.exceptions.IngestException;
import io.levelops.ingestion.models.DataQuery;
import io.levelops.ingestion.models.JobContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

public class IngestionEngineTest {
    @Mock
    CallbackService callbackService;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    public static class TestController implements DataController<TestController.Query> {

        @Override
        public ControllerIngestionResult ingest(JobContext jobContext, Query query) throws IngestException {
            return null;
        }

        @Override
        public ControllerIngestionResult ingest(JobContext jobContext, Query query, IntermediateStateUpdater intermediateStateUpdater) throws IngestException {
            for (int i = 0; i < 5; i++) {
                var currState = intermediateStateUpdater.getIntermediateState();
                if (currState == null) {
                    currState = new HashMap<>();
                }
                currState.put(String.valueOf(i), String.valueOf(i));
                intermediateStateUpdater.updateIntermediateState(currState);
                try {
                    Thread.sleep(1000) ;
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            return new ControllerIngestionResultList("", List.of());
        }

        @Override
        public Query parseQuery(Object o) {
            return null;
        }

        public static class Query implements DataQuery {
        }
    }

    @Test
    public void testIntermediateStateUpdater() throws InterruptedException, ExecutionException {
        // Use a dummy controller that uses the updater to update the state of the ingestion job.
        // While it's running periodically query the etl engine to ensure that the updates are being registered
        // with the engine
        IngestionEngine engine = new IngestionEngine(2, callbackService);
        var testController = new TestController();
        JobContext jobContext = JobContext.builder()
                .tenantId("test-tenant")
                .integrationId("1")
                .jobId("test-job-id")
                .attemptCount(1)
                .build();
        var engineJob = engine.submitJob(testController, new TestController.Query(), "test-agent", jobContext, null, null).get();
        Thread.sleep(1000);
        var jobs = engine.getJobs();
        assertThat(jobs).hasSize(1);
        var intermediateState1 = jobs.stream().findFirst().get().getIntermediateState();
        assertThat(intermediateState1).isNotEmpty();
        engineJob.getFuture().get();
        var intermediateState2 = engine.getJobs().stream().findFirst().get().getIntermediateState();
        assertThat(intermediateState2).hasSize(5);
    }

}